package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 药店微信登录响应，沿用药店会员 OAuth2 令牌字段。 */
@Schema(description = "用户 APP - 药店微信登录 Response VO")
@Data
public class AppMemberWechatLoginRespVO {

    private Long userId;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresTime;

    /** 仅供小程序支付等已存在调用使用，来源为微信实时校验结果。 */
    private String openid;

}
