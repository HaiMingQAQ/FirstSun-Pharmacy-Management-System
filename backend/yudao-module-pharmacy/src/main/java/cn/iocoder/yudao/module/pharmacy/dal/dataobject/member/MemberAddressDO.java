package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员收件地址 DO
 *
 * 对应数据表 {@code member_address}。
 * 字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 */
@TableName("member_address")
@KeySequence("member_address_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberAddressDO extends BaseDO {

    /**
     * 收件地址编号
     */
    @TableId
    private Long id;
    /**
     * 用户编号
     */
    private Long userId;
    /**
     * 收件人名称
     */
    private String name;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 地区编码
     */
    private Long areaId;
    /**
     * 收件详细地址
     */
    private String detailAddress;
    /**
     * 是否默认
     *
     * true=默认地址，false=非默认
     */
    private Boolean defaultStatus;

}
