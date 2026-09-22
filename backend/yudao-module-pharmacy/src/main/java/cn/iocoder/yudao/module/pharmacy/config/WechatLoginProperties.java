package cn.iocoder.yudao.module.pharmacy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 药店微信登录的可信配置。
 *
 * <p>tenantId 和 appId 必须由后端部署配置提供，不能从登录请求读取。微信 secret
 * 继续由现有 {@code wx.miniapp.secret} 配置提供，并由校验器检查其存在性。</p>
 */
@Component
@ConfigurationProperties(prefix = "yudao.pharmacy.wechat-login")
@Data
public class WechatLoginProperties {

    /** 微信登录归属的可信租户。 */
    private Long tenantId;

    /** 微信小程序 AppID，必须与 wx.miniapp.appid 一致。 */
    private String appId;

}
