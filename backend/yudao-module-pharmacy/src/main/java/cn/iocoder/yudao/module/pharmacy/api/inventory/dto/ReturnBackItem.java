package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.Data;

/**
 * 退货回补项
 */
@Data
public class ReturnBackItem {

    private Long drugId;

    private Long batchId;

    private Integer qty;

    private Long locationId;
}
