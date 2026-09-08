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
 * 退换货单（表：ph_sale_return）
 */
@TableName("ph_sale_return")
@Data
@EqualsAndHashCode(callSuper = true)
public class PhSaleReturnDO extends BaseDO {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退货单号 */
    private String returnNo;

    /** 原销售单 */
    private Long saleOrderId;

    /** 门店 */
    private Long storeId;

    /** 0 退货 / 1 换货 */
    private Integer returnType;

    /** 原因字典 */
    private Integer reason;

    /** 退款金额 */
    private BigDecimal totalAmount;

    /** 0 原路 / 1 现金 / 2 余额 */
    private Integer refundMethod;

    /** 0 草稿 / 1 待审批 / 2 已审核 / 3 已完成 / 4 已取消 */
    private Integer status;

    /** 经办人 */
    private Long cashierId;

    /** 药师复核(处方药退货) */
    private Integer pharmacistConfirm;

    /** 审批人 */
    private Long auditBy;

    /** 完成时间 */
    private LocalDateTime returnAt;

    /** 渠道退款关联 */
    private Long payRefundId;

    /** 0 未退款 / 1 退款中 / 2 成功 / 3 失败 */
    private Integer refundStatus;

    /** 退款失败原因 */
    private String refundError;
}
