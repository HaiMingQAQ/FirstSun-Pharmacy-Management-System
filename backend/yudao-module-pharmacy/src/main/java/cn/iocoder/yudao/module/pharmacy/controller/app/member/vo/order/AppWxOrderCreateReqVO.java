package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "用户 APP - 小程序订单创建 Request VO（从购物车已勾选商品下单）")
@Data
public class AppWxOrderCreateReqVO {

    @Schema(description = "履约门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "履约门店编号不能为空")
    private Long storeId;

    @Schema(description = "订单类型 0到店自提/1同城配送", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "订单类型不能为空")
    private Integer orderType;

    @Schema(description = "收货地址编号（同城配送时必填）", example = "1024")
    private Long addressId;

    @Schema(description = "处方案编号（处方药线上自提时必填）", example = "1")
    private Long prescId;

    @Schema(description = "备注", example = "请尽快发货")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "希望使用的抵扣积分（0 或不传表示不使用；实际可用值由后端按余额、抵扣比例与单笔上限校验）",
            example = "100")
    @Min(value = 0, message = "抵扣积分不能为负数")
    private Integer usePoints;

}
