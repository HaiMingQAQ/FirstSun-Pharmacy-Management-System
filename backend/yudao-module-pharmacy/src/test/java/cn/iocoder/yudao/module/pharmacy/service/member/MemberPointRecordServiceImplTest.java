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
 * {@link MemberPointRecordServiceImpl} 单元测试：积分幂等、扣减下限、入参校验与流水记录。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberPointRecordServiceImplTest {

    @Mock
    private MemberPointRecordMapper memberPointRecordMapper;
    @Mock
    private MemberUserMapper memberUserMapper;

    @InjectMocks
    private MemberPointRecordServiceImpl memberPointRecordService;

    /** 正常加分：更新积分 + 写入流水，流水含变动后积分与业务类型（消费奖励=2） */
    @Test
    void testAddPoints_success() {
        when(memberPointRecordMapper.selectByUserIdAndBizId(1L, "S001")).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(100), buildUser(110));

        memberPointRecordService.addPoints(1L, "S001", 10, "消费奖励");

        verify(memberUserMapper).updatePointIncr(1L, 10);
        MemberPointRecordDO record = captureRecord();
        assertEquals(10, record.getPoint());
        assertEquals(110, record.getTotalPoint());
        assertEquals(2, record.getBizType());
        assertEquals("S001", record.getBizId());
        assertEquals("消费奖励", record.getTitle());
    }

    /** 幂等：同一会员 + 同一业务编码重复调用，不重复加分、不重复写流水 */
    @Test
    void testAddPoints_idempotent() {
        when(memberPointRecordMapper.selectByUserIdAndBizId(1L, "S001"))
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
        when(memberPointRecordMapper.selectByUserIdAndBizId(anyLong(), anyString())).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(null);

        assertThrows(ServiceException.class,
                () -> memberPointRecordService.addPoints(1L, "S001", 10, "消费奖励"));
        verify(memberPointRecordMapper, never()).insert(any(MemberPointRecordDO.class));
    }

    /** 退货扣回：积分充足时按原值扣减（业务类型=退款回退 3） */
    @Test
    void testBackPoints_success() {
        when(memberPointRecordMapper.selectByUserIdAndBizId(1L, "S001")).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(100), buildUser(90));

        memberPointRecordService.backPoints(1L, "S001", 10);

        verify(memberUserMapper).updatePointIncr(1L, -10);
        MemberPointRecordDO record = captureRecord();
        assertEquals(-10, record.getPoint());
        assertEquals(90, record.getTotalPoint());
        assertEquals(3, record.getBizType());
    }

    /** 退货扣回：积分不足时只扣到 0，不出现负积分 */
    @Test
    void testBackPoints_clampToZero() {
        when(memberPointRecordMapper.selectByUserIdAndBizId(1L, "S001")).thenReturn(null);
        when(memberUserMapper.selectById(1L)).thenReturn(buildUser(3), buildUser(0));

        memberPointRecordService.backPoints(1L, "S001", 10);

        verify(memberUserMapper).updatePointIncr(1L, -3);
        MemberPointRecordDO record = captureRecord();
        assertEquals(-3, record.getPoint());
        assertEquals(0, record.getTotalPoint());
    }

    // ========== 测试辅助 ==========

    private MemberUserDO buildUser(Integer point) {
        MemberUserDO user = new MemberUserDO();
        user.setId(1L);
        user.setPoint(point);
        return user;
    }

    private MemberPointRecordDO captureRecord() {
        ArgumentCaptor<MemberPointRecordDO> captor = ArgumentCaptor.forClass(MemberPointRecordDO.class);
        verify(memberPointRecordMapper).insert(captor.capture());
        return captor.getValue();
    }

}
