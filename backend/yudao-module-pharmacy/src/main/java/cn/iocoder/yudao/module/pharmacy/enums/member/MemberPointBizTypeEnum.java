package cn.iocoder.yudao.module.pharmacy.enums.member;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 会员积分业务类型枚举。
 *
 * <p>取值与数据字典 {@code pharmacy_member_point_biz_type} 完全一致，
 * 同时与 {@code member_point_record.uk_point_event(tenant_id,user_id,biz_type,biz_id)}
 * 组成积分幂等键：同一会员 + 同一业务类型 + 同一业务编码只允许产生一条流水。
 */
@Getter
@AllArgsConstructor
public enum MemberPointBizTypeEnum {

    REGISTER(1, "注册赠送"),
    CONSUME_EARN(2, "消费获得"),
    CONSUME_DEDUCT(3, "消费抵扣"),
    ADMIN_ADJUST(4, "管理员调整"),
    SIGN(5, "每日签到"),
    REFUND(6, "退款冲回"),
    ;

    private final Integer bizType;
    private final String name;

    public static MemberPointBizTypeEnum of(Integer bizType) {
        return Arrays.stream(values())
                .filter(item -> Objects.equals(item.bizType, bizType))
                .findFirst()
                .orElse(null);
    }

}
