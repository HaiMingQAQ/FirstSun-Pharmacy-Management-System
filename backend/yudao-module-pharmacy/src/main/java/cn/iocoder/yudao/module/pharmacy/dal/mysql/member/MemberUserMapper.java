package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import org.apache.ibatis.annotations.Mapper;

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

}
