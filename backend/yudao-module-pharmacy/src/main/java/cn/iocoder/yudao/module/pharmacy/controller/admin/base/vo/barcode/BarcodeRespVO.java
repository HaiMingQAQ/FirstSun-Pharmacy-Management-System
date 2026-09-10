package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 药品条码 Response VO")
@Data
@ExcelIgnoreUnannotated
public class BarcodeRespVO {

    @Schema(description = "条码编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("条码编号")
    private Long id;

    @Schema(description = "药品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("药品编号")
    private Long drugId;

    /**
     * 药品名称（仅展示用，由 Controller 补充）
     */
    @Schema(description = "药品通用名", example = "阿莫西林胶囊")
    @ExcelProperty("药品通用名")
    private String drugName;

    @Schema(description = "条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "6901234567890")
    @ExcelProperty("条码")
    private String barcode;

    @Schema(description = "条码类型 0商品条码/1店内码/2追溯码", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "条码类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_BARCODE_TYPE)
    private Integer barcodeType;

    @Schema(description = "默认扫码码 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "默认", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_YES_NO)
    private Integer isDefault;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
