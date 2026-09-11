package cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import cn.iocoder.yudao.module.pharmacy.enums.SupplierApproveStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商 DO
 *
 * 对应数据表 {@code ph_supplier}，字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 *
 * 注意：
 * 1. {@link #status} 使用 {@link PharmacyStatusEnum}（1=启用，0=停用）；
 * 2. {@link #approveStatus} 使用 {@link SupplierApproveStatusEnum}（0待审/1通过/2驳回）；
 * 3. {@link #bankAccount} 属敏感字段，列表接口仅返回掩码，见 SupplierRespVO。
 *
 * @author B 成员
 */
@TableName("ph_supplier")
@KeySequence("ph_supplier_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 供应商编码
     *
     * 唯一，参见建表脚本 uk_supplier_code
     */
    private String supplierCode;
    /**
     * 供应商名称
     */
    private String supplierName;
    /**
     * 统一社会信用代码
     */
    private String creditCode;
    /**
     * 药品经营/生产许可证号
     */
    private String scopeCode;
    /**
     * 联系人
     */
    private String contact;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 地址
     */
    private String address;
    /**
     * 开户行
     */
    private String bankName;
    /**
     * 银行账号（列表掩码显示）
     */
    private String bankAccount;
    /**
     * 账期
     */
    private String paymentTerms;
    /**
     * 默认折扣率，取值 0.00 ~ 1.00
     */
    private BigDecimal defaultDiscount;
    /**
     * 首营审核状态
     *
     * 枚举 {@link SupplierApproveStatusEnum}
     */
    private Integer approveStatus;
    /**
     * 启用状态
     *
     * 枚举 {@link PharmacyStatusEnum}
     */
    private Integer status;
    /**
     * 审核员工编号（ph_employee.id）
     */
    private Long auditBy;
    /**
     * 审核时间
     */
    private LocalDateTime auditAt;
    /**
     * 审核意见
     */
    private String auditOpinion;

}
