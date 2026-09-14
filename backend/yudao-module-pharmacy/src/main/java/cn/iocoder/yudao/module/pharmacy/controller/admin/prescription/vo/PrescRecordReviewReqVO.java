package cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 处方审核 Request VO")
@Data
public class PrescRecordReviewReqVO {

    @Schema(description = "处方编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "处方编号不能为空")
    private Long id;

    @Schema(description = "审核结果：1 通过 / 2 驳回", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "审核结果不能为空")
    private Integer reviewStatus;

    @Schema(description = "审方意见（驳回必填）", example = "处方超量，需复查")
    private String reviewOpinion;

    @Schema(description = "电子签名信息", example = "sign:xxxx")
    private String reviewSnapshot;

    @Schema(description = "双人复核人（特管处方必填）", example = "2")
    private Long dblCheckBy;
}
