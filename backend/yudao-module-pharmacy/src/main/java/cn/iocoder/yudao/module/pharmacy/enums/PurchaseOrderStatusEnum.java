package cn.iocoder.yudao.module.pharmacy.enums;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 采购订单状态枚举
 *
 * 对应 {@code ph_po_order.status}（-1取消/0草稿/1提交/2审批/3发出/4部分到货/5完成）。
 * 字典 type：{@link DictTypeConstants#PHARMACY_PO_STATUS}
 *
 * 状态流转：
 * 草稿 --提交--> 提交 --审批--> 审批 --发出--> 发出 --部分收货--> 部分到货 --收完--> 完成
 * 草稿/提交/审批 可取消（存在已提交收货单时禁止取消）
 *
 * @author B 成员
 */
@Getter
@AllArgsConstructor
public enum PurchaseOrderStatusEnum implements ArrayValuable<Integer> {

    CANCEL(-1, "已取消"),
    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    APPROVED(2, "已审批"),
    ISSUED(3, "已发出"),
    PARTIAL_RECEIVED(4, "部分到货"),
    FINISHED(5, "已完成");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(PurchaseOrderStatusEnum::getStatus).toArray(Integer[]::new);

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

    public static boolean isApproved(Integer status) {
        return ObjUtil.equal(APPROVED.status, status);
    }

    public static boolean isIssued(Integer status) {
        return ObjUtil.equal(ISSUED.status, status);
    }

    public static boolean isPartialReceived(Integer status) {
        return ObjUtil.equal(PARTIAL_RECEIVED.status, status);
    }

    public static boolean isFinished(Integer status) {
        return ObjUtil.equal(FINISHED.status, status);
    }

    public static boolean isCancel(Integer status) {
        return ObjUtil.equal(CANCEL.status, status);
    }

    /**
     * 是否允许收货：审批通过、已发出或部分到货
     */
    public static boolean canReceive(Integer status) {
        return isApproved(status) || isIssued(status) || isPartialReceived(status);
    }

    /**
     * 是否允许取消：草稿、已提交、已审批（发出后不可直接取消）
     */
    public static boolean canCancel(Integer status) {
        return isDraft(status) || isSubmitted(status) || isApproved(status);
    }

}
