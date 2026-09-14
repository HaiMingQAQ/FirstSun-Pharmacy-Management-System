package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户 APP - 小程序订单 Response VO")
@Data
public class AppWxOrderRespVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "线上订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WX-1-20260914-0001")
    private String orderNo;

    @Schema(description = "履约门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long storeId;

    @Schema(description = "订单类型 0到店自提/1同城配送", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer orderType;

    @Schema(description = "商品金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal goodsAmount;

    @Schema(description = "券抵扣", example = "10.00")
    private BigDecimal couponAmount;

    @Schema(description = "配送费", example = "5.00")
    private BigDecimal freightAmount;

    @Schema(description = "促销优惠", example = "5.00")
    private BigDecimal discountAmount;

    @Schema(description = "应付金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "90.00")
    private BigDecimal payableAmount;

    @Schema(description = "支付状态 0待支付/1已支付/2已退款", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer payStatus;

    @Schema(description = "支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "订单状态 0待支付/1待拣货/2拣货中/3待自提/4完成/-1取消", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "取消原因", example = "用户主动取消")
    private String cancelReason;

    @Schema(description = "配送地址快照", example = "北京市朝阳区某某街道123号")
    private String addressSnapshot;

    @Schema(description = "备注", example = "请尽快发货")
    private String remark;

    @Schema(description = "未支付截止时间")
    private LocalDateTime expireAt;

    @Schema(description = "一次性取货码", example = "ABC123")
    private String pickupCode;

    @Schema(description = "核销时间")
    private LocalDateTime verifyAt;

    @Schema(description = "完成时间")
    private LocalDateTime finishAt;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "订单明细")
    private List<AppWxOrderLineRespVO> lines;

}
