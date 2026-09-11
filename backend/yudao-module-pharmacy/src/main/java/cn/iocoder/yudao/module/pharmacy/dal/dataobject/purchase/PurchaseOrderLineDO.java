package cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 采购订单明细 DO
 *
 * 对应数据表 {@code ph_po_order_line}。
 * {@code receivedQty} 只能由收货入账流程累计，直接改订单不会变更已收数量。
 *
 * @author B 成员
 */
@TableName("ph_po_order_line")
@KeySequence("ph_po_order_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderLineDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 订单头编号
     */
    private Long orderId;
    /**
     * 行号（同一订单内唯一）
     */
    private Integer lineNo;
    /**
     * 药品编号
     */
    private Long drugId;
    /**
     * 订购数量
     */
    private Integer orderQty;
    /**
     * 已收数量（收货入账累计）
     */
    private Integer receivedQty;
    /**
     * 含税单价（元）
     */
    private BigDecimal unitPrice;
    /**
     * 折扣率（0 ~ 1）
     */
    private BigDecimal discountRate;
    /**
     * 行金额（元）= orderQty × unitPrice × discountRate
     */
    private BigDecimal lineAmount;
    /**
     * 备注
     */
    private String remark;

}
