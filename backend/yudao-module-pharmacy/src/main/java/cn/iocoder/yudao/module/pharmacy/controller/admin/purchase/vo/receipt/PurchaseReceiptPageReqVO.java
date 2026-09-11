package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 采购收货单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseReceiptPageReqVO extends PageParam {

    @Schema(description = "收货单号，模糊匹配", example = "GR407")
    private String receiptNo;

    @Schema(description = "采购订单编号", example = "1024")
    private Long orderId;

    @Schema(description = "门店编号", example = "407")
    private Long storeId;

    @Schema(description = "入库仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "收货单状态 0待提交/1已提交/2已入账/3已作废", example = "2")
    private Integer status;

    @Schema(description = "差异标记 0无/1数量差异/2价格差异", example = "1")
    private Integer diffType;

    @Schema(description = "是否无单收货 0否/1是", example = "0")
    private Integer isFreeReceipt;

    @Schema(description = "收货时间范围（数组：起、止）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] receiveDate;

}
