package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.Data;
import java.util.List;

/** Actual allocations chosen during reservation. */
@Data
public class ReserveResult {
    private boolean success;
    private List<Allocation> allocations;

    @Data
    public static class Allocation {
        private Long bizLineId;
        private Long batchId;
        private Long locationId;
        private Integer qty;
    }
}
