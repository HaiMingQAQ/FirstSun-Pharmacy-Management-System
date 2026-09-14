package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Ownership, audit fields, default flag and initial status are server controlled. */
public final class InventoryCatalogCreateReqVO {
    private InventoryCatalogCreateReqVO() { }

    @Data
    public static class Warehouse {
        @NotBlank @Size(max = 16)
        private String whCode;
        @NotBlank @Size(max = 64)
        private String whName;
        @NotNull @Min(0) @Max(3)
        private Integer tempZone;
    }

    @Data
    public static class Location {
        @NotNull @Positive
        private Long warehouseId;
        @NotBlank @Size(max = 32)
        private String locationCode;
        @NotNull @Min(0) @Max(4)
        private Integer locationType;
        @Min(0) @Max(4294967295L)
        private Long maxCapacity;
    }
}
