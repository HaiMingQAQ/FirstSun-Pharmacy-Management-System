package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.pharmacy.config.WechatLoginProperties;
import com.binarywang.spring.starter.wxjava.miniapp.properties.WxMaProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_WECHAT_LOGIN_CONFIG_MISSING;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_WECHAT_LOGIN_CODE_INVALID;

/**
 * 药店微信小程序 code 校验器。
 *
 * <p>每次调用都直接请求微信的 jscode2session 接口，不查询或写入 code/state 缓存。</p>
 */
@Service
@Slf4j
public class WechatMiniAppCodeVerifier {

    @Resource
    private WxMaService wxMaService;

    @Resource
    private WxMaProperties wxMaProperties;

    @Resource
    private WechatLoginProperties wechatLoginProperties;

    /**
     * 实时校验登录 code，只返回 openid；session_key 只存在于当前调用栈。
     */
    public String verifyAndGetOpenid(String code) {
        validateConfiguration();
        if (StrUtil.isBlank(code)) {
            throw exception(PHARMACY_WECHAT_LOGIN_CODE_INVALID);
        }
        try {
            WxMaJscode2SessionResult result = wxMaService.getUserService().getSessionInfo(code);
            if (result == null || StrUtil.isBlank(result.getOpenid())) {
                throw exception(PHARMACY_WECHAT_LOGIN_CODE_INVALID);
            }
            return result.getOpenid();
        } catch (WxErrorException ex) {
            // 不记录 code、session_key 或微信完整响应，只保留固定业务日志。
            log.warn("[verifyAndGetOpenid][微信小程序 code 校验失败]");
            throw exception(PHARMACY_WECHAT_LOGIN_CODE_INVALID);
        }
    }

    private void validateConfiguration() {
        String configuredAppId = wechatLoginProperties.getAppId();
        if (wechatLoginProperties.getTenantId() == null
                || wechatLoginProperties.getTenantId() <= 0
                || StrUtil.isBlank(configuredAppId)
                || StrUtil.isBlank(wxMaProperties.getAppid())
                || StrUtil.isBlank(wxMaProperties.getSecret())
                || !configuredAppId.equals(wxMaProperties.getAppid())
                || wxMaService.getWxMaConfig() == null
                || !configuredAppId.equals(wxMaService.getWxMaConfig().getAppid())) {
            throw exception(PHARMACY_WECHAT_LOGIN_CONFIG_MISSING);
        }
    }

}
