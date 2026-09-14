package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.Objects;

/** C-local conservative scope: active employee's own store only; no admin/position bypass. */
@Service
@RequiredArgsConstructor
public class InventoryReadAccess {
    private final EmployeeService employeeService;

    public Scope requireScope(Long requestedStoreId) {
        Long tenantId = TenantContextHolder.getTenantId();
        var login = SecurityFrameworkUtils.getLoginUser();
        var authentication = SecurityFrameworkUtils.getAuthentication();
        if (tenantId == null || tenantId < 0 || login == null || login.getId() == null
                || authentication == null || !authentication.isAuthenticated()
                || !Objects.equals(login.getUserType(), UserTypeEnum.ADMIN.getValue())
                || !Objects.equals(tenantId, login.getTenantId())
                || (login.getVisitTenantId() != null && !Objects.equals(tenantId, login.getVisitTenantId()))
                || TenantContextHolder.isIgnore()) {
            throw new AccessDeniedException("库存作业需要有效登录及租户上下文");
        }
        Long userId = login.getId();
        // A's mapper uses the framework tenant interceptor and logical deletion.
        EmployeeDO employee = employeeService.getEmployeeByUserId(userId);
        if (employee == null || !Objects.equals(employee.getUserId(), userId)
                || !Objects.equals(employee.getStatus(), 1)
                || employee.getStoreId() == null || employee.getStoreId() <= 0
                || (requestedStoreId != null && !Objects.equals(requestedStoreId, employee.getStoreId()))) {
            throw new AccessDeniedException("未关联在职员工或无该门店库存权限");
        }
        return new Scope(tenantId, employee.getStoreId());
    }

    public record Scope(long tenantId, long storeId) { }
}
