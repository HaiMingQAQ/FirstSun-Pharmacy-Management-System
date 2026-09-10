package cn.iocoder.yudao.module.pharmacy.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Pharmacy 药店业务模块错误码枚举类
 *
 * pharmacy 模块错误码分段：
 * 1-029-xxx-xxx：POS 销售域（D 维护）——销售订单/库存依赖/退货单/班次
 * 1-030-xxx-xxx：基础资料域（A 维护）——药品分类/门店/员工/药品档案/药品条码
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
    ErrorCode PAY_SERVICE_UNAVAILABLE = new ErrorCode(1_029_002_003, "支付服务未就绪，退款失败");
    ErrorCode MEMBER_SERVICE_UNAVAILABLE = new ErrorCode(1_029_002_004, "会员积分服务未就绪，积分回退失败");

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
    ErrorCode SHIFT_OPENING_EXISTS = new ErrorCode(1_029_004_004, "该收银台已有进行中的班次，请先交班");

    // ========== 药品分类 1-030-001-000 ==========
    ErrorCode PHARMACY_CATEGORY_NOT_EXISTS = new ErrorCode(1_030_001_000, "药品分类不存在");
    ErrorCode PHARMACY_CATEGORY_CODE_DUPLICATE = new ErrorCode(1_030_001_001, "药品分类编码已存在");
    ErrorCode PHARMACY_CATEGORY_PARENT_NOT_EXISTS = new ErrorCode(1_030_001_002, "上级分类不存在");
    ErrorCode PHARMACY_CATEGORY_PARENT_ERROR = new ErrorCode(1_030_001_003, "不能设置自己为上级分类");
    ErrorCode PHARMACY_CATEGORY_PARENT_IS_CHILD = new ErrorCode(1_030_001_004, "不能设置自己的子分类为上级分类");
    ErrorCode PHARMACY_CATEGORY_EXISTS_CHILDREN = new ErrorCode(1_030_001_005, "存在子分类，无法删除");
    ErrorCode PHARMACY_CATEGORY_HAS_DRUG = new ErrorCode(1_030_001_006, "该分类下存在药品，无法删除");
    ErrorCode PHARMACY_CATEGORY_NOT_ENABLE = new ErrorCode(1_030_001_007, "分类({})未启用，不允许选择");

    // ========== 门店 1-030-002-000 ==========
    ErrorCode PHARMACY_STORE_NOT_EXISTS = new ErrorCode(1_030_002_000, "门店不存在");
    ErrorCode PHARMACY_STORE_CODE_DUPLICATE = new ErrorCode(1_030_002_001, "门店编码已存在");
    ErrorCode PHARMACY_STORE_DEPT_DUPLICATE = new ErrorCode(1_030_002_002, "该部门已被其他门店关联，同一部门只能关联一个门店");
    ErrorCode PHARMACY_STORE_DEPT_NOT_EXISTS = new ErrorCode(1_030_002_003, "关联的部门不存在");
    ErrorCode PHARMACY_STORE_NOT_OPEN = new ErrorCode(1_030_002_004, "门店({})已停业，不允许执行该操作");

    // ========== 员工 1-030-003-000 ==========
    ErrorCode PHARMACY_EMPLOYEE_NOT_EXISTS = new ErrorCode(1_030_003_000, "员工不存在");
    ErrorCode PHARMACY_EMPLOYEE_NO_DUPLICATE = new ErrorCode(1_030_003_001, "工号已存在");
    ErrorCode PHARMACY_EMPLOYEE_STORE_NOT_EXISTS = new ErrorCode(1_030_003_002, "所属门店不存在");
    ErrorCode PHARMACY_EMPLOYEE_STORE_NOT_OPEN = new ErrorCode(1_030_003_003, "所属门店({})已停业，不能新增在职员工");
    ErrorCode PHARMACY_EMPLOYEE_USER_NOT_EXISTS = new ErrorCode(1_030_003_004, "关联的系统用户不存在");
    ErrorCode PHARMACY_EMPLOYEE_USER_DISABLED = new ErrorCode(1_030_003_005, "关联的系统用户({})已被禁用");
    ErrorCode PHARMACY_EMPLOYEE_USER_DUPLICATE = new ErrorCode(1_030_003_006, "该系统用户已被其他员工关联，同一用户只能关联一个员工");
    ErrorCode PHARMACY_EMPLOYEE_HAS_USER = new ErrorCode(1_030_003_007, "员工已关联系统用户，不能重复关联");
    ErrorCode PHARMACY_STORE_HAS_EMPLOYEE = new ErrorCode(1_030_003_008, "门店下存在员工，无法删除");

    // ========== 药品档案 1-030-004-000 ==========
    ErrorCode PHARMACY_DRUG_NOT_EXISTS = new ErrorCode(1_030_004_000, "药品不存在");
    ErrorCode PHARMACY_DRUG_CODE_DUPLICATE = new ErrorCode(1_030_004_001, "药品编码已存在");
    ErrorCode PHARMACY_DRUG_CATEGORY_NOT_EXISTS = new ErrorCode(1_030_004_002, "所属分类不存在或已停用");
    ErrorCode PHARMACY_DRUG_APPROVAL_NO_DUPLICATE = new ErrorCode(1_030_004_003, "批准文号已被其他药品使用");
    ErrorCode PHARMACY_DRUG_PRICE_INVALID = new ErrorCode(1_030_004_004, "价格非法：零售价≥0、会员价/成本价/最低限售价≥0、且会员价≤零售价、最低限售价≤零售价");
    ErrorCode PHARMACY_DRUG_STOCK_INVALID = new ErrorCode(1_030_004_005, "库存上下限非法：上限为 0 或 ≥ 下限");
    ErrorCode PHARMACY_DRUG_RX_INVALID = new ErrorCode(1_030_004_006, "处方药标识非法：药品类型为 0(处方药) 时 is_rx 必须为 1");
    ErrorCode PHARMACY_DRUG_HAS_BARCODE = new ErrorCode(1_030_004_007, "药品下存在条码，无法删除");
    ErrorCode PHARMACY_DRUG_APPROVE_DUP = new ErrorCode(1_030_004_008, "药品已审核，不能重复审核");
    ErrorCode PHARMACY_DRUG_APPROVE_STATUS_INVALID = new ErrorCode(1_030_004_009, "审核状态非法：只能为 1(通过) 或 2(驳回)");
    ErrorCode PHARMACY_DRUG_NOT_SALEABLE = new ErrorCode(1_030_004_010, "药品不可销售：已停用或未审核通过");
    ErrorCode PHARMACY_DRUG_AUDITOR_NOT_EMPLOYEE = new ErrorCode(1_030_004_011, "当前登录用户未绑定药店员工，无法审核药品");

    // ========== 药品条码 1-030-005-000 ==========
    ErrorCode PHARMACY_BARCODE_NOT_EXISTS = new ErrorCode(1_030_005_000, "条码不存在");
    ErrorCode PHARMACY_BARCODE_DUPLICATE = new ErrorCode(1_030_005_001, "条码已存在");
    ErrorCode PHARMACY_BARCODE_DRUG_NOT_EXISTS = new ErrorCode(1_030_005_002, "关联的药品不存在");
    ErrorCode PHARMACY_BARCODE_DEFAULT_DUPLICATE = new ErrorCode(1_030_005_003, "该药品已存在默认条码，同一药品只能有一个默认条码");

}
