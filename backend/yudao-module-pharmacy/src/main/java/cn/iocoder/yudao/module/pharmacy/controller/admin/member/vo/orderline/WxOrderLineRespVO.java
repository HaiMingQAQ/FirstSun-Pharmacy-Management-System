package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.orderline;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 小程序订单明细 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WxOrderLineRespVO {

    @Schema(description = "明细编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("明细编号")
    private Long id;

    @Schema(description = "线上订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("订单编号")
    private Long wxOrderId;

    @Schema(description = "商品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("商品编号")
    private Long drugId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("数量")
    private Integer qty;

    @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "25.00")
    @ExcelProperty("单价")
    private BigDecimal price;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "50.00")
    @ExcelProperty("金额")
    private BigDecimal lineAmount;

    @Schema(description = "拣货批次编号", example = "1")
    private Long batchId;

    @Schema(description = "批号", example = "20240101")
    @ExcelProperty("批号")
    private String batchNo;

    @Schema(description = "已拣数量", example = "2")
    @ExcelProperty("已拣数量")
    private Integer pickedQty;

    @Schema(description = "锁定时绑定的货位编号", example = "1")
    private Long locationId;

    @Schema(description = "下单名称快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "阿莫西林")
    @ExcelProperty("商品名称")
    private String drugName;

    @Schema(description = "规格快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.5g*24粒")
    @ExcelProperty("规格")
    private String specification;

    @Schema(description = "单位快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "盒")
    @ExcelProperty("单位")
    private String unit;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
