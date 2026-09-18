package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存写操作 Mapper：批次/货位库存/库存流水（扣减、回补、收货入账）。
 * 与 InventoryReadMapper 区分：本 Mapper 只提供变更方法，供 InventoryFacade 实现调用。
 */
@Mapper
public interface InventoryWriteMapper {

    /** 批次行（含成本） */
    class InvBatchRow {
        public Long id;
        public Long storeId;
        public Long warehouseId;
        public Long drugId;
        public String batchNo;
        public LocalDate manufactureDate;
        public LocalDate expiryDate;
        public Long supplierId;
        public Integer sourceType;
        public String sourceNo;
        public Integer qtyTotal;
        public Integer qtyAvail;
        public Integer qtyFrozen;
        public Integer qtySold;
        public BigDecimal costPrice;
        public Long tenantId;
    }

    /** 货位库存行 */
    class InvLocationRow {
        public Long batchId;
        public Long locationId;
        public Long drugId;
        public Integer qty;
        public Long warehouseId;
        public LocalDate expiryDate;
        public BigDecimal costPrice;
        public String batchNo;
    }

    /** 流水行 */
    class InvFlowRow {
        public Long storeId;
        public Long batchId;
        public Long drugId;
        public String batchNo;
        public Integer flowType;
        public Integer inQty;
        public Integer outQty;
        public Integer balanceQty;
        public Integer bizType;
        public String bizNo;
        public LocalDateTime flowTime;
        public Long operator;
        public String remark;
        public Long tenantId;
        public Long locationId;
        public Long bizLineId;
        public Integer frozenDelta;
        public BigDecimal unitCost;
    }

    // ---------- 查询 ----------
    InvBatchRow selectBatchById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    InvBatchRow selectBatchByUk(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId,
                                @Param("drugId") Long drugId, @Param("batchNo") String batchNo);

    /** FEFO 选批：效期最早优先，同一批次按货位升序 */
    List<InvLocationRow> selectAvailableStocks(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId,
                                               @Param("drugId") Long drugId);

    /** 按批次+货位查货位库存（回补时定位） */
    InvLocationRow selectLocationStock(@Param("tenantId") Long tenantId, @Param("batchId") Long batchId,
                                       @Param("locationId") Long locationId);

    /** 流水幂等查询（uk_flow_event 各键） */
    int countFlowDup(@Param("tenantId") Long tenantId, @Param("bizType") Integer bizType,
                     @Param("bizNo") String bizNo, @Param("bizLineId") Long bizLineId,
                     @Param("batchId") Long batchId, @Param("locationId") Long locationId,
                     @Param("flowType") Integer flowType);

    // ---------- 扣减 ----------
    /** 扣批次可用量；返回 0 = 库存不足或批次不存在 */
    int deductBatch(@Param("id") Long id, @Param("qty") int qty);

    /** 扣货位库存；返回 0 = 货位库存不足 */
    int deductLocationStock(@Param("batchId") Long batchId, @Param("locationId") Long locationId, @Param("qty") int qty);

    // ---------- 回补 ----------
    int restoreBatch(@Param("id") Long id, @Param("qty") int qty);

    int restoreLocationStock(@Param("batchId") Long batchId, @Param("locationId") Long locationId, @Param("qty") int qty);

    // ---------- 收货入账 ----------
    /** 新增批次，返回自增主键 */
    int insertBatch(InvBatchRow row);

    /** 已有批次追加数量并加权平均成本 */
    int updateBatchAppend(@Param("id") Long id, @Param("inQty") int inQty, @Param("newCost") BigDecimal newCost);

    /** 货位库存 upsert（存在则累加） */
    int upsertLocationStock(@Param("batchId") Long batchId, @Param("locationId") Long locationId,
                            @Param("drugId") Long drugId, @Param("qty") int qty);

    // ---------- 流水 ----------
    int insertFlow(InvFlowRow row);
}
