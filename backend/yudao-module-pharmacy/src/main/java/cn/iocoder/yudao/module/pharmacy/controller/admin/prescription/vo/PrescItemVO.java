package cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 处方药品明细 VO")
@Data
public class PrescItemVO {

    @Schema(description = "药品 ID（ph_drug）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "药品不能为空")
    private Long drugId;

    @Schema(description = "核准数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "核准数量不能为空")
    @Min(value = 1, message = "核准数量必须大于 0")
    private Integer qty;

    @Schema(description = "用法", example = "口服")
    private String usage;

    @Schema(description = "用量", example = "每次一片")
    private String dosage;

    @Schema(description = "药品名称快照（可选，登记时冗余展示）", example = "阿莫西林胶囊")
    private String drugName;

    @Schema(description = "规格快照（可选）", example = "0.25g*24粒")
    private String specification;
}
