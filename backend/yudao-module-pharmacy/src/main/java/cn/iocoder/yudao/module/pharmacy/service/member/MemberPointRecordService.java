package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;

import jakarta.validation.Valid;
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

    // ========== 跨模块积分变动（销售奖励 / 退货回退） ==========

    /**
     * 增加积分（销售奖励等）
     *
     * 幂等：同一会员 + 同一业务编码只处理一次，重复调用直接返回，不会重复加分。
     *
     * @param userId 会员用户编号
     * @param bizId  业务编码（如销售单号）
     * @param point  增加的积分（<=0 视为无需处理）
     * @param title  积分标题
     */
    void addPoints(Long userId, String bizId, Integer point, String title);

    /**
     * 扣回积分（退货等）
     *
     * 幂等：同一会员 + 同一业务编码只处理一次。扣减不会使积分变为负数。
     *
     * @param userId 会员用户编号
     * @param bizId  业务编码（如原销售单号）
     * @param point  扣回的积分（<=0 视为无需处理）
     */
    void backPoints(Long userId, String bizId, Integer point);

}
