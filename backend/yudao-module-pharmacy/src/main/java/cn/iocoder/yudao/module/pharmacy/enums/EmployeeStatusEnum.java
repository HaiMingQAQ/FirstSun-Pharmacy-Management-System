package cn.iocoder.yudao.module.pharmacy.enums;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 员工在职状态枚举
 *
 * 对应数据表 ph_employee.status，三态：1在职/0离职/2休假。
 * 与通用启停状态 {@link PharmacyStatusEnum} 不同，单独定义。
 */
@Getter
@AllArgsConstructor
public enum EmployeeStatusEnum implements ArrayValuable<Integer> {

    RESIGNED(0, "离职"),
    ACTIVE(1, "在职"),
    LEAVE(2, "休假");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EmployeeStatusEnum::getStatus).toArray(Integer[]::new);

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

    public static boolean isActive(Integer status) {
        return ObjUtil.equal(ACTIVE.status, status);
    }

}
