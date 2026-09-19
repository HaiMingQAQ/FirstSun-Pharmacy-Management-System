package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberAuthLoginReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberAuthLoginRespVO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberAuthService;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberSmsCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 用户 APP - 会员认证
 *
 * 会员登录复用 yudao 框架的 OAuth2 令牌机制（userType=会员），
 * 会员数据取自 {@code member_user} 表。
 */
@Tag(name = "用户 APP - 会员认证")
@RestController
@RequestMapping("/member/auth")
@Validated
@Slf4j
public class AppMemberAuthController {

    @Resource
    private MemberAuthService memberAuthService;

    @Resource
    private MemberSmsCodeService memberSmsCodeService;

    @Resource
    private SecurityProperties securityProperties;

    @PostMapping("/login")
    @Operation(summary = "手机号 + 密码登录")
    @PermitAll
    public CommonResult<AppMemberAuthLoginRespVO> login(@RequestBody @Valid AppMemberAuthLoginReqVO reqVO) {
        return success(memberAuthService.login(reqVO));
    }

    /**
     * 验证码请求头。
     *
     * <p>验证码刻意放在<b>请求头</b>而不是 query 参数 / JSON body：yudao 框架的
     * {@code ApiAccessLogInterceptor} 在非 prod 环境会把请求参数与 JSON body 原样打进日志，
     * 走请求头可以保证「验证码不出现在后端日志里」（对应 F-1 要求 2）。
     */
    private static final String SMS_CODE_HEADER = "X-Sms-Code";

    @PostMapping("/login-or-register")
    @Operation(summary = "手机号 + 验证码快捷登录（不存在则自动注册）",
            description = "验证码通过请求头 X-Sms-Code 提交，由后端校验；接口不返回验证码")
    @Parameter(name = "mobile", description = "手机号", required = true)
    @Parameter(name = SMS_CODE_HEADER, in = ParameterIn.HEADER, required = true,
            description = "手机验证码（先调用 /member/auth/send-sms-code 获取）")
    @PermitAll
    public CommonResult<AppMemberAuthLoginRespVO> loginOrRegister(
            @RequestParam("mobile") String mobile,
            @RequestHeader(value = SMS_CODE_HEADER, required = false) String code) {
        return success(memberAuthService.loginOrRegister(mobile, code));
    }

    @PostMapping("/send-sms-code")
    @Operation(summary = "发送手机验证码",
            description = "仅 local/dev 环境可用，验证码取自环境变量 PHARMACY_DEV_SMS_CODE，接口不返回验证码")
    @Parameter(name = "mobile", description = "手机号", required = true)
    @PermitAll
    public CommonResult<Boolean> sendSmsCode(@RequestParam("mobile") String mobile) {
        memberSmsCodeService.sendLoginCode(mobile);
        return success(true);
    }

    @PostMapping("/logout")
    @Operation(summary = "登出系统")
    @PermitAll
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            memberAuthService.logout(token);
        }
        return success(true);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true)
    @PermitAll
    public CommonResult<AppMemberAuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return success(memberAuthService.refreshToken(refreshToken));
    }

}
