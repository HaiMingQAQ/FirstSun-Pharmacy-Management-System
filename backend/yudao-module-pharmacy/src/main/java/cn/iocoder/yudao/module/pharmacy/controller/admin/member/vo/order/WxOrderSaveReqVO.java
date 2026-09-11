package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 小程序订单创建/修改 Request VO")
@Data
public class WxOrderSaveReqVO {

    @Schema(description = "订单编号", example = "1024")
    private Long id;

    @Schema(description = "线上订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WX-001-20240101-0001")
    @NotBlank(message = "线上订单号不能为空")
    @Size(max = 32, message = "线上订单号长度不能超过 32 个字符")
    private String orderNo;

    @Schema(description = "会员用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "会员用户编号不能为空")
    private Long memberId;

    @Schema(description = "履约门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "履约门店编号不能为空")
    private Long storeId;

    @Schema(description = "订单类型 0到店自提/1同城配送", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "订单类型不能为空")
    private Integer orderType;

    @Schema(description = "商品金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "商品金额不能为空")
    @DecimalMin(value = "0", message = "商品金额不能为负数")
    private BigDecimal goodsAmount;

    @Schema(description = "券抵扣", example = "10.00")
    private BigDecimal couponAmount;

    @Schema(description = "配送费", example = "5.00")
    private BigDecimal freightAmount;

    @Schema(description = "促销优惠", example = "5.00")
    private BigDecimal discountAmount;

    @Schema(description = "应付金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "90.00")
    @NotNull(message = "应付金额不能为空")
    @DecimalMin(value = "0", message = "应付金额不能为负数")
    private BigDecimal payableAmount;

    @Schema(description = "微信支付交易号", example = "4200001234202401011234567890")
    private String payNo;

    @Schema(description = "支付状态 0待支付/1已支付/2已退款", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "支付状态不能为空")
    private Integer payStatus;

    @Schema(description = "支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "处方案编号", example = "1")
    private Long prescId;

    @Schema(description = "订单状态 0待支付/1待拣货/2拣货中/3待自提/4完成/-1取消", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "订单状态不能为空")
    private Integer status;

    @Schema(description = "取消原因", example = "用户主动取消")
    @Size(max = 200, message = "取消原因长度不能超过 200 个字符")
    private String cancelReason;

    @Schema(description = "配送地址快照", example = "北京市朝阳区某某街道123号")
    @Size(max = 300, message = "配送地址快照长度不能超过 300 个字符")
    private String addressSnapshot;

    @Schema(description = "备注", example = "请尽快发货")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "完成时间")
    private LocalDateTime finishAt;

    @Schema(description = "关联支付订单编号", example = "1")
    private Long payOrderId;

    @Schema(description = "未支付截止时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "未支付截止时间不能为空")
    private LocalDateTime expireAt;

    @Schema(description = "一次性取货码", example = "ABC123")
    private String pickupCode;

    @Schema(description = "核销员工编号", example = "1")
    private Long verifyBy;

    @Schema(description = "核销时间")
    private LocalDateTime verifyAt;

}
