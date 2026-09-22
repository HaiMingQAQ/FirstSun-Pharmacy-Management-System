package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.config.WechatLoginProperties;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberAuthLoginReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberAuthLoginRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberWechatLoginRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.system.api.logger.LoginLogApi;
import cn.iocoder.yudao.module.system.api.logger.dto.LoginLogCreateReqDTO;
import cn.iocoder.yudao.module.system.enums.logger.LoginLogTypeEnum;
import cn.iocoder.yudao.module.system.enums.logger.LoginResultEnum;
import cn.iocoder.yudao.module.system.enums.oauth2.OAuth2ClientConstants;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import java.util.Objects;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.monitor.TracerUtils.getTraceId;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_LOGIN_BAD_CREDENTIALS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_USER_DISABLED;

/**
 * 会员认证 Service 实现类（小程序端）
 *
 * 复用 yudao 框架的 OAuth2 令牌能力，登录成功后创建 userType=会员 的访问令牌。
 */
@Service
@Validated
public class MemberAuthServiceImpl implements MemberAuthService {

    /** 会员状态：0=启用（框架标准） */
    private static final Integer STATUS_ENABLE = 0;

    @Resource
    private MemberUserService memberUserService;

    @Resource
    private OAuth2TokenCommonApi oauth2TokenApi;

    @Resource
    private LoginLogApi loginLogApi;

    @Resource
    private MemberSmsCodeService memberSmsCodeService;

    @Resource
    private WechatMiniAppCodeVerifier wechatMiniAppCodeVerifier;

    @Resource
    private WechatIdentityBindingService wechatIdentityBindingService;

    @Resource
    private WechatLoginProperties wechatLoginProperties;

    @Override
    public AppMemberAuthLoginRespVO login(AppMemberAuthLoginReqVO reqVO) {
        // 1. 校验会员是否存在
        MemberUserDO user = memberUserService.getMemberUserByMobile(reqVO.getMobile());
        if (user == null) {
            createLoginLog(null, reqVO.getMobile(), LoginResultEnum.BAD_CREDENTIALS);
            throw exception(PHARMACY_MEMBER_LOGIN_BAD_CREDENTIALS);
        }
        // 2. 校验密码
        if (!memberUserService.isPasswordMatch(reqVO.getPassword(), user.getPassword())) {
            createLoginLog(user.getId(), reqVO.getMobile(), LoginResultEnum.BAD_CREDENTIALS);
            throw exception(PHARMACY_MEMBER_LOGIN_BAD_CREDENTIALS);
        }
        // 3. 校验状态并创建令牌
        return createTokenAfterLoginSuccess(user, reqVO.getMobile());
    }

    @Override
    public AppMemberAuthLoginRespVO loginOrRegister(String mobile, String code) {
        // 1. 先校验并消费手机验证码：未通过校验时不做任何账号创建（避免用手机号冒用会员身份）
        memberSmsCodeService.verifyAndConsumeLoginCode(mobile, code);
        // 2. 产品规则：验证码校验通过后，手机号不存在则自动注册（沿用快捷登录既有规则）
        MemberUserDO user = memberUserService.createMemberUserIfAbsent(mobile, getClientIP());
        return createTokenAfterLoginSuccess(user, mobile);
    }

    @Override
    public AppMemberWechatLoginRespVO wechatLogin(String code) {
        // 每次请求都实时向微信换取 openid；不接受客户端传入 openid，也不使用历史 code/state。
        String openid = wechatMiniAppCodeVerifier.verifyAndGetOpenid(code);
        String appId = wechatLoginProperties.getAppId();
        Long tenantId = wechatLoginProperties.getTenantId();

        return withTrustedTenant(() -> {
            MemberUserDO user = wechatIdentityBindingService.findMember(appId, openid, tenantId);
            if (user == null) {
                try {
                    // 独立事务中的唯一键冲突会回滚候选会员及绑定行。
                    user = wechatIdentityBindingService.findOrCreate(appId, openid, tenantId, getClientIP());
                } catch (DuplicateKeyException ex) {
                    // 冲突事务结束后重新读取已经提交的绑定。
                    user = wechatIdentityBindingService.findMember(appId, openid, tenantId);
                    if (user == null) {
                        throw ex;
                    }
                }
            }
            return createWechatTokenAfterLoginSuccess(user, openid);
        });
    }

    @Override
    public void logout(String token) {
        oauth2TokenApi.removeAccessToken(token);
    }

    @Override
    public AppMemberAuthLoginRespVO refreshToken(String refreshToken) {
        OAuth2AccessTokenRespDTO accessToken = oauth2TokenApi.refreshAccessToken(
                refreshToken, OAuth2ClientConstants.CLIENT_ID_DEFAULT);
        return convert(accessToken);
    }

    // ========== 私有方法 ==========

    private AppMemberAuthLoginRespVO createTokenAfterLoginSuccess(MemberUserDO user, String mobile) {
        // 校验会员状态
        if (!Objects.equals(user.getStatus(), STATUS_ENABLE)) {
            createLoginLog(user.getId(), mobile, LoginResultEnum.USER_DISABLED);
            throw exception(PHARMACY_MEMBER_USER_DISABLED);
        }
        // 记录登录日志 + 更新最后登录信息
        createLoginLog(user.getId(), mobile, LoginResultEnum.SUCCESS);
        memberUserService.updateMemberUserLogin(user.getId(), getClientIP());
        // 创建令牌（userType = 会员）
        OAuth2AccessTokenRespDTO accessToken = oauth2TokenApi.createAccessToken(new OAuth2AccessTokenCreateReqDTO()
                .setUserId(user.getId())
                .setUserType(UserTypeEnum.MEMBER.getValue())
                .setClientId(OAuth2ClientConstants.CLIENT_ID_DEFAULT));
        return convert(accessToken);
    }

    private AppMemberWechatLoginRespVO createWechatTokenAfterLoginSuccess(MemberUserDO user, String openid) {
        if (!Objects.equals(user.getStatus(), STATUS_ENABLE)) {
            createLoginLog(user.getId(), WECHAT_LOGIN_LOG_USERNAME,
                    LoginLogTypeEnum.LOGIN_SOCIAL, LoginResultEnum.USER_DISABLED);
            throw exception(PHARMACY_MEMBER_USER_DISABLED);
        }
        createLoginLog(user.getId(), WECHAT_LOGIN_LOG_USERNAME,
                LoginLogTypeEnum.LOGIN_SOCIAL, LoginResultEnum.SUCCESS);
        memberUserService.updateMemberUserLogin(user.getId(), getClientIP());
        OAuth2AccessTokenRespDTO accessToken = oauth2TokenApi.createAccessToken(new OAuth2AccessTokenCreateReqDTO()
                .setUserId(user.getId())
                .setUserType(UserTypeEnum.MEMBER.getValue())
                .setClientId(OAuth2ClientConstants.CLIENT_ID_DEFAULT));
        AppMemberWechatLoginRespVO respVO = new AppMemberWechatLoginRespVO();
        respVO.setUserId(accessToken.getUserId());
        respVO.setAccessToken(accessToken.getAccessToken());
        respVO.setRefreshToken(accessToken.getRefreshToken());
        respVO.setExpiresTime(accessToken.getExpiresTime());
        respVO.setOpenid(openid);
        return respVO;
    }

    private AppMemberAuthLoginRespVO convert(OAuth2AccessTokenRespDTO accessToken) {
        AppMemberAuthLoginRespVO respVO = new AppMemberAuthLoginRespVO();
        respVO.setUserId(accessToken.getUserId());
        respVO.setAccessToken(accessToken.getAccessToken());
        respVO.setRefreshToken(accessToken.getRefreshToken());
        respVO.setExpiresTime(accessToken.getExpiresTime());
        return respVO;
    }

    private void createLoginLog(Long userId, String username, LoginResultEnum loginResult) {
        createLoginLog(userId, username, LoginLogTypeEnum.LOGIN_MOBILE, loginResult);
    }

    private void createLoginLog(Long userId, String username, LoginLogTypeEnum logType,
                                LoginResultEnum loginResult) {
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logType.getType());
        reqDTO.setTraceId(getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        reqDTO.setUsername(username);
        reqDTO.setUserAgent(cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getUserAgent());
        reqDTO.setUserIp(getClientIP());
        reqDTO.setResult(loginResult.getResult());
        loginLogApi.createLoginLog(reqDTO);
    }

    private <T> T withTrustedTenant(Supplier<T> action) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        boolean previousIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setTenantId(wechatLoginProperties.getTenantId());
        // The web filter marks this endpoint as tenant-ignored only to let it reach the
        // controller. Database and token operations below must still run in the trusted tenant.
        TenantContextHolder.setIgnore(false);
        try {
            return action.get();
        } finally {
            TenantContextHolder.clear();
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            }
            if (previousIgnore) {
                TenantContextHolder.setIgnore(true);
            }
        }
    }

    private static final String WECHAT_LOGIN_LOG_USERNAME = "wechat-mini-app";

}
