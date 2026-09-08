package cn.iocoder.yudao.module.pharmacy.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SaleReturnLineMapper extends BaseMapperX<PhSaleReturnLineDO> {

    default List<PhSaleReturnLineDO> selectListByReturnId(Long returnId) {
        return selectList(PhSaleReturnLineDO::getReturnId, returnId);
    }

    /** 某销售行累计已退数量(可退量校验用) */
    default int selectSumQtyBySaleLineId(Long saleLineId) {
        List<PhSaleReturnLineDO> list = selectList(PhSaleReturnLineDO::getSaleLineId, saleLineId);
        return list.stream().mapToInt(PhSaleReturnLineDO::getQty).sum();
    }
}
