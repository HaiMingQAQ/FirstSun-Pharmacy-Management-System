package cn.iocoder.yudao.module.pharmacy.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SaleReturnMapper extends BaseMapperX<PhSaleReturnDO> {

    default PhSaleReturnDO selectByReturnNo(String returnNo) {
        return selectOne(PhSaleReturnDO::getReturnNo, returnNo);
    }

    default PageResult<PhSaleReturnDO> selectPage(SaleReturnPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PhSaleReturnDO>()
                .eqIfPresent(PhSaleReturnDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(PhSaleReturnDO::getSaleOrderId, reqVO.getSaleOrderId())
                .likeIfPresent(PhSaleReturnDO::getReturnNo, reqVO.getReturnNo())
                .eqIfPresent(PhSaleReturnDO::getStatus, reqVO.getStatus())
                .orderByDesc(PhSaleReturnDO::getCreateTime));
    }
}
