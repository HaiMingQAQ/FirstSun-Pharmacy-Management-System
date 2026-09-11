package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 小程序购物车创建/修改 Request VO")
@Data
public class WxCartSaveReqVO {

    @Schema(description = "购物车编号", example = "1024")
    private Long id;

    @Schema(description = "会员用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "会员用户编号不能为空")
    private Long memberId;

    @Schema(description = "商品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "商品编号不能为空")
    private Long drugId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer qty;

    @Schema(description = "是否勾选 1是/0否", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "是否勾选不能为空")
    private Integer selectedFlag;

    @Schema(description = "加购时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "加购时间不能为空")
    private LocalDateTime addTime;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "门店编号不能为空")
    private Long storeId;

}
