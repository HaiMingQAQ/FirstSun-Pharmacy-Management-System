package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.pharmacy.enums.EmployeeStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工创建/修改 Request VO")
@Data
public class EmployeeSaveReqVO {

    @Schema(description = "员工编号", example = "1024")
    private Long id;

    @Schema(description = "工号", requiredMode = Schema.RequiredMode.REQUIRED, example = "E001")
    @NotBlank(message = "工号不能为空")
    @Size(max = 16, message = "工号长度不能超过 16 个字符")
    private String empNo;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度不能超过 32 个字符")
    private String empName;

    @Schema(description = "加密手机号密文", example = "ENC(xxx)")
    @Size(max = 255, message = "手机号密文长度不能超过 255 个字符")
    private String phone;

    @Schema(description = "所属门店", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "所属门店不能为空")
    private Long storeId;

    @Schema(description = "岗位 1店长/2药师/3收银员/4库管员/5采购/6财务/9系统管理员", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "岗位不能为空")
    private Integer position;

    @Schema(description = "执业药师注册证号", example = "PHA2023001")
    @Size(max = 32, message = "药师注册证号长度不能超过 32 个字符")
    private String pharmacistNo;

    @Schema(description = "药师资质到期日", example = "2027-12-31")
    private LocalDate licenseExpire;

    @Schema(description = "健康证到期日", example = "2026-12-31")
    private LocalDate healthCertExpire;

    @Schema(description = "入职日期", example = "2024-01-01")
    private LocalDate hireDate;

    @Schema(description = "在职状态 1在职/0离职/2休假", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @InEnum(EmployeeStatusEnum.class)
    private Integer status;

    @Schema(description = "关联系统用户编号（system_users.id），同一租户下唯一", example = "1")
    private Long userId;

}
