package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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

}
