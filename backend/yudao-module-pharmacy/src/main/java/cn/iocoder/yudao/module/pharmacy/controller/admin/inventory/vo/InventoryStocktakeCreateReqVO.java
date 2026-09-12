package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InventoryStocktakeCreateReqVO {
    @NotNull @Positive private Long warehouseId;
    @NotNull @Min(0) @Max(1) private Integer stocktakeType;
    @NotNull @Min(0) @Max(1) private Integer blindFlag;
    /** Existing lock table cannot represent a complete stocktake scope; freeze is rejected for now. */
    @NotNull @Min(0) @Max(1) private Integer freezeFlag;
    @Size(max = 100) private String scope;
}
