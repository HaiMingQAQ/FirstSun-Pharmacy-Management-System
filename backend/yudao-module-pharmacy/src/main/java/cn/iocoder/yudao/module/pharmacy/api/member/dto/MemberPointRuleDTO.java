package cn.iocoder.yudao.module.pharmacy.api.member.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员积分规则摘要（F 会员积分服务 → 小程序积分中心 / 收银台的展示口径）。
 *
 * <p>用于让前端展示「当前余额、当前等级倍率、多少积分抵 1 元、单笔最高抵扣比例」，
 * 前端据此展示与选择使用积分，最终可用值仍以后端试算 / 结算为准。
 */
@Data
public class MemberPointRuleDTO {

    /** 会员编号 */
    private Long memberId;

    /** 当前可用积分 */
    private Integer memberPoint;

    /** 会员等级编号 */
    private Long levelId;

    /** 会员等级值 */
    private Integer level;

    /** 会员等级名称 */
    private String levelName;

    /** 该会员实际生效的赠送倍率 */
    private BigDecimal levelMultiplier;

    /** 是否启用积分赠送 */
    private Boolean earnEnabled;

    /** 每消费 1 元获得的基础积分 */
    private BigDecimal earnPerYuan;

    /** 是否启用积分抵扣 */
    private Boolean deductEnabled;

    /** 多少积分抵扣 1 元 */
    private Integer pointsPerYuan;

    /** 单笔订单最高抵扣比例（百分比） */
    private Integer maxDeductPercent;

    /** 单笔最少使用的抵扣积分 */
    private Integer minDeductPoints;

    /** 单笔最多使用的抵扣积分（0 表示不额外限制） */
    private Integer maxDeductPoints;

}
