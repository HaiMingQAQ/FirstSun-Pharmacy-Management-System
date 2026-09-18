package cn.iocoder.yudao.module.pharmacy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 会员手机验证码配置（F 会员模块）
 *
 * <p>开关与验证码全部来自配置文件 / 环境变量，代码、前端、SQL、Dockerfile 中都不硬编码验证码：
 * <ul>
 *     <li>{@code application.yaml}（基础配置）默认 {@code login-enabled=false}，prod 环境没有 local/dev 覆盖，
 *         落到基础配置 => 手机号快捷登录整体禁用，开发验证码不可能生效；</li>
 *     <li>{@code application-local.yaml} / {@code application-dev.yaml} 才打开
 *         {@code dev-code-enabled=true}，并用环境变量 {@code PHARMACY_DEV_SMS_CODE} 作为开发测试验证码。</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "yudao.pharmacy.member-sms")
@Validated
@Data
public class MemberSmsProperties {

    /**
     * 是否启用手机号验证码快捷登录
     */
    private boolean loginEnabled = false;

    /**
     * 是否允许使用开发测试验证码（仅 local / dev 打开）
     */
    private boolean devCodeEnabled = false;

    /**
     * 开发测试验证码，来源环境变量 {@code PHARMACY_DEV_SMS_CODE}，代码中不提供默认值
     */
    private String devCode;

    /**
     * 验证码有效期
     */
    private Duration expireTime = Duration.ofMinutes(10);

}
