package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;
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
        // 对接 C：支付成功即按 FEFO 正式出库（同事务），并记录实际批次/货位供后续回补
        deductStock(wxOrder);
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
        // 已支付（待拣货）的订单此前已出库，取消需按原批次、原货位回补库存。
        // 通过状态条件更新 + 出库分配表的已回补数量双重保证「只释放一次」。
        returnBackStock(wxOrder, true);
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
        // 已完成订单的出库已转销售，退款需走 D 的销售退货流程，不在本接口处理
        if (WxOrderStatusEnum.isFinished(wxOrder.getStatus())) {
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
        // 对接 C：按原批次、原货位回补库存（同事务，失败整体回滚）
        returnBackStock(wxOrder, false);
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
                                    Long addressId, Long prescId, String remark) {
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
        BigDecimal payableAmount = goodsAmount.add(freightAmount)
                .subtract(couponAmount).subtract(discountAmount);
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
    private void deductStock(WxOrderDO wxOrder) {
        List<WxOrderLineDO> lines = wxOrderLineService.getWxOrderLineListByWxOrderId(wxOrder.getId());
        if (lines == null || lines.isEmpty()) {
            // 没有明细的订单不允许出库，避免出现「订单已支付但库存无变化」
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "订单缺少明细，无法出库");
        }
        // 已存在出库分配说明本单已出库（重复支付回调），直接返回，不重复扣库
        if (!wxOrderLineAllocMapper.selectListByWxOrderId(wxOrder.getId()).isEmpty()) {
            return;
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
        if (result == null || result.getAllocations() == null || result.getAllocations().isEmpty()) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "库存服务未返回出库分配");
        }
        Map<Long, Integer> allocatedByLine = new HashMap<>();
        for (DeductResult.Allocation allocation : result.getAllocations()) {
            WxOrderLineDO line = lineMap.get(allocation.getBizLineId());
            if (line == null) {
                throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "库存服务返回了未知的订单明细分配");
            }
            allocatedByLine.merge(line.getId(), allocation.getQty(), Integer::sum);
            WxOrderLineAllocDO alloc = new WxOrderLineAllocDO();
            alloc.setWxOrderId(wxOrder.getId());
            alloc.setWxOrderLineId(line.getId());
            alloc.setOrderNo(wxOrder.getOrderNo());
            alloc.setDrugId(line.getDrugId());
            alloc.setBatchId(allocation.getBatchId());
            alloc.setLocationId(allocation.getLocationId());
            alloc.setQty(allocation.getQty());
            alloc.setReturnedQty(0);
            alloc.setStatus(WxOrderLineAllocDO.STATUS_OUT);
            wxOrderLineAllocMapper.insert(alloc);
            // 首个分配回填订单明细，便于拣货与页面展示
            if (line.getBatchId() == null) {
                WxOrderLineDO lineUpdate = new WxOrderLineDO();
                lineUpdate.setId(line.getId());
                lineUpdate.setBatchId(allocation.getBatchId());
                lineUpdate.setLocationId(allocation.getLocationId());
                wxOrderLineMapper.updateById(lineUpdate);
                line.setBatchId(allocation.getBatchId());
                line.setLocationId(allocation.getLocationId());
            }
        }
        // 分配总量必须等于明细数量，否则说明关键库存数据不完整，直接回滚本次支付
        for (WxOrderLineDO line : lines) {
            Integer allocated = allocatedByLine.get(line.getId());
            if (allocated == null || !allocated.equals(line.getQty())) {
                throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "出库分配数量与订单明细不一致");
            }
        }
    }

    /**
     * 对接 C：取消 / 退款后按原批次、原货位回补库存。
     *
     * 与 C 的契约（{@code InventoryFacade#returnBack}）：
     * - bizNo 为本次回补的业务单号（WXC-/WXR- 前缀 + 订单号），bizLineId 为出库分配记录编号，
     *   同一 (bizType, bizNo, bizLineId) 重复提交由 C 幂等重放；
     * - originalBizNo / originalBizLineId 指向原出库的业务单号与来源行，C 据此定位原出库流水，
     *   并校验累计回补数量不超过原出库数量；
     * - 已回补完成的分配不再提交，配合 C 的累计校验保证「只释放一次」。
     *
     * @param isCancel true=取消回补（WXC-），false=退款回补（WXR-）
     */
    private void returnBackStock(WxOrderDO wxOrder, boolean isCancel) {
        List<WxOrderLineAllocDO> allocations = wxOrderLineAllocMapper.selectListByWxOrderId(wxOrder.getId());
        if (allocations == null || allocations.isEmpty()) {
            // 未出库（未支付）的订单没有需要释放的库存
            return;
        }
        String bizNo = (isCancel ? CANCEL_BIZ_PREFIX : REFUND_BIZ_PREFIX) + wxOrder.getOrderNo();
        List<ReturnBackItem> items = new ArrayList<>(allocations.size());
        Map<Long, WxOrderLineAllocDO> pending = new LinkedHashMap<>();
        for (WxOrderLineAllocDO allocation : allocations) {
            int returned = allocation.getReturnedQty() == null ? 0 : allocation.getReturnedQty();
            int pendingQty = allocation.getQty() - returned;
            if (pendingQty <= 0) {
                continue;
            }
            ReturnBackItem item = new ReturnBackItem();
            item.setBizNo(bizNo);
            item.setBizLineId(allocation.getId());
            item.setOriginalBizNo(allocation.getOrderNo());
            item.setOriginalBizLineId(allocation.getWxOrderLineId());
            item.setDrugId(allocation.getDrugId());
            item.setBatchId(allocation.getBatchId());
            item.setLocationId(allocation.getLocationId());
            item.setQty(pendingQty);
            items.add(item);
            pending.put(allocation.getId(), allocation);
        }
        if (items.isEmpty()) {
            // 全部已回补，视为幂等成功
            return;
        }
        try {
            inventoryFacade.returnBack(wxOrder.getStoreId(), items);
        } catch (UnsupportedOperationException ex) {
            throw exception(INV_SERVICE_UNAVAILABLE);
        } catch (AccessDeniedException ex) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, "当前操作人不是该门店在职员工，无库存作业权限");
        } catch (ServiceException ex) {
            throw exception(PHARMACY_WX_ORDER_STOCK_OP_FAILED, ex.getMessage());
        }
        // 标记已回补，重复取消 / 退款不再重复提交回补请求
        for (Map.Entry<Long, WxOrderLineAllocDO> entry : pending.entrySet()) {
            WxOrderLineAllocDO update = new WxOrderLineAllocDO();
            update.setId(entry.getKey());
            update.setReturnedQty(entry.getValue().getQty());
            update.setStatus(WxOrderLineAllocDO.STATUS_RETURNED);
            wxOrderLineAllocMapper.updateById(update);
        }
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
