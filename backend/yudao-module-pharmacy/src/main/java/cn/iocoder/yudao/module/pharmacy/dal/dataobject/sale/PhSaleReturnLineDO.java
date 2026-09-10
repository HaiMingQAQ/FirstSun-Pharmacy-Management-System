package cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 退货单明细（表：ph_sale_return_line）
 */
@TableName("ph_sale_return_line")
@Data
@EqualsAndHashCode(callSuper = true)
public class PhSaleReturnLineDO extends BaseDO {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退货单 */
    private Long returnId;

    /** 销售行 */
    private Long saleLineId;

    /** 原批次(回补) */
    private Long batchId;

    /** 药品 */
    private Long drugId;

    /** 退货数量 */
    private Integer qty;

    /** 原价 */
    private BigDecimal price;

    /** 金额 */
    private BigDecimal amount;

    /** 扣回积分 */
    private Integer pointsDeduct;

    /** 验收回库货位 */
    private Long locationId;
}
