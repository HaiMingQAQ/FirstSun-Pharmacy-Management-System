package cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 处方登记 Request VO")
@Data
public class PrescRecordSaveReqVO {

    @Schema(description = "处方号（不传则后端生成：PX-门店-yyyyMMdd-4位流水）", example = "PX-1-20260914-0001")
    private String prescNo;

    @Schema(description = "门店", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "门店不能为空")
    private Long storeId;

    @Schema(description = "来源：0 纸质拍照 / 1 电子处方平台 / 2 复诊续方", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "处方来源不能为空")
    private Integer source;

    @Schema(description = "开具医院", example = "厦门市第一医院")
    private String hospital;

    @Schema(description = "医师姓名", example = "张医生")
    private String doctorName;

    @Schema(description = "患者姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "患者姓名不能为空")
    private String patientName;

    @Schema(description = "年龄", example = "35")
    private Integer patientAge;

    @Schema(description = "患者身份证（AES 加密）", example = "350203********0011")
    private String patientIdNo;

    @Schema(description = "诊断", example = "上呼吸道感染")
    private String diagnosis;

    @Schema(description = "用法用量", example = "一日三次，每次一片")
    private String usageDesc;

    @Schema(description = "开方日期", example = "2026-09-14")
    private LocalDate prescDate;

    @Schema(description = "处方影像 URL（主图）", example = "https://xxx/presc1.jpg")
    private String imageUrl;

    @Schema(description = "小程序上传人（管理员登记可不传）", example = "100")
    private Long wxMemberId;

    @Schema(description = "是否特管登记（双人复核）：0 否 / 1 是", example = "0")
    private Integer isSpecial;

    @Schema(description = "是否超量复核：0 否 / 1 是", example = "0")
    private Integer limitCheck;

    @Schema(description = "影像 URL 列表（最多 5 张，JSON 存储）")
    private List<String> images;

    @Schema(description = "处方药品明细（药品 ID、核准数量与用法，JSON 存储）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "处方药品明细不能为空")
    private List<PrescItemVO> items;
}
