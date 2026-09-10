package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 门店 Response VO")
@Data
@ExcelIgnoreUnannotated
public class StoreRespVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("门店编号")
    private Long id;

    @Schema(description = "门店编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "S001")
    @ExcelProperty("门店编码")
    private String storeCode;

    @Schema(description = "门店名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "朝阳店")
    @ExcelProperty("门店名称")
    private String storeName;

    @Schema(description = "地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区xx路xx号")
    @ExcelProperty("地址")
    private String address;

    @Schema(description = "联系电话", example = "010-12345678")
    @ExcelProperty("联系电话")
    private String phone;

    @Schema(description = "经营范围", example = "中成药、化学药制剂")
    @ExcelProperty("经营范围")
    private String manageScope;

    @Schema(description = "药品经营许可证号", example = "PDY2023001")
    @ExcelProperty("许可证号")
    private String licenseNo;

    @Schema(description = "证照到期日", example = "2027-12-31")
    @ExcelProperty("证照到期日")
    private LocalDate licenseExpire;

    @Schema(description = "是否医保定点 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "医保定点", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_YES_NO)
    private Integer isMedical;

    @Schema(description = "营业时间描述", example = "08:00-22:00")
    @ExcelProperty("营业时间")
    private String businessHours;

    @Schema(description = "营业状态 1营业/0停业", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "营业状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_STATUS)
    private Integer status;

    @Schema(description = "关联部门编号", example = "100")
    @ExcelProperty("关联部门编号")
    private Long deptId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
