package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 线上订单出库分配 DO
 *
 * 对应数据表 {@code ph_wx_order_line_alloc}。
 *
 * <p>记录 C 的 FEFO 实际出库分配（批次 + 货位 + 数量）。一张订单明细可能拆分到多个批次，
 * 因此单独建表保存，取消 / 退款 / 退货时按此表逐条回补原批次、原货位。
 * 迁移脚本：{@code sql/migrations/20260916_f_wx_order_line_alloc.sql}。
 *
 * @see cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO
 */
@TableName("ph_wx_order_line_alloc")
@KeySequence("ph_wx_order_line_alloc_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WxOrderLineAllocDO extends BaseDO {

    /** 出库未回补 */
    public static final Integer STATUS_OUT = 0;
    /** 已回补 */
    public static final Integer STATUS_RETURNED = 1;

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 线上订单编号
     */
    private Long wxOrderId;
    /**
     * 线上订单明细编号
     */
    private Long wxOrderLineId;
    /**
     * 订单号，与库存流水 biz_no 对齐，是回补时的 originalBizNo
     */
    private String orderNo;
    /**
     * 商品编号
     */
    private Long drugId;
    /**
     * 出库批次（原批次）
     */
    private Long batchId;
    /**
     * 出库货位（原货位）
     */
    private Long locationId;
    /**
     * 本批次出库数量
     */
    private Integer qty;
    /**
     * 已回补数量
     */
    private Integer returnedQty;
    /**
     * 状态：0已出库 / 1已回补
     */
    private Integer status;

}
