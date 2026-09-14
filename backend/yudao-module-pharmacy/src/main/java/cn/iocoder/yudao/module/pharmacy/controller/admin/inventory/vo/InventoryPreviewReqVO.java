package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator.ExpiryDayPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Explicit boundary for a read-only simulation; never authorizes a sale or inventory write. */
@Data
public class InventoryPreviewReqVO {
    @NotNull @Positive
    private Long warehouseId;
    @NotNull @Positive
    private Long drugId;
    @NotNull @Positive
    private Integer quantity;
    @NotNull
    private ExpiryDayPolicy expiryDayPolicy;
}
