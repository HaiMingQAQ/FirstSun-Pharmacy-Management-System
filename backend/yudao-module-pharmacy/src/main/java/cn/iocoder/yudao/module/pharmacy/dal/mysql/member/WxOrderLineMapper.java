package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.orderline.WxOrderLinePageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 小程序订单明细 Mapper
 */
@Mapper
public interface WxOrderLineMapper extends BaseMapperX<WxOrderLineDO> {

    default PageResult<WxOrderLineDO> selectPage(WxOrderLinePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WxOrderLineDO>()
                .eqIfPresent(WxOrderLineDO::getWxOrderId, reqVO.getWxOrderId())
                .eqIfPresent(WxOrderLineDO::getDrugId, reqVO.getDrugId())
                .likeIfPresent(WxOrderLineDO::getDrugName, reqVO.getDrugName())
                .orderByDesc(WxOrderLineDO::getId));
    }

    default List<WxOrderLineDO> selectListByWxOrderId(Long wxOrderId) {
        return selectList(WxOrderLineDO::getWxOrderId, wxOrderId);
    }

}
