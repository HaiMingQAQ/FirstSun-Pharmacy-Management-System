package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address.MemberAddressPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address.MemberAddressSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberAddressDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 会员收件地址 Service
 */
public interface MemberAddressService {

    /**
     * 创建会员收件地址
     */
    Long createMemberAddress(@Valid MemberAddressSaveReqVO createReqVO);

    /**
     * 更新会员收件地址
     */
    void updateMemberAddress(@Valid MemberAddressSaveReqVO updateReqVO);

    /**
     * 删除会员收件地址
     */
    void deleteMemberAddress(Long id);

    /**
     * 获取会员收件地址详情
     */
    MemberAddressDO getMemberAddress(Long id);

    /**
     * 批量获得会员收件地址列表
     *
     * @param ids 地址编号集合
     * @return 地址列表（仅返回未删除的）
     */
    List<MemberAddressDO> getMemberAddressList(Collection<Long> ids);

    /**
     * 获取会员收件地址分页
     */
    PageResult<MemberAddressDO> getMemberAddressPage(MemberAddressPageReqVO reqVO);

    /**
     * 根据用户编号获取收件地址列表
     */
    List<MemberAddressDO> getMemberAddressListByUserId(Long userId);

    /**
     * 获取用户的默认收件地址
     */
    MemberAddressDO getDefaultMemberAddress(Long userId);

    /**
     * 校验收件地址存在
     */
    MemberAddressDO validateMemberAddressExists(Long id);

}
