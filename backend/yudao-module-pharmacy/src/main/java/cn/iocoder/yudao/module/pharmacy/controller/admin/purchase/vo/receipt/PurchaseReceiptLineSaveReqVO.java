package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 采购收货明细 Request VO")
@Data
public class PurchaseReceiptLineSaveReqVO {

    @Schema(description = "明细编号（修改时后端按收货单整体重建明细，可不传）", example = "1024")
    private Long id;

    @Schema(description = "行号（不传时按顺序自动编号）", example = "1")
    private Integer lineNo;

    @Schema(description = "采购订单行编号（有单收货时必填）", example = "2048")
    private Long orderLineId;

    @Schema(description = "药品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "药品不能为空")
    private Long drugId;

    @Schema(description = "批号", requiredMode = Schema.RequiredMode.REQUIRED, example = "20260901A")
    @NotBlank(message = "批号不能为空")
    @Size(max = 64, message = "批号长度不能超过 64 个字符")
    private String batchNo;

    @Schema(description = "生产日期", example = "2026-08-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate manufactureDate;

    @Schema(description = "有效期至", requiredMode = Schema.RequiredMode.REQUIRED, example = "2028-08-01")
    @NotNull(message = "有效期至不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expiryDate;

    @Schema(description = "实收数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "40")
    @NotNull(message = "实收数量不能为空")
    @Min(value = 1, message = "实收数量必须大于 0")
    private Integer qty;

    @Schema(description = "采购单价（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12.50")
    @NotNull(message = "采购单价不能为空")
    @DecimalMin(value = "0.00", message = "采购单价不能小于 0")
    @Digits(integer = 16, fraction = 2, message = "采购单价最多保留 2 位小数")
    private BigDecimal unitPrice;

    @Schema(description = "入库目标货位（入账必填）", example = "1")
    private Long locationId;

    @Schema(description = "质检标记 0待检/1通过/2异常拒收", example = "1")
    private Integer qualityFlag;

    @Schema(description = "质检说明", example = "外包装完好")
    @Size(max = 200, message = "质检说明长度不能超过 200 个字符")
    private String qaRemark;

    @Schema(description = "冷链到货温度（℃）", example = "4.50")
    @Digits(integer = 3, fraction = 2, message = "冷链温度最多保留 2 位小数")
    private BigDecimal coldChainTemp;

}
