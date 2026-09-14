package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 小程序订单明细 DO
 *
 * 对应数据表 {@code ph_wx_order_line}。
 * 字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 */
@TableName("ph_wx_order_line")
@KeySequence("ph_wx_order_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WxOrderLineDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 线上订单编号
     *
     * 关联 ph_wx_order 表
     */
    private Long wxOrderId;
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
     * 单价
     */
    private BigDecimal price;
    /**
     * 金额
     */
    private BigDecimal lineAmount;
    /**
     * 拣货批次
     *
     * 拣货后回填
     */
    private Long batchId;
    /**
     * 批号
     */
    private String batchNo;
    /**
     * 已拣数量
     */
    private Integer pickedQty;
    /**
     * 锁定时绑定的货位
     */
    private Long locationId;
    /**
     * 下单名称快照
     */
    private String drugName;
    /**
     * 规格快照
     */
    private String specification;
    /**
     * 单位快照
     */
    private String unit;

}
