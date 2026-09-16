package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Same-warehouse location movement. bizNo + line id is the retry key. */
@Data
public class InventoryMovementReqVO {
    @NotNull @Positive private Long warehouseId;
    @NotBlank @Size(max = 32) private String bizNo;
    @NotEmpty @Size(max = 200) @Valid private List<Line> lines;

    @Data
    public static class Line {
        @NotNull @Positive private Long bizLineId;
        @NotNull @Positive private Long batchId;
        @NotNull @Positive private Long sourceLocationId;
        @NotNull @Positive private Long targetLocationId;
        @NotNull @Positive private Integer qty;
    }
}
