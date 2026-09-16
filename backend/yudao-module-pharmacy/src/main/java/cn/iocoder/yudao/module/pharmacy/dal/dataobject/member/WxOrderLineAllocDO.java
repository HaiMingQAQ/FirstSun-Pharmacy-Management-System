package cn.iocoder.yudao.module.pharmacy.dal.dataobject.member;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 线上订单库存分配 DO
 *
 * 对应数据表 {@code ph_wx_order_line_alloc}。
 *
 * <p>记录 C 的 FEFO 实际分配（批次 + 货位 + 数量）。一张订单明细可能拆分到多个批次，
 * 因此单独建表保存。分配从「下单冻结」开始产生，随后按订单生命周期推进：
 *
 * <pre>
 * 下单冻结（C.reserve，流水 80）        → status = 0 已冻结
 * 支付成功（C.consumeReservation，82）  → status = 1 已出库
 * 取消 / 退款（C.release / returnBack） → status = 2 已释放或已回补
 * </pre>
 *
 * 释放与回补都按本表记录的「原批次、原货位」逐条调用，是幂等的唯一依据。
 * 迁移脚本：{@code sql/migrations/20260916_f_wx_order_line_alloc.sql}（建表）、
 * {@code sql/migrations/20260916_f_wx_order_alloc_frozen_stage.sql}（补入冻结阶段语义）。
 *
 * @see cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO
 */
@TableName("ph_wx_order_line_alloc")
@KeySequence("ph_wx_order_line_alloc_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WxOrderLineAllocDO extends BaseDO {

    /** 已冻结：库存已被本订单占用，尚未正式出库 */
    public static final Integer STATUS_FROZEN = 0;
    /** 已出库：冻结已转为正式销售出库 */
    public static final Integer STATUS_OUT = 1;
    /** 已释放或已回补：冻结已释放，或已出库库存已按原批次回补 */
    public static final Integer STATUS_SETTLED = 2;

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
     * 订单号，与库存流水 biz_no 对齐，是释放 / 回补时的 originalBizNo
     */
    private String orderNo;
    /**
     * 正式出库时的来源行号，回补时作为 originalBizLineId。
     *
     * 冻结转出库（流水 82）用分配记录编号（同一明细拆多批次时行号需唯一），
     * 直接扣库（流水 20）用订单明细编号，因此必须显式记录而不是从订单明细反推。
     */
    private Long outBizLineId;
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
     * 本批次数量
     */
    private Integer qty;
    /**
     * 已回补数量（释放场景与 qty 保持一致，表示该分配已了结）
     */
    private Integer returnedQty;
    /**
     * 状态：0 已冻结 / 1 已出库 / 2 已释放或已回补
     */
    private Integer status;

}
