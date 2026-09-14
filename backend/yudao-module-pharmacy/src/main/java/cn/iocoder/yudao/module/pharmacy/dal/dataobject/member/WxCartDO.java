package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 小程序购物车 DO
 *
 * 对应数据表 {@code ph_wx_cart}。
 * 字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 */
@TableName("ph_wx_cart")
@KeySequence("ph_wx_cart_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WxCartDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 会员用户编号
     *
     * 关联 member_user 表
     */
    private Long memberId;
    /**
     * 商品编号
     *
     * 关联 ph_drug 表
     */
    private Long drugId;
    /**
     * 数量
     */
    private Integer qty;
    /**
     * 是否勾选
     *
     * 1=勾选，0=未勾选
     */
    private Integer selectedFlag;
    /**
     * 加购时间
     */
    private LocalDateTime addTime;
    /**
     * 购物车门店
     */
    private Long storeId;

}
