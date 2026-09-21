package cn.iocoder.yudao.module.pharmacy.controller.app.prescription.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户 APP - 处方记录 Response VO")
@Data
public class AppPrescRecordRespVO {

    @Schema(description = "处方编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "处方号", requiredMode = Schema.RequiredMode.REQUIRED, example = "PX-407-20260919-0001")
    private String prescNo;

    @Schema(description = "履约门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "407")
    private Long storeId;

    @Schema(description = "来源：0 纸质拍照 / 1 电子处方平台 / 2 复诊续方", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer source;

    @Schema(description = "开具医院", example = "厦门市第一医院")
    private String hospital;

    @Schema(description = "医师姓名", example = "张医生")
    private String doctorName;

    @Schema(description = "患者姓名", example = "顾晨")
    private String patientName;

    @Schema(description = "诊断", example = "上呼吸道感染")
    private String diagnosis;

    @Schema(description = "用法用量", example = "一日三次，每次一片")
    private String usageDesc;

    @Schema(description = "开方日期", example = "2026-09-19")
    private LocalDate prescDate;

    @Schema(description = "处方影像 URL 列表")
    private List<String> images;

    @Schema(description = "审方状态：0 待审 / 1 通过 / 2 驳回", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer reviewStatus;

    @Schema(description = "审方意见", example = "审核通过")
    private String reviewOpinion;

    @Schema(description = "记录状态：0 有效 / 1 已完成 / 2 作废", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
