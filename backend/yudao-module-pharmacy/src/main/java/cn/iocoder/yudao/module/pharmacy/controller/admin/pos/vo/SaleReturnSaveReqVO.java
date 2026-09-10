package cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 退货单创建 Request VO")
@Data
public class SaleReturnSaveReqVO {

    @Schema(description = "原销售单")
    @NotNull(message = "原销售单不能为空")
    private Long saleOrderId;

    @Schema(description = "0 退货 / 1 换货")
    @NotNull(message = "退货类型不能为空")
    private Integer returnType;

    @Schema(description = "原因字典")
    @NotNull(message = "退货原因不能为空")
    private Integer reason;

    @Schema(description = "0 原路 / 1 现金 / 2 余额")
    private Integer refundMethod;

    @Schema(description = "药师复核(处方药退货必须)")
    private Integer pharmacistConfirm;

    @Schema(description = "经办人(收银员编号，为空时取原销售单收银员)")
    private Long cashierId;

    @Schema(description = "备注")
    private String remark;

    @NotEmpty(message = "退货明细不能为空")
    private List<@Valid Item> items;

    @Data
    public static class Item {

        @NotNull(message = "销售明细行不能为空")
        private Long saleOrderLineId;

        @NotNull(message = "退货数量不能为空")
        private Integer qty;

        /** 验收回库货位 */
        private Long locationId;
    }
}
