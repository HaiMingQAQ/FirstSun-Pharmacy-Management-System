package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address.MemberAddressPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberAddressDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 会员收件地址 Mapper
 */
@Mapper
public interface MemberAddressMapper extends BaseMapperX<MemberAddressDO> {

    default PageResult<MemberAddressDO> selectPage(MemberAddressPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MemberAddressDO>()
                .eqIfPresent(MemberAddressDO::getUserId, reqVO.getUserId())
                .likeIfPresent(MemberAddressDO::getName, reqVO.getName())
                .likeIfPresent(MemberAddressDO::getMobile, reqVO.getMobile())
                .orderByDesc(MemberAddressDO::getId));
    }

    default List<MemberAddressDO> selectListByUserId(Long userId) {
        return selectList(MemberAddressDO::getUserId, userId);
    }

    default MemberAddressDO selectDefaultByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<MemberAddressDO>()
                .eq(MemberAddressDO::getUserId, userId)
                .eq(MemberAddressDO::getDefaultStatus, true));
    }

}
