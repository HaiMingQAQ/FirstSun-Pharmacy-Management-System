package cn.iocoder.yudao.module.pharmacy.api.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 销售积分试算结果（F 会员积分服务 → D/E/小程序 的跨模块返回值）。
 *
 * <p>抵扣积分与抵扣金额均由 F 依据会员等级、积分余额与积分规则在后端计算，
 * 调用方（POS 收银、小程序下单）只负责把金额应用到订单上，不得自行折算。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalePointCalcDTO {

    /** 本次实际可抵扣的积分（0 表示不抵扣） */
    private Integer deductPoints;

    /** 抵扣金额（元），= deductPoints / pointsPerYuan */
    private BigDecimal deductAmount;

    /** 本次可赠送的预估值，仅用于收银台展示，最终以后端结算为准 */
    private Integer estimateEarnPoints;

    /** 会员当前可用积分余额 */
    private Integer memberPoint;

    /** 本单最多可使用的抵扣积分（余额、抵扣比例、单笔上限三者取小） */
    private Integer maxDeductPoints;

}
