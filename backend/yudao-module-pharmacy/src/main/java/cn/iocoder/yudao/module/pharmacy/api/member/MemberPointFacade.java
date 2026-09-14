package cn.iocoder.yudao.module.pharmacy.api.member;

/**
 * 会员积分门面（F 与 D 的依赖边界）。
 * <p>
 * 当前 F 未实现，调用方捕获 UnsupportedOperationException 后按业务降级处理。
 */
public interface MemberPointFacade {

    /**
     * 增加积分（销售奖励），bizNo 幂等
     */
    void addPoints(Long memberId, String bizNo, Integer point, String title);

    /**
     * 扣回积分（退货），bizNo 幂等
     */
    void backPoints(Long memberId, String bizNo, Integer point);
}
