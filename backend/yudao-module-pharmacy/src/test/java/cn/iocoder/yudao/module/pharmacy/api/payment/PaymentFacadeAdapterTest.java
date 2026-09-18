package cn.iocoder.yudao.module.pharmacy.api.payment;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;
import cn.iocoder.yudao.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import cn.iocoder.yudao.module.pay.dal.dataobject.app.PayAppDO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.service.app.PayAppService;
import cn.iocoder.yudao.module.pharmacy.api.payment.dto.PayOrderDTO;
import cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.pay.enums.ErrorCodeConstants.REFUND_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PAY_AMOUNT_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PAY_ORDER_CREATE_FAIL;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PAY_REFUND_AMOUNT_EXCEED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PAY_REFUND_CREATE_FAIL;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PAY_REFUND_ORDER_NOT_FOUND;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PAY_STATUS_UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PaymentFacadeAdapter} 单元测试：支付创建、退款、状态同步与幂等。
 * <p>
 * 覆盖组长验收要求的场景：正常支付、失败支付、金额错误、部分退款、超退拒绝、
 * 重复退款幂等、现金退货放行、原单不存在、状态映射（含重复回调幂等说明）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentFacadeAdapterTest {

    @Mock
    private PayOrderApi payOrderApi;
    @Mock
    private PayRefundApi payRefundApi;
    @Mock
    private PayAppService payAppService;

    @InjectMocks
    private PaymentFacadeAdapter paymentFacade;

    @BeforeEach
    void setUp() {
        // 默认有一个启用应用（appKey=firstsun）
        PayAppDO app = new PayAppDO();
        app.setId(1L);
        app.setAppKey("firstsun");
        when(payAppService.getAppList()).thenReturn(List.of(app));
    }

    private PayOrderDTO buildPayReq() {
        PayOrderDTO req = new PayOrderDTO();
        req.setBizNo("SO-1-20260918090000-001");
        req.setPriceFen(585000); // 5850.00 元
        req.setSubject("药店销售单");
        return req;
    }

    private PayOrderRespDTO buildPaidOrder() {
        PayOrderRespDTO order = new PayOrderRespDTO();
        order.setId(1001L);
        order.setMerchantOrderId("SO-1-20260918090000-001");
        order.setPrice(585000);
        order.setStatus(PayOrderStatusEnum.SUCCESS.getStatus());
        return order;
    }

    // ========== 支付创建 ==========

    @Test
    void testCreatePayOrder_success() {
        when(payOrderApi.createOrder(any(PayOrderCreateReqDTO.class))).thenReturn(1001L);
        String payNo = paymentFacade.createPayOrder(buildPayReq());
        assertEquals("1001", payNo);
        verify(payOrderApi).createOrder(any(PayOrderCreateReqDTO.class));
    }

    @Test
    void testCreatePayOrder_sameBizNo_idempotent() {
        // 同 bizNo 重复创建：支付模块幂等返回同一支付单（不重复扣款），门面不额外建单
        when(payOrderApi.createOrder(any(PayOrderCreateReqDTO.class))).thenReturn(1001L);
        PayOrderDTO req = buildPayReq();
        assertEquals("1001", paymentFacade.createPayOrder(req));
        assertEquals("1001", paymentFacade.createPayOrder(req));
        verify(payOrderApi, org.mockito.Mockito.times(2)).createOrder(any(PayOrderCreateReqDTO.class));
    }

    @Test
    void testCreatePayOrder_amountInvalid() {
        PayOrderDTO req = buildPayReq();
        req.setPriceFen(0);
        ServiceException ex = assertThrows(ServiceException.class, () -> paymentFacade.createPayOrder(req));
        assertEquals(PAY_AMOUNT_INVALID.getCode(), ex.getCode());
        verify(payOrderApi, never()).createOrder(any());
    }

    @Test
    void testCreatePayOrder_channelFail() {
        when(payOrderApi.createOrder(any(PayOrderCreateReqDTO.class)))
                .thenThrow(new RuntimeException("channel down"));
        ServiceException ex = assertThrows(ServiceException.class, () -> paymentFacade.createPayOrder(buildPayReq()));
        assertEquals(PAY_ORDER_CREATE_FAIL.getCode(), ex.getCode());
    }

    // ========== 退款 ==========

    @Test
    void testRefund_cash_noChannel_skip() {
        // 现金退货：无渠道支付单，门面直接放行，不发起渠道退款
        paymentFacade.refund(null, "REFUND-001", 1000, "现金退货");
        verify(payRefundApi, never()).createRefund(any());
    }

    @Test
    void testRefund_partial_success() {
        when(payOrderApi.getOrder(1001L)).thenReturn(buildPaidOrder());
        when(payRefundApi.createRefund(any(PayRefundCreateReqDTO.class))).thenReturn(2001L);
        // 部分退款：100.00 元 < 原 5850.00 元
        paymentFacade.refund(1001L, "REFUND-100", 10000, "部分退货");
        verify(payRefundApi).createRefund(any(PayRefundCreateReqDTO.class));
    }

    @Test
    void testRefund_fullAmount_success() {
        when(payOrderApi.getOrder(1001L)).thenReturn(buildPaidOrder());
        when(payRefundApi.createRefund(any(PayRefundCreateReqDTO.class))).thenReturn(2002L);
        // 全额退款：等于原支付金额
        paymentFacade.refund(1001L, "REFUND-101", 585000, "整单退货");
        verify(payRefundApi).createRefund(any(PayRefundCreateReqDTO.class));
    }

    @Test
    void testRefund_exceedAmount_rejected() {
        when(payOrderApi.getOrder(1001L)).thenReturn(buildPaidOrder());
        // 超退：6000.00 元 > 原 5850.00 元，必须拒绝
        ServiceException ex = assertThrows(ServiceException.class,
                () -> paymentFacade.refund(1001L, "REFUND-102", 600000, "恶意超退"));
        assertEquals(PAY_REFUND_AMOUNT_EXCEED.getCode(), ex.getCode());
        verify(payRefundApi, never()).createRefund(any());
    }

    @Test
    void testRefund_amountInvalid() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> paymentFacade.refund(1001L, "REFUND-103", -1, "金额为负"));
        assertEquals(PAY_AMOUNT_INVALID.getCode(), ex.getCode());
        verify(payRefundApi, never()).createRefund(any());
    }

    @Test
    void testRefund_orderNotFound() {
        when(payOrderApi.getOrder(9999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> paymentFacade.refund(9999L, "REFUND-104", 100, "原单不存在"));
        assertEquals(PAY_REFUND_ORDER_NOT_FOUND.getCode(), ex.getCode());
        verify(payRefundApi, never()).createRefund(any());
    }

    @Test
    void testRefund_duplicateRefundNo_idempotent() {
        when(payOrderApi.getOrder(1001L)).thenReturn(buildPaidOrder());
        // 第一次成功
        when(payRefundApi.createRefund(any(PayRefundCreateReqDTO.class))).thenReturn(2003L);
        paymentFacade.refund(1001L, "REFUND-200", 10000, "部分退货");
        // 第二次同 refundNo：支付模块唯一约束抛 REFUND_EXISTS，门面转为幂等放行，不重复扣款
        doThrow(new ServiceException(REFUND_EXISTS.getCode(), "已经存在退款单"))
                .when(payRefundApi).createRefund(any(PayRefundCreateReqDTO.class));
        paymentFacade.refund(1001L, "REFUND-200", 10000, "部分退货重试");
        // 两次调用都发起过 createRefund（第二次被唯一约束拦截后幂等放行），未抛出业务异常
        verify(payRefundApi, org.mockito.Mockito.times(2)).createRefund(any(PayRefundCreateReqDTO.class));
    }

    @Test
    void testRefund_channelFail_translated() {
        when(payOrderApi.getOrder(1001L)).thenReturn(buildPaidOrder());
        when(payRefundApi.createRefund(any(PayRefundCreateReqDTO.class)))
                .thenThrow(new RuntimeException("channel down"));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> paymentFacade.refund(1001L, "REFUND-300", 10000, "渠道异常"));
        assertEquals(PAY_REFUND_CREATE_FAIL.getCode(), ex.getCode());
    }

    // ========== 状态同步 / 查询 ==========

    @Test
    void testQueryStatus_waiting_returnsZero() {
        PayOrderRespDTO order = buildPaidOrder();
        order.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        when(payOrderApi.getOrder(1001L)).thenReturn(order);
        assertEquals(0, paymentFacade.queryStatus(1001L));
    }

    @Test
    void testQueryStatus_success_returnsOne() {
        when(payOrderApi.getOrder(1001L)).thenReturn(buildPaidOrder());
        assertEquals(1, paymentFacade.queryStatus(1001L));
    }

    @Test
    void testQueryStatus_refunded_returnsThree() {
        PayOrderRespDTO order = buildPaidOrder();
        order.setStatus(PayOrderStatusEnum.REFUND.getStatus());
        when(payOrderApi.getOrder(1001L)).thenReturn(order);
        assertEquals(3, paymentFacade.queryStatus(1001L));
    }

    @Test
    void testQueryStatus_closed_returnsTwo() {
        PayOrderRespDTO order = buildPaidOrder();
        order.setStatus(PayOrderStatusEnum.CLOSED.getStatus());
        when(payOrderApi.getOrder(1001L)).thenReturn(order);
        assertEquals(2, paymentFacade.queryStatus(1001L));
    }

    @Test
    void testQueryStatus_unknown() {
        when(payOrderApi.getOrder(9999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> paymentFacade.queryStatus(9999L));
        assertEquals(PAY_STATUS_UNKNOWN.getCode(), ex.getCode());
    }

    @Test
    void testQueryStatus_repeatCall_noSideEffect() {
        // 重复回调/重复查询：只读状态，无副作用（幂等读取）
        when(payOrderApi.getOrder(1001L)).thenReturn(buildPaidOrder());
        assertEquals(1, paymentFacade.queryStatus(1001L));
        assertEquals(1, paymentFacade.queryStatus(1001L));
        verify(payOrderApi, org.mockito.Mockito.times(2)).getOrder(1001L);
    }

    // ========== 边界：无支付应用配置 ==========

    @Test
    void testCreatePayOrder_defaultAppKeyWhenNoApp() {
        when(payAppService.getAppList()).thenReturn(Collections.emptyList());
        when(payOrderApi.createOrder(any(PayOrderCreateReqDTO.class))).thenReturn(1001L);
        String payNo = paymentFacade.createPayOrder(buildPayReq());
        assertEquals("1001", payNo);
    }
}
