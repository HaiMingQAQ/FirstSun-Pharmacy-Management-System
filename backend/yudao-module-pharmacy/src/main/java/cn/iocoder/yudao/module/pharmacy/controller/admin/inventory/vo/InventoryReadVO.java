package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Explicit read projections; no costs or internal audit/tenant fields are exposed. */
public final class InventoryReadVO {
    private InventoryReadVO() { }

    @Data
    public static class Warehouse {
        private Long id;
        private Long storeId;
        private String whCode;
        private String whName;
        private Integer tempZone;
        private Integer isDefault;
        private Integer status;
    }

    @Data
    public static class Location {
        private Long id;
        private Long warehouseId;
        private String warehouseName;
        private String locationCode;
        private Integer locationType;
        private Long maxCapacity;
        private Integer status;
    }

    @Data
    public static class Batch {
        private Long id;
        private Long warehouseId;
        private String warehouseName;
        private Long drugId;
        private String batchNo;
        private LocalDate manufactureDate;
        private LocalDate expiryDate;
        private Integer qtyTotal;
        private Integer qtyAvail;
        private Integer qtyFrozen;
        private Integer qtySold;
        private Integer qualityStatus;
    }

    @Data
    public static class LocationStock {
        private Long id;
        private Long batchId;
        private String batchNo;
        private Long warehouseId;
        private Long locationId;
        private String locationCode;
        private Long drugId;
        private Integer qty;
        private Integer qtyFrozen;
        private Integer qtyAvail;
    }

    @Data
    public static class Flow {
        private Long id;
        private Long batchId;
        private String batchNo;
        private Long drugId;
        private Long locationId;
        private Integer flowType;
        private Integer inQty;
        private Integer outQty;
        private Integer frozenDelta;
        private Integer balanceQty;
        private Integer bizType;
        private String bizNo;
        private Long bizLineId;
        private LocalDateTime flowTime;
    }
}
