package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会员用户 DO
 *
 * 对应数据表 {@code member_user}。
 * 字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 *
 * 注意：{@link #status} 取值使用框架标准（0=启用，1=禁用），
 * 与 pharmacy 模块其他表的 PharmacyStatusEnum（1=启用，0=停用）方向相反。
 */
@TableName("member_user")
@KeySequence("member_user_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberUserDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 密码
     */
    private String password;
    /**
     * 状态
     *
     * 框架标准：0=启用，1=禁用
     */
    private Integer status;
    /**
     * 注册 IP
     */
    private String registerIp;
    /**
     * 注册终端
     */
    private Integer registerTerminal;
    /**
     * 最后登录IP
     */
    private String loginIp;
    /**
     * 最后登录时间
     */
    private LocalDateTime loginDate;
    /**
     * 用户昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 真实名字
     */
    private String name;
    /**
     * 用户性别
     */
    private Integer sex;
    /**
     * 所在地
     */
    private Long areaId;
    /**
     * 出生日期
     */
    private LocalDateTime birthday;
    /**
     * 会员备注
     */
    private String mark;
    /**
     * 积分
     */
    private Integer point;
    /**
     * 用户标签编号列表，以逗号分隔
     */
    private String tagIds;
    /**
     * 等级编号
     */
    private Long levelId;
    /**
     * 经验
     */
    private Integer experience;
    /**
     * 用户分组编号
     */
    private Long groupId;
    /**
     * 邮箱
     */
    private String email;

}
