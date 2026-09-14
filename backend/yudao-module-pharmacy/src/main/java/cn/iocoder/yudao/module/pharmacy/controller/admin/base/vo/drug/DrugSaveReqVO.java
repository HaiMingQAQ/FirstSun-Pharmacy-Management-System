package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 药品创建/修改 Request VO")
@Data
public class DrugSaveReqVO {

    @Schema(description = "药品编号", example = "1024")
    private Long id;

    @Schema(description = "药品编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRG001")
    @NotBlank(message = "药品编码不能为空")
    @Size(max = 32, message = "药品编码长度不能超过 32 个字符")
    private String drugCode;

    @Schema(description = "分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "所属分类不能为空")
    private Long categoryId;

    @Schema(description = "通用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "阿莫西林胶囊")
    @NotBlank(message = "通用名不能为空")
    @Size(max = 64, message = "通用名长度不能超过 64 个字符")
    private String genericName;

    @Schema(description = "商品名", example = "阿莫仙")
    @Size(max = 64, message = "商品名长度不能超过 64 个字符")
    private String tradeName;

    @Schema(description = "拼音码", example = "amxljn")
    @Size(max = 32, message = "拼音码长度不能超过 32 个字符")
    private String spellCode;

    @Schema(description = "规格", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.25g*24粒")
    @NotBlank(message = "规格不能为空")
    @Size(max = 64, message = "规格长度不能超过 64 个字符")
    private String specification;

    @Schema(description = "剂型", example = "胶囊剂")
    @Size(max = 16, message = "剂型长度不能超过 16 个字符")
    private String dosageForm;

    @Schema(description = "生产厂家", example = "珠海联邦")
    @Size(max = 128, message = "生产厂家长度不能超过 128 个字符")
    private String manufacturer;

    @Schema(description = "批准文号", example = "国药准字H20043221")
    @Size(max = 64, message = "批准文号长度不能超过 64 个字符")
    private String approvalNo;

    @Schema(description = "药品类型 0处方/1OTC甲/2OTC乙/3特管/4饮片/5保健/6器械/7日化/8其他", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "药品类型不能为空")
    private Integer drugType;

    @Schema(description = "是否处方药 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "是否处方药不能为空")
    private Integer isRx;

    @Schema(description = "是否特殊管理药品 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "是否特殊管理药品不能为空")
    private Integer isSpecial;

    @Schema(description = "含麻黄碱类 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "含麻黄碱类不能为空")
    private Integer isPseudoephedrine;

    @Schema(description = "是否冷链 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "是否冷链不能为空")
    private Integer isColdChain;

    @Schema(description = "销售单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "盒")
    @NotBlank(message = "销售单位不能为空")
    @Size(max = 8, message = "销售单位长度不能超过 8 个字符")
    private String unit;

    @Schema(description = "转换比（1盒=12粒）", example = "12")
    private Integer conversionRatio;

    @Schema(description = "零售价", requiredMode = Schema.RequiredMode.REQUIRED, example = "25.50")
    @NotNull(message = "零售价不能为空")
    @DecimalMin(value = "0", inclusive = true, message = "零售价不能小于 0")
    private BigDecimal retailPrice;

    @Schema(description = "会员价", example = "22.00")
    @DecimalMin(value = "0", inclusive = true, message = "会员价不能小于 0")
    private BigDecimal memberPrice;

    @Schema(description = "参考成本价", example = "15.00")
    @DecimalMin(value = "0", inclusive = true, message = "成本价不能小于 0")
    private BigDecimal costPrice;

    @Schema(description = "最低限售价", example = "20.00")
    @DecimalMin(value = "0", inclusive = true, message = "最低限售价不能小于 0")
    private BigDecimal minSalePrice;

    @Schema(description = "税率", requiredMode = Schema.RequiredMode.REQUIRED, example = "13.00")
    @NotNull(message = "税率不能为空")
    @DecimalMin(value = "0", inclusive = true, message = "税率不能小于 0")
    private BigDecimal taxRate;

    @Schema(description = "医保类别 0自费/1甲类/2乙类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "医保类别不能为空")
    private Integer insuranceType;

    @Schema(description = "库存下限", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "库存下限不能为空")
    private Integer minStock;

    @Schema(description = "库存上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "库存上限不能为空")
    private Integer maxStock;

    @Schema(description = "储存条件 0常温/1阴凉/2冷藏/3冷冻", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "储存条件不能为空")
    private Integer storageCond;

    @Schema(description = "是否效期管理", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "是否效期管理不能为空")
    private Integer needExpiry;

    @Schema(description = "默认货位编号", example = "1024")
    private Long defaultLocationId;

    @Schema(description = "线上可售 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "线上可售不能为空")
    private Integer saleableOnline;

    @Schema(description = "启用状态 1启用/0停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @InEnum(PharmacyStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "注意事项")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "封面图片 URL", example = "https://...")
    @Size(max = 512, message = "封面图片 URL 长度不能超过 512 个字符")
    private String imageUrl;

    @Schema(description = "图片地址数组")
    private List<String> images;

    @Schema(description = "药品说明信息")
    private String description;

    @Schema(description = "说明书文件 URL", example = "https://...")
    @Size(max = 512, message = "说明书文件 URL 长度不能超过 512 个字符")
    private String instructionsUrl;

    /**
     * 审核字段（approveStatus/auditBy/auditAt/auditOpinion）由独立审核接口维护，不通过保存接口直接修改。
     */
}
