package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.member.MemberPointFacade;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacade;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SalePaymentMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PAY_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NOT_ALLOW;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_QTY_EXCEED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_REFUND_AMOUNT_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SaleReturnServiceImpl} 单元测试（D-2）：部分退货触发支付退款、退款金额服务端计算、
 * 现金退款记录、超量退货与重复退款拦截、E 未实现时回滚。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaleReturnServiceImplTest {

    @Mock
    private SaleOrderMapper saleOrderMapper;
    @Mock
    private SaleOrderLineMapper saleOrderLineMapper;
    @Mock
    private SaleReturnMapper saleReturnMapper;
    @Mock
    private SaleReturnLineMapper saleReturnLineMapper;
    @Mock
    private SalePaymentMapper salePaymentMapper;
    @Mock
    private InventoryFacade inventoryFacade;
    @Mock
    private PaymentFacade paymentFacade;
    @Mock
    private MemberPointFacade memberPointFacade;

    @InjectMocks
    private SaleReturnServiceImpl saleReturnService;

    private PhSaleOrderDO order;
    private PhSaleOrderLineDO orderLine;
    private PhSalePaymentDO wxPayment;   // 微信 60（payOrderId=2001）
    private PhSalePaymentDO cashPayment; // 现金 40（无渠道支付单）

    @BeforeEach
    void setUp() {
        order = new PhSaleOrderDO();
        order.setId(1L);
        order.setOrderNo("SO-1-20260909101000-001");
        order.setStoreId(1L);
        order.setCashierId(100L);
        order.setStatus(1); // 已完成
        order.setPayableAmount(new BigDecimal("100.00"));
        order.setPaidAmount(new BigDecimal("100.00"));
        order.setPointsDeduct(BigDecimal.ZERO);
        when(saleOrderMapper.selectById(1L)).thenReturn(order);

        orderLine = new PhSaleOrderLineDO();
        orderLine.setId(11L);
        orderLine.setOrderId(1L);
        orderLine.setDrugId(1L);
        orderLine.setBatchId(21L);
        orderLine.setLocationId(31L);
        orderLine.setQty(10);
        orderLine.setPrice(new BigDecimal("10.00"));
        orderLine.setReturnedQty(0);
        orderLine.setIsRx(0);
        when(saleOrderLineMapper.selectById(11L)).thenReturn(orderLine);

        wxPayment = new PhSalePaymentDO();
        wxPayment.setId(201L);
        wxPayment.setOrderId(1L);
        wxPayment.setPayMethod(2); // 微信
        wxPayment.setPayAmount(new BigDecimal("60.00"));
        wxPayment.setStatus(1);
        wxPayment.setPayOrderId(2001L);
        cashPayment = new PhSalePaymentDO();
        cashPayment.setId(202L);
        cashPayment.setOrderId(1L);
        cashPayment.setPayMethod(1); // 现金
        cashPayment.setPayAmount(new BigDecimal("40.00"));
        cashPayment.setStatus(1);
        when(salePaymentMapper.selectListByOrderId(1L)).thenReturn(Arrays.asList(wxPayment, cashPayment));

        when(saleReturnMapper.selectByReturnNo(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            invocation.getArgument(0, PhSaleReturnDO.class).setId(301L);
            return 1;
        }).when(saleReturnMapper).insert(any(PhSaleReturnDO.class));
        when(saleReturnLineMapper.insert(any(PhSaleReturnLineDO.class))).thenReturn(1);
        when(saleReturnLineMapper.selectSumQtyBySaleLineId(anyLong())).thenReturn(0);
    }

    private SaleReturnSaveReqVO buildReqVO(int qty) {
        SaleReturnSaveReqVO reqVO = new SaleReturnSaveReqVO();
        reqVO.setSaleOrderId(1L);
        reqVO.setReturnType(0);
        reqVO.setReason(0);
        reqVO.setRefundMethod(0); // 0 原路
        SaleReturnSaveReqVO.Item item = new SaleReturnSaveReqVO.Item();
        item.setSaleOrderLineId(11L);
        item.setQty(qty);
        reqVO.setItems(Collections.singletonList(item));
        return reqVO;
    }

    // ==================== D-2：退款 ====================

    @Test
    void testCreateReturn_partialRefund_callsPaymentFacade() {
        // 部分退货 3 件（30 元，原路）：仅微信（有渠道支付单）调 E 退款；金额按占比 60% = 18 元
        saleReturnService.createReturn(buildReqVO(3));
        verify(paymentFacade).refund(eq(2001L), anyString(), eq(1800), anyString());
        // 两笔支付明细都写退款记录
        ArgumentCaptor<PhSalePaymentDO> captor = ArgumentCaptor.forClass(PhSalePaymentDO.class);
        verify(salePaymentMapper, times(2)).updateById(captor.capture());
        for (PhSalePaymentDO update : captor.getAllValues()) {
            assertNotNull(update.getRefundNo());
            assertNotNull(update.getRefundAt());
            assertTrue(update.getRefundNo().startsWith("RF-"));
        }
        // 部分退：支付明细状态不更新（update 中 status=null，DB 保持原状态 1），
        // 避免阻断后续剩余数量的退货退款
        for (PhSalePaymentDO update : captor.getAllValues()) {
            assertEquals(null, update.getStatus());
        }
        // 库存回补仍按原销售流水执行
        verify(inventoryFacade).returnBack(eq(1L), any());
    }

    @Test
    void testCreateReturn_refundAmount_allocatedByRatio() {
        // 退 3 件 30 元：微信 60% → 18.00（1800 分）；现金 40% → 12.00（本地记录）
        saleReturnService.createReturn(buildReqVO(3));
        ArgumentCaptor<Integer> fenCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(paymentFacade).refund(eq(2001L), anyString(), fenCaptor.capture(), anyString());
        assertEquals(1800, fenCaptor.getValue());
        // 两笔支付明细都落退款记录
        ArgumentCaptor<PhSalePaymentDO> captor = ArgumentCaptor.forClass(PhSalePaymentDO.class);
        verify(salePaymentMapper, times(2)).updateById(captor.capture());
        for (PhSalePaymentDO update : captor.getAllValues()) {
            assertNotNull(update.getRefundNo());
            assertNotNull(update.getRefundAt());
        }
    }

    @Test
    void testCreateReturn_fullReturn_paymentStatusRefunded() {
        // 整单全退 10 件（100 元）：微信全额 60 元调 E，两笔支付明细状态置 3（已退款）
        saleReturnService.createReturn(buildReqVO(10));
        verify(paymentFacade).refund(eq(2001L), anyString(), eq(6000), anyString());
        ArgumentCaptor<PhSalePaymentDO> captor = ArgumentCaptor.forClass(PhSalePaymentDO.class);
        verify(salePaymentMapper, times(2)).updateById(captor.capture());
        for (PhSalePaymentDO update : captor.getAllValues()) {
            assertEquals(3, update.getStatus());
            assertNotNull(update.getRefundNo());
            assertNotNull(update.getRefundAt());
        }
    }

    @Test
    void testCreateReturn_cashOnly_hasRefundRecord_noChannelCall() {
        // 纯现金订单（现金 100 元全额支付）整单全退：不调渠道（无渠道支付单），
        // 但支付明细必须有退款记录与状态变化
        cashPayment.setPayAmount(new BigDecimal("100.00"));
        when(salePaymentMapper.selectListByOrderId(1L))
                .thenReturn(Collections.singletonList(cashPayment));
        saleReturnService.createReturn(buildReqVO(10));
        verify(paymentFacade, never()).refund(any(), anyString(), any(), anyString());
        ArgumentCaptor<PhSalePaymentDO> captor = ArgumentCaptor.forClass(PhSalePaymentDO.class);
        verify(salePaymentMapper).updateById(captor.capture());
        PhSalePaymentDO update = captor.getValue();
        assertNotNull(update.getRefundNo());
        assertNotNull(update.getRefundAt());
        assertEquals(3, update.getStatus());
    }

    @Test
    void testCreateReturn_cashRefundMethod_noChannelCall() {
        // 指定现金退款（refundMethod=1）：即使有微信渠道支付单也不调渠道，全部本地记录
        SaleReturnSaveReqVO reqVO = buildReqVO(3);
        reqVO.setRefundMethod(1);
        saleReturnService.createReturn(reqVO);
        verify(paymentFacade, never()).refund(any(), anyString(), any(), anyString());
        ArgumentCaptor<PhSalePaymentDO> captor = ArgumentCaptor.forClass(PhSalePaymentDO.class);
        verify(salePaymentMapper, times(2)).updateById(captor.capture());
        for (PhSalePaymentDO update : captor.getAllValues()) {
            assertNotNull(update.getRefundNo());
            assertNotNull(update.getRefundAt());
        }
    }

    // ==================== D-2：拦截与回滚 ====================

    @Test
    void testCreateReturn_qtyExceed_rejected() {
        // 超量退货 11 件 > 可退 10 件 → 拒绝，不退款不回补
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(buildReqVO(11)));
        assertEquals(SALE_RETURN_QTY_EXCEED.getCode(), ex.getCode());
        verify(paymentFacade, never()).refund(any(), anyString(), any(), anyString());
        verify(salePaymentMapper, never()).updateById(any(PhSalePaymentDO.class));
        verify(inventoryFacade, never()).returnBack(anyLong(), any());
    }

    @Test
    void testCreateReturn_fullRefundedOrder_notAllowed() {
        // 已全额退款的订单（status=2）禁止再次退货退款
        order.setStatus(2);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(buildReqVO(3)));
        assertEquals(SALE_RETURN_NOT_ALLOW.getCode(), ex.getCode());
        verify(paymentFacade, never()).refund(any(), anyString(), any(), anyString());
    }

    @Test
    void testCreateReturn_noRefundablePayment_rejected() {
        // 原单无成功支付明细 → 退款金额无法分配，拒绝
        when(salePaymentMapper.selectListByOrderId(1L)).thenReturn(Collections.emptyList());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(buildReqVO(3)));
        assertEquals(SALE_RETURN_REFUND_AMOUNT_INVALID.getCode(), ex.getCode());
        verify(inventoryFacade, never()).returnBack(anyLong(), any());
    }

    @Test
    void testCreateReturn_paymentUnavailable_rollback() {
        // E 支付门面未实现（UnsupportedOperationException）→ PAY_SERVICE_UNAVAILABLE，事务回滚
        doThrow(new UnsupportedOperationException("E 支付未实现"))
                .when(paymentFacade).refund(anyLong(), anyString(), any(), anyString());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(buildReqVO(3)));
        assertEquals(PAY_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
        // 库存回补不得执行，退货单不得标记完成
        verify(inventoryFacade, never()).returnBack(anyLong(), any());
        verify(saleReturnMapper, never()).updateById(any(PhSaleReturnDO.class));
    }

    @Test
    void testCreateReturn_inventoryUnavailable_rollback() {
        // C 库存回补失败 → INV_SERVICE_UNAVAILABLE，事务整体回滚
        doThrow(new UnsupportedOperationException("C 库存未实现"))
                .when(inventoryFacade).returnBack(anyLong(), any());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(buildReqVO(3)));
        assertEquals(INV_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
        verify(saleReturnMapper, never()).updateById(any(PhSaleReturnDO.class));
    }

    @Test
    void testCreateReturn_success_marksReturnDone() {
        // 全部依赖成功后：退货单标记完成（status=3）、退款成功（refundStatus=2）
        saleReturnService.createReturn(buildReqVO(3));
        ArgumentCaptor<PhSaleReturnDO> captor = ArgumentCaptor.forClass(PhSaleReturnDO.class);
        verify(saleReturnMapper).updateById(captor.capture());
        assertEquals(3, captor.getValue().getStatus());
        assertEquals(2, captor.getValue().getRefundStatus());
        assertNotNull(captor.getValue().getReturnAt());
        // 原单行已退数量累加
        ArgumentCaptor<PhSaleOrderLineDO> lineCaptor = ArgumentCaptor.forClass(PhSaleOrderLineDO.class);
        verify(saleOrderLineMapper).updateById(lineCaptor.capture());
        assertEquals(3, lineCaptor.getValue().getReturnedQty());
    }
}
