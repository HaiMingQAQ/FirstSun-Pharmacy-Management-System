package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "管理后台 - 药品条码创建/修改 Request VO")
@Data
public class BarcodeSaveReqVO {

    @Schema(description = "条码编号", example = "1024")
    private Long id;

    @Schema(description = "药品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "药品编号不能为空")
    private Long drugId;

    @Schema(description = "条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "6901234567890")
    @NotBlank(message = "条码不能为空")
    @Size(max = 32, message = "条码长度不能超过 32 个字符")
    private String barcode;

    @Schema(description = "条码类型 0商品条码/1店内码/2追溯码", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "条码类型不能为空")
    private Integer barcodeType;

    @Schema(description = "默认扫码码 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "是否默认不能为空")
    private Integer isDefault;

}
