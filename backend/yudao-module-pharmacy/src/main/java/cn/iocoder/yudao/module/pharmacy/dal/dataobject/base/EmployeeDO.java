package cn.iocoder.yudao.module.pharmacy.dal.dataobject.base;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.pharmacy.enums.EmployeeStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工 DO
 *
 * 对应数据表 {@code ph_employee}，字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 *
 * 唯一约束：
 * 1. {@code uk_emp_no(emp_no)} - 工号全局唯一
 * 2. {@code uk_employee_user(tenant_id,user_id)} - 同一租户下 user_id 唯一
 *
 * 关联：
 * - {@link #storeId} → ph_store.id
 * - {@link #userId} → system_users.id（通过 AdminUserApi 只读校验，不修改 system）
 *
 * 注意：{@link #phone} 在 DB 中为 VARCHAR(255) 加密密文，应用层按密文存储。
 * {@link #status} 三态，枚举 {@link EmployeeStatusEnum}（1在职/0离职/2休假）。
 */
@TableName("ph_employee")
@KeySequence("ph_employee_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 工号（唯一）
     */
    private String empNo;
    /**
     * 姓名
     */
    private String empName;
    /**
     * 加密手机号密文
     *
     * DB 字段 phone VARCHAR(255)，应用层生成密文后存入
     */
    private String phone;
    /**
     * 所属门店
     */
    private Long storeId;
    /**
     * 岗位 1店长/2药师/3收银员/4库管员/5采购/6财务/9系统管理员
     */
    private Integer position;
    /**
     * 执业药师注册证号
     */
    private String pharmacistNo;
    /**
     * 药师资质到期日
     */
    private LocalDate licenseExpire;
    /**
     * 健康证到期日
     */
    private LocalDate healthCertExpire;
    /**
     * 入职日期
     */
    private LocalDate hireDate;
    /**
     * 在职状态 1在职/0离职/2休假
     *
     * 枚举 {@link EmployeeStatusEnum}
     */
    private Integer status;
    /**
     * 关联框架 system_users.id
     *
     * 同一租户下唯一，允许为空
     */
    private Long userId;

}
