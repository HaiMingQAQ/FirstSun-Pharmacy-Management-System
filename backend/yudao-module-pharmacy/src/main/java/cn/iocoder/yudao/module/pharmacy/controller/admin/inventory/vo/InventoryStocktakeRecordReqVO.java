package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class InventoryStocktakeRecordReqVO {
    @NotNull @Positive private Long stocktakeId;
    @NotNull @Positive private Long lineId;
    @NotNull @PositiveOrZero private Integer realQty;
}
