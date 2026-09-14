package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 采购收货明细 Response VO")
@Data
public class PurchaseReceiptLineRespVO {

    @Schema(description = "明细编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "收货单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long receiptId;

    @Schema(description = "行号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer lineNo;

    @Schema(description = "采购订单行编号", example = "2048")
    private Long orderLineId;

    @Schema(description = "药品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long drugId;

    @Schema(description = "药品编码", example = "DRG001")
    private String drugCode;

    @Schema(description = "药品通用名", example = "阿莫西林胶囊")
    private String drugName;

    @Schema(description = "规格", example = "0.25g*24粒")
    private String specification;

    @Schema(description = "单位", example = "盒")
    private String unit;

    @Schema(description = "批号", requiredMode = Schema.RequiredMode.REQUIRED, example = "20260901A")
    private String batchNo;

    @Schema(description = "生产日期", example = "2026-08-01")
    private LocalDate manufactureDate;

    @Schema(description = "有效期至", requiredMode = Schema.RequiredMode.REQUIRED, example = "2028-08-01")
    private LocalDate expiryDate;

    @Schema(description = "实收数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "40")
    private Integer qty;

    @Schema(description = "采购单价（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12.50")
    private BigDecimal unitPrice;

    @Schema(description = "金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    private BigDecimal amount;

    @Schema(description = "入库目标货位", example = "1")
    private Long locationId;

    @Schema(description = "质检标记 0待检/1通过/2异常拒收", example = "1")
    private Integer qualityFlag;

    @Schema(description = "质检说明", example = "外包装完好")
    private String qaRemark;

    @Schema(description = "冷链到货温度（℃）", example = "4.50")
    private BigDecimal coldChainTemp;

    @Schema(description = "入账生成的批次编号（库存服务回写）", example = "1")
    private Long createBatchId;

}
