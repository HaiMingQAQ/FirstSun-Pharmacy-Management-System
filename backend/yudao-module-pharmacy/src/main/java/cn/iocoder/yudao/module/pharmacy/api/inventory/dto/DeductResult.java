package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.Data;

import java.util.List;

/**
 * 扣减结果
 */
@Data
public class DeductResult {

    private boolean success;

    private String errorMsg;

    /** Actual allocations, including FEFO allocations when the caller did not choose a batch. */
    private List<Allocation> allocations;

    @Data
    public static class Allocation {
        private Long bizLineId;
        private Long batchId;
        private Long locationId;
        private Integer qty;
    }
}
