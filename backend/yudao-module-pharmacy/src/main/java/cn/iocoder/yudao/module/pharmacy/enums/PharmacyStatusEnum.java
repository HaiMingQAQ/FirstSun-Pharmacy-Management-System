package cn.iocoder.yudao.module.pharmacy.enums;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 药店通用启停状态枚举
 *
 * 注意：与 yudao {@code CommonStatusEnum}（0=开启/1=关闭）方向相反。
 * 本模块数据表 ph_category / ph_store / ph_drug / ph_drug_barcode 等
 * 均按真实建表脚本使用 1=启用、0=停用，故独立定义，不复用 CommonStatusEnum。
 */
@Getter
@AllArgsConstructor
public enum PharmacyStatusEnum implements ArrayValuable<Integer> {

    DISABLE(0, "停用"),
    ENABLE(1, "启用");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(PharmacyStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isEnable(Integer status) {
        return ObjUtil.equal(ENABLE.status, status);
    }

    public static boolean isDisable(Integer status) {
        return ObjUtil.equal(DISABLE.status, status);
    }

}
