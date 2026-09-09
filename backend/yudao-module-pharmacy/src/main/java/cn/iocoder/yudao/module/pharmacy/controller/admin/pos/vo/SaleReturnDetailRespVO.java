package cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo;

import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnLineDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - POS 退货单详情 Response VO")
@Data
public class SaleReturnDetailRespVO {

    @Schema(description = "退货单头")
    private PhSaleReturnDO returnOrder;

    @Schema(description = "退货明细行")
    private List<PhSaleReturnLineDO> lines;

}
