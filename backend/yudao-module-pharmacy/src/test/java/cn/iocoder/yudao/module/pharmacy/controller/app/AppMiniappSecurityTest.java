package cn.iocoder.yudao.module.pharmacy.controller.app;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.AppInventoryController;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.AppAvailableInventoryMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Controller logic only; the SQL projection is exercised separately on isolated MySQL. */
class AppMiniappSecurityTest {
    AppInventoryController controller;
    AppAvailableInventoryMapper mapper;
    @BeforeEach void setup() {
        mapper = mock(AppAvailableInventoryMapper.class);
        controller = new AppInventoryController();
        ReflectionTestUtils.setField(controller, "inventoryMapper", mapper);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    @Test void anonymousRequestStillRequiresTenant() {
        assertThrows(AccessDeniedException.class, () -> controller.getAvailableQty(1L, List.of(1L)));
        verifyNoInteractions(mapper);
    }
    @Test void ignoredTenantIsRejected() {
        TenantContextHolder.setTenantId(7L); TenantContextHolder.setIgnore(true);
        assertThrows(AccessDeniedException.class, () -> controller.getAvailableQty(1L, List.of(1L)));
        verifyNoInteractions(mapper);
    }
    @Test void emptyInvalidAndOversizedQueriesAreRejected() {
        TenantContextHolder.setTenantId(7L);
        assertThrows(IllegalArgumentException.class, () -> controller.getAvailableQty(1L, List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.getAvailableQty(0L, List.of(1L)));
        assertThrows(IllegalArgumentException.class, () -> controller.getAvailableQty(1L, List.of(-1L)));
        assertThrows(IllegalArgumentException.class, () -> controller.getAvailableQty(1L, java.util.Collections.nCopies(101, 1L)));
        verifyNoInteractions(mapper);
    }
    @Test void projectionReceivesOnlyCurrentTenantAndDistinctIds() {
        TenantContextHolder.setTenantId(7L);
        when(mapper.selectAvailable(7L, 9L, List.of(1L))).thenReturn(List.of());
        assertTrue(controller.getAvailableQty(9L, List.of(1L, 1L)).getData().isEmpty());
        verify(mapper).selectAvailable(7L, 9L, List.of(1L));
    }
    @Test void prescriptionRejectsAnonymousWrongOwnerAndPublicUpload() {
        var prescriptions = mock(cn.iocoder.yudao.module.pharmacy.dal.mysql.prescription.PrescRecordMapper.class);
        var app = new cn.iocoder.yudao.module.pharmacy.controller.app.prescription.AppPrescRecordController();
        ReflectionTestUtils.setField(app, "prescRecordMapper", prescriptions);
        assertThrows(AccessDeniedException.class, () -> app.getPrescRecord(1L));
        verifyNoInteractions(prescriptions);
        var login = new cn.iocoder.yudao.framework.security.core.LoginUser();
        login.setId(1L); login.setTenantId(7L);
        login.setUserType(cn.iocoder.yudao.framework.common.enums.UserTypeEnum.MEMBER.getValue());
        TenantContextHolder.setTenantId(7L);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login, null, List.of()));
        var record = new cn.iocoder.yudao.module.pharmacy.dal.dataobject.prescription.PhPrescRecordDO();
        record.setWxMemberId(2L); when(prescriptions.selectById(1L)).thenReturn(record);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> app.getPrescRecord(1L));
        record.setWxMemberId(1L); record.setImages("[\"https://public.example/prescription.jpg\"]");
        assertTrue(app.getPrescRecord(1L).getData().getImages().isEmpty());
        var request = new cn.iocoder.yudao.module.pharmacy.controller.app.prescription.vo.AppPrescRecordCreateReqVO();
        request.setImages(List.of("https://public.example/prescription.jpg"));
        assertThrows(AccessDeniedException.class, () -> app.createPrescRecord(request));
        TenantContextHolder.setTenantId(8L);
        assertThrows(AccessDeniedException.class, () -> app.getPrescRecord(1L));
    }

}
