package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberAuthLoginReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberAuthLoginRespVO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private SecurityProperties securityProperties;

    @PostMapping("/login")
    @Operation(summary = "手机号 + 密码登录")
    @PermitAll
    public CommonResult<AppMemberAuthLoginRespVO> login(@RequestBody @Valid AppMemberAuthLoginReqVO reqVO) {
        return success(memberAuthService.login(reqVO));
    }

    @PostMapping("/login-or-register")
    @Operation(summary = "手机号快捷登录（不存在则自动注册）")
    @PermitAll
    public CommonResult<AppMemberAuthLoginRespVO> loginOrRegister(@RequestParam("mobile") String mobile) {
        return success(memberAuthService.loginOrRegister(mobile));
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
