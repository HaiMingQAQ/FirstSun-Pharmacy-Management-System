package cn.iocoder.yudao.module.pharmacy.enums;

/**
 * Pharmacy 药店业务模块的字典类型常量
 *
 * 与字典表 system_dict_type.type 对应，前端 utils/dict.ts 中保持同名枚举。
 */
public interface DictTypeConstants {

    /**
     * 药店通用启停状态（1=启用，0=停用）
     */
    String PHARMACY_STATUS = "pharmacy_status";

    /**
     * 药品分类类型（0药品/1保健品/2医疗器械/3中药饮片/4日化/5其他）
     */
    String PHARMACY_CATEGORY_TYPE = "pharmacy_category_type";

    /**
     * 通用是否类型（0否/1是）
     */
    String PHARMACY_YES_NO = "pharmacy_yes_no";

    /**
     * 员工在职状态（1在职/0离职/2休假）
     */
    String PHARMACY_EMPLOYEE_STATUS = "pharmacy_employee_status";

    /**
     * 员工岗位（1店长/2药师/3收银员/4库管员/5采购/6财务/9系统管理员）
     */
    String PHARMACY_EMPLOYEE_POSITION = "pharmacy_employee_position";

    /**
     * 药品类型（0处方/1OTC甲/2OTC乙/3特管/4饮片/5保健/6器械/7日化/8其他）
     */
    String PHARMACY_DRUG_TYPE = "pharmacy_drug_type";

    /**
     * 医保类别（0自费/1甲类/2乙类）
     */
    String PHARMACY_INSURANCE_TYPE = "pharmacy_insurance_type";

    /**
     * 储存条件（0常温/1阴凉/2冷藏/3冷冻）
     */
    String PHARMACY_STORAGE_COND = "pharmacy_storage_cond";

    /**
     * 药品审核状态（0待审/1通过/2驳回）
     */
    String PHARMACY_DRUG_APPROVE_STATUS = "pharmacy_drug_approve_status";

    /**
     * 条码类型（0商品条码/1店内码/2追溯码）
     */
    String PHARMACY_BARCODE_TYPE = "pharmacy_barcode_type";

    // ==================== 采购域（B 维护） ====================

    /**
     * 供应商首营审核状态（0待审/1通过/2驳回）
     */
    String PHARMACY_SUPPLIER_APPROVE_STATUS = "pharmacy_supplier_approve_status";

    /**
     * 供应商证照类型（0经营许可证/1生产许可证/2GSP证/3营业执照/4其他）
     */
    String PHARMACY_LICENSE_TYPE = "pharmacy_license_type";

    /**
     * 供应商证照状态（1有效/0过期）
     */
    String PHARMACY_LICENSE_STATUS = "pharmacy_license_status";

    /**
     * 采购订单状态（0草稿/1提交/2审批/3发出/4部分到货/5完成/-1取消）
     */
    String PHARMACY_PO_STATUS = "pharmacy_po_status";

    /**
     * 采购收货单状态（0待提交/1已提交/2已入账/3已作废）
     */
    String PHARMACY_RECEIPT_STATUS = "pharmacy_receipt_status";

    /**
     * 收货差异标记（0无/1数量差异/2价格差异）
     */
    String PHARMACY_RECEIPT_DIFF_TYPE = "pharmacy_receipt_diff_type";

    /**
     * 收货单质检结果（0未检/1合格/2有异常）
     */
    String PHARMACY_QUALITY_STATUS = "pharmacy_quality_status";

    /**
     * 收货明细质检标记（0待检/1通过/2异常拒收）
     */
    String PHARMACY_QUALITY_FLAG = "pharmacy_quality_flag";

}
