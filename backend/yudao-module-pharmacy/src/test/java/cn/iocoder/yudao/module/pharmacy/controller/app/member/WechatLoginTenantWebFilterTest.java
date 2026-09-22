package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.security.TenantSecurityWebFilter;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.web.TenantContextWebFilter;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 通过实际租户 Web 过滤器链验证微信登录入口的边界。
 * Controller 上的 {@code @TenantIgnore} 会由自动配置转换成这里的 ignore URL 集合。
 */
class WechatLoginTenantWebFilterTest {

    private final TenantContextWebFilter contextFilter = new TenantContextWebFilter();
    private final TenantProperties tenantProperties = new TenantProperties();
    private final WebProperties webProperties = new WebProperties();
    private final GlobalExceptionHandler exceptionHandler = mock(GlobalExceptionHandler.class);
    private final TenantFrameworkService tenantFrameworkService = mock(TenantFrameworkService.class);

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void socialLoginWithoutClientTenant_reachesControllerAndMarksOnlyRequestIgnored() throws Exception {
        TenantSecurityWebFilter securityFilter = securityFilter();
        MockHttpServletRequest request = request("/app-api/member/auth/social-login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedController = new AtomicBoolean();
        AtomicBoolean ignoredAtController = new AtomicBoolean();
        FilterChain controller = (servletRequest, servletResponse) -> {
            reachedController.set(true);
            ignoredAtController.set(TenantContextHolder.isIgnore());
        };

        runThroughFilters(request, response, securityFilter, controller);

        assertTrue(reachedController.get());
        assertTrue(ignoredAtController.get());
        verifyNoInteractions(tenantFrameworkService);
    }

    @Test
    void memberProfileWithoutTenantHeader_isRejectedBeforeController() throws Exception {
        TenantSecurityWebFilter securityFilter = securityFilter();
        MockHttpServletRequest request = request("/app-api/member/user/get");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedController = new AtomicBoolean();
        FilterChain controller = (servletRequest, servletResponse) -> reachedController.set(true);

        runThroughFilters(request, response, securityFilter, controller);

        assertFalse(reachedController.get());
        assertTrue(response.getContentAsString().contains("请求的租户标识未传递，请进行排查"));
        verifyNoInteractions(tenantFrameworkService);
    }

    private TenantSecurityWebFilter securityFilter() {
        return new TenantSecurityWebFilter(webProperties, tenantProperties,
                Set.of("/app-api/member/auth/social-login"), exceptionHandler, tenantFrameworkService);
    }

    private void runThroughFilters(MockHttpServletRequest request, MockHttpServletResponse response,
                                   TenantSecurityWebFilter securityFilter, FilterChain controller) throws Exception {
        contextFilter.doFilter(request, response,
                (servletRequest, servletResponse) -> securityFilter.doFilter(servletRequest, servletResponse, controller));
    }

    private MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("POST", uri);
    }

}
