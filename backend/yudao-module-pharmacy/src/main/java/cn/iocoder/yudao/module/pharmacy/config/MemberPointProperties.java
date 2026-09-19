package cn.iocoder.yudao.module.pharmacy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会员积分规则配置（F 会员模块）。
 *
 * <p>积分赠送与抵扣全部由后端按本配置 + 会员等级计算，前端传入的积分值一律不采信。
 * 配置项通过 {@code yudao.pharmacy.member-point.*} 覆盖，默认值即课程演示环境的可用规则：
 *
 * <ul>
 *   <li>{@code earn-per-yuan}：每消费 1 元获得的基础积分；</li>
 *   <li>{@code level-multiplier}：按 {@code member_level.level} 配置等级倍率，未配置的等级取 1.0；</li>
 *   <li>{@code points-per-yuan}：多少积分抵扣 1 元；</li>
 *   <li>{@code max-deduct-percent}：单笔订单最高抵扣比例（占应付金额的百分比）。</li>
 * </ul>
 *
 * <p>本类不读取环境变量中的任何密钥，规则本身不是敏感信息，可随代码提交。
 */
@Component
@ConfigurationProperties(prefix = "yudao.pharmacy.member-point")
@Data
public class MemberPointProperties {

    /** 是否启用积分赠送 */
    private boolean earnEnabled = true;

    /** 是否启用积分抵扣 */
    private boolean deductEnabled = true;

    /** 每消费 1 元获得的基础积分 */
    private BigDecimal earnPerYuan = BigDecimal.ONE;

    /**
     * 等级倍率：key = member_level.level（等级），value = 赠送倍率。
     *
     * 等级为空或未配置时按 1.0 计算。
     */
    private Map<Integer, BigDecimal> levelMultiplier = new LinkedHashMap<>();

    /** 多少积分抵扣 1 元 */
    private Integer pointsPerYuan = 100;

    /** 单笔订单最高抵扣比例（占应付金额的百分比，0-100） */
    private Integer maxDeductPercent = 50;

    /** 单笔订单最少使用的抵扣积分（低于该值不允许抵扣） */
    private Integer minDeductPoints = 1;

    /** 单笔订单最多使用的抵扣积分（0 表示不额外限制） */
    private Integer maxDeductPoints = 0;

}
