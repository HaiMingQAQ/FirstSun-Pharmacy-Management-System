package cn.iocoder.yudao.module.pharmacy.controller.app.prescription.vo;

import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescItemVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "用户 APP - 处方登记 Request VO")
@Data
public class AppPrescRecordCreateReqVO {

    @Schema(description = "履约门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "407")
    @NotNull(message = "门店不能为空")
    private Long storeId;

    @Schema(description = "患者姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "顾晨")
    @NotEmpty(message = "患者姓名不能为空")
    private String patientName;

    @Schema(description = "开具医院", example = "厦门市第一医院")
    private String hospital;

    @Schema(description = "医师姓名", example = "张医生")
    private String doctorName;

    @Schema(description = "处方影像 URL 列表（最多 5 张）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请上传处方图片")
    @Size(max = 5, message = "处方图片最多 5 张")
    private List<String> images;

    @Schema(description = "处方药品明细（药品 ID 与核准数量）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "处方药品明细不能为空")
    @Valid
    private List<PrescItemVO> items;

}
