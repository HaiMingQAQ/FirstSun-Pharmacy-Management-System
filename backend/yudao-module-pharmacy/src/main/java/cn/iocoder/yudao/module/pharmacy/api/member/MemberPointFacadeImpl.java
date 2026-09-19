package cn.iocoder.yudao.module.pharmacy.api.member;

import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;

import java.math.BigDecimal;

/**
 * 会员积分门面【临时降级实现】。
 * <p>
 * F 已提供真实实现 {@code MemberPointFacadeAdapter}（@Service），本 Bean 因
 * {@code @ConditionalOnMissingBean} 在正常情况下不会生效，仅作为装配兜底保留：
 * 一旦真实实现缺失（例如被误删或未加入扫描范围），调用方拿到的是明确的
 * UnsupportedOperationException，而不是静默跳过积分变动。
 */
public class MemberPointFacadeImpl implements MemberPointFacade {

    private static UnsupportedOperationException notImplemented() {
        return new UnsupportedOperationException("F 会员积分服务未实现");
    }

    @Override
    public SalePointCalcDTO calcSalePoints(Long memberId, BigDecimal orderAmount, Integer wantDeductPoints) {
        throw notImplemented();
    }

    @Override
    public int settleSalePoints(Long memberId, String orderNo, BigDecimal orderAmount, Integer deductPoints) {
        throw notImplemented();
    }

    @Override
    public void refundSalePoints(Long memberId, String orderNo, String returnNo, BigDecimal orderAmount,
                                 BigDecimal returnAmount, boolean fullReturn) {
        throw notImplemented();
    }

    @Override
    public void releaseSalePoints(Long memberId, String orderNo) {
        throw notImplemented();
    }

    @Override
    public void addPoints(Long memberId, String bizNo, Integer point, String title) {
        throw notImplemented();
    }

    @Override
    public void backPoints(Long memberId, String bizNo, Integer point) {
        throw notImplemented();
    }

    @Override
    public void deductPoints(Long memberId, String bizNo, Integer point, String title) {
        throw notImplemented();
    }

    @Override
    public void returnPoints(Long memberId, String bizNo, Integer point, String title) {
        throw notImplemented();
    }

}
