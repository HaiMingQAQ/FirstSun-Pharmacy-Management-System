package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应商创建/修改 Request VO")
@Data
public class SupplierSaveReqVO {

    @Schema(description = "供应商编号", example = "1024")
    private Long id;

    @Schema(description = "供应商编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUP001")
    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 16, message = "供应商编码长度不能超过 16 个字符")
    private String supplierCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "国药控股有限公司")
    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 128, message = "供应商名称长度不能超过 128 个字符")
    private String supplierName;

    @Schema(description = "统一社会信用代码", example = "91330100MA2XXXXX1A")
    @Size(max = 64, message = "统一社会信用代码长度不能超过 64 个字符")
    private String creditCode;

    @Schema(description = "药品经营/生产许可证号", example = "浙AA1234567")
    @Size(max = 64, message = "许可证号长度不能超过 64 个字符")
    private String scopeCode;

    @Schema(description = "联系人", example = "张三")
    @Size(max = 32, message = "联系人长度不能超过 32 个字符")
    private String contact;

    @Schema(description = "联系电话", example = "13800000000")
    @Size(max = 20, message = "联系电话长度不能超过 20 个字符")
    private String phone;

    @Schema(description = "地址", example = "杭州市余杭区XX路1号")
    @Size(max = 200, message = "地址长度不能超过 200 个字符")
    private String address;

    @Schema(description = "开户行", example = "中国工商银行杭州分行")
    @Size(max = 64, message = "开户行长度不能超过 64 个字符")
    private String bankName;

    @Schema(description = "银行账号（列表接口返回掩码）", example = "6222020200000000000")
    @Size(max = 64, message = "银行账号长度不能超过 64 个字符")
    private String bankAccount;

    @Schema(description = "账期", example = "月结30天")
    @Size(max = 64, message = "账期长度不能超过 64 个字符")
    private String paymentTerms;

    @Schema(description = "默认折扣率，0.00 ~ 1.00", example = "0.95")
    @DecimalMin(value = "0.00", message = "默认折扣率不能小于 0")
    @DecimalMax(value = "1.00", message = "默认折扣率不能大于 1")
    @Digits(integer = 3, fraction = 2, message = "默认折扣率最多保留 2 位小数")
    private BigDecimal defaultDiscount;

    @Schema(description = "状态 1启用/0停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    @InEnum(PharmacyStatusEnum.class)
    private Integer status;

}
