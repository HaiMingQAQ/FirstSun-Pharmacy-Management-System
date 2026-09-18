package cn.iocoder.yudao.module.pharmacy.api.member;

/**
 * 会员积分门面（F 与 D 的依赖边界）。
 * <p>
 * 全部方法以「会员编号 + 业务单号」为幂等键，重复调用不会重复加减积分；
 * 积分不足等业务异常由本门面抛出，调用方事务整体回滚。
 */
public interface MemberPointFacade {

    /**
     * 增加积分（销售奖励），bizNo 幂等
     */
    void addPoints(Long memberId, String bizNo, Integer point, String title);

    /**
     * 扣回积分（退货扣回已发放的奖励积分），bizNo 幂等
     */
    void backPoints(Long memberId, String bizNo, Integer point);

    /**
     * 积分抵扣扣减（订单使用积分抵扣现金，对应契约中的 deduct），bizNo 幂等
     *
     * <p>积分不足时抛出业务异常（PHARMACY_MEMBER_POINT_NOT_ENOUGH），调用方事务回滚。
     */
    void deductPoints(Long memberId, String bizNo, Integer point, String title);

    /**
     * 返还积分（订单取消 / 退货，退还此前抵扣的积分，对应契约中的 returnBack），bizNo 幂等
     *
     * <p>同一业务单号只会返还一次，且返还上限为该单号已抵扣的积分。
     */
    void returnPoints(Long memberId, String bizNo, Integer point, String title);

}
