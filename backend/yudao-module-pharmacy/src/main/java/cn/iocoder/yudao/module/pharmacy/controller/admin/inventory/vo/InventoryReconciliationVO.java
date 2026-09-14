package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

public final class InventoryReconciliationVO {
    private InventoryReconciliationVO() { }

    @Data
    @ExcelIgnoreUnannotated
    public static class Row {
        @ExcelProperty("批次编号")
        private Long batchId;
        @ExcelProperty("仓库编号")
        private Long warehouseId;
        @ExcelProperty("药品编号")
        private Long drugId;
        @ExcelProperty("批号")
        private String batchNo;
        @ExcelProperty("批次总量")
        private Integer batchTotal;
        @ExcelProperty("批次可用")
        private Integer batchAvail;
        @ExcelProperty("批次冻结")
        private Integer batchFrozen;
        @ExcelProperty("货位总量")
        private Integer locationTotal;
        @ExcelProperty("货位冻结")
        private Integer locationFrozen;
        @ExcelProperty("非法货位行")
        private Integer invalidLocationRows;
        @ExcelProperty("流水净额")
        private Integer flowNet;
        @ExcelProperty("期初流水数")
        private Integer openingFlowCount;
        @ExcelProperty("有效锁数量")
        private Integer activeLockQty;
        @ExcelProperty("最近流水编号")
        private Long lastFlowId;
        @ExcelProperty("货位账一致")
        private Boolean locationMatches;
        @ExcelProperty("流水账一致")
        private Boolean flowMatches;
        @ExcelProperty("冻结账一致")
        private Boolean frozenMatches;
        @ExcelProperty("期初基线存在")
        private Boolean openingBaselinePresent;
        @ExcelProperty("是否异常")
        private Boolean difference;
    }
}
