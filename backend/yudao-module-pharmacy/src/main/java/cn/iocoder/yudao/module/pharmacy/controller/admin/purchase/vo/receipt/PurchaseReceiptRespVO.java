package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 采购收货单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class PurchaseReceiptRespVO {

    @Schema(description = "收货单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("收货单编号")
    private Long id;

    @Schema(description = "收货单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "GR407-20260912-0001")
    @ExcelProperty("收货单号")
    private String receiptNo;

    @Schema(description = "采购订单编号（无单收货为空）", example = "1024")
    @ExcelProperty("采购订单编号")
    private Long orderId;

    @Schema(description = "采购订单号", example = "PO407-20260912-0001")
    @ExcelProperty("采购订单号")
    private String orderNo;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "407")
    @ExcelProperty("门店编号")
    private Long storeId;

    @Schema(description = "门店名称", example = "FirstSun 演示门店")
    @ExcelProperty("门店名称")
    private String storeName;

    @Schema(description = "入库仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("仓库编号")
    private Long warehouseId;

    @Schema(description = "收货人员工编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "407")
    @ExcelProperty("收货人编号")
    private Long receiveBy;

    @Schema(description = "收货人姓名", example = "FirstSun")
    @ExcelProperty("收货人")
    private String receiveByName;

    @Schema(description = "收货时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("收货时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveDate;

    @Schema(description = "实收总数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "40")
    @ExcelProperty("实收总数量")
    private Integer totalQty;

    @Schema(description = "实收总金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    @ExcelProperty("实收总金额")
    private BigDecimal totalAmount;

    @Schema(description = "差异标记 0无/1数量差异/2价格差异", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "差异标记", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_RECEIPT_DIFF_TYPE)
    private Integer diffType;

    @Schema(description = "是否无单收货 0否/1是", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "无单收货", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_YES_NO)
    private Integer isFreeReceipt;

    @Schema(description = "收货单状态 0待提交/1已提交/2已入账/3已作废",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty(value = "收货单状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_RECEIPT_STATUS)
    private Integer status;

    @Schema(description = "质检结果 0未检/1合格/2有异常", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "质检结果", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_QUALITY_STATUS)
    private Integer qualityStatus;

    @Schema(description = "入账时间")
    private LocalDateTime postedAt;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
