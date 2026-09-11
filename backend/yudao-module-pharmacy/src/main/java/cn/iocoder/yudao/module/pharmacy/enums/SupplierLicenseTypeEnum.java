package cn.iocoder.yudao.module.pharmacy.enums;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 供应商证照类型枚举
 *
 * 对应 {@code ph_supplier_license.license_type}（0经营许可证/1生产许可证/2GSP证/3营业执照/4其他）。
 * 字典 type：{@link DictTypeConstants#PHARMACY_LICENSE_TYPE}
 */
@Getter
@AllArgsConstructor
public enum SupplierLicenseTypeEnum implements ArrayValuable<Integer> {

    BUSINESS_LICENSE(0, "经营许可证"),
    PRODUCE_LICENSE(1, "生产许可证"),
    GSP_LICENSE(2, "GSP证"),
    LICENSE(3, "营业执照"),
    OTHER(4, "其他");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(SupplierLicenseTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    /**
     * 是否属于「经营类」资质：经营许可证、GSP 证用于校验能否经营药品
     */
    public static boolean isOperateLicense(Integer type) {
        return ObjUtil.equal(BUSINESS_LICENSE.type, type) || ObjUtil.equal(GSP_LICENSE.type, type);
    }

}
