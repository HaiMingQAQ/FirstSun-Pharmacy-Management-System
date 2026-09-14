package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 小程序购物车 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WxCartRespVO {

    @Schema(description = "购物车编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("购物车编号")
    private Long id;

    @Schema(description = "会员用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("会员编号")
    private Long memberId;

    @Schema(description = "商品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("商品编号")
    private Long drugId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("数量")
    private Integer qty;

    @Schema(description = "是否勾选 1是/0否", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("是否勾选")
    private Integer selectedFlag;

    @Schema(description = "加购时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("加购时间")
    private LocalDateTime addTime;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("门店编号")
    private Long storeId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
