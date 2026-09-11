package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.pharmacy.enums.SupplierLicenseTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 供应商证照创建/修改 Request VO")
@Data
public class SupplierLicenseSaveReqVO {

    @Schema(description = "证照编号", example = "1024")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "所属供应商不能为空")
    private Long supplierId;

    @Schema(description = "证照类型 0经营许可证/1生产许可证/2GSP证/3营业执照/4其他",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "证照类型不能为空")
    @InEnum(SupplierLicenseTypeEnum.class)
    private Integer licenseType;

    @Schema(description = "证照号", requiredMode = Schema.RequiredMode.REQUIRED, example = "浙AA1234567")
    @NotBlank(message = "证照号不能为空")
    @Size(max = 64, message = "证照号长度不能超过 64 个字符")
    private String licenseNo;

    @Schema(description = "发证日期", example = "2025-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate issueDate;

    @Schema(description = "到期日", requiredMode = Schema.RequiredMode.REQUIRED, example = "2028-01-01")
    @NotNull(message = "到期日不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expireDate;

    @Schema(description = "影像文件地址", example = "https://example.com/license.pdf")
    @Size(max = 512, message = "影像文件地址长度不能超过 512 个字符")
    private String fileUrl;

}
