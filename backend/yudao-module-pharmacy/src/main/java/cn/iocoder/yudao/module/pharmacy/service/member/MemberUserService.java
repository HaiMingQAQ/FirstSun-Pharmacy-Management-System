package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;

import jakarta.validation.Valid;
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
     * 更新会员状态（仅更新 status，用于启用 / 停用）
     *
     * @param id     会员编号
     * @param status 状态（0 禁用 / 1 启用）
     */
    void updateMemberUserStatus(Long id, Integer status);

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

    // ========== 小程序端（app）相关 ==========

    /**
     * 校验原始密码与加密密码是否匹配
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     */
    boolean isPasswordMatch(String rawPassword, String encodedPassword);

    /**
     * 更新会员最后登录信息（登录 IP + 登录时间）
     */
    void updateMemberUserLogin(Long id, String loginIp);

    /**
     * 获取手机号对应的会员，不存在则自动注册（小程序首次登录）
     *
     * @param mobile 手机号
     * @param ip     注册 IP
     * @return 会员信息
     */
    MemberUserDO createMemberUserIfAbsent(String mobile, String ip);

    /**
     * 更新会员个人资料（小程序个人中心）
     *
     * 仅允许修改昵称、头像、性别，不允许修改手机号、积分、等级等敏感字段。
     */
    void updateMemberUserProfile(Long id, String nickname, String avatar, Integer sex);

}
