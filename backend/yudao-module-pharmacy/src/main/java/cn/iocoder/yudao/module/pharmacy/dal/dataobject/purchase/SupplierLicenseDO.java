package cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.pharmacy.enums.SupplierLicenseTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 供应商证照 DO
 *
 * 对应数据表 {@code ph_supplier_license}。
 * {@link #status} 按真实业务含义定义为 1=有效 / 0=过期（与 {@code pharmacy_status} 同向），
 * 由到期日 {@link #expireDate} 与当天比较得出，保存时自动刷新。
 *
 * @author B 成员
 */
@TableName("ph_supplier_license")
@KeySequence("ph_supplier_license_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierLicenseDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 供应商编号
     *
     * 关联 {@link SupplierDO#getId()}
     */
    private Long supplierId;
    /**
     * 证照类型
     *
     * 枚举 {@link SupplierLicenseTypeEnum}
     */
    private Integer licenseType;
    /**
     * 证照号
     */
    private String licenseNo;
    /**
     * 发证日期
     */
    private LocalDate issueDate;
    /**
     * 到期日
     */
    private LocalDate expireDate;
    /**
     * 影像文件地址
     */
    private String fileUrl;
    /**
     * 证照状态：1有效 / 0过期
     */
    private Integer status;

}
