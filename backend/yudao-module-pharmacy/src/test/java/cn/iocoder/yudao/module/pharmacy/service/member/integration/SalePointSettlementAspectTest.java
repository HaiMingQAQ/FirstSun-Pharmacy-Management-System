package cn.iocoder.yudao.module.pharmacy.service.member.integration;

import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberPointSaleLinkMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnMapper;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberPointSettlementService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * POS 销售 / 退货积分联动切面单测。
 *
 * <p>切面是 F 与 D 的集成接缝，这里锁住 4 件事：
 * <ol>
 *   <li>销售成功才结算积分，失败（含扣库失败、支付失败）不产生任何积分动作；</li>
 *   <li>赠送基数取后端单据金额，散客单不参与积分；</li>
 *   <li>退货按实际退货金额比例回退，整单全退时结清剩余；</li>
 *   <li>积分结算异常不回滚已提交的销售 / 退货单。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalePointSettlementAspectTest {

    private static final Long ORDER_ID = 10L;
    private static final Long RETURN_ID = 20L;
    private static final Long MEMBER_ID = 100L;
    private static final String ORDER_NO = "SO-20260919001";
    private static final String RETURN_NO = "SR-20260919001";

    @InjectMocks
    private SalePointSettlementAspect aspect;

    @Mock
    private MemberPointSettlementService memberPointSettlementService;

    @Mock
    private SaleOrderMapper saleOrderMapper;

    @Mock
    private SaleReturnMapper saleReturnMapper;

    @Mock
    private MemberPointSaleLinkMapper memberPointSaleLinkMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    // ========== 销售：赠送积分 ==========

    @Test
    @DisplayName("POS 销售完成：按后端成交金额赠送积分并回写单据")
    void testCreateSaleOrder_memberOrder_settlesPoints() throws Throwable {
        when(joinPoint.proceed()).thenReturn(ORDER_ID);
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(new BigDecimal("19.80")));
        when(memberPointSettlementService.settleSalePoints(MEMBER_ID, ORDER_NO, new BigDecimal("19.80"), null))
                .thenReturn(19);

        Object result = aspect.aroundCreateSaleOrder(joinPoint);

        assertEquals(ORDER_ID, result);
        verify(memberPointSettlementService).settleSalePoints(MEMBER_ID, ORDER_NO, new BigDecimal("19.80"), null);
        verify(memberPointSaleLinkMapper).updateSaleOrderPointsEarned(ORDER_ID, 19);
    }

    @Test
    @DisplayName("散客单不参与积分：不结算、不写单据")
    void testCreateSaleOrder_walkInOrder_skips() throws Throwable {
        when(joinPoint.proceed()).thenReturn(ORDER_ID);
        PhSaleOrderDO order = buildOrder(new BigDecimal("19.80"));
        order.setMemberId(null);
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        aspect.aroundCreateSaleOrder(joinPoint);

        verifyNoInteractions(memberPointSettlementService);
        verifyNoInteractions(memberPointSaleLinkMapper);
    }

    @Test
    @DisplayName("赠送 0 分时不回写单据（不产生 0 分流水）")
    void testCreateSaleOrder_zeroEarn_noWriteBack() throws Throwable {
        when(joinPoint.proceed()).thenReturn(ORDER_ID);
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(new BigDecimal("19.80")));
        when(memberPointSettlementService.settleSalePoints(any(), any(), any(), any())).thenReturn(0);

        aspect.aroundCreateSaleOrder(joinPoint);

        verify(memberPointSettlementService).settleSalePoints(eq(MEMBER_ID), eq(ORDER_NO), any(), eq(null));
        verify(memberPointSaleLinkMapper, never()).updateSaleOrderPointsEarned(any(), any());
    }

    @Test
    @DisplayName("实收为 0 时退回应付金额作为赠送基数")
    void testCreateSaleOrder_fallsBackToPayable() throws Throwable {
        when(joinPoint.proceed()).thenReturn(ORDER_ID);
        PhSaleOrderDO order = buildOrder(BigDecimal.ZERO);
        order.setPayableAmount(new BigDecimal("12.50"));
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(memberPointSettlementService.settleSalePoints(any(), any(), any(), any())).thenReturn(12);

        aspect.aroundCreateSaleOrder(joinPoint);

        verify(memberPointSettlementService).settleSalePoints(MEMBER_ID, ORDER_NO, new BigDecimal("12.50"), null);
    }

    @Test
    @DisplayName("销售失败（扣库 / 支付异常）时：异常向上抛出且没有任何积分动作")
    void testCreateSaleOrder_businessFailure_noPointAction() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("库存不足"));

        assertThrows(IllegalStateException.class, () -> aspect.aroundCreateSaleOrder(joinPoint));

        verifyNoInteractions(memberPointSettlementService);
        verifyNoInteractions(saleOrderMapper);
        verifyNoInteractions(memberPointSaleLinkMapper);
    }

    @Test
    @DisplayName("积分结算异常不影响已提交的销售单：接口仍返回销售单 id")
    void testCreateSaleOrder_settleFailure_saleStillSucceeds() throws Throwable {
        when(joinPoint.proceed()).thenReturn(ORDER_ID);
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(new BigDecimal("19.80")));
        when(memberPointSettlementService.settleSalePoints(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("积分服务异常"));

        Object result = aspect.aroundCreateSaleOrder(joinPoint);

        assertEquals(ORDER_ID, result);
        verify(memberPointSaleLinkMapper, never()).updateSaleOrderPointsEarned(any(), any());
    }

    @Test
    @DisplayName("返回值不是销售单 id 时直接跳过（不产生积分动作）")
    void testCreateSaleOrder_nonLongResult_skips() throws Throwable {
        when(joinPoint.proceed()).thenReturn(null);

        assertEquals(null, aspect.aroundCreateSaleOrder(joinPoint));

        verifyNoInteractions(memberPointSettlementService);
        verifyNoInteractions(saleOrderMapper);
    }

    // ========== 退货：按比例回退 ==========

    @Test
    @DisplayName("部分退货：按退货金额比例回退积分，且不是整单全退")
    void testCreateReturn_partialReturn_proportional() throws Throwable {
        when(joinPoint.proceed()).thenReturn(RETURN_ID);
        when(saleReturnMapper.selectById(RETURN_ID)).thenReturn(buildReturn(new BigDecimal("9.90")));
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(new BigDecimal("19.80")));
        when(memberPointSaleLinkMapper.sumReturnedAmountByOrderId(ORDER_ID)).thenReturn(new BigDecimal("9.90"));

        Object result = aspect.aroundCreateReturn(joinPoint);

        assertEquals(RETURN_ID, result);
        verify(memberPointSettlementService).refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("19.80"), new BigDecimal("9.90"), false);
    }

    @Test
    @DisplayName("全额退货：累计退货金额等于原单金额时按整单全退结清剩余积分")
    void testCreateReturn_fullReturn_settlesRemaining() throws Throwable {
        when(joinPoint.proceed()).thenReturn(RETURN_ID);
        when(saleReturnMapper.selectById(RETURN_ID)).thenReturn(buildReturn(new BigDecimal("9.90")));
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(new BigDecimal("19.80")));
        when(memberPointSaleLinkMapper.sumReturnedAmountByOrderId(ORDER_ID)).thenReturn(new BigDecimal("19.80"));

        aspect.aroundCreateReturn(joinPoint);

        verify(memberPointSettlementService).refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("19.80"), new BigDecimal("9.90"), true);
    }

    @Test
    @DisplayName("退货单幂等键使用退货单号：重复退货时由积分服务按唯一键拦截")
    void testCreateReturn_usesReturnNoAsIdempotentKey() throws Throwable {
        when(joinPoint.proceed()).thenReturn(RETURN_ID);
        when(saleReturnMapper.selectById(RETURN_ID)).thenReturn(buildReturn(new BigDecimal("5.00")));
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(new BigDecimal("19.80")));
        when(memberPointSaleLinkMapper.sumReturnedAmountByOrderId(ORDER_ID)).thenReturn(new BigDecimal("5.00"));

        aspect.aroundCreateReturn(joinPoint);
        aspect.aroundCreateReturn(joinPoint);

        // 两次调用传入完全相同的幂等键（会员 + 原单号 + 退货单号）
        verify(memberPointSettlementService,
                org.mockito.Mockito.times(2)).refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("19.80"), new BigDecimal("5.00"), false);
    }

    @Test
    @DisplayName("散客退货单不参与积分回退")
    void testCreateReturn_walkInOrder_skips() throws Throwable {
        when(joinPoint.proceed()).thenReturn(RETURN_ID);
        when(saleReturnMapper.selectById(RETURN_ID)).thenReturn(buildReturn(new BigDecimal("9.90")));
        PhSaleOrderDO order = buildOrder(new BigDecimal("19.80"));
        order.setMemberId(null);
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(order);

        aspect.aroundCreateReturn(joinPoint);

        verifyNoInteractions(memberPointSettlementService);
        verifyNoInteractions(memberPointSaleLinkMapper);
    }

    @Test
    @DisplayName("退货失败时：异常向上抛出且没有任何积分回退")
    void testCreateReturn_businessFailure_noPointAction() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("退货失败"));

        assertThrows(IllegalStateException.class, () -> aspect.aroundCreateReturn(joinPoint));

        verifyNoInteractions(memberPointSettlementService);
        verifyNoInteractions(saleReturnMapper);
    }

    @Test
    @DisplayName("积分回退异常不影响已提交的退货单")
    void testCreateReturn_refundFailure_returnStillSucceeds() throws Throwable {
        when(joinPoint.proceed()).thenReturn(RETURN_ID);
        when(saleReturnMapper.selectById(RETURN_ID)).thenReturn(buildReturn(new BigDecimal("9.90")));
        when(saleOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(new BigDecimal("19.80")));
        when(memberPointSaleLinkMapper.sumReturnedAmountByOrderId(ORDER_ID)).thenReturn(new BigDecimal("9.90"));
        org.mockito.Mockito.doThrow(new RuntimeException("积分服务异常"))
                .when(memberPointSettlementService).refundSalePoints(any(), any(), any(), any(), any(),
                        org.mockito.ArgumentMatchers.anyBoolean());

        Object result = aspect.aroundCreateReturn(joinPoint);

        assertEquals(RETURN_ID, result);
    }

    // ========== 构造数据 ==========

    private PhSaleOrderDO buildOrder(BigDecimal paidAmount) {
        PhSaleOrderDO order = new PhSaleOrderDO();
        order.setId(ORDER_ID);
        order.setOrderNo(ORDER_NO);
        order.setMemberId(MEMBER_ID);
        order.setPaidAmount(paidAmount);
        order.setPayableAmount(paidAmount);
        return order;
    }

    private PhSaleReturnDO buildReturn(BigDecimal totalAmount) {
        PhSaleReturnDO saleReturn = new PhSaleReturnDO();
        saleReturn.setId(RETURN_ID);
        saleReturn.setReturnNo(RETURN_NO);
        saleReturn.setSaleOrderId(ORDER_ID);
        saleReturn.setTotalAmount(totalAmount);
        saleReturn.setStatus(3);
        return saleReturn;
    }

}
