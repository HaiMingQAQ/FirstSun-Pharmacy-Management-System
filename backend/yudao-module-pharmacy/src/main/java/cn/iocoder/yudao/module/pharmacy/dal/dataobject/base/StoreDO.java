package cn.iocoder.yudao.module.pharmacy.dal.dataobject.base;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 门店 DO
 *
 * 对应数据表 {@code ph_store}，字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 *
 * 唯一约束：
 * 1. {@code uk_store_code(store_code)} - 门店编码全局唯一
 * 2. {@code uk_store_dept(tenant_id, dept_id)} - 同一租户下 dept_id 唯一
 *
 * 注意：{@link #status} 取值与 yudao {@code CommonStatusEnum} 方向相反，
 * 使用 {@link PharmacyStatusEnum}（1=营业，0=停业）。
 */
@TableName("ph_store")
@KeySequence("ph_store_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class StoreDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 门店编码（唯一）
     */
    private String storeCode;
    /**
     * 门店名称
     */
    private String storeName;
    /**
     * 地址
     */
    private String address;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 经营范围
     */
    private String manageScope;
    /**
     * 药品经营许可证号
     */
    private String licenseNo;
    /**
     * 证照到期日
     */
    private LocalDate licenseExpire;
    /**
     * 是否医保定点 0否/1是
     */
    private Integer isMedical;
    /**
     * 营业时间描述
     */
    private String businessHours;
    /**
     * 营业状态 1营业/0停业
     *
     * 枚举 {@link PharmacyStatusEnum}
     */
    private Integer status;
    /**
     * 关联框架 system_dept.id
     *
     * 同一租户下唯一，允许为空
     */
    private Long deptId;

}
