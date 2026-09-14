package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 供应商证照 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SupplierLicenseRespVO {

    @Schema(description = "证照编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("证照编号")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("供应商编号")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "国药控股有限公司")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "证照类型 0经营许可证/1生产许可证/2GSP证/3营业执照/4其他",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "证照类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_LICENSE_TYPE)
    private Integer licenseType;

    @Schema(description = "证照号", requiredMode = Schema.RequiredMode.REQUIRED, example = "浙AA1234567")
    @ExcelProperty("证照号")
    private String licenseNo;

    @Schema(description = "发证日期", example = "2025-01-01")
    @ExcelProperty("发证日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate issueDate;

    @Schema(description = "到期日", requiredMode = Schema.RequiredMode.REQUIRED, example = "2028-01-01")
    @ExcelProperty("到期日")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expireDate;

    @Schema(description = "影像文件地址")
    private String fileUrl;

    @Schema(description = "证照状态 1有效/0过期", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "证照状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_LICENSE_STATUS)
    private Integer status;

    @Schema(description = "距到期天数（负数表示已过期）", example = "120")
    private Long daysToExpire;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
