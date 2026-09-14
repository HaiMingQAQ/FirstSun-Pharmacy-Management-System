package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart.WxCartPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxCartDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 小程序购物车 Mapper
 *
 * 说明：购物车属临时数据，且唯一键 {@code uk_member_drug(tenant_id,member_id,store_id,drug_id)}
 * 未包含 {@code deleted} 字段，逻辑删除会导致同一商品无法再次加购，
 * 因此购物车的删除统一采用物理删除。
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

    default List<WxCartDO> selectSelectedListByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapperX<WxCartDO>()
                .eq(WxCartDO::getMemberId, memberId)
                .eq(WxCartDO::getSelectedFlag, 1)
                .orderByDesc(WxCartDO::getId));
    }

    default WxCartDO selectByMemberIdAndDrugIdAndStoreId(Long memberId, Long drugId, Long storeId) {
        return selectOne(new LambdaQueryWrapperX<WxCartDO>()
                .eq(WxCartDO::getMemberId, memberId)
                .eq(WxCartDO::getDrugId, drugId)
                .eq(WxCartDO::getStoreId, storeId));
    }

    /**
     * 物理删除单条购物车记录（购物车为临时数据，不走逻辑删除）
     */
    @Delete("DELETE FROM ph_wx_cart WHERE id = #{id}")
    int deleteByIdPhysical(@Param("id") Long id);

    /**
     * 物理清空指定会员的购物车
     */
    @Delete("DELETE FROM ph_wx_cart WHERE member_id = #{memberId}")
    int deleteByMemberIdPhysical(@Param("memberId") Long memberId);

    /**
     * 物理清理同一会员+门店+商品的历史残留记录（含逻辑删除的行），
     * 避免唯一键 {@code uk_member_drug} 冲突导致无法加购。
     */
    @Delete("DELETE FROM ph_wx_cart WHERE member_id = #{memberId} AND store_id = #{storeId} AND drug_id = #{drugId}")
    int deleteByKeyPhysical(@Param("memberId") Long memberId, @Param("storeId") Long storeId,
                            @Param("drugId") Long drugId);

}
