package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class InventoryDamageCreateReqVO {
    /** 一期只开放报损；报溢必须走盘点审批。 */
    @NotNull @Min(0) @Max(0) private Integer damageType;
    @NotNull @Min(0) @Max(6) private Integer reason;
    @Size(max = 64) private String destroyMethod;
    @Size(max = 200) private String destroyCompany;
    @NotEmpty @Size(max = 100) @Valid private List<Line> lines;

    @Data
    public static class Line {
        @NotNull @Positive private Long batchId;
        @NotNull @Positive private Long locationId;
        @NotNull @Positive private Integer qty;
        @NotNull @Min(0) @Max(2) private Integer disposeType;
    }
}
