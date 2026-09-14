package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员等级 DO
 *
 * 对应数据表 {@code member_level}。
 * 字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 *
 * 注意：{@link #status} 取值使用框架标准（0=启用，1=禁用），
 * 与 pharmacy 模块其他表的 PharmacyStatusEnum（1=启用，0=停用）方向相反。
 */
@TableName("member_level")
@KeySequence("member_level_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberLevelDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 等级名称
     */
    private String name;
    /**
     * 等级
     */
    private Integer level;
    /**
     * 升级经验
     */
    private Integer experience;
    /**
     * 享受折扣
     *
     * 取值范围 0-100，如 90 表示 9 折
     */
    private Integer discountPercent;
    /**
     * 等级图标
     */
    private String icon;
    /**
     * 等级背景图
     */
    private String backgroundUrl;
    /**
     * 状态
     *
     * 框架标准：0=启用，1=禁用
     */
    private Integer status;

}
