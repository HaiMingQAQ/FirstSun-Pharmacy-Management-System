package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 采购订单明细 Request VO")
@Data
public class PurchaseOrderLineSaveReqVO {

    @Schema(description = "明细编号（修改时后端按订单整体重建明细，可不传）", example = "1024")
    private Long id;

    @Schema(description = "行号（不传时按顺序自动编号）", example = "1")
    private Integer lineNo;

    @Schema(description = "药品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "药品不能为空")
    private Long drugId;

    @Schema(description = "订购数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "订购数量不能为空")
    @Min(value = 1, message = "订购数量必须大于 0")
    private Integer orderQty;

    @Schema(description = "含税单价（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12.50")
    @NotNull(message = "含税单价不能为空")
    @DecimalMin(value = "0.00", message = "含税单价不能小于 0")
    @Digits(integer = 16, fraction = 2, message = "含税单价最多保留 2 位小数")
    private BigDecimal unitPrice;

    @Schema(description = "折扣率 0 ~ 1，默认 1 表示不打折", example = "0.95")
    @DecimalMin(value = "0.00", message = "折扣率不能小于 0")
    @DecimalMax(value = "1.00", message = "折扣率不能大于 1")
    @Digits(integer = 3, fraction = 2, message = "折扣率最多保留 2 位小数")
    private BigDecimal discountRate;

    @Schema(description = "行备注", example = "急件")
    @Size(max = 200, message = "行备注长度不能超过 200 个字符")
    private String remark;

}
