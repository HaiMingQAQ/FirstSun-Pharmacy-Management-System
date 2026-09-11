package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 供应商证照分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierLicensePageReqVO extends PageParam {

    @Schema(description = "供应商编号", example = "1024")
    private Long supplierId;

    @Schema(description = "证照类型 0经营许可证/1生产许可证/2GSP证/3营业执照/4其他", example = "0")
    private Integer licenseType;

    @Schema(description = "证照号，模糊匹配", example = "浙AA")
    private String licenseNo;

    @Schema(description = "证照状态 1有效/0过期", example = "1")
    private Integer status;

    @Schema(description = "到期日范围（数组：起、止）")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] expireDate;

}
