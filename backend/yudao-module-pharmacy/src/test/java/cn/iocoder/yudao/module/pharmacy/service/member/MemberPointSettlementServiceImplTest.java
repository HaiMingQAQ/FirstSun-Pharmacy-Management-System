package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;
import cn.iocoder.yudao.module.pharmacy.config.MemberPointProperties;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberLevelDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberLevelMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_DEDUCT_DISABLED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_DEDUCT_EXCEED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_DEDUCT_MEMBER_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_DEDUCT_TOO_SMALL;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_NOT_ENOUGH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MemberPointSettlementServiceImpl} 单元测试：F 的统一积分结算服务。
 *
 * 覆盖：
 * - 赠送积分由后端按「金额 × 每元积分 × 等级倍率」计算，前端传值不被采信；
 * - 抵扣积分按「余额、抵扣比例、单笔上限、订单金额」逐项校验，越界必须抛业务异常；
 * - 抵扣 / 赠送 / 退货回退 / 取消返还全部以业务单号为幂等键，重复调用不重复变动积分；
 * - 退货按实际退货金额比例回退，整单全退时结清剩余，重复退货不再回退。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberPointSettlementServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final String ORDER_NO = "WX-407-20260918-0001";
    private static final String RETURN_NO = "SR-407-20260918120000-001";

    private static final Integer BIZ_TYPE_EARN = 2;
    private static final Integer BIZ_TYPE_DEDUCT = 3;
    private static final Integer BIZ_TYPE_REFUND = 6;

    @Mock
    private MemberPointRecordService memberPointRecordService;
    @Mock
    private MemberUserMapper memberUserMapper;
    @Mock
    private MemberLevelMapper memberLevelMapper;

    /** 使用真实配置对象，默认值即课程演示环境的规则 */
    private final MemberPointProperties memberPointProperties = new MemberPointProperties();

    private MemberPointSettlementServiceImpl memberPointSettlementService;

    @BeforeEach
    void setUp() {
        memberPointSettlementService = new MemberPointSettlementServiceImpl();
        inject("memberPointProperties", memberPointProperties);
        inject("memberPointRecordService", memberPointRecordService);
        inject("memberUserMapper", memberUserMapper);
        inject("memberLevelMapper", memberLevelMapper);
    }

    // ========== 赠送积分规则 ==========

    /** 无等级会员：100 元 × 每元 1 分 = 100 分 */
    @Test
    void testCalcEarnPoints_baseRule() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(0, null));

        assertEquals(100, memberPointSettlementService.calcEarnPoints(MEMBER_ID, new BigDecimal("100.00")));
    }

    /** 等级倍率生效：2 级会员倍率 1.2，100 元 × 1 × 1.2 = 120 分 */
    @Test
    void testCalcEarnPoints_levelMultiplier() {
        memberPointProperties.setLevelMultiplier(Map.of(2, new BigDecimal("1.2")));
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(0, 20L));
        when(memberLevelMapper.selectById(20L)).thenReturn(buildLevel(20L, 2, "黄金会员"));

        assertEquals(120, memberPointSettlementService.calcEarnPoints(MEMBER_ID, new BigDecimal("100.00")));
    }

    /** 未配置倍率的等级按 1.0 计算，不能凭空放大积分 */
    @Test
    void testCalcEarnPoints_unmappedLevelUsesDefaultMultiplier() {
        memberPointProperties.setLevelMultiplier(Map.of(2, new BigDecimal("1.2")));
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(0, 30L));
        when(memberLevelMapper.selectById(30L)).thenReturn(buildLevel(30L, 9, "未配置倍率等级"));

        assertEquals(100, memberPointSettlementService.calcEarnPoints(MEMBER_ID, new BigDecimal("100.00")));
    }

    /** 关闭赠送后不再计算任何积分 */
    @Test
    void testCalcEarnPoints_disabled() {
        memberPointProperties.setEarnEnabled(false);

        assertEquals(0, memberPointSettlementService.calcEarnPoints(MEMBER_ID, new BigDecimal("100.00")));
    }

    /** 金额非法 / 为 0 时不赠送积分 */
    @Test
    void testCalcEarnPoints_invalidAmount() {
        assertEquals(0, memberPointSettlementService.calcEarnPoints(MEMBER_ID, null));
        assertEquals(0, memberPointSettlementService.calcEarnPoints(MEMBER_ID, BigDecimal.ZERO));
        assertEquals(0, memberPointSettlementService.calcEarnPoints(MEMBER_ID, new BigDecimal("-1")));
    }

    // ========== 抵扣上限规则 ==========

    /** 上限 = min(余额, 订单金额 × 最高抵扣比例, 单笔上限)：此处金额比例（50 元 → 5000 分）更小 */
    @Test
    void testCalcMaxDeductPoints_limitedByDeductPercent() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(10000, null));

        assertEquals(5000, memberPointSettlementService.calcMaxDeductPoints(MEMBER_ID, new BigDecimal("100.00")));
    }

    /** 余额小于比例上限时以余额为准 */
    @Test
    void testCalcMaxDeductPoints_limitedByBalance() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(300, null));

        assertEquals(300, memberPointSettlementService.calcMaxDeductPoints(MEMBER_ID, new BigDecimal("100.00")));
    }

    /** 配置的单笔上限会再收敛一次 */
    @Test
    void testCalcMaxDeductPoints_limitedBySingleLimit() {
        memberPointProperties.setMaxDeductPoints(200);
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(10000, null));

        assertEquals(200, memberPointSettlementService.calcMaxDeductPoints(MEMBER_ID, new BigDecimal("100.00")));
    }

    /** 关闭抵扣 / 余额为 0 时上限为 0 */
    @Test
    void testCalcMaxDeductPoints_disabledOrZeroBalance() {
        memberPointProperties.setDeductEnabled(false);
        assertEquals(0, memberPointSettlementService.calcMaxDeductPoints(MEMBER_ID, new BigDecimal("100.00")));

        memberPointProperties.setDeductEnabled(true);
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(0, null));
        assertEquals(0, memberPointSettlementService.calcMaxDeductPoints(MEMBER_ID, new BigDecimal("100.00")));
    }

    // ========== 抵扣试算 ==========

    /** 不使用积分：抵扣为 0，但仍返回赠送预估 */
    @Test
    void testCalcSalePoints_noPointsRequested() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(500, null));

        SalePointCalcDTO result = memberPointSettlementService
                .calcSalePoints(MEMBER_ID, new BigDecimal("88.00"), 0);

        assertEquals(0, result.getDeductPoints());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getDeductAmount()));
        assertEquals(88, result.getEstimateEarnPoints());
        assertEquals(500, result.getMemberPoint());
        // 上限仍按余额给出（余额 500 < 金额比例上限 4400）
        assertEquals(500, result.getMaxDeductPoints());
    }

    /** 正常抵扣：1000 分抵 10 元，赠送基数变为 90 元 */
    @Test
    void testCalcSalePoints_success() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(5000, null));

        SalePointCalcDTO result = memberPointSettlementService
                .calcSalePoints(MEMBER_ID, new BigDecimal("100.00"), 1000);

        assertEquals(1000, result.getDeductPoints());
        assertEquals(0, new BigDecimal("10.00").compareTo(result.getDeductAmount()));
        assertEquals(90, result.getEstimateEarnPoints());
    }

    /** 积分余额不足：抛「积分不足」业务异常，由调用方事务回滚 */
    @Test
    void testCalcSalePoints_notEnough() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(100, null));

        ServiceException ex = assertThrows(ServiceException.class, () -> memberPointSettlementService
                .calcSalePoints(MEMBER_ID, new BigDecimal("100.00"), 300));

        assertEquals(PHARMACY_MEMBER_POINT_NOT_ENOUGH.getCode(), ex.getCode());
    }

    /** 超出「单笔最高抵扣比例」：余额充足也只能用 5000 分 */
    @Test
    void testCalcSalePoints_exceedDeductPercent() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(10000, null));

        ServiceException ex = assertThrows(ServiceException.class, () -> memberPointSettlementService
                .calcSalePoints(MEMBER_ID, new BigDecimal("100.00"), 6000));

        assertEquals(PHARMACY_MEMBER_POINT_DEDUCT_EXCEED.getCode(), ex.getCode());
    }

    /** 散客（无会员）不允许使用积分抵扣 */
    @Test
    void testCalcSalePoints_guestRejected() {
        ServiceException ex = assertThrows(ServiceException.class, () -> memberPointSettlementService
                .calcSalePoints(null, new BigDecimal("100.00"), 100));

        assertEquals(PHARMACY_MEMBER_POINT_DEDUCT_MEMBER_REQUIRED.getCode(), ex.getCode());
    }

    /** 环境关闭积分抵扣 */
    @Test
    void testCalcSalePoints_deductDisabled() {
        memberPointProperties.setDeductEnabled(false);

        ServiceException ex = assertThrows(ServiceException.class, () -> memberPointSettlementService
                .calcSalePoints(MEMBER_ID, new BigDecimal("100.00"), 100));

        assertEquals(PHARMACY_MEMBER_POINT_DEDUCT_DISABLED.getCode(), ex.getCode());
    }

    /** 低于单笔最少使用积分 */
    @Test
    void testCalcSalePoints_belowMinPoints() {
        memberPointProperties.setMinDeductPoints(10);
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(5000, null));

        ServiceException ex = assertThrows(ServiceException.class, () -> memberPointSettlementService
                .calcSalePoints(MEMBER_ID, new BigDecimal("100.00"), 5));

        assertEquals(PHARMACY_MEMBER_POINT_DEDUCT_TOO_SMALL.getCode(), ex.getCode());
    }

    // ========== 结算：抵扣 + 赠送 ==========

    /** 销售结算：先按订单号扣抵扣，再按「抵扣后金额」赠送积分，并与订单/流水保持一致 */
    @Test
    void testSettleSalePoints_deductThenEarn() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(5000, null));
        when(memberPointRecordService.changePoints(any(), anyInt(), anyString(), anyInt(), anyString(),
                any(), anyInt(), anyBoolean())).thenReturn(1000, 90);

        int earned = memberPointSettlementService
                .settleSalePoints(MEMBER_ID, ORDER_NO, new BigDecimal("100.00"), 1000);

        assertEquals(90, earned);
        // 抵扣：业务类型=3，业务编码=订单号，扣减 1000 分，积分不足必须抛异常
        verify(memberPointRecordService).changePoints(eq(MEMBER_ID), eq(BIZ_TYPE_DEDUCT), eq(ORDER_NO),
                eq(1000), anyString(), anyString(), eq(-1), eq(true));
        // 赠送：业务类型=2，业务编码=订单号，加 90 分
        verify(memberPointRecordService).changePoints(eq(MEMBER_ID), eq(BIZ_TYPE_EARN), eq(ORDER_NO),
                eq(90), anyString(), anyString(), eq(1), eq(false));
    }

    /** 重复销售回调：赠送流水已存在时不再加分，并把已落库积分返回给订单，保证订单与流水一致 */
    @Test
    void testSettleSalePoints_repeatCallbackIsIdempotent() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(4000, null));
        when(memberPointRecordService.changePoints(any(), anyInt(), anyString(), anyInt(), anyString(),
                any(), anyInt(), anyBoolean())).thenReturn(0, 0);
        MemberPointRecordDO existed = new MemberPointRecordDO();
        existed.setPoint(90);
        when(memberPointRecordService.getPointRecord(MEMBER_ID, BIZ_TYPE_EARN, ORDER_NO)).thenReturn(existed);

        int earned = memberPointSettlementService
                .settleSalePoints(MEMBER_ID, ORDER_NO, new BigDecimal("100.00"), 1000);

        assertEquals(90, earned, "重复回调应返回已落库的赠送积分，不得再加分");
    }

    /** 散客销售：不产生任何积分流水 */
    @Test
    void testSettleSalePoints_guestProducesNoFlow() {
        assertEquals(0, memberPointSettlementService
                .settleSalePoints(null, ORDER_NO, new BigDecimal("100.00"), 0));
        verify(memberPointRecordService, never()).changePoints(any(), anyInt(), anyString(), anyInt(),
                anyString(), any(), anyInt(), anyBoolean());
    }

    /** 赠送积分为 0（金额为 0）时不写流水 */
    @Test
    void testSettleSalePoints_zeroAmountProducesNoFlow() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(0, null));

        assertEquals(0, memberPointSettlementService
                .settleSalePoints(MEMBER_ID, ORDER_NO, BigDecimal.ZERO, 0));

        verify(memberPointRecordService, never()).changePoints(any(), anyInt(), anyString(), anyInt(),
                anyString(), any(), anyInt(), anyBoolean());
    }

    /** 预扣抵扣积分：幂等跳过时返回 0，不重复扣减 */
    @Test
    void testDeductSalePoints_idempotentSkip() {
        when(memberPointRecordService.changePoints(any(), anyInt(), anyString(), anyInt(), anyString(),
                any(), anyInt(), anyBoolean())).thenReturn(0);

        assertEquals(0, memberPointSettlementService.deductSalePoints(MEMBER_ID, ORDER_NO, 500));
    }

    /** 无抵扣积分时不写任何流水 */
    @Test
    void testDeductSalePoints_zeroPoints() {
        assertEquals(0, memberPointSettlementService.deductSalePoints(MEMBER_ID, ORDER_NO, 0));
        assertEquals(0, memberPointSettlementService.deductSalePoints(MEMBER_ID, ORDER_NO, null));
        verify(memberPointRecordService, never()).changePoints(any(), anyInt(), anyString(), anyInt(),
                anyString(), any(), anyInt(), anyBoolean());
    }

    // ========== 取消 / 支付失败：释放预扣积分 ==========

    /** 取消 / 支付失败：以订单号为幂等键返还抵扣积分，不赠送积分 */
    @Test
    void testReleaseSalePoints() {
        memberPointSettlementService.releaseSalePoints(MEMBER_ID, ORDER_NO);

        verify(memberPointRecordService).returnPoints(eq(MEMBER_ID), eq(ORDER_NO), anyInt(), anyString());
        verify(memberPointRecordService, never()).changePoints(any(), eq(BIZ_TYPE_EARN), anyString(), anyInt(),
                anyString(), any(), anyInt(), anyBoolean());
    }

    /** 已预扣积分时重复取消只返还一次（幂等键 = 订单号，由积分记录服务保证） */
    @Test
    void testReleaseSalePoints_repeatIsIdempotent() {
        memberPointSettlementService.releaseSalePoints(MEMBER_ID, ORDER_NO);
        memberPointSettlementService.releaseSalePoints(MEMBER_ID, ORDER_NO);

        verify(memberPointRecordService, org.mockito.Mockito.times(2))
                .returnPoints(eq(MEMBER_ID), eq(ORDER_NO), anyInt(), anyString());
        // 幂等由 returnPoints 内部按 (会员,退款冲回,订单号) 保证，第二次不会重复加分
        verify(memberPointRecordService, never()).changePoints(any(), eq(BIZ_TYPE_REFUND), anyString(), anyInt(),
                anyString(), any(), anyInt(), anyBoolean());
    }

    // ========== 退货：按比例回退 ==========

    /** 部分退货：按退货金额占原单金额比例扣回已赠送积分 */
    @Test
    void testRefundSalePoints_partialRefundIsProportional() {
        when(memberPointRecordService.getPointRecord(MEMBER_ID, BIZ_TYPE_EARN, ORDER_NO)).thenReturn(buildRecord(100));
        when(memberPointRecordService.sumAbsPointByBizIdPrefix(eq(MEMBER_ID), eq(BIZ_TYPE_REFUND), anyString()))
                .thenReturn(0);

        memberPointSettlementService.refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("100.00"), new BigDecimal("30.00"), false);

        verify(memberPointRecordService).changePoints(eq(MEMBER_ID), eq(BIZ_TYPE_REFUND),
                eq(ORDER_NO + "#EARN#" + RETURN_NO), eq(30), anyString(), anyString(), eq(-1), eq(false));
    }

    /** 整单全退：结清剩余未回退的赠送积分（原赠送 100，此前已回退 30 → 本次回退 70） */
    @Test
    void testRefundSalePoints_fullRefundSettlesRemainder() {
        when(memberPointRecordService.getPointRecord(MEMBER_ID, BIZ_TYPE_EARN, ORDER_NO)).thenReturn(buildRecord(100));
        when(memberPointRecordService.sumAbsPointByBizIdPrefix(eq(MEMBER_ID), eq(BIZ_TYPE_REFUND), anyString()))
                .thenReturn(30);

        memberPointSettlementService.refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("100.00"), new BigDecimal("70.00"), true);

        verify(memberPointRecordService).changePoints(eq(MEMBER_ID), eq(BIZ_TYPE_REFUND),
                eq(ORDER_NO + "#EARN#" + RETURN_NO), eq(70), anyString(), anyString(), eq(-1), eq(false));
    }

    /** 重复退货：同一退货单已回退过，不再重复回退（部分退货不得按整单重复回退） */
    @Test
    void testRefundSalePoints_repeatRefundIsIdempotent() {
        when(memberPointRecordService.getPointRecord(MEMBER_ID, BIZ_TYPE_EARN, ORDER_NO)).thenReturn(buildRecord(100));
        // 该退货单已回退 30 分（前缀汇总已包含本次）
        when(memberPointRecordService.sumAbsPointByBizIdPrefix(eq(MEMBER_ID), eq(BIZ_TYPE_REFUND), anyString()))
                .thenReturn(30);

        memberPointSettlementService.refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("100.00"), new BigDecimal("30.00"), false);

        verify(memberPointRecordService, never()).changePoints(any(), anyInt(), anyString(), anyInt(),
                anyString(), any(), anyInt(), anyBoolean());
    }

    /** 退货同时返还原单实际抵扣的积分（按比例） */
    @Test
    void testRefundSalePoints_returnsDeductedPoints() {
        when(memberPointRecordService.getPointRecord(MEMBER_ID, BIZ_TYPE_DEDUCT, ORDER_NO))
                .thenReturn(buildRecord(-50));
        when(memberPointRecordService.sumAbsPointByBizIdPrefix(eq(MEMBER_ID), eq(BIZ_TYPE_REFUND), anyString()))
                .thenReturn(0);

        memberPointSettlementService.refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("100.00"), new BigDecimal("50.00"), false);

        verify(memberPointRecordService).changePoints(eq(MEMBER_ID), eq(BIZ_TYPE_REFUND),
                eq(ORDER_NO + "#DEDUCT#" + RETURN_NO), eq(25), anyString(), anyString(), eq(1), eq(false));
    }

    /** 既无赠送也无需抵扣返还时，不产生任何积分流水 */
    @Test
    void testRefundSalePoints_noPointActivity() {
        when(memberPointRecordService.getPointRecord(anyLong(), anyInt(), anyString())).thenReturn(null);

        memberPointSettlementService.refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("100.00"), new BigDecimal("30.00"), false);

        verify(memberPointRecordService, never()).changePoints(any(), anyInt(), anyString(), anyInt(),
                anyString(), any(), anyInt(), anyBoolean());
    }

    // ========== 规则摘要 ==========

    @Test
    void testGetRuleSummary() {
        when(memberUserMapper.selectById(MEMBER_ID)).thenReturn(buildUser(1200, 20L));
        when(memberLevelMapper.selectById(20L)).thenReturn(buildLevel(20L, 2, "黄金会员"));
        memberPointProperties.setLevelMultiplier(Map.of(2, new BigDecimal("1.2")));

        var summary = memberPointSettlementService.getRuleSummary(MEMBER_ID);

        assertEquals(1200, summary.getMemberPoint());
        assertEquals(20L, summary.getLevelId());
        assertEquals(2, summary.getLevel());
        assertEquals("黄金会员", summary.getLevelName());
        assertEquals(0, new BigDecimal("1.2").compareTo(summary.getLevelMultiplier()));
        assertEquals(100, summary.getPointsPerYuan());
        assertEquals(50, summary.getMaxDeductPercent());
    }

    // ========== 测试辅助 ==========

    private MemberUserDO buildUser(Integer point, Long levelId) {
        MemberUserDO user = new MemberUserDO();
        user.setId(MEMBER_ID);
        user.setPoint(point);
        user.setLevelId(levelId);
        return user;
    }

    private MemberLevelDO buildLevel(Long id, Integer level, String name) {
        MemberLevelDO levelDO = new MemberLevelDO();
        levelDO.setId(id);
        levelDO.setLevel(level);
        levelDO.setName(name);
        return levelDO;
    }

    private MemberPointRecordDO buildRecord(Integer point) {
        MemberPointRecordDO record = new MemberPointRecordDO();
        record.setUserId(MEMBER_ID);
        record.setPoint(point);
        return record;
    }

    private void inject(String fieldName, Object value) {
        try {
            Field field = MemberPointSettlementServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(memberPointSettlementService, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("注入测试依赖失败：" + fieldName, ex);
        }
    }

}
