package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class InventoryDamageVO {
    private InventoryDamageVO() { }

    @Data
    public static class Summary {
        private Long id;
        private String damageNo;
        private Long storeId;
        private Integer damageType;
        private Integer reason;
        private Integer totalQty;
        private BigDecimal totalAmount;
        private Integer status;
        private Long auditBy;
        private LocalDateTime auditAt;
        private Long executeBy;
        private LocalDateTime executeAt;
        private Long reviewBy;
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
        private Long damageId;
        private Long batchId;
        private Long locationId;
        private Long drugId;
        private String batchNo;
        private Integer qty;
        private BigDecimal costPrice;
        private BigDecimal amount;
        private Integer disposeType;
    }
}
