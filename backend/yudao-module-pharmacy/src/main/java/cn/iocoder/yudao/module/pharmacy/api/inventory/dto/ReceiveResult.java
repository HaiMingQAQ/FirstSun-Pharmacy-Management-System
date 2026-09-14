package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 采购收货入账结果（C → B 的返回契约）
 *
 * B 依据 {@link ReceiveLineResult#getBizLineId()} 把生成的批次编号回写到
 * {@code ph_po_receipt_line.create_batch_id}，便于后续追溯库存流水。
 *
 * @author B 成员（契约提出方）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveResult implements Serializable {

    /**
     * 逐行入账结果
     */
    private List<ReceiveLineResult> lines;

    /**
     * 单行入账结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiveLineResult implements Serializable {

        /**
         * 业务行标识（对应 {@link ReceiveItem#getBizLineId()}）
         */
        private String bizLineId;

        /**
         * 生成的批次编号（ph_inv_batch.id）
         */
        private Long batchId;

        /**
         * 货位库存编号（ph_inv_location_stock.id）
         */
        private Long locationStockId;

    }

}
