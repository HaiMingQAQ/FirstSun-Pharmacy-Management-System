package cn.iocoder.yudao.module.pharmacy.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Pharmacy 错误码枚举类
 *
 * pharmacy 模块，使用 1-029-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 销售订单 1-029-001-000 ==========
    ErrorCode SALE_ORDER_STATUS_INVALID = new ErrorCode(1_029_001_000, "销售单状态不允许该操作");
    ErrorCode SALE_ORDER_GOODS_EMPTY = new ErrorCode(1_029_001_001, "销售商品不能为空");
    ErrorCode SALE_ORDER_PAYMENT_EMPTY = new ErrorCode(1_029_001_002, "销售支付信息不能为空");
    ErrorCode SALE_ORDER_NOT_EXISTS = new ErrorCode(1_029_001_003, "销售单不存在");
    ErrorCode SALE_ORDER_PAYMENT_DUPLICATE = new ErrorCode(1_029_001_004, "支付幂等号已存在，禁止重复提交");
    ErrorCode SALE_ORDER_NO_DUPLICATE = new ErrorCode(1_029_001_005, "销售单号已存在");
    ErrorCode SALE_ORDER_AMOUNT_INVALID = new ErrorCode(1_029_001_006, "销售金额计算不合法");
    ErrorCode SALE_ORDER_RX_PRESC_REQUIRED = new ErrorCode(1_029_001_007, "处方药必须关联已审方通过的处方");

    // ========== 库存依赖 ==========
    ErrorCode INV_SERVICE_UNAVAILABLE = new ErrorCode(1_029_002_001, "库存服务未就绪，本次操作无法扣减/回补库存");
    ErrorCode INV_RETURN_BACK_FAILED = new ErrorCode(1_029_002_002, "库存回补失败");

    // ========== 退货单 ==========
    ErrorCode SALE_RETURN_QTY_EXCEED = new ErrorCode(1_029_003_001, "退货数量超过可退数量");
    ErrorCode SALE_RETURN_PRESC_NOT_CONFIRM = new ErrorCode(1_029_003_002, "处方药退货需药师复核");
    ErrorCode SALE_RETURN_NO_DUPLICATE = new ErrorCode(1_029_003_003, "退货单号已存在");
    ErrorCode SALE_RETURN_NOT_EXISTS = new ErrorCode(1_029_003_004, "退货单不存在");
    ErrorCode SALE_RETURN_NOT_ALLOW = new ErrorCode(1_029_003_005, "原销售单状态不允许退货");

    // ========== 班次 ==========
    ErrorCode SHIFT_NOT_EXISTS = new ErrorCode(1_029_004_001, "班次不存在");
    ErrorCode SHIFT_ALREADY_CLOSED = new ErrorCode(1_029_004_002, "班次已交班，不能重复交班");
    ErrorCode SHIFT_DIFF_REASON_REQUIRED = new ErrorCode(1_029_004_003, "现金长款/短款必须填写原因");
}
