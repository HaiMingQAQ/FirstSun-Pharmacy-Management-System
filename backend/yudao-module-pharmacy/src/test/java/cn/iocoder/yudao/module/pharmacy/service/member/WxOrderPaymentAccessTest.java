package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pay.dal.dataobject.app.PayAppDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.order.PayOrderDO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.service.app.PayAppService;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderPaymentMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WxOrderPaymentAccessTest {
    WxOrderPaymentMapper mapper;
    WxOrderPaymentAccess access;
    WxOrderDO order;
    PayOrderDO payment;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        mapper = mock(WxOrderPaymentMapper.class);
        var apps = mock(PayAppService.class);
        var app = new PayAppDO(); app.setId(9L);
        when(apps.validPayApp("firstsun")).thenReturn(app);
        access = new WxOrderPaymentAccess(mapper, apps);
        ReflectionTestUtils.setField(access, "appKey", "firstsun");
        order = new WxOrderDO(); order.setId(100L); order.setMemberId(1L);
        order.setStoreId(4L); order.setOrderNo("WX100"); order.setPayNo("50");
        order.setPayStatus(1); order.setStatus(1); order.setPayableAmount(new BigDecimal("12.34"));
        payment = new PayOrderDO(); payment.setId(50L); payment.setAppId(9L);
        payment.setUserId(1L); payment.setUserType(UserTypeEnum.MEMBER.getValue());
        payment.setMerchantOrderId("WX100"); payment.setPrice(1234); payment.setRefundPrice(0);
        payment.setStatus(PayOrderStatusEnum.SUCCESS.getStatus()); payment.setSuccessTime(LocalDateTime.now());
        when(mapper.lockOrder(100L, 7L)).thenReturn(order);
        when(mapper.lockPayment(50L, 7L)).thenReturn(payment);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void authorizesOnlyPersistedMatchingPaidRecords() {
        assertSame(order, access.requirePaidOrder(100L, 50L));
        var sequence = inOrder(mapper);
        sequence.verify(mapper).lockOrder(100L, 7L);
        sequence.verify(mapper).lockPayment(50L, 7L);
    }
    @Test void rejectsEachPaymentBindingMismatch() {
        List<Consumer<PayOrderDO>> mutations = List.of(p -> p.setAppId(10L), p -> p.setUserId(2L),
                p -> p.setUserType(UserTypeEnum.ADMIN.getValue()), p -> p.setMerchantOrderId("OTHER"),
                p -> p.setPrice(1233));
        for (var mutate : mutations) {
            setup(); mutate.accept(payment);
            assertThrows(ServiceException.class, () -> access.requirePaidOrder(100L, 50L));
        }
    }
    @Test void rejectsUnsettledOrRefundedPayment() {
        List<Consumer<PayOrderDO>> mutations = List.of(p -> p.setStatus(0), p -> p.setSuccessTime(null),
                p -> p.setRefundPrice(1), p -> p.setRefundPrice(null));
        for (var mutate : mutations) {
            setup(); mutate.accept(payment);
            assertThrows(ServiceException.class, () -> access.requirePaidOrder(100L, 50L));
        }
    }
    @Test void rejectsInvalidOrderStateAndLink() {
        List<Consumer<WxOrderDO>> mutations = List.of(o -> o.setPayNo("51"), o -> o.setPayStatus(0),
                o -> o.setStatus(-1), o -> o.setStoreId(null));
        for (var mutate : mutations) {
            setup(); mutate.accept(order);
            assertThrows(ServiceException.class, () -> access.requirePaidOrder(100L, 50L));
            verify(mapper, never()).lockPayment(50L, 7L);
        }
    }
    @Test void rejectsMissingOrCrossTenantRecords() {
        TenantContextHolder.setTenantId(8L);
        assertThrows(ServiceException.class, () -> access.requirePaidOrder(100L, 50L));
        verify(mapper).lockOrder(100L, 8L);
        TenantContextHolder.setTenantId(7L); when(mapper.lockPayment(50L, 7L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> access.requirePaidOrder(100L, 50L));
    }
    @Test void rejectsAbsentOrIgnoredTenantBeforeQuery() {
        TenantContextHolder.clear();
        assertThrows(AccessDeniedException.class, () -> access.lockOrder(100L));
        TenantContextHolder.setTenantId(7L); TenantContextHolder.setIgnore(true);
        assertThrows(AccessDeniedException.class, () -> access.lockOrder(100L));
        verifyNoInteractions(mapper);
    }
    @Test void amountMustBePositiveExactServerCents() {
        for (String value : List.of("0", "-1", "1.001", "21474836.48")) {
            order.setPayableAmount(new BigDecimal(value));
            assertThrows(ServiceException.class, () -> WxOrderPaymentAccess.amountFen(order));
        }
        order.setPayableAmount(null);
        assertThrows(ServiceException.class, () -> WxOrderPaymentAccess.amountFen(order));
    }
}
