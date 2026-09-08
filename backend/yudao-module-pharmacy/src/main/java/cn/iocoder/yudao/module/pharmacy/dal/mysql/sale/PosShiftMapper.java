package cn.iocoder.yudao.module.pharmacy.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.PosShiftPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhPosShiftDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PosShiftMapper extends BaseMapperX<PhPosShiftDO> {

    default PhPosShiftDO selectByShiftNo(String shiftNo) {
        return selectOne(PhPosShiftDO::getShiftNo, shiftNo);
    }

    default PageResult<PhPosShiftDO> selectPage(PosShiftPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PhPosShiftDO>()
                .eqIfPresent(PhPosShiftDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(PhPosShiftDO::getCashierId, reqVO.getCashierId())
                .eqIfPresent(PhPosShiftDO::getStatus, reqVO.getStatus())
                .orderByDesc(PhPosShiftDO::getOpenAt));
    }
}
