package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InventoryExpiryHandleReqVO {
    @NotNull @Positive private Long id;
    /** 1促销、2退货、3报损、4继续销售（店长确认）. */
    @NotNull @Min(1) @Max(4) private Integer handleType;
}
