package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Schema(description = "管理后台 - 采购订单详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderDetailRespVO extends PurchaseOrderRespVO {

    @Schema(description = "采购明细")
    private List<PurchaseOrderLineRespVO> lines;

}
