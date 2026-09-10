package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "管理后台 - 门店创建/修改 Request VO")
@Data
public class StoreSaveReqVO {

    @Schema(description = "门店编号", example = "1024")
    private Long id;

    @Schema(description = "门店编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "S001")
    @NotBlank(message = "门店编码不能为空")
    @Size(max = 16, message = "门店编码长度不能超过 16 个字符")
    private String storeCode;

    @Schema(description = "门店名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "朝阳店")
    @NotBlank(message = "门店名称不能为空")
    @Size(max = 64, message = "门店名称长度不能超过 64 个字符")
    private String storeName;

    @Schema(description = "地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区xx路xx号")
    @NotBlank(message = "地址不能为空")
    @Size(max = 200, message = "地址长度不能超过 200 个字符")
    private String address;

    @Schema(description = "联系电话", example = "010-12345678")
    @Size(max = 20, message = "联系电话长度不能超过 20 个字符")
    private String phone;

    @Schema(description = "经营范围", example = "中成药、化学药制剂、抗生素")
    @Size(max = 500, message = "经营范围长度不能超过 500 个字符")
    private String manageScope;

    @Schema(description = "药品经营许可证号", example = "PDY2023001")
    @Size(max = 64, message = "许可证号长度不能超过 64 个字符")
    private String licenseNo;

    @Schema(description = "证照到期日", example = "2027-12-31")
    private LocalDate licenseExpire;

    @Schema(description = "是否医保定点 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "是否医保定点不能为空")
    private Integer isMedical;

    @Schema(description = "营业时间描述", example = "08:00-22:00")
    @Size(max = 32, message = "营业时间长度不能超过 32 个字符")
    private String businessHours;

    @Schema(description = "营业状态 1营业/0停业", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @InEnum(PharmacyStatusEnum.class)
    private Integer status;

    @Schema(description = "关联部门编号（system_dept.id），同一租户下唯一", example = "100")
    private Long deptId;

}
