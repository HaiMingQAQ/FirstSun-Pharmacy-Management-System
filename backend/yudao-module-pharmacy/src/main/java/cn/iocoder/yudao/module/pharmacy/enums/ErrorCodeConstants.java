package cn.iocoder.yudao.module.pharmacy.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Pharmacy 药店业务模块错误码枚举类
 *
 * pharmacy 模块，使用 1-030-000-000 段（与 system 1-002、infra 1-001 等保留区错开）
 */
public interface ErrorCodeConstants {

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
