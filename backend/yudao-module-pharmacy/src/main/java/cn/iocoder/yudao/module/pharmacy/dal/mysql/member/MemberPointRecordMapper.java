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

    /**
     * 按会员 + 业务类型 + 业务编码前缀查询积分记录。
     *
     * <p>用于「同一原单多次部分退货」的累计回退计算：业务编码形如
     * {@code 原单号#EARN#退货单号} / {@code 原单号#DEDUCT#退货单号}，
     * 每一张退货单各自幂等，同时可按原单号前缀汇总已回退的积分。
     */
    default List<MemberPointRecordDO> selectListByUserIdAndBizTypeAndBizIdPrefix(
            Long userId, Integer bizType, String bizIdPrefix) {
        if (userId == null || bizType == null || bizIdPrefix == null || bizIdPrefix.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MemberPointRecordDO>()
                .eq(MemberPointRecordDO::getUserId, userId)
                .eq(MemberPointRecordDO::getBizType, bizType)
                .likeRight(MemberPointRecordDO::getBizId, bizIdPrefix));
    }

}
