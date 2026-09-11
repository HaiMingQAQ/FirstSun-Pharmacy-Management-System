package cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseOrderStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单 DO
 *
 * 对应数据表 {@code ph_po_order}。金额单位统一为「元」，DECIMAL(18,2)。
 *
 * 金额口径（由服务端统一重算，不信任前端传入）：
 * totalAmount     = Σ(orderQty × unitPrice)
 * discountAmount  = Σ(orderQty × unitPrice × (1 - discountRate))
 * payableAmount   = totalAmount - discountAmount
 *
 * @author B 成员
 */
@TableName("ph_po_order")
@KeySequence("ph_po_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 订单号（PO-门店-yyyyMMdd-流水，唯一）
     */
    private String orderNo;
    /**
     * 采购门店
     */
    private Long storeId;
    /**
     * 收货仓库（可空，收货时确定）
     */
    private Long warehouseId;
    /**
     * 供应商编号
     */
    private Long supplierId;
    /**
     * 下单日期
     */
    private LocalDate orderDate;
    /**
     * 预计到货日期
     */
    private LocalDate expectDate;
    /**
     * 总数量
     */
    private Integer totalQty;
    /**
     * 含税总金额（元）
     */
    private BigDecimal totalAmount;
    /**
     * 优惠金额（元）
     */
    private BigDecimal discountAmount;
    /**
     * 应付金额（元）
     */
    private BigDecimal payableAmount;
    /**
     * 订单状态
     *
     * 枚举 {@link PurchaseOrderStatusEnum}
     */
    private Integer status;
    /**
     * 是否由采购建议自动生成：0否/1是
     */
    private Integer isAuto;
    /**
     * 备注
     */
    private String remark;
    /**
     * 审批人员工编号
     */
    private Long auditBy;
    /**
     * 审批时间
     */
    private LocalDateTime auditAt;

}
