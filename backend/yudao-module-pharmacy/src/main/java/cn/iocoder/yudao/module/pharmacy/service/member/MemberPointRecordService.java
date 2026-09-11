package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 会员积分记录 Service
 */
public interface MemberPointRecordService {

    /**
     * 创建会员积分记录
     */
    Long createMemberPointRecord(@Valid MemberPointRecordSaveReqVO createReqVO);

    /**
     * 更新会员积分记录
     */
    void updateMemberPointRecord(@Valid MemberPointRecordSaveReqVO updateReqVO);

    /**
     * 删除会员积分记录
     */
    void deleteMemberPointRecord(Long id);

    /**
     * 获取会员积分记录详情
     */
    MemberPointRecordDO getMemberPointRecord(Long id);

    /**
     * 批量获得会员积分记录列表
     *
     * @param ids 记录编号集合
     * @return 记录列表（仅返回未删除的）
     */
    List<MemberPointRecordDO> getMemberPointRecordList(Collection<Long> ids);

    /**
     * 获取会员积分记录分页
     */
    PageResult<MemberPointRecordDO> getMemberPointRecordPage(MemberPointRecordPageReqVO reqVO);

    /**
     * 根据用户编号获取积分记录列表
     */
    List<MemberPointRecordDO> getMemberPointRecordListByUserId(Long userId);

    /**
     * 校验积分记录存在
     */
    MemberPointRecordDO validateMemberPointRecordExists(Long id);

}
