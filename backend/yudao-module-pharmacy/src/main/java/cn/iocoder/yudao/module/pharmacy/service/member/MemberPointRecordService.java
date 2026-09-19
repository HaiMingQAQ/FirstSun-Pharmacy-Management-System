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

    /**
     * 积分抵扣扣减（订单使用积分抵扣现金）
     *
     * 幂等：同一会员 + 业务类型(消费抵扣) + 同一业务单号只处理一次，重复调用（重复支付回调等）不会重复扣减。
     * 积分不足时抛出业务异常 {@code PHARMACY_MEMBER_POINT_NOT_ENOUGH}，调用方事务整体回滚。
     *
     * @param userId 会员用户编号
     * @param bizId  业务单号（如订单号）
     * @param point  抵扣的积分（<=0 视为无需处理）
     * @param title  积分标题
     */
    void deductPoints(Long userId, String bizId, Integer point, String title);

    /**
     * 返还积分（订单取消 / 退货，退还此前抵扣的积分）
     *
     * 幂等：同一会员 + 业务类型(退款冲回) + 同一业务单号只处理一次，保证「只返还一次」；
     * 返还上限为该业务单号已抵扣的积分，超出部分按上限返还，不会超返。
     *
     * @param userId 会员用户编号
     * @param bizId  业务单号（与原抵扣同一单号，如订单号）
     * @param point  返还的积分（<=0 视为无需处理）
     * @param title  积分标题
     */
    void returnPoints(Long userId, String bizId, Integer point, String title);

    // ========== 积分变动统一入口（F 内部积分结算服务调用） ==========

    /**
     * 积分变动统一入口：由业务方指定业务类型与业务编码，保证「一业务一流水、一流水一幂等键」。
     *
     * <p>幂等键与 {@code member_point_record.uk_point_event(tenant_id,user_id,biz_type,biz_id)}
     * 完全一致：同一会员 + 同一业务类型 + 同一业务编码只处理一次，重复调用直接返回 0，
     * 从而保证「重复销售回调不重复赠送」「重复核销不重复赠送」「重复退货不重复回退」
     * 「重复取消不重复返还」。
     *
     * @param userId           会员用户编号
     * @param bizType          业务类型（与字典 pharmacy_member_point_biz_type 一致）
     * @param bizId            业务编码（幂等键）
     * @param point            积分绝对值（<=0 视为无需处理）
     * @param title            积分标题
     * @param description      积分描述（可空）
     * @param sign             +1 增加 / -1 扣减
     * @param rejectInsufficient 扣减时积分不足是否抛业务异常（抛异常则调用方事务整体回滚）
     * @return 实际变动的积分绝对值（0 表示幂等跳过或无需处理）
     */
    int changePoints(Long userId, Integer bizType, String bizId, Integer point, String title,
                     String description, int sign, boolean rejectInsufficient);

    /**
     * 查询某业务单号产生的积分流水（用于退货按比例回退、幂等校验）
     */
    MemberPointRecordDO getPointRecord(Long userId, Integer bizType, String bizId);

    /**
     * 汇总「业务编码前缀」下已产生的积分绝对值合计
     *
     * <p>用于同一原单多次部分退货的累计回退计算，避免超出原单已赠送 / 已抵扣的积分。
     */
    int sumAbsPointByBizIdPrefix(Long userId, Integer bizType, String bizIdPrefix);

}
