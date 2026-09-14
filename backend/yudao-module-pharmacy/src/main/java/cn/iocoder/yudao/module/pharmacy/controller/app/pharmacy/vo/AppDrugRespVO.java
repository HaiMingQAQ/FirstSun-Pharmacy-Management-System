package cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 APP - 药店商品 Response VO")
@Data
public class AppDrugRespVO {

    @Schema(description = "药品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "药品编码", example = "DRG001")
    private String drugCode;

    @Schema(description = "分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "分类名称", example = "抗感染类")
    private String categoryName;

    @Schema(description = "通用名", example = "阿莫西林胶囊")
    private String genericName;

    @Schema(description = "商品名", example = "阿莫仙")
    private String tradeName;

    @Schema(description = "规格", example = "0.25g*24粒")
    private String specification;

    @Schema(description = "剂型", example = "胶囊")
    private String dosageForm;

    @Schema(description = "生产厂家", example = "某某制药")
    private String manufacturer;

    @Schema(description = "销售单位", example = "盒")
    private String unit;

    @Schema(description = "药品类型 0处方/1OTC甲/2OTC乙/3特管/4饮片/5保健/6器械/7日化/8其他", example = "1")
    private Integer drugType;

    @Schema(description = "是否处方药 0否/1是", example = "0")
    private Integer isRx;

    @Schema(description = "零售价", example = "19.90")
    private BigDecimal retailPrice;

    @Schema(description = "会员价", example = "18.90")
    private BigDecimal memberPrice;

    @Schema(description = "说明书地址")
    private String instructionsUrl;

    @Schema(description = "药品说明")
    private String description;

}
