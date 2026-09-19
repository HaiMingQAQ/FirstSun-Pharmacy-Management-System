package cn.iocoder.yudao.module.pharmacy.service.member.integration;

import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberPointSaleLinkMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnMapper;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberPointSettlementService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * POS 销售 / 退货的积分联动切面（F 会员模块自有，<b>不修改 D 的任何文件</b>）。
 *
 * <p>背景：D 的 {@code SaleOrderService.createSaleOrder} 与 {@code SaleReturnService.createReturn}
 * 自身不调用 F 的积分服务（销售单 {@code points_earned} 恒为 0、退货回退分支被死条件挡住），
 * 而「D/E 不得直接写 {@code member_point_record}」要求积分统一由 F 结算。
 * 因此在 F 侧用切面挂接这两个业务节点，把积分结算收敛回
 * {@link MemberPointSettlementService}：
 *
 * <ul>
 *   <li><b>销售完成</b>：按后端规则计算赠送积分、更新会员余额、写来源明确的流水，
 *       并把实际赠送积分回写单据的 {@code points_earned}；</li>
 *   <li><b>原单退货</b>：按「本次退货金额 ÷ 原单金额」比例扣回已赠送积分、返还原单实际抵扣积分，
 *       整单全退时结清剩余，多次部分退货按累计上限收敛；</li>
 *   <li><b>幂等</b>：积分单据号一律用业务单号（销售单号 / 退货单号），
 *       重复销售回调、重复退货、重复核销都由 {@code member_point_record} 的唯一键兜住；</li>
 *   <li><b>事务安全</b>：切面顺序排在 D 的方法事务之外，只有 D 的事务成功提交后才结算积分，
 *       因此销售 / 退货失败（含扣库失败、支付失败）时不会产生任何积分流水。</li>
 * </ul>
 *
 * <p>切面本身不吞掉 D 的返回值；积分结算失败只记录错误日志，不回滚已完成并已提交的销售单，
 * 避免「单据已成交但接口报错」的错位提示。
 *
 * <p>后续 D 若在自身流程中直接调用 {@code MemberPointFacade}，删除本类即可（F 的
 * {@link MemberPointSettlementService} 与积分流水不受影响）。
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class SalePointSettlementAspect {

    /** 业务来源（写入积分流水备注，便于对账） */
    private static final String SOURCE_POS_SALE = "来源：POS 销售";
    private static final String SOURCE_POS_RETURN = "来源：POS 销售退货";

    @Resource
    private MemberPointSettlementService memberPointSettlementService;

    @Resource
    private SaleOrderMapper saleOrderMapper;

    @Resource
    private SaleReturnMapper saleReturnMapper;

    @Resource
    private MemberPointSaleLinkMapper memberPointSaleLinkMapper;

    /**
     * POS 销售完成：赠送积分（幂等键 = 销售单号）
     */
    @Around("execution(* cn.iocoder.yudao.module.pharmacy.service.sale.SaleOrderService+.createSaleOrder(..))")
    public Object aroundCreateSaleOrder(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            settleSaleOrderPoints(result);
        } catch (Exception ex) {
            // 销售单已提交，这里不能让收银台收到失败提示；仅记录错误，等待人工核对
            log.error("[aroundCreateSaleOrder][销售单积分结算失败，销售单 id({})]", result, ex);
        }
        return result;
    }

    /**
     * POS 退货完成：按实际退货金额比例回退积分（幂等键 = 退货单号）
     */
    @Around("execution(* cn.iocoder.yudao.module.pharmacy.service.sale.SaleReturnService+.createReturn(..))")
    public Object aroundCreateReturn(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            refundSaleReturnPoints(result);
        } catch (Exception ex) {
            log.error("[aroundCreateReturn][退货积分回退失败，退货单 id({})]", result, ex);
        }
        return result;
    }

    // ========== 销售赠送 ==========

    private void settleSaleOrderPoints(Object result) {
        if (!(result instanceof Long orderId)) {
            return;
        }
        PhSaleOrderDO order = saleOrderMapper.selectById(orderId);
        if (order == null || order.getMemberId() == null) {
            // 散客单不参与积分
            return;
        }
        // 赠送基数 = 本单实际成交金额（后端取数，不信任前端传入的积分值）
        BigDecimal orderAmount = resolveOrderAmount(order);
        int earned = memberPointSettlementService.settleSalePoints(order.getMemberId(), order.getOrderNo(),
                orderAmount, null);
        if (earned > 0) {
            memberPointSaleLinkMapper.updateSaleOrderPointsEarned(order.getId(), earned);
        }
        log.info("[settleSaleOrderPoints][销售单({}) 会员({}) 成交金额({}) 赠送积分({})，{}]",
                order.getOrderNo(), order.getMemberId(), orderAmount, earned, SOURCE_POS_SALE);
    }

    // ========== 退货回退 ==========

    private void refundSaleReturnPoints(Object result) {
        if (!(result instanceof Long returnId)) {
            return;
        }
        PhSaleReturnDO saleReturn = saleReturnMapper.selectById(returnId);
        if (saleReturn == null || saleReturn.getSaleOrderId() == null) {
            return;
        }
        PhSaleOrderDO order = saleOrderMapper.selectById(saleReturn.getSaleOrderId());
        if (order == null || order.getMemberId() == null) {
            return;
        }
        BigDecimal orderAmount = resolveOrderAmount(order);
        BigDecimal returnAmount = saleReturn.getTotalAmount() == null
                ? BigDecimal.ZERO : saleReturn.getTotalAmount();
        // 累计退货金额（含本次）≥ 原单金额即视为整单全退，全退时由积分服务结清剩余部分
        BigDecimal returnedTotal = memberPointSaleLinkMapper.sumReturnedAmountByOrderId(order.getId());
        boolean fullReturn = orderAmount.signum() > 0 && returnedTotal != null
                && returnedTotal.compareTo(orderAmount) >= 0;
        memberPointSettlementService.refundSalePoints(order.getMemberId(), order.getOrderNo(),
                saleReturn.getReturnNo(), orderAmount, returnAmount, fullReturn);
        log.info("[refundSaleReturnPoints][退货单({}) 原单({}) 退货金额({}) 原单金额({}) 整单全退({})，{}]",
                saleReturn.getReturnNo(), order.getOrderNo(), returnAmount, orderAmount, fullReturn,
                SOURCE_POS_RETURN);
    }

    /**
     * 取订单实际成交金额：优先实收，其次应付；都为空时返回 0
     */
    private BigDecimal resolveOrderAmount(PhSaleOrderDO order) {
        if (order.getPaidAmount() != null && order.getPaidAmount().signum() > 0) {
            return order.getPaidAmount();
        }
        return order.getPayableAmount() == null ? BigDecimal.ZERO : order.getPayableAmount();
    }

}
