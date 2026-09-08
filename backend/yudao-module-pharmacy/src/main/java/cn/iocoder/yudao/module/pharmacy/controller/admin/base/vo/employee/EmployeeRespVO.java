package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工 Response VO")
@Data
@ExcelIgnoreUnannotated
public class EmployeeRespVO {

    @Schema(description = "员工编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("员工编号")
    private Long id;

    @Schema(description = "工号", requiredMode = Schema.RequiredMode.REQUIRED, example = "E001")
    @ExcelProperty("工号")
    private String empNo;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @ExcelProperty("姓名")
    private String empName;

    @Schema(description = "加密手机号密文", example = "ENC(xxx)")
    @ExcelProperty("手机号")
    private String phone;

    @Schema(description = "所属门店", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("所属门店编号")
    private Long storeId;

    /**
     * 门店名称（仅展示用，由 Controller 补充）
     */
    @Schema(description = "所属门店名称", example = "朝阳店")
    @ExcelProperty("所属门店名称")
    private String storeName;

    @Schema(description = "岗位 1店长/2药师/3收银员/4库管员/5采购/6财务/9系统管理员", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty(value = "岗位", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_EMPLOYEE_POSITION)
    private Integer position;

    @Schema(description = "执业药师注册证号", example = "PHA2023001")
    @ExcelProperty("药师注册证号")
    private String pharmacistNo;

    @Schema(description = "药师资质到期日", example = "2027-12-31")
    @ExcelProperty("药师资质到期日")
    private LocalDate licenseExpire;

    @Schema(description = "健康证到期日", example = "2026-12-31")
    @ExcelProperty("健康证到期日")
    private LocalDate healthCertExpire;

    @Schema(description = "入职日期", example = "2024-01-01")
    @ExcelProperty("入职日期")
    private LocalDate hireDate;

    @Schema(description = "在职状态 1在职/0离职/2休假", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "在职状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_EMPLOYEE_STATUS)
    private Integer status;

    @Schema(description = "关联系统用户编号", example = "1")
    @ExcelProperty("关联用户编号")
    private Long userId;

    /**
     * 关联用户昵称（仅展示用，由 Controller 补充）
     */
    @Schema(description = "关联用户昵称", example = "张三")
    @ExcelProperty("关联用户昵称")
    private String userNickname;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
