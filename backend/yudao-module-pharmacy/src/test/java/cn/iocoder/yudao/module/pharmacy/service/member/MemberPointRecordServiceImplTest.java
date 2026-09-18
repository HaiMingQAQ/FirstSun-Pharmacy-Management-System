package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberPointRecordMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MemberPointRecordServiceImpl} 单元测试。
 *
 * 覆盖：积分获得、扣回、抵扣扣减、返还的幂等与边界，以及「积分不足」必须抛业务异常。
 *
 * 业务类型取值与字典 pharmacy_member_point_biz_type 一致：
 * 2=消费获得、3=消费抵扣、6=退款冲回（退货扣回 / 取消返还）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberPointRecordServiceImplTest {

    private static final Integer BIZ_TYPE_EARN = 2;
    private static final Integer BIZ_TYPE_DEDUCT = 3;
    private static final Integer BIZ_TYPE_REFUND = 6;

    @Mock
    private MemberPointRecordMapper memberPointRecordMapper;
    @Mock
    private MemberUserMapper memberUserMapper;

    @InjectMocks
    private MemberPointRecordServiceImpl memberPointRecordService;

    // ========== 消费获得 ==========

    /** 正常加分：更新积分 + 写入流水，流水含变动后积分与业务类型（消费获得=2） */
    @Test
    void testAddPoints_success() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_EARN, "S001")).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(100), buildUser(110));

        memberPointRecordService.addPoints(1L, "S001", 10, "消费奖励");

        verify(memberUserMapper).updatePointIncr(1L, 10);
        MemberPointRecordDO record = captureRecord();
        assertEquals(10, record.getPoint());
        assertEquals(110, record.getTotalPoint());
        assertEquals(BIZ_TYPE_EARN, record.getBizType());
        assertEquals("S001", record.getBizId());
        assertEquals("消费奖励", record.getTitle());
    }

    /** 幂等：同一会员 + 同一业务编码重复调用，不重复加分、不重复写流水 */
    @Test
    void testAddPoints_idempotent() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_EARN, "S001"))
                .thenReturn(new MemberPointRecordDO());

        memberPointRecordService.addPoints(1L, "S001", 10, "消费奖励");

        verify(memberUserMapper, never()).updatePointIncr(anyLong(), anyInt());
        verify(memberPointRecordMapper, never()).insert(any(MemberPointRecordDO.class));
    }

    /** 积分为 0 视为无需处理（销售未产生奖励积分） */
    @Test
    void testAddPoints_zeroPoint() {
        memberPointRecordService.addPoints(1L, "S001", 0, "无奖励");

        verify(memberUserMapper, never()).updatePointIncr(anyLong(), anyInt());
        verify(memberPointRecordMapper, never()).insert(any(MemberPointRecordDO.class));
    }

    /** 入参校验：会员编号与业务编码不能为空 */
    @Test
    void testAddPoints_invalidBizId() {
        assertThrows(ServiceException.class,
                () -> memberPointRecordService.addPoints(1L, "  ", 10, "消费奖励"));
    }

    /** 会员不存在时拒绝加分，避免写出脏流水 */
    @Test
    void testAddPoints_userNotExists() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(anyLong(), anyInt(), anyString())).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(null);

        assertThrows(ServiceException.class,
                () -> memberPointRecordService.addPoints(1L, "S001", 10, "消费奖励"));
        verify(memberPointRecordMapper, never()).insert(any(MemberPointRecordDO.class));
    }

    // ========== 退货扣回（退款冲回） ==========

    /** 退货扣回：积分充足时按原值扣减（业务类型=6 退款冲回） */
    @Test
    void testBackPoints_success() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_REFUND, "S001")).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(100), buildUser(90));

        memberPointRecordService.backPoints(1L, "S001", 10);

        verify(memberUserMapper).updatePointIncr(1L, -10);
        MemberPointRecordDO record = captureRecord();
        assertEquals(-10, record.getPoint());
        assertEquals(90, record.getTotalPoint());
        assertEquals(BIZ_TYPE_REFUND, record.getBizType());
    }

    /** 退货扣回：积分不足时只扣到 0，不出现负积分 */
    @Test
    void testBackPoints_clampToZero() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_REFUND, "S001")).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(3), buildUser(0));

        memberPointRecordService.backPoints(1L, "S001", 10);

        verify(memberUserMapper).updatePointIncr(1L, -3);
        MemberPointRecordDO record = captureRecord();
        assertEquals(-3, record.getPoint());
        assertEquals(0, record.getTotalPoint());
    }

    // ========== 积分抵扣扣减 ==========

    /** 抵扣成功：扣减积分并写入业务类型=3 的流水，流水带业务单号 */
    @Test
    void testDeductPoints_success() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_DEDUCT, "WX-407-20260916-0001"))
                .thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(100), buildUser(70));

        memberPointRecordService.deductPoints(1L, "WX-407-20260916-0001", 30, "订单积分抵扣");

        verify(memberUserMapper).updatePointIncr(1L, -30);
        MemberPointRecordDO record = captureRecord();
        assertEquals(-30, record.getPoint());
        assertEquals(70, record.getTotalPoint());
        assertEquals(BIZ_TYPE_DEDUCT, record.getBizType());
        assertEquals("WX-407-20260916-0001", record.getBizId());
    }

    /** 积分不足：抛业务异常，不扣积分、不写流水，保证调用方事务整体回滚 */
    @Test
    void testDeductPoints_notEnough() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_DEDUCT, "S001")).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(10));

        assertThrows(ServiceException.class,
                () -> memberPointRecordService.deductPoints(1L, "S001", 30, "订单积分抵扣"));

        verify(memberUserMapper, never()).updatePointIncr(anyLong(), anyInt());
        verify(memberPointRecordMapper, never()).insert(any(MemberPointRecordDO.class));
    }

    /** 重复抵扣（重复支付回调）：同一业务单号只扣一次 */
    @Test
    void testDeductPoints_idempotent() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_DEDUCT, "S001"))
                .thenReturn(new MemberPointRecordDO());

        memberPointRecordService.deductPoints(1L, "S001", 30, "订单积分抵扣");

        verify(memberUserMapper, never()).updatePointIncr(anyLong(), anyInt());
        verify(memberPointRecordMapper, never()).insert(any(MemberPointRecordDO.class));
    }

    // ========== 取消 / 退货返还 ==========

    /** 返还成功：按已抵扣积分返还，业务类型=6 */
    @Test
    void testReturnPoints_success() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_DEDUCT, "S001"))
                .thenReturn(buildRecord(-30));
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_REFUND, "S001")).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(70), buildUser(100));

        memberPointRecordService.returnPoints(1L, "S001", 30, "订单取消返还");

        verify(memberUserMapper).updatePointIncr(1L, 30);
        MemberPointRecordDO record = captureRecord();
        assertEquals(30, record.getPoint());
        assertEquals(100, record.getTotalPoint());
        assertEquals(BIZ_TYPE_REFUND, record.getBizType());
    }

    /** 只返还一次：同一业务单号已有返还流水时不再重复返还 */
    @Test
    void testReturnPoints_onlyOnce() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_DEDUCT, "S001"))
                .thenReturn(buildRecord(-30));
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_REFUND, "S001"))
                .thenReturn(buildRecord(30));

        memberPointRecordService.returnPoints(1L, "S001", 30, "订单取消返还");

        verify(memberUserMapper, never()).updatePointIncr(anyLong(), anyInt());
        verify(memberPointRecordMapper, never()).insert(any(MemberPointRecordDO.class));
    }

    /** 不超返：请求返还大于已抵扣积分时按已抵扣积分返还 */
    @Test
    void testReturnPoints_clampToDeducted() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_DEDUCT, "S001"))
                .thenReturn(buildRecord(-30));
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_REFUND, "S001")).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(70), buildUser(100));

        memberPointRecordService.returnPoints(1L, "S001", 100, "订单取消返还");

        verify(memberUserMapper).updatePointIncr(1L, 30);
        assertEquals(30, captureRecord().getPoint());
    }

    /** 没有抵扣记录时无需返还，不产生任何流水 */
    @Test
    void testReturnPoints_withoutDeduct() {
        when(memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(1L, BIZ_TYPE_DEDUCT, "S001")).thenReturn(null);

        memberPointRecordService.returnPoints(1L, "S001", 30, "订单取消返还");

        verify(memberUserMapper, never()).updatePointIncr(anyLong(), anyInt());
        verify(memberPointRecordMapper, never()).insert(any(MemberPointRecordDO.class));
    }

    // ========== 测试辅助 ==========

    private MemberUserDO buildUser(Integer point) {
        MemberUserDO user = new MemberUserDO();
        user.setId(1L);
        user.setPoint(point);
        return user;
    }

    private MemberPointRecordDO buildRecord(Integer point) {
        MemberPointRecordDO record = new MemberPointRecordDO();
        record.setUserId(1L);
        record.setPoint(point);
        return record;
    }

    private MemberPointRecordDO captureRecord() {
        ArgumentCaptor<MemberPointRecordDO> captor = ArgumentCaptor.forClass(MemberPointRecordDO.class);
        verify(memberPointRecordMapper).insert(captor.capture());
        return captor.getValue();
    }

}
