package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level.MemberLevelPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level.MemberLevelSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberLevelDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberLevelMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 会员等级 Service 实现类
 */
@Service
@Validated
public class MemberLevelServiceImpl implements MemberLevelService {

    /**
     * 启用状态：0=启用（框架标准）
     */
    private static final Integer STATUS_ENABLE = 0;

    @Resource
    private MemberLevelMapper memberLevelMapper;

    @Resource
    private MemberUserMapper memberUserMapper;

    @Override
    public Long createMemberLevel(MemberLevelSaveReqVO createReqVO) {
        // 校验等级值唯一
        validateLevelUnique(null, createReqVO.getLevel());
        // 写入
        MemberLevelDO memberLevel = BeanUtils.toBean(createReqVO, MemberLevelDO.class);
        memberLevelMapper.insert(memberLevel);
        return memberLevel.getId();
    }

    @Override
    public void updateMemberLevel(MemberLevelSaveReqVO updateReqVO) {
        // 校验存在
        validateMemberLevelExists(updateReqVO.getId());
        // 校验等级值唯一
        validateLevelUnique(updateReqVO.getId(), updateReqVO.getLevel());
        // 更新
        MemberLevelDO updateObj = BeanUtils.toBean(updateReqVO, MemberLevelDO.class);
        memberLevelMapper.updateById(updateObj);
    }

    @Override
    public void deleteMemberLevel(Long id) {
        // 校验存在
        validateMemberLevelExists(id);
        // 校验会员引用：存在会员使用该等级时不可删除
        Long userCount = memberLevelMapper.countUsersByLevelId(id);
        if (userCount != null && userCount > 0) {
            throw exception(PHARMACY_MEMBER_LEVEL_HAS_USER);
        }
        memberLevelMapper.deleteById(id);
    }

    @Override
    public MemberLevelDO getMemberLevel(Long id) {
        return memberLevelMapper.selectById(id);
    }

    @Override
    public List<MemberLevelDO> getMemberLevelList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return memberLevelMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<MemberLevelDO> getMemberLevelPage(MemberLevelPageReqVO reqVO) {
        return memberLevelMapper.selectPage(reqVO);
    }

    @Override
    public List<MemberLevelDO> getEnabledMemberLevelList() {
        return memberLevelMapper.selectListByStatus(STATUS_ENABLE);
    }

    @Override
    public MemberLevelDO validateMemberLevelExistsAndEnabled(Long id) {
        MemberLevelDO memberLevel = memberLevelMapper.selectById(id);
        if (memberLevel == null) {
            throw exception(PHARMACY_MEMBER_LEVEL_NOT_EXISTS);
        }
        if (!STATUS_ENABLE.equals(memberLevel.getStatus())) {
            throw exception(PHARMACY_MEMBER_LEVEL_NOT_ENABLE, memberLevel.getName());
        }
        return memberLevel;
    }

    private void validateMemberLevelExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_MEMBER_LEVEL_NOT_EXISTS);
        }
        if (memberLevelMapper.selectById(id) == null) {
            throw exception(PHARMACY_MEMBER_LEVEL_NOT_EXISTS);
        }
    }

    private void validateLevelUnique(Long id, Integer level) {
        MemberLevelDO memberLevel = memberLevelMapper.selectByLevel(level);
        if (memberLevel == null) {
            return;
        }
        if (id == null) {
            throw exception(PHARMACY_MEMBER_LEVEL_DUPLICATE);
        }
        if (!Objects.equals(memberLevel.getId(), id)) {
            throw exception(PHARMACY_MEMBER_LEVEL_DUPLICATE);
        }
    }

}
