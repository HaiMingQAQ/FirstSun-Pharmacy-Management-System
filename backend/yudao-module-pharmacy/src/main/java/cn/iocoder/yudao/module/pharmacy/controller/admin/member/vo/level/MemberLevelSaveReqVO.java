package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 会员等级创建/修改 Request VO")
@Data
public class MemberLevelSaveReqVO {

    @Schema(description = "等级编号", example = "1024")
    private Long id;

    @Schema(description = "等级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "黄金会员")
    @NotBlank(message = "等级名称不能为空")
    @Size(max = 30, message = "等级名称长度不能超过 30 个字符")
    private String name;

    @Schema(description = "等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "等级不能为空")
    private Integer level;

    @Schema(description = "升级经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    @NotNull(message = "升级经验不能为空")
    @Min(value = 0, message = "升级经验不能为负数")
    private Integer experience;

    @Schema(description = "享受折扣(%)", requiredMode = Schema.RequiredMode.REQUIRED, example = "90")
    @NotNull(message = "享受折扣不能为空")
    @Min(value = 0, message = "享受折扣最小为 0")
    @Max(value = 100, message = "享受折扣最大为 100")
    private Integer discountPercent;

    @Schema(description = "等级图标", example = "https://www.iocoder.cn/icon.png")
    private String icon;

    @Schema(description = "等级背景图", example = "https://www.iocoder.cn/bg.png")
    private String backgroundUrl;

    @Schema(description = "状态 0启用/1禁用", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
