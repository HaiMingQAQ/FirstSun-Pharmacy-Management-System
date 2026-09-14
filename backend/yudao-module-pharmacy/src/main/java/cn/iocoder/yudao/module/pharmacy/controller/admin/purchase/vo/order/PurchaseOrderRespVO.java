package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 采购订单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class PurchaseOrderRespVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("订单编号")
    private Long id;

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "PO407-20260912-0001")
    @ExcelProperty("订单号")
    private String orderNo;

    @Schema(description = "采购门店", requiredMode = Schema.RequiredMode.REQUIRED, example = "407")
    @ExcelProperty("门店编号")
    private Long storeId;

    @Schema(description = "门店名称", example = "FirstSun 演示门店")
    @ExcelProperty("门店名称")
    private String storeName;

    @Schema(description = "收货仓库", example = "1")
    @ExcelProperty("仓库编号")
    private Long warehouseId;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("供应商编号")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "国药控股有限公司")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "下单日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-12")
    @ExcelProperty("下单日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate orderDate;

    @Schema(description = "预计到货日期", example = "2026-09-15")
    @ExcelProperty("预计到货")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expectDate;

    @Schema(description = "总数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @ExcelProperty("总数量")
    private Integer totalQty;

    @Schema(description = "含税总金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1250.00")
    @ExcelProperty("含税总金额")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "62.50")
    @ExcelProperty("优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "应付金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1187.50")
    @ExcelProperty("应付金额")
    private BigDecimal payableAmount;

    @Schema(description = "订单状态 -1取消/0草稿/1提交/2审批/3发出/4部分到货/5完成",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty(value = "订单状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_PO_STATUS)
    private Integer status;

    @Schema(description = "是否采购建议自动生成 0否/1是", example = "0")
    @ExcelProperty(value = "采购建议生成", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_YES_NO)
    private Integer isAuto;

    @Schema(description = "备注", example = "常规补货")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "审批人员工编号", example = "407")
    private Long auditBy;

    @Schema(description = "审批时间")
    private LocalDateTime auditAt;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
