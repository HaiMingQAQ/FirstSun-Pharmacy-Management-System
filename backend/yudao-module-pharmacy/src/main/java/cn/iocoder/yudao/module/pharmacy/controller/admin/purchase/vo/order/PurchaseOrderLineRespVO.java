package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 采购订单明细 Response VO")
@Data
public class PurchaseOrderLineRespVO {

    @Schema(description = "明细编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "行号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer lineNo;

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

    @Schema(description = "订购数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer orderQty;

    @Schema(description = "已收数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "40")
    private Integer receivedQty;

    @Schema(description = "含税单价（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12.50")
    private BigDecimal unitPrice;

    @Schema(description = "折扣率", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.95")
    private BigDecimal discountRate;

    @Schema(description = "行金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1187.50")
    private BigDecimal lineAmount;

    @Schema(description = "行备注", example = "急件")
    private String remark;

}
