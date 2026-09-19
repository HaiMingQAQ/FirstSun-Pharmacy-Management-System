package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.config.MemberPointProperties;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberLevelMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberPointRecordMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 积分「余额 ↔ 流水」一致性测试（F-2 验收要求：积分余额和积分流水汇总必须一致）。
 *
 * <p>本用例使用<b>真实的</b> {@link MemberPointRecordServiceImpl} + {@link MemberPointSettlementServiceImpl}，
 * 只把 DAO 换成内存实现（一把余额 + 一份按 (业务类型,业务编码) 去重的流水表），
 * 从而可以在无数据库的情况下验证：
 * <ul>
 *   <li>销售结算（抵扣 + 赠送）后余额 = 初始余额 + 流水汇总；</li>
 *   <li>重复销售回调、重复退货、重复取消都不重复变动积分（幂等键与 uk_point_event 语义一致）；</li>
 *   <li>退货按实际退货金额比例回退，部分退货不会按整单重复回退；</li>
 *   <li>积分不足时整体失败，余额与流水都不变化（不产生孤立流水）；</li>
 *   <li>每笔流水的 total_point 与该笔之后的余额一致。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberPointFlowConsistencyTest {

    private static final Long MEMBER_ID = 1L;
    private static final String ORDER_NO = "SO-407-20260918100000-001";
    private static final String RETURN_NO = "SR-407-20260918120000-001";

    @Mock
    private MemberPointRecordMapper memberPointRecordMapper;
    @Mock
    private MemberUserMapper memberUserMapper;
    @Mock
    private MemberLevelMapper memberLevelMapper;

    /** 内存流水表：key = bizType + "|" + bizId，与 uk_point_event 的幂等语义一致 */
    private final Map<String, MemberPointRecordDO> flows = new LinkedHashMap<>();
    /** 会员初始积分（直接给定的存量余额，不属于任何一笔流水） */
    private int initialBalance;
    private int balance;

    private MemberPointRecordServiceImpl memberPointRecordService;
    private MemberPointSettlementServiceImpl memberPointSettlementService;

    @BeforeEach
    void setUp() {
        flows.clear();
        initialBalance = 0;
        balance = 0;

        memberPointRecordService = new MemberPointRecordServiceImpl();
        inject(memberPointRecordService, "memberPointRecordMapper", memberPointRecordMapper);
        inject(memberPointRecordService, "memberUserMapper", memberUserMapper);

        memberPointSettlementService = new MemberPointSettlementServiceImpl();
        inject(memberPointSettlementService, "memberPointProperties", new MemberPointProperties());
        inject(memberPointSettlementService, "memberPointRecordService", memberPointRecordService);
        inject(memberPointSettlementService, "memberUserMapper", memberUserMapper);
        inject(memberPointSettlementService, "memberLevelMapper", memberLevelMapper);

        // 会员当前余额（每次读取都反映最新值）
        when(memberUserMapper.selectById(anyLong())).thenAnswer(invocation -> {
            MemberUserDO user = new MemberUserDO();
            user.setId(MEMBER_ID);
            user.setPoint(balance);
            return user;
        });
        when(memberUserMapper.updatePointIncr(anyLong(), anyInt())).thenAnswer(invocation -> {
            balance += invocation.getArgument(1, Integer.class);
            return 1;
        });
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(anyLong(), anyInt(), anyString()))
                .thenAnswer(invocation -> flows.get(key(invocation.getArgument(1), invocation.getArgument(2))));
        when(memberPointRecordMapper.insert(any(MemberPointRecordDO.class))).thenAnswer(invocation -> {
            MemberPointRecordDO record = invocation.getArgument(0);
            flows.put(key(record.getBizType(), record.getBizId()), record);
            return 1;
        });
        when(memberPointRecordMapper.selectListByUserIdAndBizTypeAndBizIdPrefix(anyLong(), anyInt(), anyString()))
                .thenAnswer(invocation -> {
                    Integer bizType = invocation.getArgument(1);
                    String prefix = invocation.getArgument(2);
                    return flows.values().stream()
                            .filter(item -> item.getBizType() != null && item.getBizType().equals(bizType))
                            .filter(item -> item.getBizId() != null && item.getBizId().startsWith(prefix))
                            .toList();
                });
    }

    /** 销售结算：抵扣 + 赠送后，余额 = 初始余额 + 流水汇总；重复回调不再变化 */
    @Test
    void testSaleSettlement_balanceMatchesFlowSummary() {
        givenBalance(1000);

        // 用 300 分抵扣 3 元，实付 97 元 → 赠送 97 分
        int earned = memberPointSettlementService.settleSalePoints(MEMBER_ID, ORDER_NO,
                new BigDecimal("100.00"), 300);
        assertEquals(97, earned);
        assertEquals(1000 - 300 + 97, balance);
        assertEquals(2, flows.size(), "一次销售应产生「消费抵扣」与「消费获得」两条流水");
        assertBalanceMatchesFlowSummary();
        assertEquals(balance, flows.get(key(2, ORDER_NO)).getTotalPoint(), "流水 total_point 必须是变动后的余额");

        // 重复销售回调（重复支付/重复提交）：余额与流水都不再变化
        int again = memberPointSettlementService.settleSalePoints(MEMBER_ID, ORDER_NO,
                new BigDecimal("100.00"), 300);
        assertEquals(97, again);
        assertEquals(797, balance);
        assertEquals(2, flows.size());
        assertBalanceMatchesFlowSummary();
    }

    /** 退货：按实际退货金额比例回退赠送积分并返还原单抵扣积分；重复退货保持幂等 */
    @Test
    void testReturnSettlement_isProportionalAndIdempotent() {
        givenBalance(1000);
        memberPointSettlementService.settleSalePoints(MEMBER_ID, ORDER_NO, new BigDecimal("100.00"), 300);
        assertEquals(797, balance);

        // 部分退货 30%：赠送 97 → 扣回 29；抵扣 300 → 返还 90
        memberPointSettlementService.refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("100.00"), new BigDecimal("30.00"), false);
        assertEquals(797 - 29 + 90, balance);
        assertEquals(4, flows.size());

        // 重复提交同一张退货单：不得重复回退
        memberPointSettlementService.refundSalePoints(MEMBER_ID, ORDER_NO, RETURN_NO,
                new BigDecimal("100.00"), new BigDecimal("30.00"), false);
        assertEquals(858, balance);
        assertEquals(4, flows.size());
        assertBalanceMatchesFlowSummary();
    }

    /** 全额退货：剩余积分一次性结清，不会超出原单已赠送 / 已抵扣的积分 */
    @Test
    void testFullReturn_settlesRemainderWithoutOverRefund() {
        givenBalance(1000);
        memberPointSettlementService.settleSalePoints(MEMBER_ID, ORDER_NO, new BigDecimal("100.00"), 300);

        // 先部分退 30%，再整单全退剩余 70%
        memberPointSettlementService.refundSalePoints(MEMBER_ID, ORDER_NO, "SR-407-1",
                new BigDecimal("100.00"), new BigDecimal("30.00"), false);
        memberPointSettlementService.refundSalePoints(MEMBER_ID, ORDER_NO, "SR-407-2",
                new BigDecimal("100.00"), new BigDecimal("70.00"), true);

        // 赠送积分被全额扣回、抵扣积分被全额返还：净变动为 0，回到初始积分
        assertEquals(1000, balance);
        assertBalanceMatchesFlowSummary();
        // 赠送积分累计扣回不超过原赠送 97
        int clawBack = flows.values().stream()
                .filter(item -> item.getBizId() != null && item.getBizId().contains("#EARN#"))
                .mapToInt(item -> -item.getPoint())
                .sum();
        assertEquals(97, clawBack, "累计扣回的赠送积分不得超过原赠送积分");
    }

    /** 取消 / 支付失败：不赠送积分，并释放已预扣的抵扣积分；重复取消不重复返还 */
    @Test
    void testCancelReleasesPointsWithoutEarning() {
        givenBalance(500);

        // 下单预扣 200 分
        memberPointSettlementService.deductSalePoints(MEMBER_ID, ORDER_NO, 200);
        assertEquals(300, balance);

        // 支付失败 / 取消：释放预扣积分，不赠送
        memberPointSettlementService.releaseSalePoints(MEMBER_ID, ORDER_NO);
        assertEquals(500, balance);

        // 重复取消：幂等
        memberPointSettlementService.releaseSalePoints(MEMBER_ID, ORDER_NO);
        assertEquals(500, balance);

        assertBalanceMatchesFlowSummary();
        assertEquals(0, flows.values().stream().filter(item -> item.getBizType() == 2).count(),
                "取消订单不得产生「消费获得」流水");
    }

    /** 积分不足：抛业务异常且余额、流水都不变化（不留孤立流水） */
    @Test
    void testInsufficientBalance_changesNothing() {
        givenBalance(50);

        assertThrows(ServiceException.class,
                () -> memberPointSettlementService.deductSalePoints(MEMBER_ID, ORDER_NO, 100));

        assertEquals(50, balance);
        assertEquals(0, flows.size());
        assertBalanceMatchesFlowSummary();
    }

    // ========== 测试辅助 ==========

    /** 给定会员存量积分（直接设置的初始余额，不属于任何一笔流水） */
    private void givenBalance(int points) {
        initialBalance = points;
        balance = points;
    }

    /** 余额必须始终等于「初始余额 + 积分流水汇总」 */
    private void assertBalanceMatchesFlowSummary() {
        assertEquals(initialBalance + sumFlowPoint(), balance, "余额必须等于初始余额 + 积分流水汇总");
    }

    private int sumFlowPoint() {
        return flows.values().stream().mapToInt(MemberPointRecordDO::getPoint).sum();
    }

    private static String key(Integer bizType, String bizId) {
        return bizType + "|" + bizId;
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("注入测试依赖失败：" + fieldName, ex);
        }
    }

}
