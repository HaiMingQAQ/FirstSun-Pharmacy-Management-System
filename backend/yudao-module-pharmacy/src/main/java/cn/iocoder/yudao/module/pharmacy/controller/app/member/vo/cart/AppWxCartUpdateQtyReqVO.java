package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "用户 APP - 购物车修改数量 Request VO")
@Data
public class AppWxCartUpdateQtyReqVO {

    @Schema(description = "购物车编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "购物车编号不能为空")
    private Long id;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer qty;

}
