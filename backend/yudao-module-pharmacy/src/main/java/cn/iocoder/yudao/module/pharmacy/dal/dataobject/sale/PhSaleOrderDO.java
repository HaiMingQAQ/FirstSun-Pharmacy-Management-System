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
 * 销售单（表：ph_sale_order）
 */
@TableName("ph_sale_order")
@Data
@EqualsAndHashCode(callSuper = true)
public class PhSaleOrderDO extends BaseDO {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 销售单号 */
    private String orderNo;

    /** 门店 */
    private Long storeId;

    /** 收银台 */
    private String posNo;

    /** 班次 */
    private Long shiftId;

    /** 收银员 */
    private Long cashierId;

    /** 审方药师 */
    private Long pharmacistId;

    /** 会员 */
    private Long memberId;

    /** 顾客姓名(散客) */
    private String customerName;

    /** 来源:0 柜台 / 1 小程序 */
    private Integer source;

    /** 线上订单来源 */
    private Long wxOrderId;

    /** 0 销售 / 1 换货 */
    private Integer saleType;

    /** 0 正常 / 1 部分退 / 2 全退 */
    private Integer returnFlag;

    /** 总数量 */
    private Integer totalQty;

    /** 原价合计 */
    private BigDecimal subtotal;

    /** 折扣金额 */
    private BigDecimal discountAmount;

    /** 券抵扣 */
    private BigDecimal couponAmount;

    /** 积分抵扣 */
    private BigDecimal pointsDeduct;

    /** 应付 */
    private BigDecimal payableAmount;

    /** 实收 */
    private BigDecimal paidAmount;

    /** 找零 */
    private BigDecimal changeAmount;

    /** 成本合计 */
    private BigDecimal costAmount;

    /** 奖励积分 */
    private Integer pointsEarned;

    /** 0 待支付 / 1 完成 / 2 全额退款 / 3 部分退款 / -1 取消 */
    private Integer status;

    /** 是否离线单 */
    private Integer offlineFlag;

    /** 本地流水号 */
    private String offlineNo;

    /** 备注 */
    private String remark;

    /** 销售时间 */
    private LocalDateTime saleTime;
}
