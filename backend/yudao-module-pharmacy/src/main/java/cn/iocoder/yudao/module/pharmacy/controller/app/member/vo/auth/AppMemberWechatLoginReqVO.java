package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 药店微信小程序登录请求。 */
@Schema(description = "用户 APP - 药店微信登录 Request VO")
@Data
public class AppMemberWechatLoginReqVO {

    @Schema(description = "微信小程序登录 code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "微信登录凭证不能为空")
    private String code;

    /** 兼容现有前端请求；后端只允许微信小程序类型，不以此确定租户或 AppID。 */
    @Schema(description = "社交类型", example = "34")
    private Integer type;

    /** 微信小程序登录不使用 state，接收但不参与认证。 */
    @Schema(description = "兼容字段，不参与认证")
    private String state;

}
