package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberSmsCodeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 会员手机验证码 Mapper
 */
@Mapper
public interface MemberSmsCodeMapper extends BaseMapperX<MemberSmsCodeDO> {

    /**
     * 按手机号 + 场景查询验证码记录（同一手机号同一场景只有一条）
     */
    default MemberSmsCodeDO selectByMobileAndScene(String mobile, Integer scene) {
        return selectOne(new LambdaQueryWrapperX<MemberSmsCodeDO>()
                .eq(MemberSmsCodeDO::getMobile, mobile)
                .eq(MemberSmsCodeDO::getScene, scene));
    }

    /**
     * 条件置为已使用，保证验证码一次性（并发下只有一次能成功）
     *
     * @return 影响行数，0 表示已被使用过
     */
    @Update("UPDATE member_sms_code SET used_time = #{usedTime}, used_ip = #{usedIp}, update_time = NOW() "
            + "WHERE id = #{id} AND used_time IS NULL AND deleted = 0")
    int updateUsedTimeIfUnused(@Param("id") Long id, @Param("usedTime") LocalDateTime usedTime,
                               @Param("usedIp") String usedIp);

    /**
     * 重发：覆盖验证码并重置为未使用
     */
    @Update("UPDATE member_sms_code SET code = #{code}, expire_time = #{expireTime}, used_time = NULL, "
            + "used_ip = NULL, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int updateCodeForResend(@Param("id") Long id, @Param("code") String code,
                            @Param("expireTime") LocalDateTime expireTime);

}
