package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 药品精简信息 VO（下拉/跨模块商品查询使用）
 */
@Schema(description = "管理后台 - 药品精简信息 VO")
@Data
public class DrugSimpleRespVO {

    @Schema(description = "药品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "药品编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRG001")
    private String drugCode;

    @Schema(description = "通用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "阿莫西林胶囊")
    private String genericName;

    @Schema(description = "规格", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.25g*24粒")
    private String specification;

    @Schema(description = "销售单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "盒")
    private String unit;

    @Schema(description = "零售价", requiredMode = Schema.RequiredMode.REQUIRED, example = "25.50")
    private java.math.BigDecimal retailPrice;

}
