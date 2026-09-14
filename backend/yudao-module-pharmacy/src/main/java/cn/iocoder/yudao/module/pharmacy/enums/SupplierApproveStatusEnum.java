package cn.iocoder.yudao.module.pharmacy.enums;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 供应商首营审核状态枚举
 *
 * 对应 {@code ph_supplier.approve_status}（0待审/1通过/2驳回）。
 * 字典 type：{@link DictTypeConstants#PHARMACY_SUPPLIER_APPROVE_STATUS}
 *
 * 与 {@link PharmacyStatusEnum}（启停）相互独立：审核通过且启用才允许被采购订单选择。
 */
@Getter
@AllArgsConstructor
public enum SupplierApproveStatusEnum implements ArrayValuable<Integer> {

    WAIT(0, "待审"),
    PASS(1, "通过"),
    REJECT(2, "驳回");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(SupplierApproveStatusEnum::getStatus).toArray(Integer[]::new);

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

    public static boolean isWait(Integer status) {
        return ObjUtil.equal(WAIT.status, status);
    }

    public static boolean isPass(Integer status) {
        return ObjUtil.equal(PASS.status, status);
    }

    /**
     * 审核入参是否合法：只接受 1(通过) 与 2(驳回)
     */
    public static boolean isValidAuditStatus(Integer status) {
        return isPass(status) || ObjUtil.equal(REJECT.status, status);
    }

}
