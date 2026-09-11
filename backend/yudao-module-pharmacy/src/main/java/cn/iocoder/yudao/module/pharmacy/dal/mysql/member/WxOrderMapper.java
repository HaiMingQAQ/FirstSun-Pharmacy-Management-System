package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 小程序订单 Mapper
 */
@Mapper
public interface WxOrderMapper extends BaseMapperX<WxOrderDO> {

    default PageResult<WxOrderDO> selectPage(WxOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WxOrderDO>()
                .likeIfPresent(WxOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(WxOrderDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(WxOrderDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(WxOrderDO::getOrderType, reqVO.getOrderType())
                .eqIfPresent(WxOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(WxOrderDO::getPayStatus, reqVO.getPayStatus())
                .betweenIfPresent(WxOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WxOrderDO::getId));
    }

    default WxOrderDO selectByOrderNo(String orderNo) {
        return selectOne(WxOrderDO::getOrderNo, orderNo);
    }

    default List<WxOrderDO> selectListByMemberId(Long memberId) {
        return selectList(WxOrderDO::getMemberId, memberId);
    }

    default WxOrderDO selectByPickupCode(String pickupCode) {
        return selectOne(WxOrderDO::getPickupCode, pickupCode);
    }

}
