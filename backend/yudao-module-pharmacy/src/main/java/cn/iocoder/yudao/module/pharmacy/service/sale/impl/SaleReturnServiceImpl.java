package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;
import cn.iocoder.yudao.module.pharmacy.api.member.MemberPointFacade;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacade;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnDetailRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SalePaymentMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnMapper;
import cn.iocoder.yudao.module.pharmacy.service.sale.SaleReturnService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.MEMBER_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PAY_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NO_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NOT_ALLOW;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_PRESC_NOT_CONFIRM;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_QTY_EXCEED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_REFUND_AMOUNT_INVALID;

/**
 * 退货服务实现。
 * <p>
 * 退货依赖：渠道退款（E PaymentFacade）、积分回退（F MemberPointFacade）、
 * 库存回补（C InventoryFacade）。任一依赖未实现即抛对应“服务未就绪”错误码，
 * 整个事务回滚；本实现不写库存/支付/积分数据。
 */
@Service
public class SaleReturnServiceImpl implements SaleReturnService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Resource
    private SaleOrderMapper saleOrderMapper;
    @Resource
    private SaleOrderLineMapper saleOrderLineMapper;
    @Resource
    private SaleReturnMapper saleReturnMapper;
    @Resource
    private SaleReturnLineMapper saleReturnLineMapper;
    @Resource
    private InventoryFacade inventoryFacade;
    @Resource
    private PaymentFacade paymentFacade;
    @Resource
    private MemberPointFacade memberPointFacade;
    @Resource
    private SalePaymentMapper salePaymentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReturn(SaleReturnSaveReqVO reqVO) {
        // 1. 原单校验
        PhSaleOrderDO order = saleOrderMapper.selectById(reqVO.getSaleOrderId());
        if (order == null) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_NOT_EXISTS);
        }
        if (order.getStatus() != null && (order.getStatus() == 2 || order.getStatus() == -1)) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_NOT_ALLOW);
        }

        // 2. 明细校验与金额汇总
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PhSaleReturnLineDO> lines = new ArrayList<>();
        for (SaleReturnSaveReqVO.Item item : reqVO.getItems()) {
            PhSaleOrderLineDO line = saleOrderLineMapper.selectById(item.getSaleOrderLineId());
            if (line == null || !line.getOrderId().equals(order.getId())) {
                throw ServiceExceptionUtil.exception(SALE_RETURN_NOT_EXISTS);
            }
            int returned = saleReturnLineMapper.selectSumQtyBySaleLineId(line.getId());
            if (returned + item.getQty() > line.getQty()) {
                throw ServiceExceptionUtil.exception(SALE_RETURN_QTY_EXCEED);
            }
            if (line.getIsRx() != null && line.getIsRx() == 1
                    && (reqVO.getPharmacistConfirm() == null || reqVO.getPharmacistConfirm() != 1)) {
                throw ServiceExceptionUtil.exception(SALE_RETURN_PRESC_NOT_CONFIRM);
            }
            PhSaleReturnLineDO rl = new PhSaleReturnLineDO();
            rl.setSaleLineId(line.getId());
            rl.setBatchId(line.getBatchId());
            rl.setDrugId(line.getDrugId());
            rl.setQty(item.getQty());
            rl.setPrice(line.getPrice());
            rl.setAmount(line.getPrice().multiply(BigDecimal.valueOf(item.getQty())));
            rl.setPointsDeduct(0);
            // A return must restore the exact outbound allocation. Do not accept a caller-selected location.
            if (line.getBatchId() == null || line.getLocationId() == null) {
                throw ServiceExceptionUtil.invalidParamException("原销售明细缺少批次或货位，不能执行原批次退货");
            }
            rl.setLocationId(line.getLocationId());
            totalAmount = totalAmount.add(rl.getAmount());
            lines.add(rl);
        }
        if (totalAmount.signum() <= 0) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_QTY_EXCEED);
        }

        // 3. Persist the return source rows before calling C. Their generated ids are the idempotency
        // key, and all later payment/points/inventory failures still roll this transaction back.
        String returnNo = genReturnNo(order.getStoreId());
        if (saleReturnMapper.selectByReturnNo(returnNo) != null) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_NO_DUPLICATE);
        }
        PhSaleReturnDO ret = new PhSaleReturnDO();
        ret.setReturnNo(returnNo);
        ret.setSaleOrderId(order.getId());
        ret.setStoreId(order.getStoreId());
        ret.setReturnType(reqVO.getReturnType());
        ret.setReason(reqVO.getReason());
        ret.setTotalAmount(totalAmount);
        ret.setRefundMethod(reqVO.getRefundMethod() == null ? 0 : reqVO.getRefundMethod());
        ret.setStatus(1);
        ret.setCashierId(reqVO.getCashierId() != null ? reqVO.getCashierId() : order.getCashierId());
        ret.setPharmacistConfirm(reqVO.getPharmacistConfirm());
        ret.setRefundStatus(0);
        saleReturnMapper.insert(ret);
        for (PhSaleReturnLineDO line : lines) {
            line.setReturnId(ret.getId());
            saleReturnLineMapper.insert(line);
        }

        // 4. 渠道退款（E PaymentFacade）+ 销售支付明细退款记录（D-2）
        //    退款金额由服务端按原支付明细占比计算，逐笔退款；现金支付同样落退款流水。
        refundPayments(order, ret, lines, totalAmount, reqVO.getRefundMethod());

        // 5. 库存回补（C 服务；真实实现按原销售流水回补）
        List<ReturnBackItem> returnItems = new ArrayList<>();
        for (PhSaleReturnLineDO rl : lines) {
            ReturnBackItem rbi = new ReturnBackItem();
            rbi.setDrugId(rl.getDrugId());
            rbi.setBatchId(rl.getBatchId());
            rbi.setQty(rl.getQty());
            rbi.setLocationId(rl.getLocationId());
            rbi.setBizNo(ret.getReturnNo());
            rbi.setBizLineId(rl.getId());
            rbi.setOriginalBizNo(order.getOrderNo());
            rbi.setOriginalBizLineId(rl.getSaleLineId());
            returnItems.add(rbi);
        }
        try {
            inventoryFacade.returnBack(order.getStoreId(), returnItems);
        } catch (UnsupportedOperationException ex) {
            throw ServiceExceptionUtil.exception(INV_SERVICE_UNAVAILABLE);
        }

        // 6. 更新原单行已退数量与整单退货标志
        boolean allReturned = true;
        for (PhSaleReturnLineDO rl : lines) {
            PhSaleOrderLineDO line = saleOrderLineMapper.selectById(rl.getSaleLineId());
            int newReturned = line.getReturnedQty() + rl.getQty();
            line.setReturnedQty(newReturned);
            saleOrderLineMapper.updateById(line);
            if (newReturned < line.getQty()) {
                allReturned = false;
            }
        }
        order.setReturnFlag(allReturned ? 2 : 1);
        order.setStatus(allReturned ? 2 : 3);
        saleOrderMapper.updateById(order);

        // 7. 积分结算（F 的统一积分服务，D 不直接写 member_point_record）：
        //    - 按「本次退货金额 / 原单商品金额」的比例扣回本单已赠送积分；
        //    - 返还原单实际抵扣的积分（同样按比例，本次构成整单全退时结清剩余）；
        //    - 幂等键 = 退货单号：同一退货单重复提交不会重复回退，部分退货也不会按整单重复回退；
        //    - 与库存 / 支付同一事务，任一环节失败整体回滚，不留下孤立积分流水或错误余额。
        try {
            memberPointFacade.refundSalePoints(order.getMemberId(), order.getOrderNo(), ret.getReturnNo(),
                    order.getSubtotal(), totalAmount, allReturned);
        } catch (UnsupportedOperationException ex) {
            throw ServiceExceptionUtil.exception(MEMBER_SERVICE_UNAVAILABLE);
        }

        // 8. 退款、积分回退、库存回补全部成功后，标记退货单完成
        ret.setStatus(3);        // 已完成
        ret.setRefundStatus(2);  // 退款成功
        ret.setReturnAt(LocalDateTime.now());
        saleReturnMapper.updateById(ret);
        return ret.getId();
    }

    @Override
    public PageResult<PhSaleReturnDO> getReturnPage(SaleReturnPageReqVO reqVO) {
        return saleReturnMapper.selectPage(reqVO);
    }

    @Override
    public PhSaleReturnDO getReturn(Long id) {
        PhSaleReturnDO ret = saleReturnMapper.selectById(id);
        if (ret == null) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_NOT_EXISTS);
        }
        return ret;
    }

    @Override
    public SaleReturnDetailRespVO getReturnDetail(Long id) {
        PhSaleReturnDO ret = getReturn(id);
        SaleReturnDetailRespVO detail = new SaleReturnDetailRespVO();
        detail.setReturnOrder(ret);
        detail.setLines(saleReturnLineMapper.selectListByReturnId(id));
        return detail;
    }

    /**
     * 退款（D-2）：按原销售支付明细逐笔退款，并落支付退款流水。
     * <p>
     * 1) 退款金额由服务端计算：按每笔支付占实付金额的比例分摊 totalAmount，末笔补差保证合计一致；
     * 2) 原路退款（refundMethod=0）且该笔存在渠道支付单时，调用 E 的统一退款门面（refundNo 幂等）；
     *    现金（无渠道支付单）或指定现金退款（refundMethod=1）时本地记录退款流水，不伪造渠道成功；
     * 3) 每笔支付明细写 refund_no / refund_at；整单全退时置 status=3（已退款），部分退保持原状态，
     *    避免阻断后续剩余数量的退货退款（超量/重复退款由数量校验与整单状态拦截）；
     * 4) E 未实现（UnsupportedOperationException）时转 PAY_SERVICE_UNAVAILABLE，事务整体回滚。
     */
    private void refundPayments(PhSaleOrderDO order, PhSaleReturnDO ret, List<PhSaleReturnLineDO> lines,
                                BigDecimal totalAmount, Integer refundMethodRaw) {
        List<PhSalePaymentDO> payments = salePaymentMapper.selectListByOrderId(order.getId());
        List<PhSalePaymentDO> refundable = new ArrayList<>();
        for (PhSalePaymentDO pm : payments) {
            if (pm.getStatus() != null && pm.getStatus() == 1) {
                refundable.add(pm);
            }
        }
        if (refundable.isEmpty()) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_REFUND_AMOUNT_INVALID);
        }
        BigDecimal paidTotal = BigDecimal.ZERO;
        for (PhSalePaymentDO pm : refundable) {
            paidTotal = paidTotal.add(pm.getPayAmount());
        }
        if (paidTotal.signum() <= 0 || totalAmount.compareTo(paidTotal) > 0) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_REFUND_AMOUNT_INVALID);
        }
        // 本次退货是否导致整单全退：决定支付明细状态是否置为已退款
        boolean allReturned = true;
        for (PhSaleReturnLineDO rl : lines) {
            PhSaleOrderLineDO line = saleOrderLineMapper.selectById(rl.getSaleLineId());
            int newReturned = line.getReturnedQty() + rl.getQty();
            if (newReturned < line.getQty()) {
                allReturned = false;
                break;
            }
        }
        int refundMethod = refundMethodRaw == null ? 0 : refundMethodRaw;
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < refundable.size(); i++) {
            PhSalePaymentDO pm = refundable.get(i);
            BigDecimal refundAmt = (i == refundable.size() - 1)
                    ? totalAmount.subtract(allocated)
                    : totalAmount.multiply(pm.getPayAmount()).divide(paidTotal, 2, RoundingMode.HALF_UP);
            allocated = allocated.add(refundAmt);
            // refundNo 为退款幂等键（E 侧唯一），采用 退货单ID-支付明细ID 短格式，避免超出 refund_no(32) 列宽
            String refundNo = "RF-" + ret.getId() + "-" + pm.getId();
            LocalDateTime refundAt = LocalDateTime.now();
            // 原路退款且该笔存在渠道支付单 → 调 E 统一退款门面（refundNo 幂等）
            boolean channelRefund = (refundMethod != 1) && (pm.getPayOrderId() != null);
            if (channelRefund) {
                try {
                    paymentFacade.refund(pm.getPayOrderId(), refundNo,
                            refundAmt.movePointRight(2).intValue(), "药店销售退货");
                } catch (UnsupportedOperationException ex) {
                    throw ServiceExceptionUtil.exception(PAY_SERVICE_UNAVAILABLE);
                }
            }
            // 现金支付同样必须有明确退款记录与状态变化
            PhSalePaymentDO update = new PhSalePaymentDO();
            update.setId(pm.getId());
            update.setRefundNo(refundNo);
            update.setRefundAt(refundAt);
            if (allReturned) {
                update.setStatus(3); // 已退款
            }
            salePaymentMapper.updateById(update);
        }
    }

    private String genReturnNo(Long storeId) {
        return "SR-" + storeId + "-" + LocalDateTime.now().format(TIME_FMT) + "-"
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
