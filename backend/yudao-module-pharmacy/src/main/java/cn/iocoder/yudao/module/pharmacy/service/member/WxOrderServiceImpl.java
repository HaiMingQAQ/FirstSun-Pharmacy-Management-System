package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.AvailableQty;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ConsumeItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReleaseItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReserveItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReserveResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;
import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacade;
import cn.iocoder.yudao.module.pharmacy.api.payment.dto.PayOrderDTO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberAddressDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxCartDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineAllocDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderLineAllocMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderMapper;
import cn.iocoder.yudao.module.pharmacy.enums.WxOrderStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 小程序订单 Service 实现类
 */
@Service
@Validated
public class WxOrderServiceImpl implements WxOrderService {

    /** 支付状态：待支付 */
    private static final Integer PAY_STATUS_WAIT = 0;
    /** 支付状态：已支付 */
    private static final Integer PAY_STATUS_PAID = 1;
    /** 支付状态：已退款 */
    private static final Integer PAY_STATUS_REFUNDED = 2;
    /** 订单类型：到店自提 */
    private static final Integer ORDER_TYPE_PICKUP = 0;
    /** 订单类型：同城配送 */
    private static final Integer ORDER_TYPE_DELIVERY = 1;
    /** 未支付订单有效期（分钟） */
    private static final int ORDER_EXPIRE_MINUTES = 30;
    /** 订单号日期格式 */
    private static final DateTimeFormatter ORDER_NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 取消回补业务单号前缀（作为库存回补幂等键的一部分，与订单号拼成 ≤32 位） */
    private static final String CANCEL_BIZ_PREFIX = "WXC-";
    /** 退款回补业务单号前缀 */
    private static final String REFUND_BIZ_PREFIX = "WXR-";
    /** 门店批量清理（关闭超时订单 / 释放冻结）的默认单次处理上限 */
    private static final int STOCK_CLEANUP_LIMIT = 100;
    /** 门店批量清理的单次处理硬上限，防止一次请求拖垮库存作业 */
    private static final int STOCK_CLEANUP_MAX_LIMIT = 200;

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

    /** 订单明细 Mapper：小程序下单时批量写入明细 */
    @Resource
    private WxOrderLineMapper wxOrderLineMapper;

    /** 出库分配 Mapper：记录 C 的 FEFO 实际批次/货位，作为取消、退款回补的依据 */
    @Resource
    private WxOrderLineAllocMapper wxOrderLineAllocMapper;

    /** 购物车服务：小程序下单时读取已勾选商品并清空 */
    @Resource
    private WxCartService wxCartService;

    /** 收货地址服务：同城配送时读取地址快照 */
    @Resource
    private MemberAddressService memberAddressService;

    /** A 的商品查询接口：下单时校验药品并获取价格/名称/规格快照 */
    @Resource
    private DrugApi drugApi;

    /** F 的统一积分结算服务：抵扣预扣 / 赠送 / 取消释放全部走这里，本类不直接写积分流水 */
    @Resource
    private MemberPointSettlementService memberPointSettlementService;

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
    @Transactional(rollbackFor = Exception.class)
    public void payWxOrder(Long id, String payNo) {
        WxOrderDO wxOrder = validateWxOrderExists(id);
        // 幂等：已支付、已退款、已完成、已取消均不重复处理（重复支付回调）
        if (Objects.equals(wxOrder.getPayStatus(), PAY_STATUS_PAID)
                || Objects.equals(wxOrder.getPayStatus(), PAY_STATUS_REFUNDED)
                || WxOrderStatusEnum.isFinished(wxOrder.getStatus())) {
            return;
        }
        // 仅待支付状态允许支付
        if (!Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.WAIT_PAY.getStatus())) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        // 先条件更新抢占状态，再由抢到的请求出库：
        // 并发重复回调 rows=0 直接返回，只有真正把「待支付」翻转为「待拣货」的请求才会扣库，
        // 从而保证「重复回调不重复扣库」。同一事务内扣库失败会整体回滚，不会出现订单已支付但库存未扣。
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
        // 对接 C：已冻结的订单把冻结转正式出库；未冻结的历史订单退化为按 FEFO 直接出库（同事务）
        consumeReservedOrDeductStock(wxOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveWxOrder(Long id) {
        WxOrderDO wxOrder = validateWxOrderExists(id);
        // 仅待支付订单需要冻结库存
        if (!Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.WAIT_PAY.getStatus())) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        // 幂等：已存在分配记录说明本单已冻结，直接返回
        if (!wxOrderLineAllocMapper.selectListByWxOrderId(wxOrder.getId()).isEmpty()) {
            return;
        }
        reserveStock(wxOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        // 条件更新，防止并发重复取消。已支付订单取消等同于退款，同步把支付状态置为已退款。
        boolean paid = Objects.equals(wxOrder.getPayStatus(), PAY_STATUS_PAID);
        WxOrderDO updateObj = new WxOrderDO();
        updateObj.setStatus(WxOrderStatusEnum.CANCELED.getStatus());
        updateObj.setCancelReason(cancelReason);
        if (paid) {
            updateObj.setPayStatus(PAY_STATUS_REFUNDED);
        }
        int rows = wxOrderMapper.update(updateObj, new LambdaUpdateWrapper<WxOrderDO>()
                .eq(WxOrderDO::getId, id)
                .in(WxOrderDO::getStatus, WxOrderStatusEnum.WAIT_PAY.getStatus(), WxOrderStatusEnum.WAIT_PICK.getStatus()));
        if (rows == 0) {
            // 并发下已被其他请求取消，视为幂等成功
            return;
        }
        if (paid) {
            // 对接 E：已支付订单取消需退回款项（E 未就绪时降级不阻塞）
            refundChannelPayOrder(wxOrder);
        }
        // 对接 C：关闭订单时结算库存 —— 仍冻结的释放，已出库的按原批次、原货位回补
        settleStockOnClose(wxOrder, CANCEL_BIZ_PREFIX, false);
        // 对接 F：取消 / 退款即释放本单预扣的抵扣积分（幂等键 = 订单号，重复取消不重复返还）
        memberPointSettlementService.releaseSalePoints(wxOrder.getMemberId(), wxOrder.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWxOrderByMember(Long id, String cancelReason) {
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
        // 小程序端不具备「目标门店在职管理员」身份，C 的库存门禁会拒绝库存作业，
        // 因此会员取消只关闭订单状态，库存由门店节点释放或回补（见 releaseFrozenStockOfClosedOrders / refundWxOrder）。
        // 注意：订单支付状态保持「已支付」，等门店退款节点才置为「已退款」，避免出现未退款却标记退款。
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
        // 对接 F：订单已关闭即释放本单预扣的抵扣积分（幂等键 = 订单号）；
        // 未预扣积分时内部直接跳过，不产生孤立流水。库存仍由门店节点释放 / 回补。
        memberPointSettlementService.releaseSalePoints(wxOrder.getMemberId(), wxOrder.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredWxOrders(Long storeId, Integer limit) {
        LocalDateTime now = LocalDateTime.now();
        List<WxOrderDO> expired = wxOrderMapper.selectExpiredList(storeId, WxOrderStatusEnum.WAIT_PAY.getStatus(),
                now, normalizeCleanupLimit(limit));
        int closed = 0;
        for (WxOrderDO order : expired) {
            WxOrderDO updateObj = new WxOrderDO();
            updateObj.setStatus(WxOrderStatusEnum.CANCELED.getStatus());
            updateObj.setCancelReason("支付超时自动关闭");
            // 条件更新保证并发下只关闭一次
            int updated = wxOrderMapper.update(updateObj, new LambdaUpdateWrapper<WxOrderDO>()
                    .eq(WxOrderDO::getId, order.getId())
                    .eq(WxOrderDO::getStatus, WxOrderStatusEnum.WAIT_PAY.getStatus()));
            if (updated == 0) {
                continue;
            }
            closed += updated;
            // 对接 F：支付超时 = 支付失败，不赠送积分，并释放本单预扣的抵扣积分（幂等）
            memberPointSettlementService.releaseSalePoints(order.getMemberId(), order.getOrderNo());
        }
        return closed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int releaseFrozenStockOfClosedOrders(Long storeId, Integer limit) {
        List<WxOrderDO> closed = wxOrderMapper.selectListByStoreIdAndStatus(storeId,
                WxOrderStatusEnum.CANCELED.getStatus(), normalizeCleanupLimit(limit));
        int released = 0;
        for (WxOrderDO order : closed) {
            // frozenOnly=true：只释放仍冻结的分配，已出库的留给退款 / 退货节点回补
            released += settleStockOnClose(order, CANCEL_BIZ_PREFIX, true);
        }
        return released;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundWxOrder(Long id, String refundReason) {
        WxOrderDO wxOrder = validateWxOrderExists(id);
        // 幂等：已退款直接返回（重复退款回调）
        if (Objects.equals(wxOrder.getPayStatus(), PAY_STATUS_REFUNDED)) {
            return;
        }
        // 只有已支付且未退款的订单才存在退款动作
        if (!Objects.equals(wxOrder.getPayStatus(), PAY_STATUS_PAID)) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        // 已完成订单的出库已转销售，退款需走 D 的销售退货流程，不在本接口处理。
        // 已取消但尚未退款的订单（例如会员先取消）仍允许退款，因此这里只排除已完成状态。
        if (Objects.equals(wxOrder.getStatus(), WxOrderStatusEnum.COMPLETED.getStatus())) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        // 条件更新抢占：仅当仍为「已支付」时翻转，重复退款回调 rows=0 直接返回
        WxOrderDO updateObj = new WxOrderDO();
        updateObj.setPayStatus(PAY_STATUS_REFUNDED);
        updateObj.setStatus(WxOrderStatusEnum.CANCELED.getStatus());
        updateObj.setCancelReason(refundReason);
        int rows = wxOrderMapper.update(updateObj, new LambdaUpdateWrapper<WxOrderDO>()
                .eq(WxOrderDO::getId, id)
                .eq(WxOrderDO::getPayStatus, PAY_STATUS_PAID));
        if (rows == 0) {
            return;
        }
        // 对接 E：渠道退款。E 未就绪时降级不阻塞（与下单创建支付单的降级策略一致）
        refundChannelPayOrder(wxOrder);
        // 对接 C：结算库存 —— 仍冻结的释放，已出库的按原批次、原货位回补（同事务，失败整体回滚）
        settleStockOnClose(wxOrder, REFUND_BIZ_PREFIX, false);
        // 对接 F：退款即释放本单预扣的抵扣积分（幂等键 = 订单号，重复退款不重复返还）
        memberPointSettlementService.releaseSalePoints(wxOrder.getMemberId(), wxOrder.getOrderNo());
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
    @Transactional(rollbackFor = Exception.class)
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
        // 对接 F：完成 / 核销即赠送积分（幂等键 = 订单号，重复核销不重复赠送）。
        // 赠送基数 = 实际成交金额（应付金额，已扣除积分抵扣），积分规则见 yudao.pharmacy.member-point。
        int earned = memberPointSettlementService.earnSalePoints(wxOrder.getMemberId(), wxOrder.getOrderNo(),
                wxOrder.getPayableAmount());
        if (earned > 0) {
            WxOrderDO pointUpdate = new WxOrderDO();
            pointUpdate.setId(wxOrder.getId());
            pointUpdate.setPointEarned(earned);
            wxOrderMapper.updateById(pointUpdate);
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

    // ========== 小程序端（app）下单 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrderFromCart(Long memberId, Long storeId, Integer orderType,
                                    Long addressId, Long prescId, String remark, Integer usePoints) {
        // 1. 校验订单类型
        if (!Objects.equals(orderType, ORDER_TYPE_PICKUP) && !Objects.equals(orderType, ORDER_TYPE_DELIVERY)) {
            throw exception(PHARMACY_WX_ORDER_TYPE_INVALID);
        }
        // 2. 读取该会员已勾选的购物车记录，并限定本次履约门店
        List<WxCartDO> checkedList = wxCartService.getSelectedCartListByMemberId(memberId).stream()
                .filter(item -> Objects.equals(item.getStoreId(), storeId))
                .collect(Collectors.toList());
        if (checkedList.isEmpty()) {
            throw exception(PHARMACY_WX_CART_SELECTED_EMPTY);
        }
        // 3. 对接 A 商品接口：批量校验药品可销售，并生成明细快照
        List<Long> drugIds = checkedList.stream().map(WxCartDO::getDrugId).distinct()
                .collect(Collectors.toList());
        Map<Long, DrugRespDTO> drugMap = drugApi.getDrugList(drugIds).stream()
                .collect(Collectors.toMap(DrugRespDTO::getId, Function.identity(), (a, b) -> a));
        BigDecimal goodsAmount = BigDecimal.ZERO;
        boolean hasRx = false;
        List<WxOrderLineDO> lines = new ArrayList<>(checkedList.size());
        for (WxCartDO cart : checkedList) {
            DrugRespDTO drug = drugMap.get(cart.getDrugId());
            if (drug == null) {
                throw exception(PHARMACY_DRUG_NOT_EXISTS);
            }
            if (!Objects.equals(drug.getStatus(), 1) || !Objects.equals(drug.getApproveStatus(), 1)) {
                throw exception(PHARMACY_DRUG_NOT_SALEABLE);
            }
            if (Objects.equals(drug.getIsRx(), 1)) {
                hasRx = true;
            }
            // 会员价优先，无会员价则使用零售价
            BigDecimal price = drug.getMemberPrice() != null ? drug.getMemberPrice() : drug.getRetailPrice();
            if (price == null) {
                price = BigDecimal.ZERO;
            }
            price = price.setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineAmount = price.multiply(BigDecimal.valueOf(cart.getQty()))
                    .setScale(2, RoundingMode.HALF_UP);
            goodsAmount = goodsAmount.add(lineAmount);
            // 明细快照
            WxOrderLineDO line = new WxOrderLineDO();
            line.setDrugId(drug.getId());
            line.setQty(cart.getQty());
            line.setPrice(price);
            line.setLineAmount(lineAmount);
            line.setPickedQty(0);
            line.setDrugName(drug.getGenericName() != null ? drug.getGenericName() : drug.getTradeName());
            line.setSpecification(drug.getSpecification());
            line.setUnit(drug.getUnit());
            lines.add(line);
        }
        // 4. 处方药必须关联已审方通过的处方（审方状态由 E 的处方服务保证）
        if (hasRx && prescId == null) {
            throw exception(PHARMACY_WX_ORDER_PRESC_REQUIRED);
        }
        // 5. 同城配送必须选择本人收货地址，并生成地址快照
        String addressSnapshot = null;
        BigDecimal freightAmount = BigDecimal.ZERO;
        if (Objects.equals(orderType, ORDER_TYPE_DELIVERY)) {
            if (addressId == null) {
                throw exception(PHARMACY_WX_ORDER_ADDRESS_REQUIRED);
            }
            MemberAddressDO address = memberAddressService.validateMemberAddressExists(addressId);
            if (!Objects.equals(address.getUserId(), memberId)) {
                throw exception(PHARMACY_MEMBER_ADDRESS_NOT_OWNER);
            }
            addressSnapshot = address.getName() + " " + address.getMobile() + " " + address.getDetailAddress();
            // 配送费规则暂未配置，保持 0，避免虚构业务规则
            freightAmount = BigDecimal.ZERO;
        }
        // 6. 服务端精确计算金额（元），不接受前端传入金额
        BigDecimal couponAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payableBase = goodsAmount.add(freightAmount)
                .subtract(couponAmount).subtract(discountAmount);
        if (payableBase.signum() < 0) {
            payableBase = BigDecimal.ZERO;
        }
        // 6.1 积分抵扣：前端只表达「想用多少积分」，可用值由 F 的积分结算服务按
        //     会员余额、抵扣比例、单笔上限与订单金额校验后确定；校验不通过整笔下单失败
        SalePointCalcDTO pointCalc = memberPointSettlementService
                .calcSalePoints(memberId, payableBase, usePoints);
        int pointDeduct = pointCalc.getDeductPoints() == null ? 0 : pointCalc.getDeductPoints();
        BigDecimal pointDeductAmount = pointCalc.getDeductAmount() == null
                ? BigDecimal.ZERO : pointCalc.getDeductAmount();
        BigDecimal payableAmount = payableBase.subtract(pointDeductAmount);
        if (payableAmount.signum() < 0) {
            payableAmount = BigDecimal.ZERO;
        }
        // 7. 生成订单号与一次性取货码
        LocalDateTime now = LocalDateTime.now();
        WxOrderDO order = new WxOrderDO();
        order.setOrderNo(generateOrderNo(storeId, now));
        order.setMemberId(memberId);
        order.setStoreId(storeId);
        order.setOrderType(orderType);
        order.setGoodsAmount(goodsAmount);
        order.setCouponAmount(couponAmount);
        order.setFreightAmount(freightAmount);
        order.setDiscountAmount(discountAmount);
        order.setPointDeduct(pointDeduct);
        order.setPointDeductAmount(pointDeductAmount);
        order.setPointEarned(0);
        order.setPayableAmount(payableAmount);
        order.setPayStatus(PAY_STATUS_WAIT);
        order.setStatus(WxOrderStatusEnum.WAIT_PAY.getStatus());
        order.setPrescId(prescId);
        order.setAddressSnapshot(addressSnapshot);
        order.setRemark(remark);
        order.setPickupCode(generatePickupCode());
        // 未支付截止时间：下单后 30 分钟
        order.setExpireAt(now.plusMinutes(ORDER_EXPIRE_MINUTES));
        wxOrderMapper.insert(order);
        // 8. 写入订单明细
        for (WxOrderLineDO line : lines) {
            line.setWxOrderId(order.getId());
            wxOrderLineMapper.insert(line);
        }
        // 8.1 预扣抵扣积分：幂等键 = 订单号；积分不足抛业务异常，下单事务整体回滚
        memberPointSettlementService.deductSalePoints(memberId, order.getOrderNo(), pointDeduct);
        // 9. 清空本次已结算的购物车记录
        for (WxCartDO cart : checkedList) {
            wxCartService.deleteWxCart(cart.getId());
        }
        return order.getId();
    }

    // ========== 私有方法 ==========

    /**
     * 生成线上订单号，格式：WX-{门店}-{yyyyMMdd}-{4位流水}
     *
     * 依据当日已有订单数生成流水号，并校验唯一性，避免并发下重复。
     */
    private String generateOrderNo(Long storeId, LocalDateTime now) {
        String prefix = "WX-" + storeId + "-" + now.format(ORDER_NO_DATE_FORMAT) + "-";
        Long count = wxOrderMapper.selectCountByOrderNoPrefix(prefix);
        long seq = (count == null ? 0L : count) + 1L;
        String orderNo;
        do {
            orderNo = prefix + String.format("%04d", seq);
            seq++;
        } while (wxOrderMapper.selectByOrderNo(orderNo) != null);
        return orderNo;
    }

    /**
     * 生成一次性取货码（6 位大写字母数字），用于到店自提核销
     */
    private String generatePickupCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }

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
     * 对接 C：支付成功后按 FEFO 正式出库，并记录实际分配的批次与货位。
     *
     * 与 C 的契约（{@code InventoryFacade#deduct}）：
     * - 每行必须携带 bizNo（订单号，全行一致）与 bizLineId（订单明细编号），C 以
     *   (bizType, bizNo, bizLineId) 作为操作级幂等键，重复调用不会重复扣库；
     * - 不指定批次/货位时由 C 按效期最早优先选批，实际分配通过 {@link DeductResult} 回传；
     * - 出库分配写入 {@code ph_wx_order_line_alloc}，是取消/退款回补「原批次、原货位」的唯一依据。
     */
    /**
     * 对接 C：下单冻结库存（{@code InventoryFacade#reserve}，门店管理端节点）
     *
     * 与 C 的契约：
     * - 每行必填 {@code bizNo}（订单号，全行一致）与 {@code bizLineId}（订单明细编号），是操作级幂等键（流水 80）；
     * - 不指定批次/货位时由 C 按效期最早优先选批，实际分配通过 {@link ReserveResult} 回传；
     * - 库存不足、批次停售或过期时 C 抛业务异常，调用方事务整体回滚，不会留下半个冻结。
     *
     * 返回的实际分配写入 {@code ph_wx_order_line_alloc}（状态=已冻结），
     * 后续「转出库 / 释放 / 回补」都按该分配逐条调用。
     */
    private void reserveStock(WxOrderDO wxOrder) {
        List<WxOrderLineDO> lines = wxOrderLineService.getWxOrderLineListByWxOrderId(wxOrder.getId());
        if (lines == null || lines.isEmpty()) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "订单缺少明细，无法冻结库存");
        }
        // 下单前粗校验：C 的可售量为门店级汇总且已扣冻结，仅用于快速拒绝，权威判断由 reserve 完成
        assertAvailableQty(wxOrder.getStoreId(), lines);
        Map<Long, WxOrderLineDO> lineMap = new HashMap<>();
        List<ReserveItem> items = new ArrayList<>(lines.size());
        for (WxOrderLineDO line : lines) {
            ReserveItem item = new ReserveItem();
            item.setBizNo(wxOrder.getOrderNo());
            item.setBizLineId(line.getId());
            item.setDrugId(line.getDrugId());
            item.setQty(line.getQty());
            items.add(item);
            lineMap.put(line.getId(), line);
        }
        ReserveResult result;
        try {
            result = inventoryFacade.reserve(wxOrder.getStoreId(), items);
        } catch (UnsupportedOperationException ex) {
            throw exception(INV_SERVICE_UNAVAILABLE);
        } catch (AccessDeniedException ex) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "当前操作人不是该门店在职员工，无库存作业权限");
        } catch (ServiceException ex) {
            throw translateStockException(ex);
        }
        persistAllocations(wxOrder, lineMap, lines,
                fromReserve(result == null ? null : result.getAllocations()), WxOrderLineAllocDO.STATUS_FROZEN);
    }

    /**
     * 对接 C：订单关闭（取消 / 退款）时结算库存。
     *
     * - 仍处于「已冻结」的分配 → {@code release}（流水 81，业务单号 WXC-/WXR- + 订单号），释放占用；
     * - 「已出库」的分配 → {@code returnBack}（流水 21），携带 originalBizNo/originalBizLineId 指向原出库流水，
     *   按原批次、原货位回补；
     * - 「已释放或已回补」的分配 → 跳过，配合状态标记保证只结算一次。
     *
     * @param bizPrefix  本次结算的业务单号前缀（取消 WXC- / 退款 WXR-）
     * @param frozenOnly true=只释放冻结（门店批量清理超时订单时使用），不处理已出库分配
     * @return 本次结算的分配条数
     */
    private int settleStockOnClose(WxOrderDO wxOrder, String bizPrefix, boolean frozenOnly) {

        List<WxOrderLineAllocDO> allocations = wxOrderLineAllocMapper.selectListByWxOrderId(wxOrder.getId());
        if (allocations == null || allocations.isEmpty()) {
            // 从未冻结 / 出库的订单没有库存动作
            return 0;
        }
        String bizNo = bizPrefix + wxOrder.getOrderNo();
        List<ReleaseItem> releaseItems = new ArrayList<>(allocations.size());
        List<ReturnBackItem> returnItems = new ArrayList<>(allocations.size());
        List<WxOrderLineAllocDO> settled = new ArrayList<>(allocations.size());
        for (WxOrderLineAllocDO allocation : allocations) {
            // 已了结的分配跳过，配合 C 的累计校验保证「只结算一次」
            if (Objects.equals(allocation.getStatus(), WxOrderLineAllocDO.STATUS_SETTLED)) {
                continue;
            }
            if (Objects.equals(allocation.getStatus(), WxOrderLineAllocDO.STATUS_FROZEN)) {
                ReleaseItem item = new ReleaseItem();
                item.setBizNo(bizNo);
                item.setBizLineId(allocation.getId());
                item.setOriginalBizNo(allocation.getOrderNo());
                item.setOriginalBizLineId(allocation.getWxOrderLineId());
                item.setDrugId(allocation.getDrugId());
                item.setBatchId(allocation.getBatchId());
                item.setLocationId(allocation.getLocationId());
                item.setQty(allocation.getQty());
                releaseItems.add(item);
                settled.add(allocation);
                continue;
            }
            // 已出库的分配：批量释放冻结的场景不处理，交由退款 / 退货节点回补，避免「未退款先回补」
            if (frozenOnly) {
                continue;
            }
            int returned = allocation.getReturnedQty() == null ? 0 : allocation.getReturnedQty();
            int pendingQty = allocation.getQty() - returned;
            if (pendingQty <= 0) {
                continue;
            }
            ReturnBackItem item = new ReturnBackItem();
            item.setBizNo(bizNo);
            // 行号用分配记录编号，保证同一批次拆分出的多条分配各自唯一
            item.setBizLineId(allocation.getId());
            // 原出库引用必须精确指向「正式出库流水」的业务单号与行号：
            // 冻结转出库用分配记录编号，直接扣库用订单明细编号，取落库的出库来源行号
            item.setOriginalBizNo(allocation.getOrderNo());
            item.setOriginalBizLineId(allocation.getOutBizLineId() == null
                    ? allocation.getWxOrderLineId() : allocation.getOutBizLineId());
            item.setDrugId(allocation.getDrugId());
            item.setBatchId(allocation.getBatchId());
            item.setLocationId(allocation.getLocationId());
            item.setQty(pendingQty);
            returnItems.add(item);
            settled.add(allocation);
        }
        if (!releaseItems.isEmpty()) {
            try {
                inventoryFacade.release(wxOrder.getStoreId(), releaseItems);
            } catch (UnsupportedOperationException ex) {
                throw exception(INV_SERVICE_UNAVAILABLE);
            } catch (AccessDeniedException ex) {
                throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "当前操作人不是该门店在职员工，无库存作业权限");
            } catch (ServiceException ex) {
                throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, ex.getMessage());
            }
        }
        if (!returnItems.isEmpty()) {
            try {
                inventoryFacade.returnBack(wxOrder.getStoreId(), returnItems);
            } catch (UnsupportedOperationException ex) {
                throw exception(INV_SERVICE_UNAVAILABLE);
            } catch (AccessDeniedException ex) {
                throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "当前操作人不是该门店在职员工，无库存作业权限");
            } catch (ServiceException ex) {
                throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, ex.getMessage());
            }
        }
        // 标记已了结，重复取消 / 退款 / 批量清理不再重复提交库存请求
        for (WxOrderLineAllocDO allocation : settled) {
            settleAllocation(allocation);
        }
        return settled.size();
    }

    /** 把分配标记为已释放 / 已回补 */
    private void settleAllocation(WxOrderLineAllocDO allocation) {
        WxOrderLineAllocDO update = new WxOrderLineAllocDO();
        update.setId(allocation.getId());
        update.setReturnedQty(allocation.getQty());
        update.setStatus(WxOrderLineAllocDO.STATUS_SETTLED);
        wxOrderLineAllocMapper.updateById(update);
    }

    /**
     * 对接 C：支付成功后的出库。
     *
     * 已冻结的订单走「冻结转正式出库」（{@code consumeReservation}，流水 82）；
     * 从未冻结的历史订单退化为按 FEFO 直接扣库（{@code deduct}，流水 20），保证上线期间的订单仍可支付。
     */
    private void consumeReservedOrDeductStock(WxOrderDO wxOrder) {
        List<WxOrderLineAllocDO> allocations = wxOrderLineAllocMapper.selectListByWxOrderId(wxOrder.getId());
        if (allocations != null && !allocations.isEmpty()) {
            consumeReservedStock(wxOrder, allocations);
            return;
        }
        deductStock(wxOrder);
    }

    /**
     * 对接 C：冻结转正式出库（{@code InventoryFacade#consumeReservation}）
     *
     * 必须携带原冻结的 originalBizNo / originalBizLineId 与原批次、原货位、药品、数量；
     * C 按原冻结流水累计校验不超过冻结量。新业务单号用订单号、行号用分配记录编号，
     * 与冻结（流水 80）属不同流水类型，重复调用不会重复出库。
     */
    private void consumeReservedStock(WxOrderDO wxOrder, List<WxOrderLineAllocDO> allocations) {
        List<ConsumeItem> items = new ArrayList<>(allocations.size());
        List<WxOrderLineAllocDO> pending = new ArrayList<>(allocations.size());
        for (WxOrderLineAllocDO allocation : allocations) {
            // 已出库 / 已了结的分配不重复转出库
            if (!Objects.equals(allocation.getStatus(), WxOrderLineAllocDO.STATUS_FROZEN)) {
                continue;
            }
            ConsumeItem item = new ConsumeItem();
            item.setBizNo(wxOrder.getOrderNo());
            item.setBizLineId(allocation.getId());
            item.setOriginalBizNo(allocation.getOrderNo());
            item.setOriginalBizLineId(allocation.getWxOrderLineId());
            item.setDrugId(allocation.getDrugId());
            item.setBatchId(allocation.getBatchId());
            item.setLocationId(allocation.getLocationId());
            item.setQty(allocation.getQty());
            items.add(item);
            pending.add(allocation);
        }
        if (items.isEmpty()) {
            return;
        }
        try {
            inventoryFacade.consumeReservation(wxOrder.getStoreId(), items);
        } catch (UnsupportedOperationException ex) {
            throw exception(INV_SERVICE_UNAVAILABLE);
        } catch (AccessDeniedException ex) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "当前操作人不是该门店在职员工，无库存作业权限");
        } catch (ServiceException ex) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, ex.getMessage());
        }
        for (WxOrderLineAllocDO allocation : pending) {
            WxOrderLineAllocDO update = new WxOrderLineAllocDO();
            update.setId(allocation.getId());
            update.setStatus(WxOrderLineAllocDO.STATUS_OUT);
            // 记录本次出库流水的来源行号（冻结转出库用分配记录编号），供后续回补精确引用
            update.setOutBizLineId(allocation.getId());
            wxOrderLineAllocMapper.updateById(update);
        }
    }

    /**
     * 对接 C：按 FEFO 直接出库（未冻结订单的兼容路径，{@code InventoryFacade#deduct}）
     */
    private void deductStock(WxOrderDO wxOrder) {
        List<WxOrderLineDO> lines = wxOrderLineService.getWxOrderLineListByWxOrderId(wxOrder.getId());
        if (lines == null || lines.isEmpty()) {
            // 没有明细的订单不允许出库，避免出现「订单已支付但库存无变化」
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "订单缺少明细，无法出库");
        }
        Map<Long, WxOrderLineDO> lineMap = new HashMap<>();
        List<DeductItem> items = new ArrayList<>(lines.size());
        for (WxOrderLineDO line : lines) {
            DeductItem item = new DeductItem();
            item.setBizNo(wxOrder.getOrderNo());
            item.setBizLineId(line.getId());
            item.setDrugId(line.getDrugId());
            item.setQty(line.getQty());
            items.add(item);
            lineMap.put(line.getId(), line);
        }
        DeductResult result;
        try {
            result = inventoryFacade.deduct(wxOrder.getStoreId(), items);
        } catch (UnsupportedOperationException ex) {
            throw exception(INV_SERVICE_UNAVAILABLE);
        } catch (AccessDeniedException ex) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "当前操作人不是该门店在职员工，无库存作业权限");
        } catch (ServiceException ex) {
            throw translateStockException(ex);
        }
        persistAllocations(wxOrder, lineMap, lines,
                fromDeduct(result == null ? null : result.getAllocations()), WxOrderLineAllocDO.STATUS_OUT);
    }

    /** 库存分配的统一视图：C 的 reserve / deduct 返回结构不同，落库逻辑共用 */
    private record Allocation(Long bizLineId, Long batchId, Long locationId, Integer qty) {
    }

    private List<Allocation> fromReserve(List<ReserveResult.Allocation> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .map(item -> new Allocation(item.getBizLineId(), item.getBatchId(), item.getLocationId(), item.getQty()))
                .toList();
    }

    private List<Allocation> fromDeduct(List<DeductResult.Allocation> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .map(item -> new Allocation(item.getBizLineId(), item.getBatchId(), item.getLocationId(), item.getQty()))
                .toList();
    }

    /**
     * 把 C 返回的库存分配落库，并把首个分配回填订单明细便于拣货。
     *
     * 分配总量必须等于明细数量，否则说明关键库存数据不完整，抛异常让调用方事务整体回滚。
     */
    private void persistAllocations(WxOrderDO wxOrder, Map<Long, WxOrderLineDO> lineMap, List<WxOrderLineDO> lines,
                                    List<Allocation> allocations, Integer status) {
        if (allocations.isEmpty()) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "库存服务未返回库存分配");
        }
        Map<Long, Integer> allocatedByLine = new HashMap<>();
        for (Allocation allocation : allocations) {
            WxOrderLineDO line = lineMap.get(allocation.bizLineId());
            if (line == null) {
                throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "库存服务返回了未知的订单明细分配");
            }
            allocatedByLine.merge(line.getId(), allocation.qty(), Integer::sum);
            WxOrderLineAllocDO alloc = new WxOrderLineAllocDO();
            alloc.setWxOrderId(wxOrder.getId());
            alloc.setWxOrderLineId(line.getId());
            alloc.setOrderNo(wxOrder.getOrderNo());
            alloc.setDrugId(line.getDrugId());
            alloc.setBatchId(allocation.batchId());
            alloc.setLocationId(allocation.locationId());
            alloc.setQty(allocation.qty());
            alloc.setReturnedQty(0);
            alloc.setStatus(status);
            if (Objects.equals(status, WxOrderLineAllocDO.STATUS_OUT)) {
                // 直接扣库路径的出库来源行号是订单明细编号，回补时需按此精确引用
                alloc.setOutBizLineId(line.getId());
            }
            wxOrderLineAllocMapper.insert(alloc);
            // 首个分配回填订单明细，便于拣货与页面展示
            if (line.getBatchId() == null) {
                WxOrderLineDO lineUpdate = new WxOrderLineDO();
                lineUpdate.setId(line.getId());
                lineUpdate.setBatchId(allocation.batchId());
                lineUpdate.setLocationId(allocation.locationId());
                wxOrderLineMapper.updateById(lineUpdate);
                line.setBatchId(allocation.batchId());
                line.setLocationId(allocation.locationId());
            }
        }
        for (WxOrderLineDO line : lines) {
            Integer allocated = allocatedByLine.get(line.getId());
            if (allocated == null || !allocated.equals(line.getQty())) {
                throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "库存分配数量与订单明细不一致");
            }
        }
    }

    /**
     * 下单前粗校验可售量（{@code InventoryFacade#getAvailableQty}）
     *
     * C 的可售量为门店级汇总（已扣冻结）且不校验效期与质量状态，因此只能用于快速拒绝，
     * 权威判断仍由 {@code reserve} 在具体批次、货位上完成。
     */
    private void assertAvailableQty(Long storeId, List<WxOrderLineDO> lines) {
        Map<Long, Integer> required = new LinkedHashMap<>();
        for (WxOrderLineDO line : lines) {
            required.merge(line.getDrugId(), line.getQty(), Integer::sum);
        }
        List<AvailableQty> available;
        try {
            available = inventoryFacade.getAvailableQty(storeId, new ArrayList<>(required.keySet()));
        } catch (UnsupportedOperationException ex) {
            throw exception(INV_SERVICE_UNAVAILABLE);
        } catch (AccessDeniedException ex) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "当前操作人不是该门店在职员工，无库存作业权限");
        } catch (ServiceException ex) {
            throw translateStockException(ex);
        }
        for (AvailableQty qty : available == null ? List.<AvailableQty>of() : available) {
            Integer need = required.get(qty.getDrugId());
            if (need != null && (qty.getQtyAvail() == null || qty.getQtyAvail() < need)) {
                throw exception(PHARMACY_WX_ORDER_STOCK_NOT_ENOUGH);
            }
        }
    }

    /** 门店批量清理的单次处理上限，避免一次请求触发过多库存作业 */
    private int normalizeCleanupLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return STOCK_CLEANUP_LIMIT;
        }
        return Math.min(limit, STOCK_CLEANUP_MAX_LIMIT);
    }

    /**
     * 把 C 的库存业务异常翻译为 F 的业务异常。
     *
     * 「可用库存不足」类错误给出明确的库存不足提示；其余保留 C 的原始原因，便于排障。
     */
    private ServiceException translateStockException(ServiceException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.contains("库存不足") || message.contains("可用库存")) {
            return exception(PHARMACY_WX_ORDER_STOCK_NOT_ENOUGH);
        }
        return exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, message);
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
     * 对接 E：渠道退款。
     *
     * 退款单号用 WXR-{订单号}（退款幂等键），金额用应付金额元转分。
     * E 未就绪或订单没有支付单时降级不阻塞（与下单时创建支付单的降级策略一致），
     * 订单侧的退款状态与库存回补仍会完成，渠道对账由后续人工/对账处理。
     */
    private void refundChannelPayOrder(WxOrderDO wxOrder) {
        if (wxOrder.getPayOrderId() == null) {
            return;
        }
        try {
            paymentFacade.refund(wxOrder.getPayOrderId(), REFUND_BIZ_PREFIX + wxOrder.getOrderNo(),
                    yuanToFen(wxOrder.getPayableAmount()), "线上订单退款");
        } catch (UnsupportedOperationException ex) {
            // E 支付服务未就绪，降级：只记录订单退款状态，不阻塞库存回补
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
