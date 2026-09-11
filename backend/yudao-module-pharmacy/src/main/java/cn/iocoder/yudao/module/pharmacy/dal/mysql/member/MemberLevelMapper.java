package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level.MemberLevelPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberLevelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会员等级 Mapper
 */
@Mapper
public interface MemberLevelMapper extends BaseMapperX<MemberLevelDO> {

    default PageResult<MemberLevelDO> selectPage(MemberLevelPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MemberLevelDO>()
                .likeIfPresent(MemberLevelDO::getName, reqVO.getName())
                .eqIfPresent(MemberLevelDO::getStatus, reqVO.getStatus())
                .orderByAsc(MemberLevelDO::getLevel));
    }

    default MemberLevelDO selectByLevel(Integer level) {
        return selectOne(MemberLevelDO::getLevel, level);
    }

    default List<MemberLevelDO> selectListByStatus(Integer status) {
        return selectList(MemberLevelDO::getStatus, status);
    }

    /**
     * 统计某等级下未删除的会员数量。
     *
     * 用于删除等级前校验：存在引用时拒绝删除。
     */
    @Select("SELECT COUNT(*) FROM member_user WHERE level_id = #{levelId} AND deleted = 0")
    Long countUsersByLevelId(@Param("levelId") Long levelId);

}
