package cn.iocoder.yudao.module.pharmacy.api.member;


/**
 * 会员积分门面【临时降级实现】。
 * <p>
 * F 已提供真实实现 {@code MemberPointFacadeAdapter}（@Service），本 Bean 因
 * {@code @ConditionalOnMissingBean} 在正常情况下不会生效，仅作为装配兜底保留：
 * 一旦真实实现缺失（例如被误删或未加入扫描范围），调用方拿到的是明确的
 * UnsupportedOperationException，而不是静默跳过积分变动。
 */
public class MemberPointFacadeImpl implements MemberPointFacade {

    @Override
    public void addPoints(Long memberId, String bizNo, Integer point, String title) {
        throw new UnsupportedOperationException("F 会员积分服务未实现");
    }

    @Override
    public void backPoints(Long memberId, String bizNo, Integer point) {
        throw new UnsupportedOperationException("F 会员积分服务未实现");
    }

    @Override
    public void deductPoints(Long memberId, String bizNo, Integer point, String title) {
        throw new UnsupportedOperationException("F 会员积分服务未实现");
    }

    @Override
    public void returnPoints(Long memberId, String bizNo, Integer point, String title) {
        throw new UnsupportedOperationException("F 会员积分服务未实现");
    }

}
