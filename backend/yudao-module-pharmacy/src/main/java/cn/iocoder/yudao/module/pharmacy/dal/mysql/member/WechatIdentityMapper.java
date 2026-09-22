package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WechatIdentityDO;
import org.apache.ibatis.annotations.Mapper;

/** 药店会员微信身份绑定 Mapper。 */
@Mapper
public interface WechatIdentityMapper extends BaseMapperX<WechatIdentityDO> {

    default WechatIdentityDO selectByAppIdAndOpenid(String appId, String openid) {
        return selectOne(new LambdaQueryWrapperX<WechatIdentityDO>()
                .eq(WechatIdentityDO::getAppId, appId)
                .eq(WechatIdentityDO::getOpenid, openid));
    }

}
