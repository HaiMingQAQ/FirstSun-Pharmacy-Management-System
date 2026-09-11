package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level.MemberLevelPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level.MemberLevelSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberLevelDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 会员等级 Service
 */
public interface MemberLevelService {

    /**
     * 创建会员等级
     */
    Long createMemberLevel(@Valid MemberLevelSaveReqVO createReqVO);

    /**
     * 更新会员等级
     */
    void updateMemberLevel(@Valid MemberLevelSaveReqVO updateReqVO);

    /**
     * 删除会员等级
     *
     * 删除前校验：存在会员使用该等级时拒绝删除。
     */
    void deleteMemberLevel(Long id);

    /**
     * 获取会员等级详情
     */
    MemberLevelDO getMemberLevel(Long id);

    /**
     * 批量获得会员等级列表
     *
     * @param ids 等级编号集合
     * @return 等级列表（仅返回未删除的）
     */
    List<MemberLevelDO> getMemberLevelList(Collection<Long> ids);

    /**
     * 获取会员等级分页
     */
    PageResult<MemberLevelDO> getMemberLevelPage(MemberLevelPageReqVO reqVO);

    /**
     * 获取启用状态的等级列表
     */
    List<MemberLevelDO> getEnabledMemberLevelList();

    /**
     * 校验等级存在且启用
     */
    MemberLevelDO validateMemberLevelExistsAndEnabled(Long id);

}
