package cn.iocoder.yudao.module.pharmacy.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SalePaymentPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SalePaymentMapper extends BaseMapperX<PhSalePaymentDO> {

    default PhSalePaymentDO selectByPaymentNo(String paymentNo) {
        return selectOne(PhSalePaymentDO::getPaymentNo, paymentNo);
    }

    default List<PhSalePaymentDO> selectListByOrderId(Long orderId) {
        return selectList(PhSalePaymentDO::getOrderId, orderId);
    }

    default PageResult<PhSalePaymentDO> selectPage(SalePaymentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PhSalePaymentDO>()
                .eqIfPresent(PhSalePaymentDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(PhSalePaymentDO::getPayMethod, reqVO.getPayMethod())
                .eqIfPresent(PhSalePaymentDO::getStatus, reqVO.getStatus()));
    }
}
