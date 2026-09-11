package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小程序订单 DO
 *
 * 对应数据表 {@code ph_wx_order}。
 * 字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 */
@TableName("ph_wx_order")
@KeySequence("ph_wx_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WxOrderDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 线上订单号
     *
     * 格式：WX-门店-yyyyMMdd-流水，唯一
     */
    private String orderNo;
    /**
     * 会员用户编号
     *
     * 关联 member_user 表
     */
    private Long memberId;
    /**
     * 履约门店
     */
    private Long storeId;
    /**
     * 订单类型
     *
     * 0=到店自提，1=同城配送
     */
    private Integer orderType;
    /**
     * 商品金额
     */
    private BigDecimal goodsAmount;
    /**
     * 券抵扣
     */
    private BigDecimal couponAmount;
    /**
     * 配送费
     */
    private BigDecimal freightAmount;
    /**
     * 促销优惠
     */
    private BigDecimal discountAmount;
    /**
     * 应付金额
     */
    private BigDecimal payableAmount;
    /**
     * 微信支付交易号
     */
    private String payNo;
    /**
     * 支付状态
     *
     * 0=待支付，1=已支付，2=已退款
     */
    private Integer payStatus;
    /**
     * 支付时间
     */
    private LocalDateTime paidAt;
    /**
     * 处方案编号
     *
     * 处方药线上自提使用
     */
    private Long prescId;
    /**
     * 订单状态
     *
     * 0=待支付，1=待拣货，2=拣货中，3=待自提，4=完成，-1=取消
     */
    private Integer status;
    /**
     * 取消原因
     */
    private String cancelReason;
    /**
     * 配送地址快照
     */
    private String addressSnapshot;
    /**
     * 备注
     */
    private String remark;
    /**
     * 完成时间
     */
    private LocalDateTime finishAt;
    /**
     * 关联 pay_order
     */
    private Long payOrderId;
    /**
     * 未支付截止时间
     *
     * 建议创建后30分钟
     */
    private LocalDateTime expireAt;
    /**
     * 一次性取货码
     */
    private String pickupCode;
    /**
     * 核销员工
     */
    private Long verifyBy;
    /**
     * 核销时间
     */
    private LocalDateTime verifyAt;

}
