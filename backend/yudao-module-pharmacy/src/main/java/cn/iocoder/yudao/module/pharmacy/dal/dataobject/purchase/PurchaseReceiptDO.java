package cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseReceiptStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购收货单 DO
 *
 * 对应数据表 {@code ph_po_receipt}。
 * {@link #postedAt} 与 {@link #status} 一起构成入账防重复（CAS）：只有 status=已提交(1)
 * 且 posted_at 为空时才允许入账，重复提交不会二次入库。
 *
 * @author B 成员
 */
@TableName("ph_po_receipt")
@KeySequence("ph_po_receipt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseReceiptDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 收货单号（GR-门店-yyyyMMdd-流水，唯一）
     */
    private String receiptNo;
    /**
     * 关联采购订单编号（无单收货时为 null）
     */
    private Long orderId;
    /**
     * 门店编号
     */
    private Long storeId;
    /**
     * 入库仓库编号
     */
    private Long warehouseId;
    /**
     * 收货人员工编号
     */
    private Long receiveBy;
    /**
     * 收货时间
     */
    private LocalDateTime receiveDate;
    /**
     * 实收总数量
     */
    private Integer totalQty;
    /**
     * 实收总金额（元）
     */
    private BigDecimal totalAmount;
    /**
     * 差异标记：0无/1数量差异/2价格差异
     */
    private Integer diffType;
    /**
     * 是否无单收货：0否/1是（限店长）
     */
    private Integer isFreeReceipt;
    /**
     * 收货单状态
     *
     * 枚举 {@link PurchaseReceiptStatusEnum}
     */
    private Integer status;
    /**
     * 质检结果：0未检/1合格/2有异常
     */
    private Integer qualityStatus;
    /**
     * 入账时间（防重复入账标记）
     */
    private LocalDateTime postedAt;

}
