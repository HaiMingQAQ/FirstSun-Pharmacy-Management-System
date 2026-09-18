package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.point;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 APP - 积分抵扣试算 Request VO")
@Data
public class AppMemberPointDeductPreviewReqVO {

    @Schema(description = "订单金额（元，抵扣前）", requiredMode = Schema.RequiredMode.REQUIRED, example = "88.00")
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.00", message = "订单金额不能为负数")
    private BigDecimal orderAmount;

    @Schema(description = "希望使用的抵扣积分（为空或 0 表示不使用）", example = "1000")
    @Min(value = 0, message = "抵扣积分不能为负数")
    private Integer usePoints;

}
