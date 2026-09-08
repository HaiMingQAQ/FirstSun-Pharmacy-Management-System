package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 员工分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePageReqVO extends PageParam {

    @Schema(description = "工号，模糊匹配", example = "E001")
    private String empNo;

    @Schema(description = "姓名，模糊匹配", example = "张三")
    private String empName;

    @Schema(description = "所属门店", example = "1024")
    private Long storeId;

    @Schema(description = "岗位 1店长/2药师/3收银员/4库管员/5采购/6财务/9系统管理员", example = "2")
    private Integer position;

    @Schema(description = "在职状态 1在职/0离职/2休假", example = "1")
    private Integer status;

    @Schema(description = "关联系统用户编号", example = "1")
    private Long userId;

}
