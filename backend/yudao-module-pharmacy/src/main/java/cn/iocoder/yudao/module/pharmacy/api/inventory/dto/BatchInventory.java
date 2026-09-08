package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 批次库存快照（契约，待 C 复核）
 */
@Data
public class BatchInventory {

    private Long drugId;

    private Long batchId;

    private String batchNo;

    private LocalDate expiryDate;

    private Integer qtyAvail;

    private Integer qtyFrozen;
}
