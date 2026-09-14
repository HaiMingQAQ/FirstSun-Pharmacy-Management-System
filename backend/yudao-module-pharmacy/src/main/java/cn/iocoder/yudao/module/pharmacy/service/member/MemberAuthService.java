package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberAuthLoginReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.auth.AppMemberAuthLoginRespVO;

/**
 * 会员认证 Service（小程序端）
 *
 * 复用 yudao 框架的 OAuth2 Token 机制（{@code OAuth2TokenCommonApi}），
 * 不重复实现令牌体系；会员数据使用 {@code member_user} 表。
 */
public interface MemberAuthService {

    /**
     * 手机号 + 密码登录
     */
    AppMemberAuthLoginRespVO login(AppMemberAuthLoginReqVO reqVO);

    /**
     * 手机号首次登录时自动注册并登录（小程序快捷登录）
     */
    AppMemberAuthLoginRespVO loginOrRegister(String mobile);

    /**
     * 登出，删除访问令牌
     *
     * @param token 访问令牌
     */
    void logout(String token);

    /**
     * 刷新令牌
     */
    AppMemberAuthLoginRespVO refreshToken(String refreshToken);

}
