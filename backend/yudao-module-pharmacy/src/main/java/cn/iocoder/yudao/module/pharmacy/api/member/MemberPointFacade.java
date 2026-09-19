package cn.iocoder.yudao.module.pharmacy.api.member;

import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;

import java.math.BigDecimal;

/**
 * 会员积分门面（F 提供，D 的 POS 销售/退货、E 的支付结果、小程序订单共同依赖的边界）。
 * <p>
 * 约定：
 * <ol>
 *   <li>积分赠送 / 抵扣 / 回退的计算与落库全部在 F 侧完成，
 *       <b>D / E / 小程序都不得直接写 {@code member_point_record}，也不得自行折算积分金额</b>；</li>
 *   <li>所有写方法以「业务单号」为幂等键，重复回调不会重复变动积分；</li>
 *   <li>积分不足等业务异常由本门面抛出，调用方事务整体回滚，不留下孤立积分流水或错误余额。</li>
 * </ol>
 */
public interface MemberPointFacade {

    // ========== 一站式契约（推荐 D / E / 小程序直接使用） ==========

    /**
     * 销售积分试算：按会员等级与积分规则校验余额、抵扣比例、单笔上限与订单金额
     *
     * @param memberId         会员编号（散客可为空，为空时视为不使用抵扣）
     * @param orderAmount      抵扣前的订单金额（元）
     * @param wantDeductPoints 希望使用的抵扣积分（为空或 <=0 表示不使用）
     * @return 实际可抵扣积分、抵扣金额与赠送预估；校验不通过抛业务异常
     */
    SalePointCalcDTO calcSalePoints(Long memberId, BigDecimal orderAmount, Integer wantDeductPoints);

    /**
     * 销售结算：扣减抵扣积分 + 赠送奖励积分（幂等键 = 订单号）
     *
     * @param memberId     会员编号（为空表示散客，不产生积分）
     * @param orderNo      业务单号（销售单号 / 线上订单号）
     * @param orderAmount  抵扣前的订单金额（元）
     * @param deductPoints 实际抵扣的积分（可为空）
     * @return 实际赠送积分，调用方应写回订单的 points_earned，保证订单与流水一致
     */
    int settleSalePoints(Long memberId, String orderNo, BigDecimal orderAmount, Integer deductPoints);

    /**
     * 销售退货结算：按实际退货金额比例扣回赠送积分 + 返还原单抵扣积分（幂等键 = 退货单号）
     *
     * @param memberId     会员编号
     * @param orderNo      原销售单号
     * @param returnNo     退货单号
     * @param orderAmount  原单商品金额（元）
     * @param returnAmount 本次退货金额（元）
     * @param fullReturn   本次是否构成整单全退（全退时结清剩余积分）
     */
    void refundSalePoints(Long memberId, String orderNo, String returnNo, BigDecimal orderAmount,
                          BigDecimal returnAmount, boolean fullReturn);

    /**
     * 订单取消 / 支付失败 / 渠道退款：返还本单已预扣的抵扣积分且不赠送积分（幂等键 = 订单号）
     */
    void releaseSalePoints(Long memberId, String orderNo);

    // ========== 原子方法（保留：单笔积分变动的细粒度入口） ==========

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
