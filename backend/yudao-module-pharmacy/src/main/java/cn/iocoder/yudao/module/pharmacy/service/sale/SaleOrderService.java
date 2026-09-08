package cn.iocoder.yudao.module.pharmacy.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;

public interface SaleOrderService {

    /**
     * 创建销售单（收银）。服务端计算金额、校验幂等、同事务扣库存。
     */
    Long createSaleOrder(SaleOrderSaveReqVO reqVO);

    PageResult<PhSaleOrderDO> getSaleOrderPage(SaleOrderPageReqVO reqVO);

    PhSaleOrderDO getSaleOrder(Long id);
}
