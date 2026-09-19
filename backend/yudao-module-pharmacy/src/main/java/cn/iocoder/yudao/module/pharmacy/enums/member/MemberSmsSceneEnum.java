package cn.iocoder.yudao.module.pharmacy.enums.member;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会员手机验证码使用场景枚举
 *
 * 验证码与场景绑定，避免「A 场景的验证码」被用于「B 场景」。
 */
@Getter
@AllArgsConstructor
public enum MemberSmsSceneEnum {

    /**
     * 会员手机号快捷登录
     */
    LOGIN(1, "会员手机号快捷登录");

    private final Integer scene;
    private final String name;

}
