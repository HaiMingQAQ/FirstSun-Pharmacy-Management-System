package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberPointRecordMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 会员积分记录 Service 实现类
 */
@Service
@Validated
public class MemberPointRecordServiceImpl implements MemberPointRecordService {

    /** 积分业务类型：消费获得（与字典 pharmacy_member_point_biz_type 对应） */
    private static final Integer BIZ_TYPE_CONSUME_EARN = 2;
    /** 积分业务类型：消费抵扣（订单使用积分抵扣现金） */
    private static final Integer BIZ_TYPE_CONSUME_DEDUCT = 3;
    /** 积分业务类型：退款冲回（退货扣回奖励积分 / 取消返还抵扣积分） */
    private static final Integer BIZ_TYPE_REFUND = 6;

    @Resource
    private MemberPointRecordMapper memberPointRecordMapper;

    @Resource
    private MemberUserMapper memberUserMapper;

    @Override
    public Long createMemberPointRecord(MemberPointRecordSaveReqVO createReqVO) {
        // 写入
        MemberPointRecordDO memberPointRecord = BeanUtils.toBean(createReqVO, MemberPointRecordDO.class);
        memberPointRecordMapper.insert(memberPointRecord);
        return memberPointRecord.getId();
    }

    @Override
    public void updateMemberPointRecord(MemberPointRecordSaveReqVO updateReqVO) {
        // 校验存在
        validateMemberPointRecordExists(updateReqVO.getId());
        // 更新
        MemberPointRecordDO updateObj = BeanUtils.toBean(updateReqVO, MemberPointRecordDO.class);
        memberPointRecordMapper.updateById(updateObj);
    }

    @Override
    public void deleteMemberPointRecord(Long id) {
        // 校验存在
        validateMemberPointRecordExists(id);
        // 删除
        memberPointRecordMapper.deleteById(id);
    }

    @Override
    public MemberPointRecordDO getMemberPointRecord(Long id) {
        return memberPointRecordMapper.selectById(id);
    }

    @Override
    public List<MemberPointRecordDO> getMemberPointRecordList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return memberPointRecordMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<MemberPointRecordDO> getMemberPointRecordPage(MemberPointRecordPageReqVO reqVO) {
        return memberPointRecordMapper.selectPage(reqVO);
    }

    @Override
    public List<MemberPointRecordDO> getMemberPointRecordListByUserId(Long userId) {
        return memberPointRecordMapper.selectListByUserId(userId);
    }

    @Override
    public MemberPointRecordDO validateMemberPointRecordExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_MEMBER_POINT_RECORD_NOT_EXISTS);
        }
        MemberPointRecordDO memberPointRecord = memberPointRecordMapper.selectById(id);
        if (memberPointRecord == null) {
            throw exception(PHARMACY_MEMBER_POINT_RECORD_NOT_EXISTS);
        }
        return memberPointRecord;
    }

    // ========== 跨模块积分变动（销售奖励 / 退货扣回 / 积分抵扣 / 取消返还） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, String bizId, Integer point, String title) {
        changePoints(userId, bizId, point, title, BIZ_TYPE_CONSUME_EARN, 1, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void backPoints(Long userId, String bizId, Integer point) {
        changePoints(userId, bizId, point, "退货积分回退", BIZ_TYPE_REFUND, -1, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long userId, String bizId, Integer point, String title) {
        changePoints(userId, bizId, point, title, BIZ_TYPE_CONSUME_DEDUCT, -1, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnPoints(Long userId, String bizId, Integer point, String title) {
        if (point == null || point <= 0) {
            return;
        }
        // 入参校验
        if (userId == null || bizId == null || bizId.trim().isEmpty()) {
            throw exception(PHARMACY_MEMBER_POINT_BIZ_INVALID);
        }
        // 返还上限 = 该业务单号已抵扣的积分；没有抵扣记录说明本次无需返还
        MemberPointRecordDO deducted = memberPointRecordMapper
                .selectByUserIdAndBizTypeAndBizId(userId, BIZ_TYPE_CONSUME_DEDUCT, bizId);
        if (deducted == null || deducted.getPoint() == null || deducted.getPoint() >= 0) {
            return;
        }
        int amount = Math.min(point, Math.abs(deducted.getPoint()));
        // 幂等键为 (会员, 退款冲回, 业务单号)，同一单号重复返还只会成功一次
        changePoints(userId, bizId, amount, title, BIZ_TYPE_REFUND, 1, false);
    }

    /**
     * 积分变动统一入口
     *
     * 1. 幂等：同一会员 + 同一业务类型 + 同一业务编码只处理一次（与 uk_point_event 一致）
     * 2. 原子增减：通过 UPDATE ... SET point = point + delta 避免并发覆盖
     * 3. 记录变动后积分，保证流水与会员积分一致
     * 4. 扣减类：{@code rejectInsufficient=true} 时积分不足直接抛业务异常，使调用方事务回滚
     *
     * @param sign               +1=增加，-1=扣减
     * @param rejectInsufficient 扣减时积分不足是否抛业务异常
     */
    private void changePoints(Long userId, String bizId, Integer point, String title,
                              Integer bizType, int sign, boolean rejectInsufficient) {
        // 积分为 0 视为无需处理（如销售未产生奖励积分）
        if (point == null || point <= 0) {
            return;
        }
        // 入参校验
        if (userId == null || bizId == null || bizId.trim().isEmpty()) {
            throw exception(PHARMACY_MEMBER_POINT_BIZ_INVALID);
        }
        // 幂等校验：同一业务类型 + 同一业务编码已处理过，直接返回，避免重复积分
        if (memberPointRecordMapper.selectByUserIdAndBizTypeAndBizId(userId, bizType, bizId) != null) {
            return;
        }
        // 会员必须存在
        MemberUserDO user = memberUserMapper.selectById(userId);
        if (user == null) {
            throw exception(PHARMACY_MEMBER_USER_NOT_EXISTS);
        }
        int current = user.getPoint() == null ? 0 : user.getPoint();
        int delta = sign * point;
        if (delta < 0 && current + delta < 0) {
            if (rejectInsufficient) {
                // 积分不足：抛业务异常，保证「订单成功但积分未扣减」不会出现
                throw exception(PHARMACY_MEMBER_POINT_NOT_ENOUGH, current, point);
            }
            // 扣回类操作不允许把积分扣成负数
            delta = -current;
        }
        if (delta == 0) {
            return;
        }
        memberUserMapper.updatePointIncr(userId, delta);
        // 读取变动后的积分，保证流水 totalPoint 与会员积分一致
        MemberUserDO after = memberUserMapper.selectById(userId);
        MemberPointRecordDO record = new MemberPointRecordDO();
        record.setUserId(userId);
        record.setBizId(bizId);
        record.setBizType(bizType);
        record.setTitle(title);
        record.setPoint(delta);
        record.setTotalPoint(after == null || after.getPoint() == null ? 0 : after.getPoint());
        memberPointRecordMapper.insert(record);
    }

}
