package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 会员积分记录 Mapper
 */
@Mapper
public interface MemberPointRecordMapper extends BaseMapperX<MemberPointRecordDO> {

    default PageResult<MemberPointRecordDO> selectPage(MemberPointRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MemberPointRecordDO>()
                .eqIfPresent(MemberPointRecordDO::getUserId, reqVO.getUserId())
                .eqIfPresent(MemberPointRecordDO::getBizType, reqVO.getBizType())
                .likeIfPresent(MemberPointRecordDO::getTitle, reqVO.getTitle())
                .betweenIfPresent(MemberPointRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MemberPointRecordDO::getId));
    }

    default List<MemberPointRecordDO> selectListByUserId(Long userId) {
        return selectList(MemberPointRecordDO::getUserId, userId);
    }

    /**
     * 按会员 + 业务编码查询积分记录，用于积分幂等校验（同一业务单号只处理一次）
     */
    default MemberPointRecordDO selectByUserIdAndBizId(Long userId, String bizId) {
        return selectOne(new LambdaQueryWrapperX<MemberPointRecordDO>()
                .eq(MemberPointRecordDO::getUserId, userId)
                .eq(MemberPointRecordDO::getBizId, bizId));
    }

    /**
     * 按会员 + 业务类型 + 业务编码查询积分记录。
     *
     * <p>幂等键与 {@code member_point_record.uk_point_event(tenant_id,user_id,biz_type,biz_id)} 一致，
     * 同一订单号可以分别产生「消费获得」「消费抵扣」「退款冲回」各一条流水。
     */
    default MemberPointRecordDO selectByUserIdAndBizTypeAndBizId(Long userId, Integer bizType, String bizId) {
        return selectOne(new LambdaQueryWrapperX<MemberPointRecordDO>()
                .eq(MemberPointRecordDO::getUserId, userId)
                .eq(MemberPointRecordDO::getBizType, bizType)
                .eq(MemberPointRecordDO::getBizId, bizId));
    }

}
