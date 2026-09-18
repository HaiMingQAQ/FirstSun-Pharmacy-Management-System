package cn.iocoder.yudao.module.pharmacy.service.member;

/**
 * 会员手机验证码 Service
 *
 * <p>用于手机号快捷登录的验证码校验。验证码与「手机号 + 登录用途」绑定，
 * 校验通过后立即消费，保证一次性与不可重放；空值、错误、过期、重复使用一律拒绝。
 */
public interface MemberSmsCodeService {

    /**
     * 发送登录验证码
     *
     * <p>当前未接入收费短信通道：仅 local/dev 环境（dev-code-enabled=true）允许把环境变量
     * 中的开发测试验证码写入验证码记录，接口不返回验证码，日志也不打印验证码。
     *
     * @param mobile 手机号
     */
    void sendLoginCode(String mobile);

    /**
     * 校验并消费登录验证码
     *
     * @param mobile 手机号
     * @param code   验证码
     */
    void verifyAndConsumeLoginCode(String mobile, String code);

}
