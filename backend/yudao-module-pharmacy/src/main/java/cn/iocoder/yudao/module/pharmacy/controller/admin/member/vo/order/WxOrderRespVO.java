package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 小程序订单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WxOrderRespVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("订单编号")
    private Long id;

    @Schema(description = "线上订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WX-001-20240101-0001")
    @ExcelProperty("订单号")
    private String orderNo;

    @Schema(description = "会员用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("会员编号")
    private Long memberId;

    @Schema(description = "履约门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("门店编号")
    private Long storeId;

    @Schema(description = "订单类型 0到店自提/1同城配送", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "订单类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_WX_ORDER_TYPE)
    private Integer orderType;

    @Schema(description = "商品金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @ExcelProperty("商品金额")
    private BigDecimal goodsAmount;

    @Schema(description = "券抵扣", example = "10.00")
    @ExcelProperty("券抵扣")
    private BigDecimal couponAmount;

    @Schema(description = "配送费", example = "5.00")
    @ExcelProperty("配送费")
    private BigDecimal freightAmount;

    @Schema(description = "促销优惠", example = "5.00")
    @ExcelProperty("促销优惠")
    private BigDecimal discountAmount;

    @Schema(description = "应付金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "90.00")
    @ExcelProperty("应付金额")
    private BigDecimal payableAmount;

    @Schema(description = "微信支付交易号", example = "4200001234202401011234567890")
    private String payNo;

    @Schema(description = "支付状态 0待支付/1已支付/2已退款", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "支付状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_WX_ORDER_PAY_STATUS)
    private Integer payStatus;

    @Schema(description = "支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "处方案编号", example = "1")
    private Long prescId;

    @Schema(description = "订单状态 0待支付/1待拣货/2拣货中/3待自提/4完成/-1取消", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "订单状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_WX_ORDER_STATUS)
    private Integer status;

    @Schema(description = "取消原因", example = "用户主动取消")
    private String cancelReason;

    @Schema(description = "配送地址快照", example = "北京市朝阳区某某街道123号")
    private String addressSnapshot;

    @Schema(description = "备注", example = "请尽快发货")
    private String remark;

    @Schema(description = "完成时间")
    private LocalDateTime finishAt;

    @Schema(description = "关联支付订单编号", example = "1")
    private Long payOrderId;

    @Schema(description = "未支付截止时间")
    private LocalDateTime expireAt;

    @Schema(description = "一次性取货码", example = "ABC123")
    private String pickupCode;

    @Schema(description = "核销员工编号", example = "1")
    private Long verifyBy;

    @Schema(description = "核销时间")
    private LocalDateTime verifyAt;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
