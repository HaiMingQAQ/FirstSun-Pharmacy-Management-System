package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.point;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 APP - 会员积分中心 Response VO（余额 + 当前生效的积分规则）")
@Data
public class AppMemberPointSummaryRespVO {

    @Schema(description = "当前可用积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "1200")
    private Integer memberPoint;

    @Schema(description = "会员等级编号", example = "2")
    private Long levelId;

    @Schema(description = "会员等级值", example = "2")
    private Integer level;

    @Schema(description = "会员等级名称", example = "黄金会员")
    private String levelName;

    @Schema(description = "该会员实际生效的赠送倍率", example = "1.2")
    private BigDecimal levelMultiplier;

    @Schema(description = "是否启用积分赠送", example = "true")
    private Boolean earnEnabled;

    @Schema(description = "每消费 1 元获得的基础积分", example = "1")
    private BigDecimal earnPerYuan;

    @Schema(description = "是否启用积分抵扣", example = "true")
    private Boolean deductEnabled;

    @Schema(description = "多少积分抵扣 1 元", example = "100")
    private Integer pointsPerYuan;

    @Schema(description = "单笔订单最高抵扣比例（百分比）", example = "50")
    private Integer maxDeductPercent;

    @Schema(description = "单笔最少使用的抵扣积分", example = "1")
    private Integer minDeductPoints;

    @Schema(description = "单笔最多使用的抵扣积分（0 表示不额外限制）", example = "0")
    private Integer maxDeductPoints;

}
