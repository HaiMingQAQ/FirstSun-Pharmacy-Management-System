package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Authorization logic tests only. Does not verify A's SQL, token authentication or real MySQL. */
class InventoryReadAccessTest {
    private final EmployeeService employees = mock(EmployeeService.class);
    private final InventoryReadAccess access = new InventoryReadAccess(employees);
    private LoginUser login;

    @BeforeEach
    void setup() {
        login = new LoginUser();
        login.setId(11L);
        login.setTenantId(1L);
        login.setUserType(UserTypeEnum.ADMIN.getValue());
        TenantContextHolder.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, Collections.emptyList()));
        var employee = new EmployeeDO();
        employee.setId(5L);
        employee.setUserId(11L);
        employee.setStatus(1);
        employee.setStoreId(7L);
        when(employees.getEmployeeByUserId(11L)).thenReturn(employee);
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test void ownStoreResolvedWithoutClientStore() {
        assertEquals(new InventoryReadAccess.Scope(1, 7), access.requireScope(null));
    }
    @Test void ownStoreAllowed() {
        assertEquals(7, access.requireScope(7L).storeId());
    }
    @Test void otherStoreDenied() {
        assertThrows(AccessDeniedException.class, () -> access.requireScope(8L));
    }
    @Test void noEmployeeDenied() {
        when(employees.getEmployeeByUserId(11L)).thenReturn(null);
        assertThrows(AccessDeniedException.class, () -> access.requireScope(null));
    }
    @Test void inactiveEmployeeDenied() {
        employees.getEmployeeByUserId(11L).setStatus(0);
        assertThrows(AccessDeniedException.class, () -> access.requireScope(null));
    }
    @Test void mismatchedEmployeeIdentityDenied() {
        employees.getEmployeeByUserId(11L).setUserId(12L);
        assertThrows(AccessDeniedException.class, () -> access.requireScope(null));
    }
    @Test void ignoredTenantDeniedBeforeEmployeeLookup() {
        TenantContextHolder.setIgnore(true);
        assertThrows(AccessDeniedException.class, () -> access.requireScope(null));
        verify(employees, never()).getEmployeeByUserId(anyLong());
    }
    @Test void crossTenantVisitDenied() {
        login.setVisitTenantId(2L);
        assertThrows(AccessDeniedException.class, () -> access.requireScope(null));
        verify(employees, never()).getEmployeeByUserId(anyLong());
    }
    @Test void changedTenantContextDenied() {
        TenantContextHolder.setTenantId(2L);
        assertThrows(AccessDeniedException.class, () -> access.requireScope(null));
    }
    @Test void anonymousDenied() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> access.requireScope(null));
    }
    @Test void memberIdentityDenied() {
        login.setUserType(UserTypeEnum.MEMBER.getValue());
        assertThrows(AccessDeniedException.class, () -> access.requireScope(null));
    }
}
