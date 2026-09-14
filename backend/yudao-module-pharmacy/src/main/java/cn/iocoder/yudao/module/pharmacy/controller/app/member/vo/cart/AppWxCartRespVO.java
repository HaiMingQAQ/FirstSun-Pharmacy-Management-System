package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "用户 APP - 购物车 Response VO")
@Data
public class AppWxCartRespVO {

    @Schema(description = "购物车编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "商品（药品）编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long drugId;

    @Schema(description = "商品名称（通用名）", example = "阿莫西林胶囊")
    private String drugName;

    @Schema(description = "规格", example = "0.25g*24粒")
    private String specification;

    @Schema(description = "销售单位", example = "盒")
    private String unit;

    @Schema(description = "是否处方药 0否/1是", example = "0")
    private Integer isRx;

    @Schema(description = "零售价", example = "19.90")
    private BigDecimal retailPrice;

    @Schema(description = "会员价", example = "18.90")
    private BigDecimal memberPrice;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer qty;

    @Schema(description = "是否勾选 1是/0否", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer selectedFlag;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long storeId;

    @Schema(description = "加购时间")
    private LocalDateTime addTime;

}
