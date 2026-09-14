package cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售单明细（表：ph_sale_order_line）
 */
@TableName("ph_sale_order_line")
@Data
@EqualsAndHashCode(callSuper = true)
public class PhSaleOrderLineDO extends BaseDO {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 销售单 */
    private Long orderId;

    /** 行号 */
    private Integer lineNo;

    /** 药品 */
    private Long drugId;

    /** 批次(先进先出) */
    private Long batchId;

    /** 批号快照 */
    private String batchNo;

    /** 效期快照 */
    private LocalDate expiryDate;

    /** 数量 */
    private Integer qty;

    /** 成交单价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal normalPrice;

    /** 行优惠 */
    private BigDecimal discount;

    /** 行金额 */
    private BigDecimal lineAmount;

    /** 出库成本价 */
    private BigDecimal costPrice;

    /** 是否处方药行 */
    private Integer isRx;

    /** 关联处方 */
    private Long prescId;

    /** 已退货数量 */
    private Integer returnedQty;

    /** 是否赠品 */
    private Integer isGift;

    /** 出库货位 */
    private Long locationId;

    /** 成交名称快照 */
    private String drugName;

    /** 规格快照 */
    private String specification;

    /** 单位快照 */
    private String unit;
}
