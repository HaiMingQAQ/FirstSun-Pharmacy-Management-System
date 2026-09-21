package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.Objects;

/** Require a member principal bound to the current non-ignored tenant. */
public final class AppMemberAccess {
    private AppMemberAccess() { }
    public static Long requireMember() {
        var login = SecurityFrameworkUtils.getLoginUser();
        var auth = SecurityFrameworkUtils.getAuthentication();
        Long tenant = TenantContextHolder.getTenantId();
        if (login == null || login.getId() == null || auth == null || !auth.isAuthenticated()
                || tenant == null || tenant < 0 || TenantContextHolder.isIgnore()
                || !Objects.equals(login.getUserType(), UserTypeEnum.MEMBER.getValue())
                || !Objects.equals(login.getTenantId(), tenant)
                || (login.getVisitTenantId() != null && !Objects.equals(login.getVisitTenantId(), tenant))) {
            throw new AccessDeniedException("需要当前租户的有效会员登录");
        }
        return login.getId();
    }
}
