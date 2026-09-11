package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberPointRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
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

    @Resource
    private MemberPointRecordMapper memberPointRecordMapper;

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

}
