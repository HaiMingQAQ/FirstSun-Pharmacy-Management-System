package cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 销售单创建 Request VO")
@Data
public class SaleOrderSaveReqVO {

    @Schema(description = "销售单号(前端预生成, 幂等键)")
    @NotEmpty(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "门店编号")
    @NotNull(message = "门店编号不能为空")
    private Long storeId;

    @Schema(description = "班次编号")
    private Long shiftId;

    @Schema(description = "收银员编号")
    private Long cashierId;

    @Schema(description = "收银台号")
    private String posNo;

    @Schema(description = "会员编号")
    private Long memberId;

    @Schema(description = "顾客姓名(散客)")
    private String customerName;

    @Schema(description = "商品明细")
    @NotEmpty(message = "销售商品不能为空")
    private List<@Valid Item> items;

    @Schema(description = "支付明细")
    @NotEmpty(message = "销售支付信息不能为空")
    private List<@Valid Payment> payments;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "商品行")
    @Data
    public static class Item {

        @NotNull(message = "药品不能为空")
        private Long drugId;

        @NotNull(message = "批次不能为空")
        private Long batchId;

        @NotNull(message = "数量不能为空")
        private Integer qty;

        @NotNull(message = "成交价不能为空")
        private BigDecimal price;

        /** 是否处方药 0/1 */
        private Integer isRx;

        /** 关联处方 */
        private Long prescId;

        /** 出库货位 */
        private Long locationId;

        /** 是否赠品 */
        private Integer isGift;

        /** 商品名称快照(用于小票) */
        @NotEmpty(message = "商品名称不能为空")
        private String drugName;

        /** 规格快照 */
        private String specification;

        /** 单位快照 */
        private String unit;
    }

    @Schema(description = "支付行")
    @Data
    public static class Payment {

        /** 1现金/2微信/3支付宝/4银行卡/5储值/6医保/7积分 */
        @NotNull(message = "支付方式不能为空")
        private Integer payMethod;

        @NotNull(message = "支付金额不能为空")
        private BigDecimal payAmount;

        /** 渠道(如 wxpay/alipay) */
        private String channel;

        /** 外部交易号(现金可不填) */
        private String payNo;

        /** 本系统支付幂等号(现金也生成) */
        @NotNull(message = "支付幂等号不能为空")
        private String paymentNo;
    }
}
