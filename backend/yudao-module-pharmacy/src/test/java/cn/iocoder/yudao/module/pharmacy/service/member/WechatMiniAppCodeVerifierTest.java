package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.config.WxMaConfig;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.config.WechatLoginProperties;
import com.binarywang.spring.starter.wxjava.miniapp.properties.WxMaProperties;
import me.chanjar.weixin.common.error.WxErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_WECHAT_LOGIN_CODE_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_WECHAT_LOGIN_CONFIG_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 微信 code 校验替身测试；不宣称真实微信接口已验证。 */
@ExtendWith(MockitoExtension.class)
class WechatMiniAppCodeVerifierTest {

    @Mock
    private WxMaService wxMaService;
    @Mock
    private WxMaProperties wxMaProperties;
    @Mock
    private WechatLoginProperties loginProperties;
    @Mock
    private WxMaUserService wxMaUserService;

    @InjectMocks
    private WechatMiniAppCodeVerifier verifier;

    @BeforeEach
    void setUp() {
        lenient().when(loginProperties.getTenantId()).thenReturn(99L);
        lenient().when(loginProperties.getAppId()).thenReturn("wx-test-app");
        lenient().when(wxMaProperties.getAppid()).thenReturn("wx-test-app");
        lenient().when(wxMaProperties.getSecret()).thenReturn("secret-present-only-in-test");
        WxMaConfig config = mock(WxMaConfig.class);
        lenient().when(config.getAppid()).thenReturn("wx-test-app");
        lenient().when(wxMaService.getWxMaConfig()).thenReturn(config);
        lenient().when(wxMaService.getUserService()).thenReturn(wxMaUserService);
    }

    @Test
    void validCode_callsWechatRealtimeAndReturnsOnlyOpenid() throws Exception {
        WxMaJscode2SessionResult result = new WxMaJscode2SessionResult();
        result.setOpenid("openid-test");
        result.setSessionKey("session-key-must-not-persist");
        when(wxMaUserService.getSessionInfo("code-1")).thenReturn(result);

        assertEquals("openid-test", verifier.verifyAndGetOpenid("code-1"));
        verify(wxMaUserService).getSessionInfo("code-1");
    }

    @Test
    void invalidCode_isRejectedAndCanBeRetriedAgainstWechat() throws Exception {
        when(wxMaUserService.getSessionInfo(anyString()))
                .thenThrow(new WxErrorException(new IllegalStateException("invalid")));

        ServiceException first = assertThrows(ServiceException.class,
                () -> verifier.verifyAndGetOpenid("code-1"));
        ServiceException second = assertThrows(ServiceException.class,
                () -> verifier.verifyAndGetOpenid("code-1"));

        assertEquals(PHARMACY_WECHAT_LOGIN_CODE_INVALID.getCode(), first.getCode());
        assertEquals(PHARMACY_WECHAT_LOGIN_CODE_INVALID.getCode(), second.getCode());
        verify(wxMaUserService, org.mockito.Mockito.times(2)).getSessionInfo("code-1");
    }

    @Test
    void missingTrustedConfiguration_isRejectedBeforeWechatCall() {
        when(loginProperties.getTenantId()).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> verifier.verifyAndGetOpenid("code-1"));

        assertEquals(PHARMACY_WECHAT_LOGIN_CONFIG_MISSING.getCode(), exception.getCode());
        verify(wxMaService, org.mockito.Mockito.never()).getUserService();
    }

}
