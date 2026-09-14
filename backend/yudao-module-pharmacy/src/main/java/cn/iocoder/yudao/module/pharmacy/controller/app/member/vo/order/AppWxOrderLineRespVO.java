package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 APP - 小程序订单明细 Response VO")
@Data
public class AppWxOrderLineRespVO {

    @Schema(description = "明细编号", example = "1024")
    private Long id;

    @Schema(description = "商品（药品）编号", example = "1")
    private Long drugId;

    @Schema(description = "商品名称快照", example = "阿莫西林胶囊")
    private String drugName;

    @Schema(description = "规格快照", example = "0.25g*24粒")
    private String specification;

    @Schema(description = "单位快照", example = "盒")
    private String unit;

    @Schema(description = "数量", example = "2")
    private Integer qty;

    @Schema(description = "已拣数量", example = "2")
    private Integer pickedQty;

    @Schema(description = "单价", example = "19.90")
    private BigDecimal price;

    @Schema(description = "金额", example = "39.80")
    private BigDecimal lineAmount;

    @Schema(description = "批号", example = "B20260101")
    private String batchNo;

}
