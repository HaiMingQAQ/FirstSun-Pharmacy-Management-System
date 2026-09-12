package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import lombok.Data;

public final class InventoryReconciliationVO {
    private InventoryReconciliationVO() { }

    @Data
    public static class Row {
        private Long batchId;
        private Long warehouseId;
        private Long drugId;
        private String batchNo;
        private Integer batchTotal;
        private Integer batchAvail;
        private Integer batchFrozen;
        private Integer locationTotal;
        private Integer locationFrozen;
        private Integer invalidLocationRows;
        private Integer flowNet;
        private Integer openingFlowCount;
        private Integer activeLockQty;
        private Long lastFlowId;
        private Boolean locationMatches;
        private Boolean flowMatches;
        private Boolean frozenMatches;
        private Boolean openingBaselinePresent;
        private Boolean difference;
    }
}
