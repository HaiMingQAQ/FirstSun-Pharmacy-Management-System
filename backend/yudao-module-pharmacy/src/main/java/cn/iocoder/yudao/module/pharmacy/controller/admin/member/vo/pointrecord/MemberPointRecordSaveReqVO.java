package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 会员积分记录创建/修改 Request VO")
@Data
public class MemberPointRecordSaveReqVO {

    @Schema(description = "记录编号", example = "1024")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "业务编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER_001")
    @NotBlank(message = "业务编码不能为空")
    @Size(max = 255, message = "业务编码长度不能超过 255 个字符")
    private String bizId;

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "业务类型不能为空")
    private Integer bizType;

    @Schema(description = "积分标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "签到奖励")
    @NotBlank(message = "积分标题不能为空")
    @Size(max = 255, message = "积分标题长度不能超过 255 个字符")
    private String title;

    @Schema(description = "积分描述", example = "每日签到获得10积分")
    @Size(max = 5000, message = "积分描述长度不能超过 5000 个字符")
    private String description;

    @Schema(description = "积分变动值", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "积分变动值不能为空")
    private Integer point;

    @Schema(description = "变动后的积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "变动后的积分不能为空")
    private Integer totalPoint;

}
