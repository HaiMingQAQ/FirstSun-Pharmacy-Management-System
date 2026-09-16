package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.Data;

/** Online-order reservation item. Batch and location are optional; absent values use FEFO. */
@Data
public class ReserveItem {
    private Long drugId;
    private Long batchId;
    private Long locationId;
    private Integer qty;
    private String bizNo;
    private Long bizLineId;
}
