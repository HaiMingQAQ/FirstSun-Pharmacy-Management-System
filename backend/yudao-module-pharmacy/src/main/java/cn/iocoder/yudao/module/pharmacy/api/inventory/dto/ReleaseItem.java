package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.Data;

/** Release item. The new business line is idempotent and points at one original reservation allocation. */
@Data
public class ReleaseItem {
    private Long drugId;
    private Long batchId;
    private Long locationId;
    private Integer qty;
    private String bizNo;
    private Long bizLineId;
    private String originalBizNo;
    private Long originalBizLineId;
}
