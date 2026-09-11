package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address.MemberAddressPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address.MemberAddressSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberAddressDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberAddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 会员收件地址 Service 实现类
 */
@Service
@Validated
public class MemberAddressServiceImpl implements MemberAddressService {

    @Resource
    private MemberAddressMapper memberAddressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMemberAddress(MemberAddressSaveReqVO createReqVO) {
        // 如果设为默认，先取消该用户原有默认地址
        if (Boolean.TRUE.equals(createReqVO.getDefaultStatus())) {
            cancelDefaultAddress(createReqVO.getUserId());
        }
        // 写入
        MemberAddressDO memberAddress = BeanUtils.toBean(createReqVO, MemberAddressDO.class);
        memberAddressMapper.insert(memberAddress);
        return memberAddress.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberAddress(MemberAddressSaveReqVO updateReqVO) {
        // 校验存在
        validateMemberAddressExists(updateReqVO.getId());
        // 如果设为默认，先取消该用户原有默认地址
        if (Boolean.TRUE.equals(updateReqVO.getDefaultStatus())) {
            cancelDefaultAddress(updateReqVO.getUserId());
        }
        // 更新
        MemberAddressDO updateObj = BeanUtils.toBean(updateReqVO, MemberAddressDO.class);
        memberAddressMapper.updateById(updateObj);
    }

    @Override
    public void deleteMemberAddress(Long id) {
        // 校验存在
        validateMemberAddressExists(id);
        // 删除
        memberAddressMapper.deleteById(id);
    }

    @Override
    public MemberAddressDO getMemberAddress(Long id) {
        return memberAddressMapper.selectById(id);
    }

    @Override
    public List<MemberAddressDO> getMemberAddressList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return memberAddressMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<MemberAddressDO> getMemberAddressPage(MemberAddressPageReqVO reqVO) {
        return memberAddressMapper.selectPage(reqVO);
    }

    @Override
    public List<MemberAddressDO> getMemberAddressListByUserId(Long userId) {
        return memberAddressMapper.selectListByUserId(userId);
    }

    @Override
    public MemberAddressDO getDefaultMemberAddress(Long userId) {
        return memberAddressMapper.selectDefaultByUserId(userId);
    }

    @Override
    public MemberAddressDO validateMemberAddressExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_MEMBER_ADDRESS_NOT_EXISTS);
        }
        MemberAddressDO memberAddress = memberAddressMapper.selectById(id);
        if (memberAddress == null) {
            throw exception(PHARMACY_MEMBER_ADDRESS_NOT_EXISTS);
        }
        return memberAddress;
    }

    /**
     * 取消用户的默认地址
     */
    private void cancelDefaultAddress(Long userId) {
        MemberAddressDO defaultAddress = memberAddressMapper.selectDefaultByUserId(userId);
        if (defaultAddress != null) {
            defaultAddress.setDefaultStatus(false);
            memberAddressMapper.updateById(defaultAddress);
        }
    }

}
