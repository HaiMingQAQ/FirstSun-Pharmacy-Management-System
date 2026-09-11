package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 采购订单创建/修改 Request VO")
@Data
public class PurchaseOrderSaveReqVO {

    @Schema(description = "订单编号", example = "1024")
    private Long id;

    @Schema(description = "采购门店", requiredMode = Schema.RequiredMode.REQUIRED, example = "407")
    @NotNull(message = "采购门店不能为空")
    private Long storeId;

    @Schema(description = "收货仓库（可空，收货时确定）", example = "1")
    private Long warehouseId;

    @Schema(description = "供应商", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "下单日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-12")
    @NotNull(message = "下单日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate orderDate;

    @Schema(description = "预计到货日期", example = "2026-09-15")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expectDate;

    @Schema(description = "是否采购建议自动生成 0否/1是", example = "0")
    private Integer isAuto;

    @Schema(description = "备注", example = "常规补货")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "采购明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "采购明细不能为空")
    @Valid
    private List<PurchaseOrderLineSaveReqVO> lines;

}
