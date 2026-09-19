package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.system.api.logger.LoginLogApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_USED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_LOGIN_DISABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会员快捷登录（手机号 + 验证码）单元测试
 *
 * 重点验证：验证码先于账号创建校验；验证失败不创建新会员；响应中不含验证码；
 * 验证成功后正常签发令牌并保留自动注册规则。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberAuthServiceImplTest {

    private static final String MOBILE = "13800138000";
    private static final String CODE = "246813";

    @Mock
    private MemberUserService memberUserService;

    @Mock
    private MemberSmsCodeService memberSmsCodeService;

    @Mock
    private OAuth2TokenCommonApi oauth2TokenApi;

    @Mock
    private LoginLogApi loginLogApi;

    @InjectMocks
    private MemberAuthServiceImpl memberAuthService;

    private MemberUserDO enabledUser() {
        MemberUserDO user = new MemberUserDO();
        user.setId(100L);
        user.setMobile(MOBILE);
        user.setStatus(0);
        return user;
    }

    private void mockTokenApi() {
        OAuth2AccessTokenRespDTO respDTO = new OAuth2AccessTokenRespDTO();
        respDTO.setUserId(100L);
        respDTO.setAccessToken("access-token-for-test");
        respDTO.setRefreshToken("refresh-token-for-test");
        respDTO.setExpiresTime(LocalDateTime.now().plusDays(30));
        when(oauth2TokenApi.createAccessToken(any())).thenReturn(respDTO);
    }

    @Test
    void testLoginOrRegister_correctCode_success() {
        mockTokenApi();
        when(memberUserService.createMemberUserIfAbsent(eq(MOBILE), any())).thenReturn(enabledUser());

        var respVO = memberAuthService.loginOrRegister(MOBILE, CODE);

        verify(memberSmsCodeService).verifyAndConsumeLoginCode(MOBILE, CODE);
        assertNotNull(respVO.getAccessToken());
        assertEquals(100L, respVO.getUserId());
    }

    @Test
    void testLoginOrRegister_blankCode_noMemberCreated() {
        doThrow(new ServiceException(cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants
                .PHARMACY_MEMBER_SMS_CODE_REQUIRED.getCode(), "请输入手机验证码"))
                .when(memberSmsCodeService).verifyAndConsumeLoginCode(MOBILE, null);

        assertThrows(ServiceException.class, () -> memberAuthService.loginOrRegister(MOBILE, null));
        // 验证码缺失时不得创建任何会员账号
        verify(memberUserService, never()).createMemberUserIfAbsent(anyString(), any());
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    void testLoginOrRegister_wrongCode_noMemberCreated() {
        doThrow(new ServiceException(cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants
                .PHARMACY_MEMBER_SMS_CODE_INVALID.getCode(), "验证码错误，请重新输入"))
                .when(memberSmsCodeService).verifyAndConsumeLoginCode(MOBILE, "000000");

        assertThrows(ServiceException.class, () -> memberAuthService.loginOrRegister(MOBILE, "000000"));
        verify(memberUserService, never()).createMemberUserIfAbsent(anyString(), any());
    }

    @Test
    void testLoginOrRegister_reusedCode_noMemberCreated() {
        doThrow(new ServiceException(PHARMACY_MEMBER_SMS_CODE_USED.getCode(), "验证码已被使用"))
                .when(memberSmsCodeService).verifyAndConsumeLoginCode(MOBILE, CODE);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberAuthService.loginOrRegister(MOBILE, CODE));
        assertEquals(PHARMACY_MEMBER_SMS_CODE_USED.getCode(), ex.getCode());
        verify(memberUserService, never()).createMemberUserIfAbsent(anyString(), any());
    }

    @Test
    void testLoginOrRegister_prodDisabled_devCodeInvalid() {
        doThrow(new ServiceException(PHARMACY_MEMBER_SMS_LOGIN_DISABLED.getCode(), "当前环境未启用短信登录"))
                .when(memberSmsCodeService).verifyAndConsumeLoginCode(MOBILE, CODE);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberAuthService.loginOrRegister(MOBILE, CODE));
        assertEquals(PHARMACY_MEMBER_SMS_LOGIN_DISABLED.getCode(), ex.getCode());
        verify(memberUserService, never()).createMemberUserIfAbsent(anyString(), any());
    }

    @Test
    void testLoginOrRegister_failureMessageAndResponseNeverExposeCode() {
        doThrow(new ServiceException(cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants
                .PHARMACY_MEMBER_SMS_CODE_INVALID.getCode(), "验证码错误，请重新输入"))
                .when(memberSmsCodeService).verifyAndConsumeLoginCode(MOBILE, "000000");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberAuthService.loginOrRegister(MOBILE, "000000"));
        // 错误信息中不得出现验证码本身
        assertFalse(ex.getMessage().contains("000000"), "错误信息不能回显验证码");
        assertFalse(ex.getMessage().contains(CODE), "错误信息不能回显验证码");

        // 成功响应体只有令牌相关字段，不含任何验证码字段
        mockTokenApi();
        when(memberUserService.createMemberUserIfAbsent(eq(MOBILE), any())).thenReturn(enabledUser());
        var respVO = memberAuthService.loginOrRegister(MOBILE, CODE);
        assertEquals(4, respVO.getClass().getDeclaredFields().length - countInheritedFields(respVO),
                "登录响应只包含 userId/accessToken/refreshToken/expiresTime 四个字段");
        assertFalse(respVO.toString().contains(CODE), "登录响应不能包含验证码");
    }

    private long countInheritedFields(Object obj) {
        return obj.getClass().getSuperclass() == Object.class ? 0 : obj.getClass().getSuperclass().getDeclaredFields().length;
    }

    @Test
    void testLoginOrRegister_disabledMember_doesNotIssueToken() {
        MemberUserDO disabled = enabledUser();
        disabled.setStatus(1);
        when(memberUserService.createMemberUserIfAbsent(eq(MOBILE), any())).thenReturn(disabled);

        assertThrows(ServiceException.class, () -> memberAuthService.loginOrRegister(MOBILE, CODE));
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    void testLoginOrRegister_existingMember_reusesSameAccount() {
        // 保留既有会员账号：已存在的手机号返回同一 userId，不新建
        mockTokenApi();
        when(memberUserService.createMemberUserIfAbsent(eq(MOBILE), any())).thenReturn(enabledUser());

        memberAuthService.loginOrRegister(MOBILE, CODE);

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(oauth2TokenApi).createAccessToken(any());
        verify(memberUserService).updateMemberUserLogin(eq(100L), any());
        verify(memberUserService, never()).createMemberUser(any());
    }

}
