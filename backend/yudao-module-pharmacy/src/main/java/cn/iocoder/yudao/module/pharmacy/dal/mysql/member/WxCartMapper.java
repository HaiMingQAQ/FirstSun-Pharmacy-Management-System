package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart.WxCartPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxCartDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 小程序购物车 Mapper
 */
@Mapper
public interface WxCartMapper extends BaseMapperX<WxCartDO> {

    default PageResult<WxCartDO> selectPage(WxCartPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WxCartDO>()
                .eqIfPresent(WxCartDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(WxCartDO::getDrugId, reqVO.getDrugId())
                .eqIfPresent(WxCartDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(WxCartDO::getSelectedFlag, reqVO.getSelectedFlag())
                .orderByDesc(WxCartDO::getId));
    }

    default List<WxCartDO> selectListByMemberId(Long memberId) {
        return selectList(WxCartDO::getMemberId, memberId);
    }

    default WxCartDO selectByMemberIdAndDrugIdAndStoreId(Long memberId, Long drugId, Long storeId) {
        return selectOne(new LambdaQueryWrapperX<WxCartDO>()
                .eq(WxCartDO::getMemberId, memberId)
                .eq(WxCartDO::getDrugId, drugId)
                .eq(WxCartDO::getStoreId, storeId));
    }

}
