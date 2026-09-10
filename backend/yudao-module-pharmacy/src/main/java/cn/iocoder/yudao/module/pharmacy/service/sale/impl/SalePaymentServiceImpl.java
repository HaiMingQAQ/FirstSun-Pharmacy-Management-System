package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SalePaymentPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SalePaymentMapper;
import cn.iocoder.yudao.module.pharmacy.service.sale.SalePaymentService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class SalePaymentServiceImpl implements SalePaymentService {

    @Resource
    private SalePaymentMapper salePaymentMapper;

    @Override
    public PageResult<PhSalePaymentDO> getPaymentPage(SalePaymentPageReqVO reqVO) {
        return salePaymentMapper.selectPage(reqVO);
    }

    @Override
    public List<PhSalePaymentDO> getPaymentsByOrderId(Long orderId) {
        return salePaymentMapper.selectListByOrderId(orderId);
    }
}
