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

    /** Newly created return document number. */
    private String bizNo;

    /** Newly created return document line id. */
    private Long bizLineId;

    /** Original sale order number used to locate the exact outbound flow. */
    private String originalBizNo;

    /** Original sale order line id used to locate the exact outbound flow. */
    private Long originalBizLineId;
}
