package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.module.pharmacy.api.member.dto.MemberPointRuleDTO;
import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;

import java.math.BigDecimal;

/**
 * 会员积分结算服务（F 会员模块对外提供的统一积分服务）。
 *
 * <p>职责边界：
 * <ul>
 *   <li>积分赠送、抵扣、退货回退、取消返还<b>全部</b>由本服务计算并落库，
 *       D（POS 销售/退货）、E（支付结果）、小程序订单、管理后台都不得直接写
 *       {@code member_point_record}，也不得自行折算积分金额；</li>
 *   <li>赠送与抵扣规则来自 {@code yudao.pharmacy.member-point.*} 配置与会员等级，
 *       前端传入的积分值只作为「希望使用的抵扣积分」，最终可用值由后端校验后确定；</li>
 *   <li>所有写操作都带业务幂等键（会员 + 业务类型 + 业务编码），
 *       重复销售回调、重复核销、重复退货、重复取消都不会重复变动积分。</li>
 * </ul>
 */
public interface MemberPointSettlementService {

    /**
     * 销售积分试算（收银台 / 小程序下单前调用）
     *
     * <p>校验会员积分余额、抵扣比例、单笔上限与订单金额，返回实际可用的抵扣积分与抵扣金额。
     * 校验不通过时抛业务异常，调用方事务回滚。
     *
     * @param memberId          会员编号（散客为空）
     * @param orderAmount       抵扣前的订单金额（元）
     * @param wantDeductPoints  希望使用的抵扣积分（为空或 <=0 表示不使用）
     */
    SalePointCalcDTO calcSalePoints(Long memberId, BigDecimal orderAmount, Integer wantDeductPoints);

    /**
     * 销售积分结算：扣减抵扣积分 + 赠送奖励积分
     *
     * <p>幂等键 = 订单号，重复销售回调 / 重复完成订单不会重复赠送或重复抵扣。
     * 积分为 0 或非会员时不产生任何流水。
     *
     * @param memberId     会员编号
     * @param orderNo      业务单号（销售单号 / 线上订单号）
     * @param orderAmount  抵扣前的订单金额（元），赠送基数 = 订单金额 - 抵扣金额
     * @param deductPoints 本单实际抵扣的积分（可为空）
     * @return 实际赠送的积分（写入订单 points_earned，保证订单与流水一致）
     */
    int settleSalePoints(Long memberId, String orderNo, BigDecimal orderAmount, Integer deductPoints);

    /**
     * 预扣抵扣积分（下单即预扣，幂等键 = 订单号）
     *
     * <p>线上订单下单时预扣，积分不足直接抛业务异常使下单事务回滚；
     * 后续取消 / 支付超时 / 退款通过 {@link #releaseSalePoints} 释放。
     *
     * @return 实际扣减的积分（0 表示幂等跳过或无需扣减）
     */
    int deductSalePoints(Long memberId, String orderNo, Integer deductPoints);

    /**
     * 赠送奖励积分（订单完成 / 核销时触发，幂等键 = 订单号，重复核销不重复赠送）
     *
     * @param paidAmount 实际成交金额（元），作为赠送基数
     * @return 实际赠送的积分
     */
    int earnSalePoints(Long memberId, String orderNo, BigDecimal paidAmount);

    /**
     * 销售退货积分结算：按实际退货金额比例扣回赠送积分 + 返还原单抵扣积分
     *
     * <p>幂等：每张退货单（{@code returnNo}）只会回退一次；同一原单多次部分退货时
     * 按累计回退上限收敛，不会超过原单已赠送 / 已抵扣的积分，也不会按整单重复回退。
     *
     * @param memberId     会员编号
     * @param orderNo      原销售单号
     * @param returnNo     退货单号（幂等键）
     * @param orderAmount  原单商品金额（元）
     * @param returnAmount 本次退货金额（元）
     * @param fullReturn   本次退货是否已构成整单全退（全退时一次性结清剩余积分）
     */
    void refundSalePoints(Long memberId, String orderNo, String returnNo, BigDecimal orderAmount,
                          BigDecimal returnAmount, boolean fullReturn);

    /**
     * 订单取消 / 支付失败 / 渠道退款：返还本单已预扣的抵扣积分，且不赠送积分
     *
     * <p>幂等键 = 订单号，重复取消不会重复返还；未预扣积分时为无操作。
     *
     * @param memberId 会员编号
     * @param orderNo  业务单号
     */
    void releaseSalePoints(Long memberId, String orderNo);

    /**
     * 计算可赠送积分（后端按会员等级与积分规则计算，不信任前端传值）
     */
    int calcEarnPoints(Long memberId, BigDecimal amount);

    /**
     * 计算本单最多可使用的抵扣积分（取「余额、抵扣比例上限、单笔上限」三者最小值）
     */
    int calcMaxDeductPoints(Long memberId, BigDecimal orderAmount);

    /**
     * 积分折算金额（元），向下取整到分
     */
    BigDecimal pointsToAmount(Integer points);

    /**
     * 查询会员当前可用积分
     */
    int getMemberPoint(Long memberId);

    /**
     * 获得会员积分规则摘要（余额 + 当前生效的赠送 / 抵扣规则），用于小程序积分中心与收银台展示
     */
    MemberPointRuleDTO getRuleSummary(Long memberId);

}
