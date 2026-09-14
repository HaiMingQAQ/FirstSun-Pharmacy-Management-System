package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.Data;

/**
 * 扣减结果
 */
@Data
public class DeductResult {

    private boolean success;

    private String errorMsg;
}
