package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 会员用户 Service
 */
public interface MemberUserService {

    /**
     * 创建会员用户
     */
    Long createMemberUser(@Valid MemberUserSaveReqVO createReqVO);

    /**
     * 更新会员用户
     */
    void updateMemberUser(@Valid MemberUserSaveReqVO updateReqVO);

    /**
     * 删除会员用户
     */
    void deleteMemberUser(Long id);

    /**
     * 获取会员用户详情
     */
    MemberUserDO getMemberUser(Long id);

    /**
     * 批量获得会员用户列表
     *
     * @param ids 用户编号集合
     * @return 用户列表（仅返回未删除的）
     */
    List<MemberUserDO> getMemberUserList(Collection<Long> ids);

    /**
     * 获取会员用户分页
     */
    PageResult<MemberUserDO> getMemberUserPage(MemberUserPageReqVO reqVO);

    /**
     * 校验会员用户存在
     */
    MemberUserDO validateMemberUserExists(Long id);

    /**
     * 根据手机号获取会员用户
     */
    MemberUserDO getMemberUserByMobile(String mobile);

}
