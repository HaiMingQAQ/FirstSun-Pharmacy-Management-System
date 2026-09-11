package cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购收货明细 DO
 *
 * 对应数据表 {@code ph_po_receipt_line}。
 * 批号、有效期、入库货位为收货入账的必填要素；{@link #createBatchId} 由库存服务入账后回写。
 *
 * @author B 成员
 */
@TableName("ph_po_receipt_line")
@KeySequence("ph_po_receipt_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseReceiptLineDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 收货单头编号
     */
    private Long receiptId;
    /**
     * 行号（同一收货单内唯一）
     */
    private Integer lineNo;
    /**
     * 采购订单行编号（无单收货时为 null）
     */
    private Long orderLineId;
    /**
     * 药品编号
     */
    private Long drugId;
    /**
     * 批号（必填）
     */
    private String batchNo;
    /**
     * 生产日期
     */
    private LocalDate manufactureDate;
    /**
     * 有效期至（必填）
     */
    private LocalDate expiryDate;
    /**
     * 实收数量
     */
    private Integer qty;
    /**
     * 采购单价（元）
     */
    private BigDecimal unitPrice;
    /**
     * 金额（元）
     */
    private BigDecimal amount;
    /**
     * 质检标记：0待检/1通过/2异常拒收
     */
    private Integer qualityFlag;
    /**
     * 质检说明
     */
    private String qaRemark;
    /**
     * 冷链到货温度（℃）
     */
    private BigDecimal coldChainTemp;
    /**
     * 入账后生成的批次编号（由库存服务回写）
     */
    private Long createBatchId;
    /**
     * 入库目标货位（入账必须填写）
     */
    private Long locationId;

}
