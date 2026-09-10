package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 药品分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DrugPageReqVO extends PageParam {

    @Schema(description = "药品编码", example = "DRG001")
    private String drugCode;

    @Schema(description = "通用名，模糊匹配", example = "阿莫西林")
    private String genericName;

    @Schema(description = "商品名，模糊匹配", example = "阿莫仙")
    private String tradeName;

    @Schema(description = "拼音码", example = "amxl")
    private String spellCode;

    @Schema(description = "分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "药品类型", example = "1")
    private Integer drugType;

    @Schema(description = "是否处方药 0否/1是", example = "0")
    private Integer isRx;

    @Schema(description = "是否特殊管理 0否/1是", example = "0")
    private Integer isSpecial;

    @Schema(description = "含麻黄碱类 0否/1是", example = "0")
    private Integer isPseudoephedrine;

    @Schema(description = "是否冷链 0否/1是", example = "0")
    private Integer isColdChain;

    @Schema(description = "医保类别", example = "1")
    private Integer insuranceType;

    @Schema(description = "储存条件", example = "0")
    private Integer storageCond;

    @Schema(description = "线上可售 0否/1是", example = "0")
    private Integer saleableOnline;

    @Schema(description = "启用状态 1启用/0停用", example = "1")
    private Integer status;

    @Schema(description = "审核状态 0待审/1通过/2驳回", example = "1")
    private Integer approveStatus;

    @Schema(description = "批准文号，模糊匹配", example = "国药准字")
    private String approvalNo;

    @Schema(description = "生产厂家，模糊匹配", example = "珠海")
    private String manufacturer;

}
