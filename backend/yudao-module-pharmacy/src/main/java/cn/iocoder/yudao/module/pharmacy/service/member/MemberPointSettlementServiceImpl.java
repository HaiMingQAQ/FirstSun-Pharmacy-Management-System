package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.pharmacy.api.member.dto.MemberPointRuleDTO;
import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;
import cn.iocoder.yudao.module.pharmacy.config.MemberPointProperties;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberLevelDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberLevelMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberUserMapper;
import cn.iocoder.yudao.module.pharmacy.enums.member.MemberPointBizTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_DEDUCT_AMOUNT_EXCEED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_DEDUCT_DISABLED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_DEDUCT_EXCEED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_DEDUCT_MEMBER_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_DEDUCT_TOO_SMALL;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_NOT_ENOUGH;

/**
 * 会员积分结算服务实现（F 会员模块统一积分服务）。
 *
 * <p>积分计算全部在后端完成：
 * <pre>
 *   赠送积分 = floor(实付金额 × earn-per-yuan × 等级倍率)
 *   抵扣金额 = 抵扣积分 ÷ points-per-yuan（向下取整到分）
 *   可用抵扣积分 = min(会员余额, 订单金额 × max-deduct-percent, 单笔上限)
 * </pre>
 * 退货回退按「实际退货金额 ÷ 原单金额」比例折算，并在全退时结清剩余部分。
 */
@Slf4j
@Service
@Validated
public class MemberPointSettlementServiceImpl implements MemberPointSettlementService {

    /** 退货回退流水的业务编码分隔符：原单号#EARN#退货单号 / 原单号#DEDUCT#退货单号 */
    private static final String EARN_BIZ_MARK = "#EARN#";
    private static final String DEDUCT_BIZ_MARK = "#DEDUCT#";

    /** 积分标题（写入流水，作为「业务来源」可读凭证） */
    private static final String TITLE_SALE_EARN = "消费获得积分";
    private static final String TITLE_SALE_DEDUCT = "订单积分抵扣";
    private static final String TITLE_RETURN_EARN_BACK = "退货积分回退";
    private static final String TITLE_RETURN_DEDUCT_BACK = "退货返还抵扣积分";
    private static final String TITLE_RELEASE_DEDUCT = "订单取消返还抵扣积分";

    @Resource
    private MemberPointProperties memberPointProperties;

    @Resource
    private MemberPointRecordService memberPointRecordService;

    @Resource
    private MemberUserMapper memberUserMapper;

    @Resource
    private MemberLevelMapper memberLevelMapper;

    // ========== 试算 ==========

    @Override
    public SalePointCalcDTO calcSalePoints(Long memberId, BigDecimal orderAmount, Integer wantDeductPoints) {
        SalePointCalcDTO result = new SalePointCalcDTO();
        result.setMemberPoint(getMemberPoint(memberId));
        BigDecimal amount = orderAmount == null ? BigDecimal.ZERO : orderAmount;
        result.setMaxDeductPoints(calcMaxDeductPoints(memberId, amount));
        int want = wantDeductPoints == null ? 0 : wantDeductPoints;
        if (want <= 0) {
            // 不使用积分：仍返回赠送预估，便于收银台展示
            result.setDeductPoints(0);
            result.setDeductAmount(BigDecimal.ZERO);
            result.setEstimateEarnPoints(calcEarnPoints(memberId, amount));
            return result;
        }
        // 校验会员：散客不能使用积分抵扣
        if (memberId == null) {
            throw exception(PHARMACY_MEMBER_POINT_DEDUCT_MEMBER_REQUIRED);
        }
        if (!memberPointProperties.isDeductEnabled()) {
            throw exception(PHARMACY_MEMBER_POINT_DEDUCT_DISABLED);
        }
        int minPoints = memberPointProperties.getMinDeductPoints() == null ? 0
                : memberPointProperties.getMinDeductPoints();
        if (want < minPoints) {
            throw exception(PHARMACY_MEMBER_POINT_DEDUCT_TOO_SMALL, minPoints);
        }
        // 校验余额：余额不足直接给出「当前可用积分 / 本次需要」的业务提示
        int balance = getMemberPoint(memberId);
        if (balance <= 0 || want > balance) {
            throw exception(PHARMACY_MEMBER_POINT_NOT_ENOUGH, balance, want);
        }
        // 校验抵扣比例与单笔上限
        int maxDeduct = calcMaxDeductPoints(memberId, amount);
        if (want > maxDeduct) {
            throw exception(PHARMACY_MEMBER_POINT_DEDUCT_EXCEED, maxDeduct);
        }
        // 校验抵扣金额不能超过订单金额
        BigDecimal deductAmount = pointsToAmount(want);
        if (amount.signum() > 0 && deductAmount.compareTo(amount) > 0) {
            throw exception(PHARMACY_MEMBER_POINT_DEDUCT_AMOUNT_EXCEED, amount);
        }
        result.setDeductPoints(want);
        result.setDeductAmount(deductAmount);
        result.setEstimateEarnPoints(calcEarnPoints(memberId, amount.subtract(deductAmount)));
        return result;
    }

    // ========== 结算 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int settleSalePoints(Long memberId, String orderNo, BigDecimal orderAmount, Integer deductPoints) {
        if (memberId == null || StrUtil.isBlank(orderNo)) {
            // 散客 / 无业务单号：不产生任何积分流水
            return 0;
        }
        int usePoints = deductSalePoints(memberId, orderNo, deductPoints);
        BigDecimal deductAmount = usePoints > 0 ? pointsToAmount(usePoints) : BigDecimal.ZERO;
        BigDecimal base = orderAmount == null ? BigDecimal.ZERO : orderAmount;
        BigDecimal earnBase = base.subtract(deductAmount);
        if (earnBase.signum() < 0) {
            earnBase = BigDecimal.ZERO;
        }
        return earnSalePoints(memberId, orderNo, earnBase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deductSalePoints(Long memberId, String orderNo, Integer deductPoints) {
        if (memberId == null || StrUtil.isBlank(orderNo) || deductPoints == null || deductPoints <= 0) {
            return 0;
        }
        BigDecimal deductAmount = pointsToAmount(deductPoints);
        // 幂等键 = 订单号（重复支付回调 / 重复下单回调不会重复扣减）；
        // 积分不足时 rejectInsufficient=true，抛业务异常让调用方事务整体回滚
        return memberPointRecordService.changePoints(memberId, MemberPointBizTypeEnum.CONSUME_DEDUCT.getBizType(),
                orderNo, deductPoints, TITLE_SALE_DEDUCT,
                StrUtil.format("订单 {} 使用 {} 积分抵扣 {} 元", orderNo, deductPoints, deductAmount),
                -1, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int earnSalePoints(Long memberId, String orderNo, BigDecimal paidAmount) {
        if (memberId == null || StrUtil.isBlank(orderNo)) {
            return 0;
        }
        BigDecimal earnBase = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        int earnPoints = calcEarnPoints(memberId, earnBase);
        if (earnPoints <= 0) {
            // 未达到赠送条件（关闭赠送 / 金额为 0 / 规则折算为 0）：不写流水，避免产生 0 分流水
            return 0;
        }
        int actual = memberPointRecordService.changePoints(memberId, MemberPointBizTypeEnum.CONSUME_EARN.getBizType(),
                orderNo, earnPoints, TITLE_SALE_EARN,
                StrUtil.format("订单 {} 实付 {} 元，按会员等级与积分规则赠送", orderNo, earnBase), 1, false);
        if (actual > 0) {
            return actual;
        }
        // 幂等跳过（并发重复回调）：返回已落库的赠送积分，保证订单 pointsEarned 与流水汇总一致
        MemberPointRecordDO exist = memberPointRecordService.getPointRecord(memberId,
                MemberPointBizTypeEnum.CONSUME_EARN.getBizType(), orderNo);
        return exist == null || exist.getPoint() == null ? 0 : Math.max(0, exist.getPoint());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundSalePoints(Long memberId, String orderNo, String returnNo, BigDecimal orderAmount,
                                 BigDecimal returnAmount, boolean fullReturn) {
        if (memberId == null || StrUtil.isBlank(orderNo) || StrUtil.isBlank(returnNo)) {
            return;
        }
        // 1. 按实际退货金额比例扣回已赠送积分
        int totalEarn = existedAbsPoint(memberId, MemberPointBizTypeEnum.CONSUME_EARN.getBizType(), orderNo);
        if (totalEarn > 0) {
            String prefix = orderNo + EARN_BIZ_MARK;
            int alreadyBack = memberPointRecordService.sumAbsPointByBizIdPrefix(memberId,
                    MemberPointBizTypeEnum.REFUND.getBizType(), prefix);
            int targetBack = calcRefundShare(totalEarn, orderAmount, returnAmount, fullReturn);
            int thisBack = Math.max(0, targetBack - alreadyBack);
            if (thisBack > 0) {
                memberPointRecordService.changePoints(memberId, MemberPointBizTypeEnum.REFUND.getBizType(),
                        prefix + returnNo, thisBack, TITLE_RETURN_EARN_BACK,
                        StrUtil.format("原单 {} 退货 {}，退货金额 {} / 原单金额 {}，按比例扣回赠送积分 {}",
                                orderNo, returnNo, returnAmount, orderAmount, thisBack),
                        -1, false);
            }
        }
        // 2. 返还原单实际抵扣的积分（同样按退货金额比例，全退时结清剩余）
        int totalDeduct = existedAbsPoint(memberId, MemberPointBizTypeEnum.CONSUME_DEDUCT.getBizType(), orderNo);
        if (totalDeduct > 0) {
            String prefix = orderNo + DEDUCT_BIZ_MARK;
            int alreadyBack = memberPointRecordService.sumAbsPointByBizIdPrefix(memberId,
                    MemberPointBizTypeEnum.REFUND.getBizType(), prefix);
            int targetBack = calcRefundShare(totalDeduct, orderAmount, returnAmount, fullReturn);
            int thisBack = Math.max(0, targetBack - alreadyBack);
            if (thisBack > 0) {
                memberPointRecordService.changePoints(memberId, MemberPointBizTypeEnum.REFUND.getBizType(),
                        prefix + returnNo, thisBack, TITLE_RETURN_DEDUCT_BACK,
                        StrUtil.format("原单 {} 退货 {}，退还原单抵扣积分 {}（原单共抵扣 {}）",
                                orderNo, returnNo, thisBack, totalDeduct),
                        1, false);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSalePoints(Long memberId, String orderNo) {
        if (memberId == null || StrUtil.isBlank(orderNo)) {
            return;
        }
        // 幂等键 = 订单号（退款冲回）；未预扣积分时 returnPoints 内部直接返回，不产生孤立流水
        memberPointRecordService.returnPoints(memberId, orderNo, Integer.MAX_VALUE, TITLE_RELEASE_DEDUCT);
    }

    // ========== 规则计算 ==========

    @Override
    public int calcEarnPoints(Long memberId, BigDecimal amount) {
        if (!memberPointProperties.isEarnEnabled()) {
            return 0;
        }
        if (amount == null || amount.signum() <= 0) {
            return 0;
        }
        BigDecimal earnPerYuan = memberPointProperties.getEarnPerYuan() == null
                ? BigDecimal.ONE : memberPointProperties.getEarnPerYuan();
        BigDecimal earn = amount.multiply(earnPerYuan).multiply(resolveLevelMultiplier(memberId));
        return Math.max(0, earn.setScale(0, RoundingMode.DOWN).intValue());
    }

    @Override
    public int calcMaxDeductPoints(Long memberId, BigDecimal orderAmount) {
        if (!memberPointProperties.isDeductEnabled() || memberId == null) {
            return 0;
        }
        int balance = getMemberPoint(memberId);
        if (balance <= 0) {
            return 0;
        }
        int maxByAmount = 0;
        if (orderAmount != null && orderAmount.signum() > 0) {
            Integer percent = memberPointProperties.getMaxDeductPercent() == null
                    ? 0 : memberPointProperties.getMaxDeductPercent();
            BigDecimal maxAmount = orderAmount.multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
            maxByAmount = maxAmount.multiply(BigDecimal.valueOf(pointsPerYuan()))
                    .setScale(0, RoundingMode.DOWN).intValue();
        }
        int max = Math.min(balance, maxByAmount);
        Integer singleLimit = memberPointProperties.getMaxDeductPoints();
        if (singleLimit != null && singleLimit > 0) {
            max = Math.min(max, singleLimit);
        }
        return Math.max(0, max);
    }

    @Override
    public BigDecimal pointsToAmount(Integer points) {
        if (points == null || points <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(points)
                .divide(BigDecimal.valueOf(pointsPerYuan()), 2, RoundingMode.DOWN);
    }

    @Override
    public int getMemberPoint(Long memberId) {
        if (memberId == null) {
            return 0;
        }
        MemberUserDO user = memberUserMapper.selectById(memberId);
        return user == null || user.getPoint() == null ? 0 : user.getPoint();
    }

    @Override
    public MemberPointRuleDTO getRuleSummary(Long memberId) {
        MemberPointRuleDTO summary = new MemberPointRuleDTO();
        summary.setMemberId(memberId);
        summary.setMemberPoint(getMemberPoint(memberId));
        summary.setEarnEnabled(memberPointProperties.isEarnEnabled());
        summary.setDeductEnabled(memberPointProperties.isDeductEnabled());
        summary.setEarnPerYuan(memberPointProperties.getEarnPerYuan() == null
                ? BigDecimal.ONE : memberPointProperties.getEarnPerYuan());
        summary.setPointsPerYuan(pointsPerYuan());
        summary.setMaxDeductPercent(memberPointProperties.getMaxDeductPercent());
        summary.setMinDeductPoints(memberPointProperties.getMinDeductPoints());
        summary.setMaxDeductPoints(memberPointProperties.getMaxDeductPoints());
        summary.setLevelMultiplier(resolveLevelMultiplier(memberId));
        // 等级信息（仅用于展示，缺失时保持为空）
        MemberUserDO user = memberId == null ? null : memberUserMapper.selectById(memberId);
        if (user != null && user.getLevelId() != null) {
            MemberLevelDO level = memberLevelMapper.selectById(user.getLevelId());
            if (level != null) {
                summary.setLevelId(level.getId());
                summary.setLevel(level.getLevel());
                summary.setLevelName(level.getName());
            }
        }
        return summary;
    }

    // ========== 私有方法 ==========

    /**
     * 按退货金额比例折算应回退的积分；整单全退时一次性结清全部积分。
     */
    private int calcRefundShare(int totalPoints, BigDecimal orderAmount, BigDecimal returnAmount, boolean fullReturn) {
        if (totalPoints <= 0) {
            return 0;
        }
        if (fullReturn) {
            return totalPoints;
        }
        if (orderAmount == null || orderAmount.signum() <= 0 || returnAmount == null || returnAmount.signum() <= 0) {
            return 0;
        }
        if (returnAmount.compareTo(orderAmount) >= 0) {
            return totalPoints;
        }
        // 向下取整：部分退货累计回退不会超过原单积分，差额由「全退」这一次结清
        return BigDecimal.valueOf(totalPoints).multiply(returnAmount)
                .divide(orderAmount, 0, RoundingMode.DOWN).intValue();
    }

    /**
     * 解析会员等级倍率；会员不存在、未分配等级或等级未配置倍率时按 1.0 计算。
     */
    private BigDecimal resolveLevelMultiplier(Long memberId) {
        BigDecimal defaultMultiplier = BigDecimal.ONE;
        if (memberId == null) {
            return defaultMultiplier;
        }
        MemberUserDO user = memberUserMapper.selectById(memberId);
        if (user == null || user.getLevelId() == null) {
            return defaultMultiplier;
        }
        MemberLevelDO level = memberLevelMapper.selectById(user.getLevelId());
        if (level == null || level.getLevel() == null) {
            return defaultMultiplier;
        }
        BigDecimal multiplier = memberPointProperties.getLevelMultiplier() == null ? null
                : memberPointProperties.getLevelMultiplier().get(level.getLevel());
        return multiplier == null || multiplier.signum() < 0 ? defaultMultiplier : multiplier;
    }

    private int pointsPerYuan() {
        Integer pointsPerYuan = memberPointProperties.getPointsPerYuan();
        return pointsPerYuan == null || pointsPerYuan <= 0 ? 1 : pointsPerYuan;
    }

    private int existedAbsPoint(Long memberId, Integer bizType, String bizId) {
        MemberPointRecordDO record = memberPointRecordService.getPointRecord(memberId, bizType, bizId);
        if (record == null || record.getPoint() == null) {
            return 0;
        }
        return Math.abs(record.getPoint());
    }

}
