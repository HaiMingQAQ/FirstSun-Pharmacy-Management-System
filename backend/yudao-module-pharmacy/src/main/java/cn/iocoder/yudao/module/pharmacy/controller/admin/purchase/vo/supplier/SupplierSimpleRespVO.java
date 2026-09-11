package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 供应商精简 Response VO")
@Data
public class SupplierSimpleRespVO {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "供应商编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUP001")
    private String supplierCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "国药控股有限公司")
    private String supplierName;

    @Schema(description = "默认折扣率", example = "0.95")
    private java.math.BigDecimal defaultDiscount;

}
