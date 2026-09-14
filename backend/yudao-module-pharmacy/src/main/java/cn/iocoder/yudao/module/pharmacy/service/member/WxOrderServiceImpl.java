package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacade;
import cn.iocoder.yudao.module.pharmacy.api.payment.dto.PayOrderDTO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderMapper;
import cn.iocoder.yudao.module.pharmacy.enums.WxOrderStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 小程序订单 Service 实现类
 */
@Service
@Validated
public class WxOrderServiceImpl implements WxOrderService {

    /** 支付状态：已支付 */
    private static final Integer PAY_STATUS_PAID = 1;

    @Resource
    private WxOrderMapper wxOrderMapper;

    /** C 的库存门面：选批/扣减/回补（C 未实现时抛 UnsupportedOperationException） */
    @Resource
    private InventoryFacade inventoryFacade;

    /** E 的支付门面：创建支付单/查询状态（E 未实现时抛 UnsupportedOperationException） */
    @Resource
    private PaymentFacade paymentFacade;

    /** 订单明细服务：查询订单下的药品与数量，用于构建扣减/回补项 */
    @Resource
    private WxOrderLineService wxOrderLineService;

    @Override
    public Long createWxOrder(WxOrderSaveReqVO createReqVO) {
        // 校验订单号唯一
        validateOrderNoUnique(null, createReqVO.getOrderNo());
        // 校验状态合法
        validateStatus(createReqVO.getStatus());
        // 写入
        WxOrderDO wxOrder = BeanUtils.toBean(createReqVO, WxOrderDO.class);
        wxOrderMapper.insert(wxOrder);
        // 对接 E：待支付订单创建渠道支付单（金额元→分，业务单号=订单号）。
        // E 支付服务未实现时降级为不创建支付单，不阻塞下单流程。
        if (Objects.equals(wxOrder.getPayStatus(), 0)) {
            String payNo = createChannelPayOrder(wxOrder);
            if (payNo != null) {
                wxOrder.setPayNo(payNo);
                wxOrderMapper.updateById(wxOrder);
            }
        }
        return wxOrder.getId();
    }

    @Override
    public void updateWxOrder(WxOrderSaveReqVO updateReqVO) {
        // 校验存在
        validateWxOrderExists(updateReqVO.getId());
        // 校验订单号唯一
        validateOrderNoUnique(updateReqVO.getId(), updateReqVO.getOrderNo());
        // 校验状态合法
        validateStatus(updateReqVO.getStatus());
        // 更新
        WxOrderDO updateObj = BeanUtils.toBean(updateReqVO, WxOrderDO.class);
        wxOrderMapper.updateById(updateObj);
    }

    @Override
    public void deleteWxOrder(Long id) {
        // 校验存在
        validateWxOrderExists(id);
        // 删除
        wxOrderMapper.deleteById(id);
    }

    @Override
    public WxOrderDO getWxOrder(Long id) {
        return wxOrderMapper.selectById(id);
    }

    @Override
    public List<WxOrderDO> getWxOrderList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return wxOrderMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<WxOrderDO> getWxOrderPage(WxOrderPageReqVO reqVO) {
        return wxOrderMapper.selectPage(reqVO);
    }

    @Override
    public WxOrderDO getWxOrderByOrderNo(String orderNo) {
        return wxOrderMapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<WxOrderDO> getWxOrderListByMemberId(Long memberId) {
        return wxOrderMapper.selectListByMemberId(memberId);
    }

    @Override
    public WxOrderDO getWxOrderByPickupCode(String pickupCode) {
        return wxOrderMapper.selectByPickupCode(pickupCode);
    }

    @Override
    public WxOrderDO validateWxOrderExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_WX_ORDER_NOT_EXISTS);
        }
        WxOrderDO wxOrder = wxOrderMapper.selectById(id);
        if (wxOrder == null) {
            throw exception(PHARMACY_WX_ORDER_NOT_EXISTS);
        }
        return wxOrder;
    }

    // ========== 状态流转与核销 ==========

    @Override
    public void payWxOrder(Long id, String payNo) {
        WxOrderDO wxOrder = validateWxOrderExists(id);
        // 幂等：已支付、已完成、已取消均不重复处理
        if (Objects.equals(wxOrder.getPayStatus(), PAY_STATUS_PAID)
                || WxOrderStatusEnum.isFinished(wxOrder.getStatus())) {
            return;
        }
        // 仅待支付状态允许支付
        if (!Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.WAIT_PAY.getStatus())) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        // 对接 C：支付成功即扣减库存（锁定），核销时不重复扣库。
        // 选批由 C 内部按效期最早优先完成，DeductItem 仅传药品与数量。
        deductStock(wxOrder);
        // 条件更新，防止并发重复支付
        WxOrderDO updateObj = new WxOrderDO();
        updateObj.setPayStatus(PAY_STATUS_PAID);
        updateObj.setPaidAt(LocalDateTime.now());
        updateObj.setPayNo(payNo);
        updateObj.setStatus(WxOrderStatusEnum.WAIT_PICK.getStatus());
        int rows = wxOrderMapper.update(updateObj, new LambdaUpdateWrapper<WxOrderDO>()
                .eq(WxOrderDO::getId, id)
                .eq(WxOrderDO::getStatus, WxOrderStatusEnum.WAIT_PAY.getStatus()));
        if (rows == 0) {
            // 并发下已被其他请求支付，视为幂等成功
            return;
        }
    }

    @Override
    public void cancelWxOrder(Long id, String cancelReason) {
        WxOrderDO wxOrder = validateWxOrderExists(id);
        // 幂等：已取消直接返回
        if (Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.CANCELED.getStatus())) {
            return;
        }
        // 已完成订单不允许取消
        if (Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.COMPLETED.getStatus())) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        // 仅待支付、待拣货状态允许取消
        if (!Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.WAIT_PAY.getStatus())
                && !Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.WAIT_PICK.getStatus())) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        // 条件更新，防止并发重复取消
        WxOrderDO updateObj = new WxOrderDO();
        updateObj.setStatus(WxOrderStatusEnum.CANCELED.getStatus());
        updateObj.setCancelReason(cancelReason);
        int rows = wxOrderMapper.update(updateObj, new LambdaUpdateWrapper<WxOrderDO>()
                .eq(WxOrderDO::getId, id)
                .in(WxOrderDO::getStatus, WxOrderStatusEnum.WAIT_PAY.getStatus(), WxOrderStatusEnum.WAIT_PICK.getStatus()));
        if (rows == 0) {
            // 并发下已被其他请求取消，视为幂等成功
            return;
        }
        // 已支付（待拣货）的订单此前已扣库存，取消需回补库存（调用 C 的库存服务）。
        // 通过状态幂等保证不重复释放：只有本次成功从「待拣货」翻转为「取消」时才释放。
        if (Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.WAIT_PICK.getStatus())) {
            returnBackStock(wxOrder);
        }
    }

    @Override
    public void startPicking(Long id) {
        WxOrderDO wxOrder = validateWxOrderExists(id);
        // 仅待拣货状态允许开始拣货
        if (!Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.WAIT_PICK.getStatus())) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        WxOrderDO updateObj = new WxOrderDO();
        updateObj.setStatus(WxOrderStatusEnum.PICKING.getStatus());
        wxOrderMapper.update(updateObj, new LambdaUpdateWrapper<WxOrderDO>()
                .eq(WxOrderDO::getId, id)
                .eq(WxOrderDO::getStatus, WxOrderStatusEnum.WAIT_PICK.getStatus()));
    }

    @Override
    public void finishPicking(Long id) {
        WxOrderDO wxOrder = validateWxOrderExists(id);
        // 仅拣货中状态允许拣货完成
        if (!Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.PICKING.getStatus())) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        WxOrderDO updateObj = new WxOrderDO();
        updateObj.setStatus(WxOrderStatusEnum.WAIT_VERIFY.getStatus());
        wxOrderMapper.update(updateObj, new LambdaUpdateWrapper<WxOrderDO>()
                .eq(WxOrderDO::getId, id)
                .eq(WxOrderDO::getStatus, WxOrderStatusEnum.PICKING.getStatus()));
    }

    @Override
    public void verifyWxOrder(Long id, String pickupCode, Long verifyBy) {
        WxOrderDO wxOrder = validateWxOrderExists(id);
        // 幂等：已核销（完成）直接返回
        if (Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.COMPLETED.getStatus())) {
            return;
        }
        // 仅待自提状态允许核销
        if (!Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.WAIT_VERIFY.getStatus())) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        // 校验取货码
        if (pickupCode == null || wxOrder.getPickupCode() == null
                || !pickupCode.trim().equalsIgnoreCase(wxOrder.getPickupCode().trim())) {
            throw exception(PHARMACY_WX_ORDER_PICKUP_CODE_ERROR);
        }
        // 条件更新，防止并发重复核销
        WxOrderDO updateObj = new WxOrderDO();
        updateObj.setStatus(WxOrderStatusEnum.COMPLETED.getStatus());
        updateObj.setVerifyBy(verifyBy);
        updateObj.setVerifyAt(LocalDateTime.now());
        updateObj.setFinishAt(LocalDateTime.now());
        int rows = wxOrderMapper.update(updateObj, new LambdaUpdateWrapper<WxOrderDO>()
                .eq(WxOrderDO::getId, id)
                .eq(WxOrderDO::getStatus, WxOrderStatusEnum.WAIT_VERIFY.getStatus()));
        if (rows == 0) {
            // 并发下已被其他请求核销，视为幂等成功
            return;
        }
        // 对接 D：核销即自提完成，触发线上订单转销售。
        // 扣库已在支付时由 C 完成，转销售不重复扣库（约定：不能在支付与核销各扣一次）。
        transferToSale(wxOrder);
    }

    @Override
    public WxOrderDO validateWxOrderOwner(Long memberId, Long orderId) {
        WxOrderDO wxOrder = validateWxOrderExists(orderId);
        if (memberId == null || !Objects.equals(wxOrder.getMemberId(), memberId)) {
            throw exception(PHARMACY_WX_ORDER_NOT_OWNER);
        }
        return wxOrder;
    }

    // ========== 私有方法 ==========

    private void validateStatus(Integer status) {
        if (!WxOrderStatusEnum.isValid(status)) {
            throw exception(PHARMACY_WX_ORDER_STATUS_INVALID);
        }
    }

    private void validateOrderNoUnique(Long id, String orderNo) {
        WxOrderDO wxOrder = wxOrderMapper.selectByOrderNo(orderNo);
        if (wxOrder == null) {
            return;
        }
        if (id == null || !Objects.equals(wxOrder.getId(), id)) {
            throw exception(PHARMACY_WX_ORDER_NO_DUPLICATE);
        }
    }

    /**
     * 对接 C：支付成功时扣减库存。
     *
     * 从订单明细构建扣减项（仅药品+数量，批次由 C 内部按效期最早优先选批）。
     * C 库存服务未实现时抛 {@link UnsupportedOperationException}，转换为
     * {@link ErrorCodeConstants#INV_SERVICE_UNAVAILABLE}，使支付事务整体回滚，不静默放行。
     */
    private void deductStock(WxOrderDO wxOrder) {
        List<WxOrderLineDO> lines = wxOrderLineService.getWxOrderLineListByWxOrderId(wxOrder.getId());
        if (lines == null || lines.isEmpty()) {
            return;
        }
        List<DeductItem> items = new ArrayList<>(lines.size());
        for (WxOrderLineDO line : lines) {
            DeductItem item = new DeductItem();
            item.setDrugId(line.getDrugId());
            item.setQty(line.getQty());
            // batchId / locationId 由 C 选批后回填，扣减契约仅传药品+数量
            items.add(item);
        }
        try {
            inventoryFacade.deduct(wxOrder.getStoreId(), items);
        } catch (UnsupportedOperationException ex) {
            throw exception(INV_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 对接 C：取消已支付订单时回补库存（原批次退回）。
     *
     * 仅当订单明细已回填批次（拣货后）时才可精确回补；否则按契约由 C 处理。
     * 通过取消幂等保证不重复释放库存。
     */
    private void returnBackStock(WxOrderDO wxOrder) {
        List<WxOrderLineDO> lines = wxOrderLineService.getWxOrderLineListByWxOrderId(wxOrder.getId());
        if (lines == null || lines.isEmpty()) {
            return;
        }
        List<ReturnBackItem> items = new ArrayList<>(lines.size());
        for (WxOrderLineDO line : lines) {
            ReturnBackItem item = new ReturnBackItem();
            item.setDrugId(line.getDrugId());
            item.setQty(line.getQty());
            item.setBatchId(line.getBatchId());
            item.setLocationId(line.getLocationId());
            items.add(item);
        }
        try {
            inventoryFacade.returnBack(wxOrder.getStoreId(), items);
        } catch (UnsupportedOperationException ex) {
            throw exception(INV_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 金额转换：元 → 分（药店订单金额为 DECIMAL 元，支付接口为整数分）。
     */
    private Integer yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return 0;
        }
        return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 金额转换：分 → 元。
     */
    private BigDecimal fenToYuan(Integer fen) {
        if (fen == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * 对接 E：创建渠道支付单。
     *
     * 业务单号用订单号（orderNo），金额用应付金额元转分。
     * E 支付服务未实现时降级返回 null（不阻塞下单），支付回调时仍可走 payWxOrder 手工标记。
     */
    private String createChannelPayOrder(WxOrderDO wxOrder) {
        PayOrderDTO req = new PayOrderDTO();
        req.setBizNo(wxOrder.getOrderNo());
        req.setPriceFen(yuanToFen(wxOrder.getPayableAmount()));
        req.setMemberId(wxOrder.getMemberId());
        req.setSubject("药店线上订单");
        try {
            return paymentFacade.createPayOrder(req);
        } catch (UnsupportedOperationException ex) {
            // E 支付服务未就绪，降级：不创建支付单，后续支付回调仍可手工标记已支付
            return null;
        }
    }

    /**
     * 对接 D：线上订单转销售（核销即自提完成时触发）。
     *
     * 约定：扣库已在支付时由 C 完成，转销售不得重复扣库；失败需补偿。
     * D 尚未提供跨模块转销售接口，先预留调用点，待 D 提供后接入。
     */
    private void transferToSale(WxOrderDO wxOrder) {
        // TODO 待 D 提供线上订单转销售接口后接入，例如 saleFacade.createFromWxOrder(orderNo)
    }

}
