package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员积分记录 DO
 *
 * 对应数据表 {@code member_point_record}。
 * 字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 */
@TableName("member_point_record")
@KeySequence("member_point_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberPointRecordDO extends BaseDO {

    /**
     * 自增主键
     */
    @TableId
    private Long id;
    /**
     * 用户编号
     */
    private Long userId;
    /**
     * 业务编码
     */
    private String bizId;
    /**
     * 业务类型
     */
    private Integer bizType;
    /**
     * 积分标题
     */
    private String title;
    /**
     * 积分描述
     */
    private String description;
    /**
     * 积分
     *
     * 正数表示增加，负数表示扣减
     */
    private Integer point;
    /**
     * 变动后的积分
     */
    private Integer totalPoint;

}
