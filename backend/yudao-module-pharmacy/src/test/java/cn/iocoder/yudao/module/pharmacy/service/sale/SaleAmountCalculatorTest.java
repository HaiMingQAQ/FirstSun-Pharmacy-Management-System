package cn.iocoder.yudao.module.pharmacy.service.sale;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link SaleAmountCalculator} 单元测试：金额计算（折扣/券/积分/找零）。
 */
class SaleAmountCalculatorTest {

    @Test
    void testCalcPayable_normal() {
        BigDecimal payable = SaleAmountCalculator.calcPayable(
                new BigDecimal("100.00"), new BigDecimal("10.00"),
                new BigDecimal("5.00"), new BigDecimal("5.00"));
        assertEquals(new BigDecimal("80.00"), payable);
    }

    @Test
    void testCalcPayable_noDeduction() {
        BigDecimal payable = SaleAmountCalculator.calcPayable(
                new BigDecimal("88.50"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("88.50"), payable);
    }

    @Test
    void testCalcPayable_negativeSubtotal() {
        assertNull(SaleAmountCalculator.calcPayable(
                new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void testCalcPayable_null() {
        assertNull(SaleAmountCalculator.calcPayable(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void testCalcPayable_negativeDeduction() {
        assertNull(SaleAmountCalculator.calcPayable(
                new BigDecimal("100"), new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void testCalcPayable_resultNegative() {
        // 抵扣超过原价合计，应付款为负，视为不合法
        assertNull(SaleAmountCalculator.calcPayable(
                new BigDecimal("10"), new BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void testCalcChange_normal() {
        BigDecimal change = SaleAmountCalculator.calcChange(new BigDecimal("80.00"), new BigDecimal("100.00"));
        assertEquals(new BigDecimal("20.00"), change);
    }

    @Test
    void testCalcChange_zero() {
        BigDecimal change = SaleAmountCalculator.calcChange(new BigDecimal("80.00"), new BigDecimal("80.00"));
        // 0.00 与 0 数值相等（scale 不同），用 compareTo 比较
        assertEquals(0, change.compareTo(BigDecimal.ZERO));
    }

    @Test
    void testCalcChange_insufficientPaid() {
        // 实收小于应付，不合法
        assertNull(SaleAmountCalculator.calcChange(new BigDecimal("100.00"), new BigDecimal("80.00")));
    }

    @Test
    void testCalcChange_null() {
        assertNull(SaleAmountCalculator.calcChange(null, BigDecimal.ZERO));
        assertNull(SaleAmountCalculator.calcChange(BigDecimal.ZERO, null));
    }

}
