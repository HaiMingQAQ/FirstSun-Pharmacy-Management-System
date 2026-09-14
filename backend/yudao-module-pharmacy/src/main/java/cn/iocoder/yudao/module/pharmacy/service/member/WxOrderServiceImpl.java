package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderMapper;
import cn.iocoder.yudao.module.pharmacy.enums.WxOrderStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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

    @Override
    public Long createWxOrder(WxOrderSaveReqVO createReqVO) {
        // 校验订单号唯一
        validateOrderNoUnique(null, createReqVO.getOrderNo());
        // 校验状态合法
        validateStatus(createReqVO.getStatus());
        // 写入
        WxOrderDO wxOrder = BeanUtils.toBean(createReqVO, WxOrderDO.class);
        wxOrderMapper.insert(wxOrder);
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
        // 已支付（待拣货）的订单此前可能已扣库存，取消需回补库存（调用 C 的库存服务）。
        // 通过状态幂等保证不重复释放：只有本次成功从「待拣货」翻转为「取消」时才释放。
        // TODO 待 C 提供库存回补接口后接入，例如 inventoryApi.releaseStock(orderNo)
        if (Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.WAIT_PICK.getStatus())) {
            releaseStockIfNeeded(wxOrder);
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
     * 取消已支付订单时释放库存。
     *
     * 当前 C 的库存回补接口尚未建立，仅预留调用点；通过取消幂等保证不重复释放。
     */
    private void releaseStockIfNeeded(WxOrderDO wxOrder) {
        // TODO 待 C 提供库存服务后接入：inventoryApi.releaseStock(wxOrder.getOrderNo())
    }

}
