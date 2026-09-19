package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会员手机验证码 DO
 *
 * 对应数据表 {@code member_sms_code}。
 *
 * <p>验证码与「手机号 + 登录用途(scene)」绑定：同一手机号同一用途只保留一条记录，
 * 重发覆盖；校验通过后立即置为「已使用」，保证一次性与不可重放。
 * 验证码本身来自环境变量（local/dev），代码中不含任何硬编码验证码。
 */
@TableName("member_sms_code")
@KeySequence("member_sms_code_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberSmsCodeDO extends BaseDO {

    /**
     * 自增主键
     */
    @TableId
    private Long id;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 使用场景，见 {@link cn.iocoder.yudao.module.pharmacy.enums.member.MemberSmsSceneEnum}
     */
    private Integer scene;
    /**
     * 验证码
     */
    private String code;
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 使用时间，非空表示已被使用（重复使用将被拒绝）
     */
    private LocalDateTime usedTime;
    /**
     * 使用 IP
     */
    private String usedIp;

}
