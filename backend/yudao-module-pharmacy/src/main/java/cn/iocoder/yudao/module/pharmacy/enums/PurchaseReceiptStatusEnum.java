package cn.iocoder.yudao.module.pharmacy.enums;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 采购收货单状态枚举
 *
 * 对应 {@code ph_po_receipt.status}（0待提交/1已提交/2已入账/3已作废）。
 * 字典 type：{@link DictTypeConstants#PHARMACY_RECEIPT_STATUS}
 *
 * 状态流转：待提交 --提交--> 已提交 --入账（调用库存服务，同事务）--> 已入账；
 * 待提交可作废；已入账不可作废（需库存回补能力，暂不支持）。
 *
 * @author B 成员
 */
@Getter
@AllArgsConstructor
public enum PurchaseReceiptStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "待提交"),
    SUBMITTED(1, "已提交"),
    POSTED(2, "已入账"),
    VOIDED(3, "已作废");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(PurchaseReceiptStatusEnum::getStatus).toArray(Integer[]::new);

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

    public static boolean isDraft(Integer status) {
        return ObjUtil.equal(DRAFT.status, status);
    }

    public static boolean isSubmitted(Integer status) {
        return ObjUtil.equal(SUBMITTED.status, status);
    }

    public static boolean isPosted(Integer status) {
        return ObjUtil.equal(POSTED.status, status);
    }

    public static boolean isVoided(Integer status) {
        return ObjUtil.equal(VOIDED.status, status);
    }

}
