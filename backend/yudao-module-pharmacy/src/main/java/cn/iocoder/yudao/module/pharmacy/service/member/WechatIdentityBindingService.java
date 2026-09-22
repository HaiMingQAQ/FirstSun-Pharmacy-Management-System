package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WechatIdentityDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WechatIdentityMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_WECHAT_IDENTITY_MEMBER_MISSING;

/**
 * 药店微信身份绑定服务。
 *
 * <p>新会员与绑定行在同一个本地事务中创建。唯一键冲突向调用方传播，事务代理会回滚
 * 新会员；调用方在事务结束后重新读取已经成功提交的绑定。</p>
 */
@Service
public class WechatIdentityBindingService {

    @Resource
    private WechatIdentityMapper wechatIdentityMapper;

    @Resource
    private MemberUserService memberUserService;

    public MemberUserDO findMember(String appId, String openid, Long tenantId) {
        WechatIdentityDO identity = wechatIdentityMapper.selectByAppIdAndOpenid(appId, openid);
        if (identity == null) {
            return null;
        }
        MemberUserDO member = memberUserService.getMemberUserByIdAndTenantId(identity.getMemberId(), tenantId);
        if (member == null) {
            throw exception(PHARMACY_WECHAT_IDENTITY_MEMBER_MISSING);
        }
        return member;
    }

    @Transactional(rollbackFor = Exception.class)
    public MemberUserDO findOrCreate(String appId, String openid, Long tenantId, String registerIp) {
        MemberUserDO existing = findMember(appId, openid, tenantId);
        if (existing != null) {
            return existing;
        }

        MemberUserDO member = memberUserService.createWechatMember(tenantId, registerIp);
        WechatIdentityDO identity = new WechatIdentityDO();
        identity.setTenantId(tenantId);
        identity.setAppId(appId);
        identity.setOpenid(openid);
        identity.setMemberId(member.getId());
        try {
            wechatIdentityMapper.insert(identity);
        } catch (DuplicateKeyException ex) {
            // 不吞掉异常：让 @Transactional 回滚刚创建的会员，避免孤立会员。
            throw ex;
        }
        return member;
    }

}
