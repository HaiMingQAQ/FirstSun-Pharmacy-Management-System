package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 供应商分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierPageReqVO extends PageParam {

    @Schema(description = "供应商编码，模糊匹配", example = "SUP")
    private String supplierCode;

    @Schema(description = "供应商名称，模糊匹配", example = "国药")
    private String supplierName;

    @Schema(description = "统一社会信用代码，模糊匹配", example = "91330")
    private String creditCode;

    @Schema(description = "联系人，模糊匹配", example = "张")
    private String contact;

    @Schema(description = "联系电话，模糊匹配", example = "138")
    private String phone;

    @Schema(description = "首营审核状态 0待审/1通过/2驳回", example = "1")
    private Integer approveStatus;

    @Schema(description = "状态 1启用/0停用", example = "1")
    private Integer status;

}
