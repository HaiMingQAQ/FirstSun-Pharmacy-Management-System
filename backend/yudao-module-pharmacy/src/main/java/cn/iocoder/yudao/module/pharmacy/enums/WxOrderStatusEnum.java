package cn.iocoder.yudao.module.pharmacy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 小程序线上订单状态枚举
 *
 * 状态流转：待支付(0) → 待拣货(1) → 拣货中(2) → 待自提(3) → 完成(4)
 * 取消：待支付、待拣货状态可取消，取消后进入 -1（终态）
 */
@Getter
@AllArgsConstructor
public enum WxOrderStatusEnum {

    /** 待支付 */
    WAIT_PAY(0, "待支付"),
    /** 待拣货 */
    WAIT_PICK(1, "待拣货"),
    /** 拣货中 */
    PICKING(2, "拣货中"),
    /** 待自提 */
    WAIT_VERIFY(3, "待自提"),
    /** 完成 */
    COMPLETED(4, "完成"),
    /** 取消 */
    CANCELED(-1, "取消");

    private final Integer status;
    private final String name;

    /**
     * 校验状态值是否合法
     */
    public static boolean isValid(Integer status) {
        if (status == null) {
            return false;
        }
        for (WxOrderStatusEnum item : values()) {
            if (item.status.equals(status)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断订单是否已结束（完成或取消）
     */
    public static boolean isFinished(Integer status) {
        return COMPLETED.status.equals(status) || CANCELED.status.equals(status);
    }

}
