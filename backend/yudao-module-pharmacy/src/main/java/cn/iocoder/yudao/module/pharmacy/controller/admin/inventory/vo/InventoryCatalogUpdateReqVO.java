package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

/** Compare the editable fields observed by the client before applying a full replacement. */
public final class InventoryCatalogUpdateReqVO {
    private InventoryCatalogUpdateReqVO() { }

    @Data
    public static class WarehouseFields {
        @NotBlank @Size(max = 16) private String whCode;
        @NotBlank @Size(max = 64) private String whName;
        @NotNull @Min(0) @Max(3) private Integer tempZone;
        @NotNull @Min(0) @Max(1) private Integer status;
    }

    @Data
    public static class LocationFields {
        @NotBlank @Size(max = 32) private String locationCode;
        @NotNull @Min(0) @Max(4) private Integer locationType;
        @Min(0) @Max(4294967295L) private Long maxCapacity;
        @NotNull @Min(0) @Max(1) private Integer status;
    }

    @Data
    public static class Warehouse {
        @NotNull @Positive private Long id;
        @NotNull @Valid private WarehouseFields expected;
        @NotNull @Valid private WarehouseFields value;
    }

    @Data
    public static class Location {
        @NotNull @Positive private Long id;
        @NotNull @Positive private Long warehouseId;
        @NotNull @Valid private LocationFields expected;
        @NotNull @Valid private LocationFields value;
    }
}
