package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Write-side mapper used only by the inventory facade. Every mutation is tenant and store scoped. */
@Mapper
public interface InventoryFacadeMapper {

    List<Flow> selectFlows(@Param("scope") Scope scope, @Param("bizType") int bizType,
                           @Param("bizNo") String bizNo, @Param("bizLineId") long bizLineId);
    Warehouse lockWarehouse(@Param("scope") Scope scope, @Param("id") long id);
    Location lockLocation(@Param("scope") Scope scope, @Param("warehouseId") long warehouseId,
                          @Param("id") long id);
    long sumLocationQty(@Param("scope") Scope scope, @Param("locationId") long locationId);
    Batch lockBatch(@Param("scope") Scope scope, @Param("id") long id);
    Batch lockBatchByNaturalKey(@Param("scope") Scope scope, @Param("warehouseId") long warehouseId,
                                @Param("drugId") long drugId, @Param("batchNo") String batchNo);
    List<FefoStock> lockFefoStocks(@Param("scope") Scope scope, @Param("drugId") long drugId,
                                   @Param("today") LocalDate today);
    Stock lockStock(@Param("scope") Scope scope, @Param("batchId") long batchId,
                    @Param("locationId") long locationId);
    Flow lockOriginalSaleFlow(@Param("scope") Scope scope, @Param("bizNo") String bizNo,
                              @Param("bizLineId") long bizLineId, @Param("batchId") long batchId,
                              @Param("locationId") long locationId);
    int returnedQty(@Param("scope") Scope scope, @Param("originalFlowId") long originalFlowId);

    int upsertBatch(@Param("scope") Scope scope, @Param("warehouseId") long warehouseId,
                    @Param("drugId") long drugId, @Param("batchNo") String batchNo,
                    @Param("manufactureDate") LocalDate manufactureDate, @Param("expiryDate") LocalDate expiryDate,
                    @Param("operator") String operator);
    int insertStock(@Param("scope") Scope scope, @Param("batchId") long batchId,
                    @Param("locationId") long locationId, @Param("drugId") long drugId,
                    @Param("operator") String operator);
    int increaseStock(@Param("scope") Scope scope, @Param("id") long id, @Param("qty") int qty,
                      @Param("operator") String operator);
    int increaseBatchForReceipt(@Param("scope") Scope scope, @Param("id") long id, @Param("qty") int qty,
                                @Param("unitPrice") BigDecimal unitPrice, @Param("operator") String operator);
    int deductStock(@Param("scope") Scope scope, @Param("id") long id, @Param("qty") int qty,
                    @Param("operator") String operator);
    int deductBatch(@Param("scope") Scope scope, @Param("id") long id, @Param("qty") int qty,
                    @Param("operator") String operator);
    int returnStock(@Param("scope") Scope scope, @Param("id") long id, @Param("qty") int qty,
                    @Param("operator") String operator);
    int returnBatch(@Param("scope") Scope scope, @Param("id") long id, @Param("qty") int qty,
                    @Param("operator") String operator);
    int insertFlow(@Param("scope") Scope scope, @Param("flow") FlowCommand flow);

    @Data
    class Warehouse { private Long id; private Integer status; }
    @Data
    class Location { private Long id; private Long warehouseId; private Long maxCapacity; private Integer status; }
    @Data
    class Batch {
        private Long id; private Long warehouseId; private Long drugId; private String batchNo;
        private LocalDate manufactureDate; private LocalDate expiryDate; private Integer qualityStatus;
        private Integer qtyTotal; private Integer qtyAvail; private Integer qtyFrozen; private Integer qtySold;
        private BigDecimal costPrice;
    }
    @Data
    class Stock { private Long id; private Long batchId; private Long locationId; private Long drugId; private Integer qty; private Integer qtyFrozen; }
    @Data
    class FefoStock extends Stock {
        private Long warehouseId; private String batchNo; private LocalDate expiryDate; private Integer qualityStatus;
        private Integer batchTotal; private Integer batchAvail; private Integer batchFrozen; private Integer batchSold;
    }
    @Data
    class Flow { private Long id; private Long batchId; private Long locationId; private Integer inQty; private Integer outQty; }
    @Data
    class FlowCommand {
        private Long batchId; private Long locationId; private Long drugId; private String batchNo;
        private int flowType; private int inQty; private int outQty; private int balanceQty;
        private int bizType; private String bizNo; private long bizLineId; private Long originalFlowId;
        private BigDecimal unitCost; private long operator; private LocalDateTime flowTime;
    }
}
