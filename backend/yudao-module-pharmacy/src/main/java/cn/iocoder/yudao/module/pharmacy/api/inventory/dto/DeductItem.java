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
}
