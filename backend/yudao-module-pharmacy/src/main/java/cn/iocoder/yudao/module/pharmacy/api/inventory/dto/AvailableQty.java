package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.Data;

/** Store-level sellable quantity, excluding frozen quantity. */
@Data
public class AvailableQty {
    private Long drugId;
    private Integer qtyAvail;
}
