package cn.iocoder.yudao.module.ai.service.pharmacy;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PharmacyAiContextService {
    private final EmployeeService employeeService;

    public Context requireContext(boolean requireStore) {
        var login = SecurityFrameworkUtils.getLoginUser();
        Long tenantId = TenantContextHolder.getTenantId();
        if (login == null || login.getId() == null || tenantId == null
                || !Objects.equals(tenantId, login.getTenantId()) || TenantContextHolder.isIgnore()) {
            throw new AccessDeniedException("AI 助手需要有效的登录及租户上下文");
        }
        EmployeeDO employee = employeeService.getEmployeeByUserId(login.getId());
        if (requireStore && (employee == null || employee.getStoreId() == null
                || !Objects.equals(employee.getStatus(), 1))) {
            throw new AccessDeniedException("当前用户未绑定在职门店员工，不能查询门店库存");
        }
        return new Context(tenantId, login.getId(), employee == null ? null : employee.getId(),
                employee == null ? null : employee.getStoreId());
    }

    public record Context(Long tenantId, Long userId, Long employeeId, Long storeId) { }
}
