package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Schema(description = "管理后台 - 采购收货单详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseReceiptDetailRespVO extends PurchaseReceiptRespVO {

    @Schema(description = "收货明细")
    private List<PurchaseReceiptLineRespVO> lines;

}
