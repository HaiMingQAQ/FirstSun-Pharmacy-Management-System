package cn.iocoder.yudao.module.pharmacy.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SalePaymentPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;

import java.util.List;

public interface SalePaymentService {

    PageResult<PhSalePaymentDO> getPaymentPage(SalePaymentPageReqVO reqVO);

    List<PhSalePaymentDO> getPaymentsByOrderId(Long orderId);
}
