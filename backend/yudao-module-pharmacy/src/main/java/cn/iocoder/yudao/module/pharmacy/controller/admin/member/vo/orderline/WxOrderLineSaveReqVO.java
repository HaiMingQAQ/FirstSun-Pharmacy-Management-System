package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.orderline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 小程序订单明细创建/修改 Request VO")
@Data
public class WxOrderLineSaveReqVO {

    @Schema(description = "明细编号", example = "1024")
    private Long id;

    @Schema(description = "线上订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "线上订单编号不能为空")
    private Long wxOrderId;

    @Schema(description = "商品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "商品编号不能为空")
    private Long drugId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer qty;

    @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "25.00")
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal price;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "50.00")
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0", message = "金额不能为负数")
    private BigDecimal lineAmount;

    @Schema(description = "拣货批次编号", example = "1")
    private Long batchId;

    @Schema(description = "批号", example = "20240101")
    private String batchNo;

    @Schema(description = "已拣数量", example = "2")
    @Min(value = 0, message = "已拣数量不能为负数")
    private Integer pickedQty;

    @Schema(description = "锁定时绑定的货位编号", example = "1")
    private Long locationId;

    @Schema(description = "下单名称快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "阿莫西林")
    @NotBlank(message = "下单名称快照不能为空")
    @Size(max = 128, message = "下单名称快照长度不能超过 128 个字符")
    private String drugName;

    @Schema(description = "规格快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.5g*24粒")
    @NotBlank(message = "规格快照不能为空")
    @Size(max = 64, message = "规格快照长度不能超过 64 个字符")
    private String specification;

    @Schema(description = "单位快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "盒")
    @NotBlank(message = "单位快照不能为空")
    @Size(max = 8, message = "单位快照长度不能超过 8 个字符")
    private String unit;

}
