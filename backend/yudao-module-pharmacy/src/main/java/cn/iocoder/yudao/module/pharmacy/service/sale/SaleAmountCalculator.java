package cn.iocoder.yudao.module.pharmacy.service.sale;

import java.math.BigDecimal;

/**
 * 销售金额计算器（纯函数，便于单元测试）。
 */
public final class SaleAmountCalculator {

    private SaleAmountCalculator() {
    }

    /**
     * 计算应付款：原价合计 - 折扣 - 券 - 积分。
     * 任一项为负或结果为负返回 null（表示不合法）。
     */
    public static BigDecimal calcPayable(BigDecimal subtotal, BigDecimal discountAmount,
                                         BigDecimal couponAmount, BigDecimal pointsDeduct) {
        if (subtotal == null || subtotal.signum() < 0
                || discountAmount == null || discountAmount.signum() < 0
                || couponAmount == null || couponAmount.signum() < 0
                || pointsDeduct == null || pointsDeduct.signum() < 0) {
            return null;
        }
        BigDecimal payable = subtotal.subtract(discountAmount)
                .subtract(couponAmount).subtract(pointsDeduct);
        return payable.signum() < 0 ? null : payable;
    }

    /**
     * 校验实收不小于应付，并计算找零；不合法返回 null。
     */
    public static BigDecimal calcChange(BigDecimal payable, BigDecimal paid) {
        if (payable == null || paid == null || paid.compareTo(payable) < 0) {
            return null;
        }
        return paid.subtract(payable);
    }
}
