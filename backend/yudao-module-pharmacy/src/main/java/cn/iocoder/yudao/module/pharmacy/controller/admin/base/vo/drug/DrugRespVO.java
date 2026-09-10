package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 药品 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DrugRespVO {

    @Schema(description = "药品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("药品编号")
    private Long id;

    @Schema(description = "药品编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRG001")
    @ExcelProperty("药品编码")
    private String drugCode;

    @Schema(description = "分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("分类编号")
    private Long categoryId;

    /**
     * 分类名称（仅展示用，由 Controller 补充）
     */
    @Schema(description = "分类名称", example = "抗生素")
    @ExcelProperty("分类名称")
    private String categoryName;

    @Schema(description = "通用名", requiredMode = Schema.RequiredMode.REQUIRED, example = "阿莫西林胶囊")
    @ExcelProperty("通用名")
    private String genericName;

    @Schema(description = "商品名", example = "阿莫仙")
    @ExcelProperty("商品名")
    private String tradeName;

    @Schema(description = "拼音码", example = "amxljn")
    @ExcelProperty("拼音码")
    private String spellCode;

    @Schema(description = "规格", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.25g*24粒")
    @ExcelProperty("规格")
    private String specification;

    @Schema(description = "剂型", example = "胶囊剂")
    @ExcelProperty("剂型")
    private String dosageForm;

    @Schema(description = "生产厂家", example = "珠海联邦")
    @ExcelProperty("生产厂家")
    private String manufacturer;

    @Schema(description = "批准文号", example = "国药准字H20043221")
    @ExcelProperty("批准文号")
    private String approvalNo;

    @Schema(description = "药品类型 0处方/1OTC甲/2OTC乙/3特管/4饮片/5保健/6器械/7日化/8其他", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "药品类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_DRUG_TYPE)
    private Integer drugType;

    @Schema(description = "是否处方药 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "是否处方药", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_YES_NO)
    private Integer isRx;

    @Schema(description = "是否特殊管理 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "是否特殊管理", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_YES_NO)
    private Integer isSpecial;

    @Schema(description = "含麻黄碱类 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "含麻黄碱类", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_YES_NO)
    private Integer isPseudoephedrine;

    @Schema(description = "是否冷链 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "是否冷链", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_YES_NO)
    private Integer isColdChain;

    @Schema(description = "销售单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "盒")
    @ExcelProperty("销售单位")
    private String unit;

    @Schema(description = "转换比", example = "12")
    @ExcelProperty("转换比")
    private Integer conversionRatio;

    @Schema(description = "零售价", requiredMode = Schema.RequiredMode.REQUIRED, example = "25.50")
    @ExcelProperty("零售价")
    private BigDecimal retailPrice;

    @Schema(description = "会员价", example = "22.00")
    @ExcelProperty("会员价")
    private BigDecimal memberPrice;

    @Schema(description = "参考成本价", example = "15.00")
    @ExcelProperty("参考成本价")
    private BigDecimal costPrice;

    @Schema(description = "最低限售价", example = "20.00")
    @ExcelProperty("最低限售价")
    private BigDecimal minSalePrice;

    @Schema(description = "税率", requiredMode = Schema.RequiredMode.REQUIRED, example = "13.00")
    @ExcelProperty("税率")
    private BigDecimal taxRate;

    @Schema(description = "医保类别 0自费/1甲类/2乙类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "医保类别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_INSURANCE_TYPE)
    private Integer insuranceType;

    @Schema(description = "库存下限", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("库存下限")
    private Integer minStock;

    @Schema(description = "库存上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @ExcelProperty("库存上限")
    private Integer maxStock;

    @Schema(description = "储存条件 0常温/1阴凉/2冷藏/3冷冻", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "储存条件", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_STORAGE_COND)
    private Integer storageCond;

    @Schema(description = "是否效期管理", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("是否效期管理")
    private Integer needExpiry;

    @Schema(description = "默认货位编号", example = "1024")
    private Long defaultLocationId;

    @Schema(description = "线上可售 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "线上可售", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_YES_NO)
    private Integer saleableOnline;

    @Schema(description = "启用状态 1启用/0停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "启用状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "注意事项")
    private String remark;

    @Schema(description = "封面图片 URL", example = "https://...")
    private String imageUrl;

    @Schema(description = "图片地址数组")
    private List<String> images;

    @Schema(description = "药品说明信息")
    private String description;

    @Schema(description = "说明书文件 URL", example = "https://...")
    private String instructionsUrl;

    @Schema(description = "审核状态 0待审/1通过/2驳回", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "审核状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_DRUG_APPROVE_STATUS)
    private Integer approveStatus;

    @Schema(description = "审核员工编号", example = "1024")
    private Long auditBy;

    @Schema(description = "审核时间")
    private LocalDateTime auditAt;

    @Schema(description = "审核意见")
    private String auditOpinion;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
