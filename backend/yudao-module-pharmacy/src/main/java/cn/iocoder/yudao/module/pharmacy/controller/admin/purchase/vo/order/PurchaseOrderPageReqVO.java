package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 采购订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderPageReqVO extends PageParam {

    @Schema(description = "订单号，模糊匹配", example = "PO407")
    private String orderNo;

    @Schema(description = "门店编号", example = "407")
    private Long storeId;

    @Schema(description = "供应商编号", example = "1024")
    private Long supplierId;

    @Schema(description = "订单状态 -1取消/0草稿/1提交/2审批/3发出/4部分到货/5完成", example = "2")
    private Integer status;

    @Schema(description = "是否采购建议自动生成 0否/1是", example = "0")
    private Integer isAuto;

    @Schema(description = "下单日期范围（数组：起、止）")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] orderDate;

}
