package cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo;

import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - POS 销售单详情 Response VO")
@Data
public class SaleOrderDetailRespVO {

    @Schema(description = "销售单头")
    private PhSaleOrderDO order;

    @Schema(description = "销售明细行")
    private List<PhSaleOrderLineDO> lines;

    @Schema(description = "支付明细")
    private List<PhSalePaymentDO> payments;

}
