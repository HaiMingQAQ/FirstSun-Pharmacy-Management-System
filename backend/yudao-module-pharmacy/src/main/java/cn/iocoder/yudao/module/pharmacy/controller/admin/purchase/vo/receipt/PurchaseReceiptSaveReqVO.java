package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 采购收货单创建/修改 Request VO")
@Data
public class PurchaseReceiptSaveReqVO {

    @Schema(description = "收货单编号", example = "1024")
    private Long id;

    @Schema(description = "关联采购订单（无单收货时为空）", example = "1024")
    private Long orderId;

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "407")
    @NotNull(message = "门店不能为空")
    private Long storeId;

    @Schema(description = "入库仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "入库仓库不能为空")
    private Long warehouseId;

    @Schema(description = "收货时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-12 10:00:00")
    @NotNull(message = "收货时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveDate;

    @Schema(description = "是否无单收货 0否/1是（限店长，WH-002）", example = "0")
    private Integer isFreeReceipt;

    @Schema(description = "收货明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "收货明细不能为空")
    @Valid
    private List<PurchaseReceiptLineSaveReqVO> lines;

}
