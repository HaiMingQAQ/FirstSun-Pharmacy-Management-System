package cn.iocoder.yudao.module.pharmacy.api.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购收货入账明细（B → C 的库存入库契约）
 *
 * 由 B 的采购收货单生成，C 的库存服务据此创建批次、写入货位库存与库存流水。
 *
 * @author B 成员（契约提出方）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveItem implements Serializable {

    /**
     * 业务行标识：收货明细编号（字符串形式），作为幂等键的一部分
     */
    private String bizLineId;

    /**
     * 门店编号
     */
    private Long storeId;

    /**
     * 药品编号
     */
    private Long drugId;

    /**
     * 批号
     */
    private String batchNo;

    /**
     * 生产日期
     */
    private LocalDate manufactureDate;

    /**
     * 有效期至
     */
    private LocalDate expiryDate;

    /**
     * 入库数量
     */
    private Integer qty;

    /**
     * 采购单价（元）
     */
    private BigDecimal unitPrice;

    /**
     * 入库仓库编号
     */
    private Long warehouseId;

    /**
     * 入库货位编号
     */
    private Long locationId;

}
