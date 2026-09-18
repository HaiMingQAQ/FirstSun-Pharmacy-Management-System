package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.pharmacy.config.MemberSmsProperties;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberSmsCodeDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberSmsCodeMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_EXPIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_CODE_USED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_DEV_CODE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_SMS_LOGIN_DISABLED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_USER_MOBILE_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.member.MemberSmsSceneEnum.LOGIN;

/**
 * 会员手机验证码 Service 实现类
 *
 * <p>实现要点：
 * <ol>
 *     <li>开关与验证码都来自配置/环境变量，代码中不含任何硬编码验证码；</li>
 *     <li>验证码记录按「手机号 + 场景」唯一，重发覆盖；</li>
 *     <li>校验通过后用条件更新置为「已使用」，并发下也只有一次成功，重复使用被拒绝；</li>
 *     <li>接口响应与日志都不输出验证码（日志只打印脱敏手机号）。</li>
 * </ol>
 */
@Service
@Validated
@Slf4j
public class MemberSmsCodeServiceImpl implements MemberSmsCodeService {

    /**
     * 手机号格式：11 位数字
     */
    private static final String MOBILE_PATTERN = "\\d{11}";

    @Resource
    private MemberSmsCodeMapper memberSmsCodeMapper;

    @Resource
    private MemberSmsProperties memberSmsProperties;

    @Override
    public void sendLoginCode(String mobile) {
        validateMobile(mobile);
        ensureLoginEnabled();
        // 未接入收费短信通道：只有 local/dev（dev-code-enabled=true）才允许写入开发测试验证码
        if (!memberSmsProperties.isDevCodeEnabled()) {
            throw exception(PHARMACY_MEMBER_SMS_LOGIN_DISABLED);
        }
        String devCode = StrUtil.trim(memberSmsProperties.getDevCode());
        if (StrUtil.isBlank(devCode)) {
            // 提示变量名是安全的：变量本身不是密钥
            throw exception(PHARMACY_MEMBER_SMS_DEV_CODE_NOT_CONFIGURED);
        }
        LocalDateTime expireTime = LocalDateTime.now().plus(memberSmsProperties.getExpireTime());
        MemberSmsCodeDO exists = memberSmsCodeMapper.selectByMobileAndScene(mobile, LOGIN.getScene());
        if (exists == null) {
            MemberSmsCodeDO record = new MemberSmsCodeDO();
            record.setMobile(mobile);
            record.setScene(LOGIN.getScene());
            record.setCode(devCode);
            record.setExpireTime(expireTime);
            memberSmsCodeMapper.insert(record);
        } else {
            memberSmsCodeMapper.updateCodeForResend(exists.getId(), devCode, expireTime);
        }
        // 只记录脱敏手机号与有效期，禁止打印验证码
        log.info("[sendLoginCode][手机号({}) 验证码已生成，有效期至 {}]", maskMobile(mobile), expireTime);
    }

    @Override
    public void verifyAndConsumeLoginCode(String mobile, String code) {
        validateMobile(mobile);
        ensureLoginEnabled();
        // 1. 空验证码直接拒绝
        if (StrUtil.isBlank(code)) {
            throw exception(PHARMACY_MEMBER_SMS_CODE_REQUIRED);
        }
        // 2. 必须存在「该手机号 + 该用途」的验证码记录
        MemberSmsCodeDO record = memberSmsCodeMapper.selectByMobileAndScene(mobile, LOGIN.getScene());
        if (record == null) {
            throw exception(PHARMACY_MEMBER_SMS_CODE_NOT_EXISTS);
        }
        // 3. 重复使用拒绝
        if (record.getUsedTime() != null) {
            throw exception(PHARMACY_MEMBER_SMS_CODE_USED);
        }
        // 4. 过期拒绝
        if (record.getExpireTime() != null && record.getExpireTime().isBefore(LocalDateTime.now())) {
            throw exception(PHARMACY_MEMBER_SMS_CODE_EXPIRED);
        }
        // 5. 错误验证码拒绝
        if (!StrUtil.equals(record.getCode(), StrUtil.trim(code))) {
            throw exception(PHARMACY_MEMBER_SMS_CODE_INVALID);
        }
        // 6. 消费验证码：条件更新，保证一次性
        int rows = memberSmsCodeMapper.updateUsedTimeIfUnused(record.getId(), LocalDateTime.now(), getClientIP());
        if (rows == 0) {
            throw exception(PHARMACY_MEMBER_SMS_CODE_USED);
        }
    }

    // ========== 私有方法 ==========

    private void validateMobile(String mobile) {
        if (StrUtil.isBlank(mobile) || !mobile.matches(MOBILE_PATTERN)) {
            throw exception(PHARMACY_MEMBER_USER_MOBILE_INVALID);
        }
    }

    private void ensureLoginEnabled() {
        if (!memberSmsProperties.isLoginEnabled()) {
            throw exception(PHARMACY_MEMBER_SMS_LOGIN_DISABLED);
        }
    }

    private String maskMobile(String mobile) {
        return StrUtil.hide(mobile, 3, 7);
    }

}
