package cn.iocoder.yudao.module.pharmacy.api.member;


/**
 * 会员积分门面【临时降级实现】。
 * <p>
 * F 成员实现 {@link MemberPointFacade} 后，本 Bean 因 @ConditionalOnMissingBean 自动失效；
 * 在此之前，销售奖励积分（本期 pointsDeduct/pointsEarned 恒为 0，不触发调用）与
 * 退货积分回退会收到 UnsupportedOperationException，由 Service 层按业务降级。
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

}
