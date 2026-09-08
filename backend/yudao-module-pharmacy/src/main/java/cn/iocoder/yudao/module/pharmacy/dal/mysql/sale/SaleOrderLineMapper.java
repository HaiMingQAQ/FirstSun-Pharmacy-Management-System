package cn.iocoder.yudao.module.pharmacy.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SaleOrderLineMapper extends BaseMapperX<PhSaleOrderLineDO> {

    default List<PhSaleOrderLineDO> selectListByOrderId(Long orderId) {
        return selectList(PhSaleOrderLineDO::getOrderId, orderId);
    }

    default List<PhSaleOrderLineDO> selectListByOrderIds(List<Long> orderIds) {
        return selectList(PhSaleOrderLineDO::getOrderId, orderIds);
    }
}
