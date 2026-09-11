package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 会员用户 Service 实现类
 */
@Service
@Validated
public class MemberUserServiceImpl implements MemberUserService {

    @Resource
    private MemberUserMapper memberUserMapper;

    @Override
    public Long createMemberUser(MemberUserSaveReqVO createReqVO) {
        // 校验手机号唯一
        validateMobileUnique(null, createReqVO.getMobile());
        // 写入
        MemberUserDO memberUser = BeanUtils.toBean(createReqVO, MemberUserDO.class);
        memberUserMapper.insert(memberUser);
        return memberUser.getId();
    }

    @Override
    public void updateMemberUser(MemberUserSaveReqVO updateReqVO) {
        // 校验存在
        validateMemberUserExists(updateReqVO.getId());
        // 校验手机号唯一
        validateMobileUnique(updateReqVO.getId(), updateReqVO.getMobile());
        // 更新
        MemberUserDO updateObj = BeanUtils.toBean(updateReqVO, MemberUserDO.class);
        memberUserMapper.updateById(updateObj);
    }

    @Override
    public void deleteMemberUser(Long id) {
        // 校验存在
        validateMemberUserExists(id);
        // 删除
        memberUserMapper.deleteById(id);
    }

    @Override
    public MemberUserDO getMemberUser(Long id) {
        return memberUserMapper.selectById(id);
    }

    @Override
    public List<MemberUserDO> getMemberUserList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return memberUserMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<MemberUserDO> getMemberUserPage(MemberUserPageReqVO reqVO) {
        return memberUserMapper.selectPage(reqVO);
    }

    @Override
    public MemberUserDO validateMemberUserExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_MEMBER_USER_NOT_EXISTS);
        }
        MemberUserDO memberUser = memberUserMapper.selectById(id);
        if (memberUser == null) {
            throw exception(PHARMACY_MEMBER_USER_NOT_EXISTS);
        }
        return memberUser;
    }

    @Override
    public MemberUserDO getMemberUserByMobile(String mobile) {
        return memberUserMapper.selectByMobile(mobile);
    }

    private void validateMobileUnique(Long id, String mobile) {
        MemberUserDO memberUser = memberUserMapper.selectByMobile(mobile);
        if (memberUser == null) {
            return;
        }
        if (id == null) {
            throw exception(PHARMACY_MEMBER_USER_MOBILE_DUPLICATE);
        }
        if (!Objects.equals(memberUser.getId(), id)) {
            throw exception(PHARMACY_MEMBER_USER_MOBILE_DUPLICATE);
        }
    }

}
