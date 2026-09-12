package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class InventoryExpiryVO {
    private InventoryExpiryVO() { }

    @Data
    public static class Alert {
        private Long id;
        private Long batchId;
        private Long drugId;
        private Long warehouseId;
        private String batchNo;
        private LocalDate expiryDate;
        private Integer qtyTotal;
        private Integer qtyAvail;
        private Integer qtyFrozen;
        private Integer alertLevel;
        private Integer expireDays;
        private Integer handleType;
        private Long handleBy;
        private LocalDateTime handleAt;
        private LocalDate alertDate;
    }
}
