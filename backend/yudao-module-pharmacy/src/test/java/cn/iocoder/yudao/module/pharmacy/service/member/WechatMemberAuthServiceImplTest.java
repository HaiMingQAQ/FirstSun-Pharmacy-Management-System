package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.config.WechatLoginProperties;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberWechatLoginRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.system.api.logger.LoginLogApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_USER_DISABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 微信登录替身测试：不访问微信、MySQL 或 Redis。
 * 并发场景仅验证唯一键冲突后的重新读取分支；真实回滚仍需 MySQL 集成测试。
 */
@ExtendWith(MockitoExtension.class)
class WechatMemberAuthServiceImplTest {

    private static final Long TRUSTED_TENANT_ID = 99L;
    private static final String APP_ID = "wx-test-app";
    private static final String OPENID = "openid-test";

    @Mock
    private MemberUserService memberUserService;
    @Mock
    private MemberSmsCodeService memberSmsCodeService;
    @Mock
    private OAuth2TokenCommonApi oauth2TokenApi;
    @Mock
    private LoginLogApi loginLogApi;
    @Mock
    private WechatMiniAppCodeVerifier codeVerifier;
    @Mock
    private WechatIdentityBindingService identityBindingService;

    @InjectMocks
    private MemberAuthServiceImpl memberAuthService;

    private final WechatLoginProperties properties = new WechatLoginProperties();

    @BeforeEach
    void setUp() {
        properties.setTenantId(TRUSTED_TENANT_ID);
        properties.setAppId(APP_ID);
        injectProperties();
        when(codeVerifier.verifyAndGetOpenid("code-1")).thenReturn(OPENID);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void firstLogin_createsAndIssuesTokenWithoutPhone() {
        MemberUserDO user = enabledUser(101L);
        when(identityBindingService.findMember(APP_ID, OPENID, TRUSTED_TENANT_ID)).thenReturn(null);
        when(identityBindingService.findOrCreate(eq(APP_ID), eq(OPENID), eq(TRUSTED_TENANT_ID), any()))
                .thenReturn(user);
        mockToken(101L);

        AppMemberWechatLoginRespVO response = memberAuthService.wechatLogin("code-1");

        assertEquals(101L, response.getUserId());
        assertEquals(OPENID, response.getOpenid());
        verify(identityBindingService).findOrCreate(eq(APP_ID), eq(OPENID), eq(TRUSTED_TENANT_ID), any());
        verify(oauth2TokenApi).createAccessToken(any());
    }

    @Test
    void repeatLogin_reusesExistingBinding() {
        MemberUserDO user = enabledUser(102L);
        when(identityBindingService.findMember(APP_ID, OPENID, TRUSTED_TENANT_ID)).thenReturn(user);
        mockToken(102L);

        memberAuthService.wechatLogin("code-1");

        verify(identityBindingService, never()).findOrCreate(any(), any(), any(), any());
        verify(oauth2TokenApi).createAccessToken(any());
    }

    @Test
    void concurrentUniqueConflict_reloadsWinnerAndDoesNotIssueForCandidate() {
        MemberUserDO winner = enabledUser(103L);
        when(identityBindingService.findMember(APP_ID, OPENID, TRUSTED_TENANT_ID))
                .thenReturn(null, winner);
        when(identityBindingService.findOrCreate(eq(APP_ID), eq(OPENID), eq(TRUSTED_TENANT_ID), any()))
                .thenThrow(new DuplicateKeyException("unique identity"));
        mockToken(103L);

        AppMemberWechatLoginRespVO response = memberAuthService.wechatLogin("code-1");

        assertEquals(103L, response.getUserId());
        verify(oauth2TokenApi).createAccessToken(any());
        // 实际候选会员回滚由 findOrCreate 的事务代理保证；本替身测试只覆盖重读分支。
    }

    @Test
    void disabledMember_isRejectedBeforeToken() {
        MemberUserDO disabled = enabledUser(104L);
        disabled.setStatus(1);
        when(identityBindingService.findMember(APP_ID, OPENID, TRUSTED_TENANT_ID)).thenReturn(disabled);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> memberAuthService.wechatLogin("code-1"));

        assertEquals(PHARMACY_MEMBER_USER_DISABLED.getCode(), exception.getCode());
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    void clientTenantContext_isIgnoredForWechatLookup() {
        TenantContextHolder.setTenantId(777L);
        MemberUserDO user = enabledUser(105L);
        when(identityBindingService.findMember(APP_ID, OPENID, TRUSTED_TENANT_ID)).thenReturn(user);
        mockToken(105L);

        memberAuthService.wechatLogin("code-1");

        verify(identityBindingService).findMember(APP_ID, OPENID, TRUSTED_TENANT_ID);
        assertEquals(777L, TenantContextHolder.getTenantId());
    }

    @Test
    void trustedTenantContext_disablesIgnoreDuringPersistenceAndRestoresIt() {
        TenantContextHolder.setTenantId(777L);
        TenantContextHolder.setIgnore(true);
        MemberUserDO user = enabledUser(106L);
        when(identityBindingService.findMember(APP_ID, OPENID, TRUSTED_TENANT_ID)).thenAnswer(invocation -> {
            assertEquals(TRUSTED_TENANT_ID, TenantContextHolder.getTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            return user;
        });
        mockToken(106L);

        memberAuthService.wechatLogin("code-1");

        assertEquals(777L, TenantContextHolder.getTenantId());
        assertTrue(TenantContextHolder.isIgnore());
    }

    private MemberUserDO enabledUser(Long id) {
        MemberUserDO user = new MemberUserDO();
        user.setId(id);
        user.setTenantId(TRUSTED_TENANT_ID);
        user.setMobile(null);
        user.setStatus(0);
        return user;
    }

    private void mockToken(Long userId) {
        OAuth2AccessTokenRespDTO token = new OAuth2AccessTokenRespDTO();
        token.setUserId(userId);
        token.setAccessToken("access-token-test");
        token.setRefreshToken("refresh-token-test");
        token.setExpiresTime(LocalDateTime.now().plusHours(1));
        when(oauth2TokenApi.createAccessToken(any())).thenReturn(token);
    }

    private void injectProperties() {
        try {
            var field = MemberAuthServiceImpl.class.getDeclaredField("wechatLoginProperties");
            field.setAccessible(true);
            field.set(memberAuthService, properties);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

}
