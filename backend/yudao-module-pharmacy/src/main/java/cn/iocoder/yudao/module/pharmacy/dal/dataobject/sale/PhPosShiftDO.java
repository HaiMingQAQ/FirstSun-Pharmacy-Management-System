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
 * 收银班次（表：ph_pos_shift）
 */
@TableName("ph_pos_shift")
@Data
@EqualsAndHashCode(callSuper = true)
public class PhPosShiftDO extends BaseDO {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班次号 */
    private String shiftNo;

    /** 门店 */
    private Long storeId;

    /** 收银台号 */
    private String posNo;

    /** 收银员 */
    private Long cashierId;

    /** 开台时间 */
    private LocalDateTime openAt;

    /** 交班时间 */
    private LocalDateTime closeAt;

    /** 系统应收现金 */
    private BigDecimal cashExpected;

    /** 实盘现金 */
    private BigDecimal cashActual;

    /** 短长款 */
    private BigDecimal diffAmount;

    /** 差异原因 */
    private String diffReason;

    /** 交易笔数 */
    private Integer saleCount;

    /** 销售额 */
    private BigDecimal saleAmount;

    /** 0 营业中 / 1 已交班 */
    private Integer status;
}
