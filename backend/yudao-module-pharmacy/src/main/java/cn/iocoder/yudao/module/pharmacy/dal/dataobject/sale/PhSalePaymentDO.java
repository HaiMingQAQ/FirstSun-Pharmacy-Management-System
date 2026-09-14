package cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售支付明细（表：ph_sale_payment）
 */
@TableName("ph_sale_payment")
@Data
@EqualsAndHashCode(callSuper = true)
public class PhSalePaymentDO extends BaseDO {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 销售单 */
    private Long orderId;

    /** 外部交易号 */
    private String payNo;

    /** 1现金/2微信/3支付宝/4银行卡/5储值/6医保/7积分 */
    private Integer payMethod;

    /** 金额 */
    private BigDecimal payAmount;

    /** 渠道 */
    private String channel;

    /** 0 支付中 / 1 成功 / 2 失败 / 3 已退款 / 4 已冲正 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 退款流水号 */
    private String refundNo;

    /** 退款时间 */
    private LocalDateTime refundAt;

    /** 本系统分笔支付幂等号 */
    private String paymentNo;

    /** 关联渠道支付单(现金为空) */
    private Long payOrderId;
}
