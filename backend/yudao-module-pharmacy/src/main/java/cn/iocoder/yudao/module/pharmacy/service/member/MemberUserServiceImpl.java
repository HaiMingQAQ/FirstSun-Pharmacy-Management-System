package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberUserMapper;
import cn.hutool.core.util.StrUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
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

    /**
     * 密码加密器：与 yudao 框架保持一致（BCrypt），复用框架能力，不重复造轮子
     */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /** 会员状态：0=启用（框架标准，与 pharmacy 其他表方向相反） */
    private static final Integer STATUS_ENABLE = 0;

    /** 注册 IP 兜底值：非 Web 上下文（定时任务、单元测试）取不到请求 IP 时使用 */
    private static final String DEFAULT_REGISTER_IP = "0.0.0.0";

    /** member_user.register_ip 列长度上限 varchar(32) */
    private static final int REGISTER_IP_MAX_LENGTH = 32;

    @Resource
    private MemberUserMapper memberUserMapper;

    @Override
    public Long createMemberUser(MemberUserSaveReqVO createReqVO) {
        // 校验手机号唯一
        validateMobileUnique(null, createReqVO.getMobile());
        // 写入
        MemberUserDO memberUser = BeanUtils.toBean(createReqVO, MemberUserDO.class);
        // register_ip 为 NOT NULL 且无数据库默认值，必须显式写入，否则新增报 500
        memberUser.setRegisterIp(resolveRegisterIp(null));
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
    public void updateMemberUserStatus(Long id, Integer status) {
        // 校验存在，避免对不存在的会员静默成功
        validateMemberUserExists(id);
        // 只更新状态字段，不触碰其它档案信息
        MemberUserDO updateObj = new MemberUserDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
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

    // ========== 小程序端（app）相关 ==========

    @Override
    public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }

    @Override
    public void updateMemberUserLogin(Long id, String loginIp) {
        MemberUserDO updateObj = new MemberUserDO();
        updateObj.setId(id);
        updateObj.setLoginIp(loginIp);
        updateObj.setLoginDate(LocalDateTime.now());
        memberUserMapper.updateById(updateObj);
    }

    @Override
    public MemberUserDO createMemberUserIfAbsent(String mobile, String ip) {
        // 校验手机号格式（避免超出 member_user.mobile 长度导致写入失败）
        if (mobile == null || !mobile.matches("\\d{11}")) {
            throw exception(PHARMACY_MEMBER_USER_MOBILE_INVALID);
        }
        MemberUserDO exist = memberUserMapper.selectByMobile(mobile);
        if (exist != null) {
            return exist;
        }
        // 首次登录，自动注册一个会员账号
        MemberUserDO memberUser = new MemberUserDO();
        memberUser.setMobile(mobile);
        // 默认密码为空加密串；小程序以手机号+密码登录，用户后续可修改密码
        memberUser.setPassword(PASSWORD_ENCODER.encode(""));
        memberUser.setStatus(STATUS_ENABLE);
        memberUser.setRegisterIp(resolveRegisterIp(ip));
        memberUser.setNickname("会员" + mobile.substring(mobile.length() - 4));
        memberUser.setAvatar("");
        memberUser.setPoint(0);
        memberUser.setExperience(0);
        memberUserMapper.insert(memberUser);
        return memberUser;
    }

    @Override
    public void updateMemberUserProfile(Long id, String nickname, String avatar, Integer sex) {
        // 校验会员存在
        validateMemberUserExists(id);
        // 仅更新允许修改的字段，避免越权修改手机号/积分/等级
        MemberUserDO updateObj = new MemberUserDO();
        updateObj.setId(id);
        updateObj.setNickname(nickname);
        updateObj.setAvatar(avatar);
        updateObj.setSex(sex);
        memberUserMapper.updateById(updateObj);
    }

    /**
     * 解析注册 IP。
     *
     * <p>member_user.register_ip 为 NOT NULL 且无数据库默认值，写入 null 会触发
     * Field 'register_ip' doesn't have a default value。取值优先级：
     * 调用方传入值 → 当前请求 IP → 兜底值，并按列长度 varchar(32) 截断。
     *
     * @param ip 调用方已知的 IP，可为空
     * @return 非空的注册 IP
     */
    private String resolveRegisterIp(String ip) {
        String registerIp = StrUtil.isNotBlank(ip) ? ip : ServletUtils.getClientIP();
        if (StrUtil.isBlank(registerIp)) {
            registerIp = DEFAULT_REGISTER_IP;
        }
        return registerIp.length() > REGISTER_IP_MAX_LENGTH
                ? registerIp.substring(0, REGISTER_IP_MAX_LENGTH) : registerIp;
    }

}
