package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 会员用户 Mapper
 */
@Mapper
public interface MemberUserMapper extends BaseMapperX<MemberUserDO> {

    default PageResult<MemberUserDO> selectPage(MemberUserPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MemberUserDO>()
                .likeIfPresent(MemberUserDO::getMobile, reqVO.getMobile())
                .likeIfPresent(MemberUserDO::getNickname, reqVO.getNickname())
                .eqIfPresent(MemberUserDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MemberUserDO::getLevelId, reqVO.getLevelId())
                .orderByDesc(MemberUserDO::getId));
    }

    default MemberUserDO selectByMobile(String mobile) {
        return selectOne(MemberUserDO::getMobile, mobile);
    }

    default MemberUserDO selectByIdAndTenantId(Long id, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<MemberUserDO>()
                .eq(MemberUserDO::getId, id)
                .eq(MemberUserDO::getTenantId, tenantId));
    }

    /**
     * 原子增减会员积分（正数增加、负数扣减），避免并发下的覆盖写
     *
     * @param id    会员编号
     * @param delta 积分变动值
     * @return 影响行数
     */
    @Update("UPDATE member_user SET point = point + #{delta} WHERE id = #{id} AND deleted = 0")
    int updatePointIncr(@Param("id") Long id, @Param("delta") Integer delta);

}
