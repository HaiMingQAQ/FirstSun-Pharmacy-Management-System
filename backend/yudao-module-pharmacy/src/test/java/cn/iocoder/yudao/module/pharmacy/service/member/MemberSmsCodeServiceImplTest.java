package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.config.MemberSmsProperties;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberSmsCodeDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberSmsCodeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_EXPIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_USED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_DEV_CODE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_LOGIN_DISABLED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_USER_MOBILE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会员手机验证码 Service 单元测试
 *
 * 覆盖 F-1 验收要求：正确验证码可登录、缺失/错误/过期/重复使用/其他手机号被拒绝、
 * prod（未开启短信登录或未开启开发验证码）时开发验证码无效。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberSmsCodeServiceImplTest {

    private static final String MOBILE = "13800138000";
    private static final String DEV_CODE = "246813";

    @Mock
    private MemberSmsCodeMapper memberSmsCodeMapper;

    @Mock
    private MemberSmsProperties memberSmsProperties;

    @InjectMocks
    private MemberSmsCodeServiceImpl memberSmsCodeService;

    private void mockDevEnv(String devCode) {
        when(memberSmsProperties.isLoginEnabled()).thenReturn(true);
        when(memberSmsProperties.isDevCodeEnabled()).thenReturn(true);
        when(memberSmsProperties.getDevCode()).thenReturn(devCode);
        when(memberSmsProperties.getExpireTime()).thenReturn(Duration.ofMinutes(10));
    }

    // ========== 发送验证码 ==========

    @Test
    void testSendLoginCode_localDev_writesDevCodeFromEnv() {
        mockDevEnv(DEV_CODE);
        when(memberSmsCodeMapper.selectByMobileAndScene(MOBILE, 1)).thenReturn(null);

        memberSmsCodeService.sendLoginCode(MOBILE);

        ArgumentCaptor<MemberSmsCodeDO> captor = ArgumentCaptor.forClass(MemberSmsCodeDO.class);
        verify(memberSmsCodeMapper).insert(captor.capture());
        MemberSmsCodeDO saved = captor.getValue();
        assertEquals(MOBILE, saved.getMobile());
        assertEquals(1, saved.getScene(), "验证码必须与「登录」用途绑定");
        assertEquals(DEV_CODE, saved.getCode(), "验证码取自环境变量，不得硬编码");
        assertNotNull(saved.getExpireTime());
        assertTrue(saved.getExpireTime().isAfter(LocalDateTime.now()));
        assertNull(saved.getUsedTime(), "刚发送的验证码必须是未使用状态");
    }

    @Test
    void testSendLoginCode_resend_overwritesAndResetsUsedState() {
        mockDevEnv(DEV_CODE);
        MemberSmsCodeDO exists = new MemberSmsCodeDO();
        exists.setId(9L);
        when(memberSmsCodeMapper.selectByMobileAndScene(MOBILE, 1)).thenReturn(exists);

        memberSmsCodeService.sendLoginCode(MOBILE);

        verify(memberSmsCodeMapper).updateCodeForResend(eq(9L), eq(DEV_CODE), any(LocalDateTime.class));
        verify(memberSmsCodeMapper, never()).insert(any(MemberSmsCodeDO.class));
    }

    @Test
    void testSendLoginCode_devCodeMissing_throws() {
        mockDevEnv("");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.sendLoginCode(MOBILE));
        assertEquals(PHARMACY_MEMBER_SMS_DEV_CODE_NOT_CONFIGURED.getCode(), ex.getCode());
        verify(memberSmsCodeMapper, never()).insert(any(MemberSmsCodeDO.class));
    }

    @Test
    void testSendLoginCode_prodWithoutDevCodeEnabled_throws() {
        // prod：没有 local/dev 覆盖 => login-enabled=false，手机号快捷登录整体禁用
        when(memberSmsProperties.isLoginEnabled()).thenReturn(false);
        when(memberSmsProperties.isDevCodeEnabled()).thenReturn(false);
        when(memberSmsProperties.getDevCode()).thenReturn(DEV_CODE);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.sendLoginCode(MOBILE));
        assertEquals(PHARMACY_MEMBER_SMS_LOGIN_DISABLED.getCode(), ex.getCode());
        verify(memberSmsCodeMapper, never()).insert(any(MemberSmsCodeDO.class));
    }

    @Test
    void testSendLoginCode_loginEnabledButRealSmsNotIntegrated_throws() {
        // 若开启登录但未接入真实短信通道（dev-code-enabled=false），不允许写入开发验证码
        when(memberSmsProperties.isLoginEnabled()).thenReturn(true);
        when(memberSmsProperties.isDevCodeEnabled()).thenReturn(false);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.sendLoginCode(MOBILE));
        assertEquals(PHARMACY_MEMBER_SMS_LOGIN_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void testSendLoginCode_illegalMobile_throws() {
        mockDevEnv(DEV_CODE);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.sendLoginCode("1380013"));
        assertEquals(PHARMACY_MEMBER_USER_MOBILE_INVALID.getCode(), ex.getCode());
    }

    // ========== 校验验证码 ==========

    @Test
    void testVerify_correctCode_success() {
        mockDevEnv(DEV_CODE);
        when(memberSmsCodeMapper.selectByMobileAndScene(MOBILE, 1)).thenReturn(unusedCode(DEV_CODE));
        when(memberSmsCodeMapper.updateUsedTimeIfUnused(eq(1L), any(LocalDateTime.class), any())).thenReturn(1);

        memberSmsCodeService.verifyAndConsumeLoginCode(MOBILE, DEV_CODE);

        verify(memberSmsCodeMapper).updateUsedTimeIfUnused(eq(1L), any(LocalDateTime.class), any());
    }

    @Test
    void testVerify_nullOrBlankCode_rejected() {
        mockDevEnv(DEV_CODE);

        ServiceException nullEx = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.verifyAndConsumeLoginCode(MOBILE, null));
        assertEquals(PHARMACY_MEMBER_SMS_CODE_REQUIRED.getCode(), nullEx.getCode());

        ServiceException blankEx = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.verifyAndConsumeLoginCode(MOBILE, "  "));
        assertEquals(PHARMACY_MEMBER_SMS_CODE_REQUIRED.getCode(), blankEx.getCode());
        verify(memberSmsCodeMapper, never()).updateUsedTimeIfUnused(any(), any(), any());
    }

    @Test
    void testVerify_wrongCode_rejected() {
        mockDevEnv(DEV_CODE);
        when(memberSmsCodeMapper.selectByMobileAndScene(MOBILE, 1)).thenReturn(unusedCode(DEV_CODE));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.verifyAndConsumeLoginCode(MOBILE, "000000"));
        assertEquals(PHARMACY_MEMBER_SMS_CODE_INVALID.getCode(), ex.getCode());
        verify(memberSmsCodeMapper, never()).updateUsedTimeIfUnused(any(), any(), any());
    }

    @Test
    void testVerify_noRecordForMobile_rejected() {
        // 其他手机号（未发过码）不能使用别人的验证码
        mockDevEnv(DEV_CODE);
        when(memberSmsCodeMapper.selectByMobileAndScene("13900139000", 1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.verifyAndConsumeLoginCode("13900139000", DEV_CODE));
        assertEquals(PHARMACY_MEMBER_SMS_CODE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void testVerify_expiredCode_rejected() {
        mockDevEnv(DEV_CODE);
        MemberSmsCodeDO record = unusedCode(DEV_CODE);
        record.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(memberSmsCodeMapper.selectByMobileAndScene(MOBILE, 1)).thenReturn(record);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.verifyAndConsumeLoginCode(MOBILE, DEV_CODE));
        assertEquals(PHARMACY_MEMBER_SMS_CODE_EXPIRED.getCode(), ex.getCode());
        verify(memberSmsCodeMapper, never()).updateUsedTimeIfUnused(any(), any(), any());
    }

    @Test
    void testVerify_usedCode_rejected() {
        // 验证码重复使用：已消费的验证码再次提交必须拒绝
        mockDevEnv(DEV_CODE);
        MemberSmsCodeDO record = unusedCode(DEV_CODE);
        record.setUsedTime(LocalDateTime.now().minusSeconds(5));
        when(memberSmsCodeMapper.selectByMobileAndScene(MOBILE, 1)).thenReturn(record);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.verifyAndConsumeLoginCode(MOBILE, DEV_CODE));
        assertEquals(PHARMACY_MEMBER_SMS_CODE_USED.getCode(), ex.getCode());
        verify(memberSmsCodeMapper, never()).updateUsedTimeIfUnused(any(), any(), any());
    }

    @Test
    void testVerify_concurrentConsumeLostRace_rejected() {
        // 并发下条件更新影响 0 行，说明已被消费，同样按「重复使用」拒绝
        mockDevEnv(DEV_CODE);
        when(memberSmsCodeMapper.selectByMobileAndScene(MOBILE, 1)).thenReturn(unusedCode(DEV_CODE));
        when(memberSmsCodeMapper.updateUsedTimeIfUnused(eq(1L), any(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.verifyAndConsumeLoginCode(MOBILE, DEV_CODE));
        assertEquals(PHARMACY_MEMBER_SMS_CODE_USED.getCode(), ex.getCode());
    }

    @Test
    void testVerify_prodDisabled_devCodeInvalid() {
        // prod：开发验证码无效（登录整体禁用，且不存在任何验证码记录）
        when(memberSmsProperties.isLoginEnabled()).thenReturn(false);
        when(memberSmsProperties.getDevCode()).thenReturn(DEV_CODE);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> memberSmsCodeService.verifyAndConsumeLoginCode(MOBILE, DEV_CODE));
        assertEquals(PHARMACY_MEMBER_SMS_LOGIN_DISABLED.getCode(), ex.getCode());
        verify(memberSmsCodeMapper, never()).selectByMobileAndScene(anyString(), anyInt());
    }

    private MemberSmsCodeDO unusedCode(String code) {
        MemberSmsCodeDO record = new MemberSmsCodeDO();
        record.setId(1L);
        record.setMobile(MOBILE);
        record.setScene(1);
        record.setCode(code);
        record.setExpireTime(LocalDateTime.now().plusMinutes(5));
        return record;
    }

}
