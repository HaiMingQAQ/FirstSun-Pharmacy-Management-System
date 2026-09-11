package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 供应商 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SupplierRespVO {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("供应商编号")
    private Long id;

    @Schema(description = "供应商编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUP001")
    @ExcelProperty("供应商编码")
    private String supplierCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "国药控股有限公司")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "统一社会信用代码", example = "91330100MA2XXXXX1A")
    @ExcelProperty("统一社会信用代码")
    private String creditCode;

    @Schema(description = "药品经营/生产许可证号", example = "浙AA1234567")
    @ExcelProperty("许可证号")
    private String scopeCode;

    @Schema(description = "联系人", example = "张三")
    @ExcelProperty("联系人")
    private String contact;

    @Schema(description = "联系电话", example = "13800000000")
    @ExcelProperty("联系电话")
    private String phone;

    @Schema(description = "地址", example = "杭州市余杭区XX路1号")
    @ExcelProperty("地址")
    private String address;

    @Schema(description = "开户行", example = "中国工商银行杭州分行")
    @ExcelProperty("开户行")
    private String bankName;

    @Schema(description = "银行账号（详情接口返回原文，编辑表单使用）", example = "6222020200000000000")
    private String bankAccount;

    @Schema(description = "银行账号掩码（列表接口与导出返回，NFR-12）", example = "************0000")
    @ExcelProperty("银行账号")
    private String bankAccountMasked;

    @Schema(description = "账期", example = "月结30天")
    @ExcelProperty("账期")
    private String paymentTerms;

    @Schema(description = "默认折扣率", example = "0.95")
    @ExcelProperty("默认折扣率")
    private BigDecimal defaultDiscount;

    @Schema(description = "首营审核状态 0待审/1通过/2驳回", example = "1")
    @ExcelProperty(value = "首营审核状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_SUPPLIER_APPROVE_STATUS)
    private Integer approveStatus;

    @Schema(description = "状态 1启用/0停用", example = "1")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_STATUS)
    private Integer status;

    @Schema(description = "审核员工编号", example = "1")
    @ExcelProperty("审核员工编号")
    private Long auditBy;

    @Schema(description = "审核时间")
    @ExcelProperty("审核时间")
    private LocalDateTime auditAt;

    @Schema(description = "审核意见")
    @ExcelProperty("审核意见")
    private String auditOpinion;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    /**
     * 生成银行账号掩码：保留末 4 位，其余用 * 填充（不足 4 位则整体打码）。
     *
     * 列表接口调用，避免在列表页回传完整账号；详情接口保留原文供编辑使用。
     */
    public void maskBankAccount() {
        this.bankAccountMasked = mask(this.bankAccount);
    }

    public static String mask(String account) {
        if (StringUtils.isBlank(account)) {
            return account;
        }
        if (account.length() <= 4) {
            return StringUtils.repeat('*', account.length());
        }
        return StringUtils.repeat('*', account.length() - 4) + account.substring(account.length() - 4);
    }

}
