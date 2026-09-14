package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public final class InventoryStocktakeVO {
    private InventoryStocktakeVO() { }

    @Data
    public static class Summary {
        private Long id;
        private String stocktakeNo;
        private Long warehouseId;
        private Integer stocktakeType;
        private Integer blindFlag;
        private Integer freezeFlag;
        private Integer totalItem;
        private Integer doneItem;
        private Integer status;
        private Long initiatorId;
        private Long auditBy;
        private LocalDateTime auditAt;
        private LocalDateTime adjustedAt;
        private LocalDateTime createTime;
    }

    @Data
    public static class Detail {
        private Summary summary;
        private List<Line> lines;
    }

    @Data
    public static class Line {
        private Long id;
        private Long stocktakeId;
        private Long batchId;
        private Long locationId;
        private Long drugId;
        private String batchNo;
        private Integer bookQty;
        private Integer realQty;
        private Integer diffQty;
        private Integer diffFlag;
        private Integer scannedFlag;
    }
}
