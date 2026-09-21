package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Mockito service regression tests; not HTTP, DB transactions or concurrent DB proof. */
class WxOrderMemberActionTest {
    WxOrderMapper mapper;
    MemberPointSettlementService points;
    WxOrderServiceImpl service;
    Environment environment;
    LoginUser login;
    WxOrderDO order;

    @BeforeAll static void metadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WxOrderDO.class);
    }
    @BeforeEach void setup() {
        mapper = mock(WxOrderMapper.class); points = mock(MemberPointSettlementService.class);
        environment = mock(Environment.class); service = new WxOrderServiceImpl();
        ReflectionTestUtils.setField(service, "wxOrderMapper", mapper);
        ReflectionTestUtils.setField(service, "memberPointSettlementService", points);
        ReflectionTestUtils.setField(service, "environment", environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        login = new LoginUser(); login.setId(1L); login.setTenantId(7L);
        login.setUserType(UserTypeEnum.MEMBER.getValue());
        TenantContextHolder.setTenantId(7L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(login, null, List.of()));
        order = new WxOrderDO(); order.setId(100L); order.setMemberId(1L);
        order.setOrderType(1); order.setPayStatus(1); order.setStatus(3);
        order.setOrderNo("WX-test-100"); order.setPayableAmount(new BigDecimal("53.60"));
        when(mapper.selectById(100L)).thenReturn(order);
        when(mapper.update(any(), any())).thenReturn(1);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }

    @Test void mockPaymentDisabledByDefault() {
        assertThrows(AccessDeniedException.class, () -> service.simulatePayWxOrderByMember(100L));
        verifyNoInteractions(mapper, points);
    }
    @Test void enabledFlagCannotOverrideProductionOrUnknownProfiles() {
        ReflectionTestUtils.setField(service, "mockPaymentEnabled", true);
        for (String[] profiles : List.of(new String[]{"prod"}, new String[]{"dev", "prod"}, new String[]{"staging"}, new String[0])) {
            when(environment.getActiveProfiles()).thenReturn(profiles);
            assertThrows(AccessDeniedException.class, () -> service.simulatePayWxOrderByMember(100L));
        }
        verifyNoInteractions(mapper, points);
    }
    @Test void enabledDevelopmentStillRejectsOtherOwnerBeforePayment() {
        ReflectionTestUtils.setField(service, "mockPaymentEnabled", true);
        var access = mock(WxOrderPaymentAccess.class);
        ReflectionTestUtils.setField(service, "orderPaymentAccess", access);
        order.setStatus(0); order.setPayStatus(0); order.setMemberId(2L);
        when(access.lockOrder(100L)).thenReturn(order);
        assertThrows(ServiceException.class, () -> service.simulatePayWxOrderByMember(100L));
        verify(access, never()).lockPayment(any(), any());
        verify(mapper, never()).update(any(), any()); verifyNoInteractions(points);
    }
    @Test void confirmDeliveryCompletesAndEarnsPointsOnce() {
        when(points.earnSalePoints(1L, "WX-test-100", new BigDecimal("53.60"))).thenReturn(53);
        service.confirmReceiveWxOrderByMember(100L);
        var capture = org.mockito.ArgumentCaptor.forClass(WxOrderDO.class);
        verify(mapper).update(capture.capture(), any());
        assertEquals(4, capture.getValue().getStatus()); assertNotNull(capture.getValue().getFinishAt());
        verify(mapper).updateById(argThat((WxOrderDO value) -> value.getPointEarned() == 53));
        order.setStatus(4); service.confirmReceiveWxOrderByMember(100L);
        verify(points, times(1)).earnSalePoints(anyLong(), anyString(), any());
    }
    @Test void cannotConfirmPickupIncludingCompletedPickup() {
        order.setOrderType(0);
        for (int status : List.of(3, 4)) {
            order.setStatus(status);
            assertThrows(ServiceException.class, () -> service.confirmReceiveWxOrderByMember(100L));
        }
        verify(mapper, never()).update(any(), any()); verifyNoInteractions(points);
    }
    @Test void rejectsEveryStateExceptReadyAndCompleted() {
        for (int status : List.of(-1, 0, 1, 2)) {
            order.setStatus(status);
            assertThrows(ServiceException.class, () -> service.confirmReceiveWxOrderByMember(100L));
        }
        verify(mapper, never()).update(any(), any()); verifyNoInteractions(points);
    }
    @Test void rejectsUnpaidAndRefundedEvenIfCompleted() {
        order.setStatus(4);
        for (int paid : List.of(0, 2)) {
            order.setPayStatus(paid);
            assertThrows(ServiceException.class, () -> service.confirmReceiveWxOrderByMember(100L));
        }
        verify(mapper, never()).update(any(), any()); verifyNoInteractions(points);
    }
    @Test void rejectsOtherOwner() {
        order.setMemberId(2L);
        assertThrows(ServiceException.class, () -> service.confirmReceiveWxOrderByMember(100L));
        verify(mapper, never()).update(any(), any()); verifyNoInteractions(points);
    }
    @Test void rejectsWrongTenantAndIgnoredTenantBeforeLookup() {
        TenantContextHolder.setTenantId(8L);
        assertThrows(AccessDeniedException.class, () -> service.confirmReceiveWxOrderByMember(100L));
        TenantContextHolder.setTenantId(7L); TenantContextHolder.setIgnore(true);
        assertThrows(AccessDeniedException.class, () -> service.confirmReceiveWxOrderByMember(100L));
        verify(mapper, never()).selectById(anyLong());
    }
    @Test void rejectsAnonymousAndAdminPrincipal() {
        login.setUserType(UserTypeEnum.ADMIN.getValue());
        assertThrows(AccessDeniedException.class, () -> service.confirmReceiveWxOrderByMember(100L));
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> service.confirmReceiveWxOrderByMember(100L));
        verify(mapper, never()).selectById(anyLong());
    }
    @Test void conditionalUpdateLoserDoesNotEarnPoints() {
        when(mapper.update(any(), any())).thenReturn(0);
        var completed = new WxOrderDO(); completed.setMemberId(1L); completed.setOrderType(1);
        completed.setPayStatus(1); completed.setStatus(4);
        when(mapper.selectById(100L)).thenReturn(order, completed);
        service.confirmReceiveWxOrderByMember(100L); verifyNoInteractions(points);
    }
    @Test void concurrentCancellationIsNotReportedAsSuccessfulReceipt() {
        when(mapper.update(any(), any())).thenReturn(0);
        var cancelled = new WxOrderDO(); cancelled.setMemberId(1L); cancelled.setStatus(-1);
        when(mapper.selectById(100L)).thenReturn(order, cancelled);
        assertThrows(ServiceException.class, () -> service.confirmReceiveWxOrderByMember(100L));
        verifyNoInteractions(points);
    }
    @Test void prescriptionReferenceRequiresOwnerStoreAndApprovalBeforeOrderWrites() {
        var prescriptions = mock(cn.iocoder.yudao.module.pharmacy.dal.mysql.prescription.PrescRecordMapper.class);
        ReflectionTestUtils.setField(service, "prescRecordMapper", prescriptions);
        var prescription = new cn.iocoder.yudao.module.pharmacy.dal.dataobject.prescription.PhPrescRecordDO();
        prescription.setWxMemberId(2L); prescription.setStoreId(9L);
        prescription.setStatus(0); prescription.setReviewStatus(1);
        when(prescriptions.selectById(20L)).thenReturn(prescription);
        assertThrows(ServiceException.class, () -> service.createOrderFromCart(1L, 9L, 0, null, 20L, null, 0));
        prescription.setWxMemberId(1L); prescription.setStoreId(10L);
        assertThrows(ServiceException.class, () -> service.createOrderFromCart(1L, 9L, 0, null, 20L, null, 0));
        prescription.setStoreId(9L); prescription.setReviewStatus(0);
        assertThrows(ServiceException.class, () -> service.createOrderFromCart(1L, 9L, 0, null, 20L, null, 0));
        prescription.setReviewStatus(2);
        assertThrows(ServiceException.class, () -> service.createOrderFromCart(1L, 9L, 0, null, 20L, null, 0));
        prescription.setReviewStatus(1); prescription.setStatus(2);
        assertThrows(ServiceException.class, () -> service.createOrderFromCart(1L, 9L, 0, null, 20L, null, 0));
        verifyNoInteractions(mapper, points);
    }

}
