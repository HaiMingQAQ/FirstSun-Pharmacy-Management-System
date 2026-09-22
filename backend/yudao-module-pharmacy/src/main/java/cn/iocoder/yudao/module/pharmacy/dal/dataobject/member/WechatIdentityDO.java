package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 药店会员微信小程序身份绑定。
 *
 * <p>只保存已经过微信实时校验的 openid 与会员关系，不保存授权 code 或会话密钥。</p>
 */
@TableName("ph_member_wechat_identity")
@KeySequence("ph_member_wechat_identity_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WechatIdentityDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 可信微信小程序 AppID。 */
    private String appId;

    /** 微信小程序 openid。 */
    private String openid;

    /** 药店会员编号。 */
    private Long memberId;

}
