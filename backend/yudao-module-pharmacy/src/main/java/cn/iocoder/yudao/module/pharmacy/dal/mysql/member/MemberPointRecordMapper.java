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

}
