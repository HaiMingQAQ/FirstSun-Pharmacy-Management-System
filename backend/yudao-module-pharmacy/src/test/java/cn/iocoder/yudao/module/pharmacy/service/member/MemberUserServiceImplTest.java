package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MemberUserServiceImpl} 单元测试：会员新增必填列（register_ip）、手机号唯一校验。
 *
 * <p>背景（ACC-20260915-008 / P1）：member_user.register_ip 为 NOT NULL 且无数据库默认值，
 * 管理端新增会员未写入该列时数据库直接抛 "Field 'register_ip' doesn't have a default value"，
 * 接口返回 500。这里锁定修复后的行为，避免回归。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberUserServiceImplTest {

    @Mock
    private MemberUserMapper memberUserMapper;

    @InjectMocks
    private MemberUserServiceImpl memberUserService;

    /** 管理端新增会员：必须显式写入非空 register_ip，长度不超过列上限 32 */
    @Test
    void testCreateMemberUser_setsRegisterIp() {
        when(memberUserMapper.selectByMobile("13800138000")).thenReturn(null);

        memberUserService.createMemberUser(buildCreateReqVO("13800138000"));

        MemberUserDO inserted = captureInsert();
        assertNotNull(inserted.getRegisterIp(), "register_ip 不能为空，否则数据库报 500");
        assertFalse(inserted.getRegisterIp().isBlank(), "register_ip 不能为空白字符串");
        assertTrue(inserted.getRegisterIp().length() <= 32, "register_ip 不能超过 varchar(32)");
        assertEquals("13800138000", inserted.getMobile());
        assertEquals("测试会员", inserted.getNickname());
        assertEquals(0, inserted.getStatus());
    }

    /** 手机号重复：抛业务异常（提示重复），不写库，避免前端看到 500 */
    @Test
    void testCreateMemberUser_duplicateMobile() {
        MemberUserDO exist = new MemberUserDO();
        exist.setId(9L);
        exist.setMobile("13800138000");
        when(memberUserMapper.selectByMobile("13800138000")).thenReturn(exist);

        assertThrows(ServiceException.class,
                () -> memberUserService.createMemberUser(buildCreateReqVO("13800138000")));
        verify(memberUserMapper, never()).insert(any(MemberUserDO.class));
    }

    /** 小程序首次登录自动注册：register_ip 取调用方传入的 IP */
    @Test
    void testCreateMemberUserIfAbsent_setsRegisterIpFromCaller() {
        when(memberUserMapper.selectByMobile("13900139000")).thenReturn(null);

        MemberUserDO created = memberUserService.createMemberUserIfAbsent("13900139000", "10.1.2.3");

        assertEquals("10.1.2.3", created.getRegisterIp());
        verify(memberUserMapper).insert(any(MemberUserDO.class));
    }

    /** 小程序自动注册：调用方未传 IP 时回落到非空兜底值，仍满足 NOT NULL 约束 */
    @Test
    void testCreateMemberUserIfAbsent_fallbackRegisterIp() {
        when(memberUserMapper.selectByMobile("13900139001")).thenReturn(null);

        MemberUserDO created = memberUserService.createMemberUserIfAbsent("13900139001", null);

        assertNotNull(created.getRegisterIp());
        assertFalse(created.getRegisterIp().isBlank());
        assertTrue(created.getRegisterIp().length() <= 32);
    }

    // ========== 测试辅助 ==========

    private MemberUserSaveReqVO buildCreateReqVO(String mobile) {
        MemberUserSaveReqVO reqVO = new MemberUserSaveReqVO();
        reqVO.setMobile(mobile);
        reqVO.setNickname("测试会员");
        reqVO.setStatus(0);
        return reqVO;
    }

    private MemberUserDO captureInsert() {
        ArgumentCaptor<MemberUserDO> captor = ArgumentCaptor.forClass(MemberUserDO.class);
        verify(memberUserMapper).insert(captor.capture());
        return captor.getValue();
    }

}
