package cn.iocoder.yudao.module.pharmacy.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Pharmacy 药店业务模块错误码枚举类
 *
 * pharmacy 模块错误码分段：
 * 1-029-xxx-xxx：POS 销售域（D 维护）——销售订单/库存依赖/退货单/班次
 * 1-030-xxx-xxx：基础资料域（A 维护）——药品分类/门店/员工/药品档案/药品条码
 * 1-031-xxx-xxx：采购域（B 维护）——供应商/供应商证照/采购订单/收货单
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
    ErrorCode SALE_ORDER_CASHIER_REQUIRED = new ErrorCode(1_029_001_008, "收银员不能为空：柜台销售必须指定当前操作员工");

    // ========== 库存依赖 ==========
    ErrorCode INV_SERVICE_UNAVAILABLE = new ErrorCode(1_029_002_001, "库存服务未就绪，本次操作无法扣减/回补库存");
    ErrorCode INV_RETURN_BACK_FAILED = new ErrorCode(1_029_002_002, "库存回补失败");
    ErrorCode INV_STOCK_NOT_ENOUGH = new ErrorCode(1_029_002_005, "库存不足，无法完成本次出库");
    ErrorCode PAY_SERVICE_UNAVAILABLE = new ErrorCode(1_029_002_003, "支付服务未就绪，退款失败");
    ErrorCode MEMBER_SERVICE_UNAVAILABLE = new ErrorCode(1_029_002_004, "会员积分服务未就绪，积分回退失败");

    // ========== 退货单 ==========
    ErrorCode SALE_RETURN_QTY_EXCEED = new ErrorCode(1_029_003_001, "退货数量超过可退数量");
    ErrorCode SALE_RETURN_PRESC_NOT_CONFIRM = new ErrorCode(1_029_003_002, "处方药退货需药师复核");
    ErrorCode SALE_RETURN_NO_DUPLICATE = new ErrorCode(1_029_003_003, "退货单号已存在");
    ErrorCode SALE_RETURN_NOT_EXISTS = new ErrorCode(1_029_003_004, "退货单不存在");
    ErrorCode SALE_RETURN_NOT_ALLOW = new ErrorCode(1_029_003_005, "原销售单状态不允许退货");
    ErrorCode SALE_RETURN_REFUND_AMOUNT_INVALID = new ErrorCode(1_029_003_006, "退款金额计算不合法：原单无有效支付明细或退款金额超过实付金额");

    // ========== 班次 ==========
    ErrorCode SHIFT_NOT_EXISTS = new ErrorCode(1_029_004_001, "班次不存在");
    ErrorCode SHIFT_ALREADY_CLOSED = new ErrorCode(1_029_004_002, "班次已交班，不能重复交班");
    ErrorCode SHIFT_DIFF_REASON_REQUIRED = new ErrorCode(1_029_004_003, "现金长款/短款必须填写原因");
    ErrorCode SHIFT_OPENING_EXISTS = new ErrorCode(1_029_004_004, "该收银台已有进行中的班次，请先交班");
    ErrorCode SALE_ORDER_SHIFT_NOT_OPEN = new ErrorCode(1_029_004_005, "未开班：当前收银员在门店没有进行中的班次，请先开台");
    ErrorCode SALE_ORDER_SHIFT_NOT_OWNER = new ErrorCode(1_029_004_006, "该班次不属于当前收银员，不能用于本次销售");
    ErrorCode SALE_ORDER_SHIFT_CONFLICT = new ErrorCode(1_029_004_007, "当前收银员存在多个进行中班次，无法确定归属，请核对收银台号");

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

    // ========== 会员用户 1-030-006-000 ==========
    ErrorCode PHARMACY_MEMBER_USER_NOT_EXISTS = new ErrorCode(1_030_006_000, "会员用户不存在");
    ErrorCode PHARMACY_MEMBER_USER_MOBILE_DUPLICATE = new ErrorCode(1_030_006_001, "手机号已被其他会员使用");
    ErrorCode PHARMACY_MEMBER_LOGIN_BAD_CREDENTIALS = new ErrorCode(1_030_006_002, "账号或密码错误");
    ErrorCode PHARMACY_MEMBER_USER_DISABLED = new ErrorCode(1_030_006_003, "会员账号已被禁用");
    ErrorCode PHARMACY_MEMBER_USER_MOBILE_INVALID = new ErrorCode(1_030_006_004, "手机号格式不正确");
    ErrorCode PHARMACY_MEMBER_SMS_LOGIN_DISABLED = new ErrorCode(1_030_006_005, "当前环境未启用短信登录");
    ErrorCode PHARMACY_MEMBER_SMS_CODE_REQUIRED = new ErrorCode(1_030_006_006, "请输入手机验证码");
    ErrorCode PHARMACY_MEMBER_SMS_CODE_INVALID = new ErrorCode(1_030_006_007, "验证码错误，请重新输入");
    ErrorCode PHARMACY_MEMBER_SMS_CODE_EXPIRED = new ErrorCode(1_030_006_008, "验证码已过期，请重新获取");
    ErrorCode PHARMACY_MEMBER_SMS_CODE_USED = new ErrorCode(1_030_006_009, "验证码已被使用，请重新获取");
    ErrorCode PHARMACY_MEMBER_SMS_CODE_NOT_EXISTS = new ErrorCode(1_030_006_010, "请先获取该手机号的验证码");
    ErrorCode PHARMACY_MEMBER_SMS_DEV_CODE_NOT_CONFIGURED = new ErrorCode(1_030_006_011,
            "开发环境未配置测试验证码，请通过环境变量 PHARMACY_DEV_SMS_CODE 配置");
    ErrorCode PHARMACY_WECHAT_LOGIN_CONFIG_MISSING = new ErrorCode(1_030_006_012,
            "微信登录未配置可信租户或小程序应用");
    ErrorCode PHARMACY_WECHAT_LOGIN_CODE_INVALID = new ErrorCode(1_030_006_013,
            "微信登录凭证无效或已失效");
    ErrorCode PHARMACY_WECHAT_LOGIN_TYPE_INVALID = new ErrorCode(1_030_006_014,
            "微信登录类型不支持");
    ErrorCode PHARMACY_WECHAT_IDENTITY_MEMBER_MISSING = new ErrorCode(1_030_006_015,
            "微信身份绑定的会员不存在");

    // ========== 会员等级 1-030-007-000 ==========
    ErrorCode PHARMACY_MEMBER_LEVEL_NOT_EXISTS = new ErrorCode(1_030_007_000, "会员等级不存在");
    ErrorCode PHARMACY_MEMBER_LEVEL_DUPLICATE = new ErrorCode(1_030_007_001, "该等级值已存在，同一等级值只能有一个等级");
    ErrorCode PHARMACY_MEMBER_LEVEL_HAS_USER = new ErrorCode(1_030_007_002, "该等级下存在会员，无法删除");
    ErrorCode PHARMACY_MEMBER_LEVEL_NOT_ENABLE = new ErrorCode(1_030_007_003, "等级({})未启用，不允许选择");

    // ========== 会员积分记录 1-030-008-000 ==========
    ErrorCode PHARMACY_MEMBER_POINT_RECORD_NOT_EXISTS = new ErrorCode(1_030_008_000, "会员积分记录不存在");
    ErrorCode PHARMACY_MEMBER_POINT_BIZ_INVALID = new ErrorCode(1_030_008_001, "积分业务参数非法：会员编号与业务编码不能为空");
    ErrorCode PHARMACY_MEMBER_POINT_NOT_ENOUGH = new ErrorCode(1_030_008_002, "会员积分不足：当前可用积分 {}，本次需要 {}");
    ErrorCode PHARMACY_MEMBER_POINT_RETURN_EXCEED = new ErrorCode(1_030_008_003, "返还积分超过该业务单号已扣减的积分");
    ErrorCode PHARMACY_MEMBER_POINT_DEDUCT_DISABLED = new ErrorCode(1_030_008_004, "当前环境未启用积分抵扣");
    ErrorCode PHARMACY_MEMBER_POINT_DEDUCT_MEMBER_REQUIRED = new ErrorCode(1_030_008_005, "散客不能使用积分抵扣，请先选择会员");
    ErrorCode PHARMACY_MEMBER_POINT_DEDUCT_TOO_SMALL = new ErrorCode(1_030_008_006, "使用积分不能少于 {} 分");
    ErrorCode PHARMACY_MEMBER_POINT_DEDUCT_EXCEED = new ErrorCode(1_030_008_007, "使用积分超出上限：本单最多可使用 {} 分");
    ErrorCode PHARMACY_MEMBER_POINT_DEDUCT_AMOUNT_EXCEED = new ErrorCode(1_030_008_008, "抵扣金额不能大于订单应付金额：本单最多抵扣 {} 元");
    ErrorCode PHARMACY_MEMBER_POINT_EARN_DISABLED = new ErrorCode(1_030_008_009, "当前环境未启用积分赠送");
    ErrorCode PHARMACY_MEMBER_POINT_ORDER_AMOUNT_INVALID = new ErrorCode(1_030_008_010, "积分结算参数非法：订单金额不能为空或小于 0");

    // ========== 会员收件地址 1-030-009-000 ==========
    ErrorCode PHARMACY_MEMBER_ADDRESS_NOT_EXISTS = new ErrorCode(1_030_009_000, "会员收件地址不存在");
    ErrorCode PHARMACY_MEMBER_ADDRESS_NOT_OWNER = new ErrorCode(1_030_009_001, "无权操作他人收货地址");

    // ========== 小程序购物车 1-030-010-000 ==========
    ErrorCode PHARMACY_WX_CART_NOT_EXISTS = new ErrorCode(1_030_010_000, "购物车记录不存在");
    ErrorCode PHARMACY_WX_CART_ITEM_DUPLICATE = new ErrorCode(1_030_010_001, "该商品已在购物车中，同一门店同一商品只能有一条记录");
    ErrorCode PHARMACY_WX_CART_QTY_INVALID = new ErrorCode(1_030_010_002, "购物车数量必须大于 0");
    ErrorCode PHARMACY_WX_CART_SELECTED_EMPTY = new ErrorCode(1_030_010_003, "请先勾选要结算的商品");
    ErrorCode PHARMACY_WX_CART_NOT_OWNER = new ErrorCode(1_030_010_004, "无权操作他人购物车记录");

    // ========== 小程序订单 1-030-011-000 ==========
    ErrorCode PHARMACY_WX_ORDER_NOT_EXISTS = new ErrorCode(1_030_011_000, "小程序订单不存在");
    ErrorCode PHARMACY_WX_ORDER_NO_DUPLICATE = new ErrorCode(1_030_011_001, "订单号已存在");
    ErrorCode PHARMACY_WX_ORDER_STATUS_INVALID = new ErrorCode(1_030_011_002, "订单状态非法");
    ErrorCode PHARMACY_WX_ORDER_STATUS_FLOW_ERROR = new ErrorCode(1_030_011_003, "订单状态流转非法：当前状态不允许该操作");
    ErrorCode PHARMACY_WX_ORDER_ALREADY_PAID = new ErrorCode(1_030_011_004, "订单已支付，不能重复支付");
    ErrorCode PHARMACY_WX_ORDER_PICKUP_CODE_ERROR = new ErrorCode(1_030_011_005, "取货码错误");
    ErrorCode PHARMACY_WX_ORDER_ALREADY_VERIFIED = new ErrorCode(1_030_011_006, "订单已核销，不能重复核销");
    ErrorCode PHARMACY_WX_ORDER_NOT_OWNER = new ErrorCode(1_030_011_007, "无权查看他人订单");
    ErrorCode PHARMACY_WX_ORDER_TYPE_INVALID = new ErrorCode(1_030_011_008, "订单类型非法：只能为 0(到店自提) 或 1(同城配送)");
    ErrorCode PHARMACY_WX_ORDER_ADDRESS_REQUIRED = new ErrorCode(1_030_011_009, "同城配送必须选择收货地址");
    ErrorCode PHARMACY_WX_ORDER_PRESC_REQUIRED = new ErrorCode(1_030_011_010, "含处方药，必须关联已审方通过的处方");
    ErrorCode PHARMACY_WX_ORDER_STOCK_NOT_ENOUGH = new ErrorCode(1_030_011_011, "门店可用库存不足，订单无法继续");
    ErrorCode PHARMACY_WX_ORDER_STOCK_OP_FAILED = new ErrorCode(1_030_011_012, "库存作业失败：{}");
    ErrorCode PHARMACY_WX_ORDER_ALLOC_MISSING = new ErrorCode(1_030_011_013, "订单缺少出库批次/货位记录，无法精确回补库存");

    // ========== 小程序订单明细 1-030-012-000 ==========
    ErrorCode PHARMACY_WX_ORDER_LINE_NOT_EXISTS = new ErrorCode(1_030_012_000, "小程序订单明细不存在");
    // ========== 供应商 1-031-001-000（B 维护）==========
    ErrorCode PURCHASE_SUPPLIER_NOT_EXISTS = new ErrorCode(1_031_001_000, "供应商不存在");
    ErrorCode PURCHASE_SUPPLIER_CODE_DUPLICATE = new ErrorCode(1_031_001_001, "供应商编码已存在");
    ErrorCode PURCHASE_SUPPLIER_CREDIT_CODE_DUPLICATE = new ErrorCode(1_031_001_002, "统一社会信用代码已被其他供应商使用");
    ErrorCode PURCHASE_SUPPLIER_DISCOUNT_INVALID = new ErrorCode(1_031_001_003, "默认折扣率非法：取值必须在 0 到 1 之间");
    ErrorCode PURCHASE_SUPPLIER_NOT_ENABLE = new ErrorCode(1_031_001_004, "供应商({})已停用，不允许选择");
    ErrorCode PURCHASE_SUPPLIER_NOT_APPROVED = new ErrorCode(1_031_001_005, "供应商({})首营审核未通过，不允许采购");
    ErrorCode PURCHASE_SUPPLIER_HAS_ORDER = new ErrorCode(1_031_001_006, "该供应商已被采购订单引用，无法删除");
    ErrorCode PURCHASE_SUPPLIER_APPROVE_DUP = new ErrorCode(1_031_001_007, "供应商已审核，不能重复审核");
    ErrorCode PURCHASE_SUPPLIER_APPROVE_STATUS_INVALID = new ErrorCode(1_031_001_008, "审核状态非法：只能为 1(通过) 或 2(驳回)");
    ErrorCode PURCHASE_SUPPLIER_AUDITOR_NOT_EMPLOYEE = new ErrorCode(1_031_001_009, "当前登录用户未绑定药店员工，无法审核供应商");

    // ========== 供应商证照 1-031-002-000（B 维护）==========
    ErrorCode PURCHASE_LICENSE_NOT_EXISTS = new ErrorCode(1_031_002_000, "供应商证照不存在");
    ErrorCode PURCHASE_LICENSE_SUPPLIER_NOT_EXISTS = new ErrorCode(1_031_002_001, "所属供应商不存在");
    ErrorCode PURCHASE_LICENSE_NO_DUPLICATE = new ErrorCode(1_031_002_002, "该供应商下同一类型的证照号已存在");
    ErrorCode PURCHASE_LICENSE_DATE_INVALID = new ErrorCode(1_031_002_003, "证照日期非法：到期日必须不早于发证日期");
    ErrorCode PURCHASE_LICENSE_EXPIRED = new ErrorCode(1_031_002_004, "证照({})已过期，不允许用于采购收货");
    ErrorCode PURCHASE_LICENSE_OPERATE_MISSING = new ErrorCode(1_031_002_005, "供应商({})未登记有效的经营许可证或 GSP 证，不允许采购收货");

    // ========== 采购订单 1-031-003-000（B 维护）==========
    ErrorCode PURCHASE_ORDER_NOT_EXISTS = new ErrorCode(1_031_003_000, "采购订单不存在");
    ErrorCode PURCHASE_ORDER_NO_DUPLICATE = new ErrorCode(1_031_003_001, "采购订单号已存在");
    ErrorCode PURCHASE_ORDER_LINE_EMPTY = new ErrorCode(1_031_003_002, "采购订单明细不能为空");
    ErrorCode PURCHASE_ORDER_STATUS_INVALID = new ErrorCode(1_031_003_003, "采购订单状态不允许该操作");
    ErrorCode PURCHASE_ORDER_DRUG_INVALID = new ErrorCode(1_031_003_004, "采购订单中的药品不存在或不可采购");
    ErrorCode PURCHASE_ORDER_AMOUNT_INVALID = new ErrorCode(1_031_003_005, "采购订单金额计算不合法");
    ErrorCode PURCHASE_ORDER_AUDITOR_NOT_EMPLOYEE = new ErrorCode(1_031_003_006, "当前登录用户未绑定药店员工，无法审批采购订单");
    ErrorCode PURCHASE_ORDER_HAS_RECEIPT = new ErrorCode(1_031_003_007, "采购订单已存在收货记录，无法删除或取消");
    ErrorCode PURCHASE_ORDER_QTY_INVALID = new ErrorCode(1_031_003_008, "采购数量必须大于 0");
    ErrorCode PURCHASE_ORDER_DISCOUNT_INVALID = new ErrorCode(1_031_003_009, "折扣率非法：取值必须在 0 到 1 之间");
    ErrorCode PURCHASE_ORDER_APPROVE_DUP = new ErrorCode(1_031_003_010, "采购订单已审批，不能重复审批");
    ErrorCode PURCHASE_ORDER_REJECT_REASON_REQUIRED = new ErrorCode(1_031_003_011, "驳回原因不能为空");
    ErrorCode PURCHASE_ORDER_REJECT_STATUS_INVALID = new ErrorCode(1_031_003_012, "仅「已提交」状态的采购订单可以驳回");
    ErrorCode PURCHASE_ORDER_REJECT_DUP = new ErrorCode(1_031_003_013, "采购订单已驳回，不能重复驳回");

    // ========== 采购收货 1-031-004-000（B 维护）==========
    ErrorCode PURCHASE_RECEIPT_NOT_EXISTS = new ErrorCode(1_031_004_000, "采购收货单不存在");
    ErrorCode PURCHASE_RECEIPT_NO_DUPLICATE = new ErrorCode(1_031_004_001, "收货单号已存在");
    ErrorCode PURCHASE_RECEIPT_LINE_EMPTY = new ErrorCode(1_031_004_002, "收货明细不能为空");
    ErrorCode PURCHASE_RECEIPT_STATUS_INVALID = new ErrorCode(1_031_004_003, "收货单状态不允许该操作");
    ErrorCode PURCHASE_RECEIPT_QTY_EXCEED = new ErrorCode(1_031_004_004, "收货数量超过采购订单剩余可收数量");
    ErrorCode PURCHASE_RECEIPT_EXPIRY_INVALID = new ErrorCode(1_031_004_005, "有效期非法：有效期必须不早于生产日期且不能早于今天");
    ErrorCode PURCHASE_RECEIPT_BATCH_NO_REQUIRED = new ErrorCode(1_031_004_006, "批号必须填写");
    ErrorCode PURCHASE_RECEIPT_POST_DUP = new ErrorCode(1_031_004_007, "收货单已入账，不能重复入账");
    ErrorCode PURCHASE_RECEIPT_ORDER_NOT_RECEIVABLE = new ErrorCode(1_031_004_008, "采购订单未审批、已取消或已完成，不能收货");
    ErrorCode PURCHASE_RECEIPT_FREE_NOT_MANAGER = new ErrorCode(1_031_004_009, "无单收货仅限店长操作");
    ErrorCode PURCHASE_RECEIPT_ORDER_LINE_MISMATCH = new ErrorCode(1_031_004_010, "收货明细与采购订单行不匹配");
    ErrorCode PURCHASE_RECEIPT_DRUG_INVALID = new ErrorCode(1_031_004_011, "收货明细中的药品不存在");
    ErrorCode PURCHASE_RECEIPT_RECEIVER_NOT_EMPLOYEE = new ErrorCode(1_031_004_012, "当前登录用户未绑定药店员工，无法收货");
    ErrorCode PURCHASE_RECEIPT_LOCATION_REQUIRED = new ErrorCode(1_031_004_013, "入账前必须填写入库货位");
    ErrorCode PURCHASE_RECEIPT_DATE_INVALID = new ErrorCode(1_031_004_014, "收货时间非法：不能为空且必须是合理的业务日期（不能是 1970 等 Unix 起点时间）");


    // ========== 处方记录 1-032-001-000（E 维护）==========
    ErrorCode PRESC_NOT_EXISTS = new ErrorCode(1_032_001_000, "处方记录不存在");
    ErrorCode PRESC_NO_DUPLICATE = new ErrorCode(1_032_001_001, "处方号已存在");
    ErrorCode PRESC_PATIENT_REQUIRED = new ErrorCode(1_032_001_002, "患者姓名不能为空");
    ErrorCode PRESC_ITEMS_REQUIRED = new ErrorCode(1_032_001_003, "处方药品明细不能为空");
    ErrorCode PRESC_STATUS_INVALID = new ErrorCode(1_032_001_004, "处方状态不允许该操作");
    ErrorCode PRESC_SOURCE_INVALID = new ErrorCode(1_032_001_005, "处方来源非法：只能为 0(纸质拍照)/1(电子处方平台)/2(复诊续方)");
    ErrorCode PRESC_IMAGES_EXCEED = new ErrorCode(1_032_001_006, "处方影像最多上传 5 张");
    ErrorCode PRESC_NOT_OWNER = new ErrorCode(1_032_001_007, "无权查看他人处方");

    // ========== 审方 1-032-002-000（E 维护）==========
    ErrorCode PRESC_REVIEW_ALREADY = new ErrorCode(1_032_002_000, "处方已审核，不能重复审核");
    ErrorCode PRESC_REVIEW_STATUS_INVALID = new ErrorCode(1_032_002_001, "审核结果非法：只能为 1(通过) 或 2(驳回)");
    ErrorCode PRESC_REVIEW_OPINION_REQUIRED = new ErrorCode(1_032_002_002, "驳回处方必须填写审方意见");
    ErrorCode PRESC_REVIEW_AUDITOR_NOT_EMPLOYEE = new ErrorCode(1_032_002_003, "当前登录用户未绑定药店员工，无法审方");
    ErrorCode PRESC_REVIEW_DBL_REQUIRED = new ErrorCode(1_032_002_004, "特管处方必须选择双人复核人");
    ErrorCode PRESC_REVIEW_LIMIT_OPINION_REQUIRED = new ErrorCode(1_032_002_005, "超量处方必须填写超量复核意见");

    // ========== 统一支付适配 1-033-001-000（E 维护）==========
    ErrorCode PAY_APP_NOT_FOUND = new ErrorCode(1_033_001_000, "支付应用未配置，无法发起支付");
    ErrorCode PAY_ORDER_CREATE_FAIL = new ErrorCode(1_033_001_001, "支付单创建失败");
    ErrorCode PAY_REFUND_CREATE_FAIL = new ErrorCode(1_033_001_002, "退款单创建失败");
    ErrorCode PAY_STATUS_UNKNOWN = new ErrorCode(1_033_001_003, "支付单状态未知");
    ErrorCode PAY_AMOUNT_INVALID = new ErrorCode(1_033_001_004, "支付/退款金额不合法：必须大于 0（单位分）");
    ErrorCode PAY_REFUND_AMOUNT_EXCEED = new ErrorCode(1_033_001_005, "退款金额超过原支付金额，已拒绝");
    ErrorCode PAY_REFUND_ORDER_NOT_FOUND = new ErrorCode(1_033_001_006, "原支付单不存在，无法退款");

}
