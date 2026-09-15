package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.Data;

/**
 * 销售扣减项
 */
@Data
public class DeductItem {

    private Long drugId;

    private Long batchId;

    private Integer qty;

    private Long locationId;

    /** Source sale order number. Required by the real adapter for idempotency. */
    private String bizNo;

    /** Source sale order line id. Required by the real adapter for idempotency. */
    private Long bizLineId;
}
