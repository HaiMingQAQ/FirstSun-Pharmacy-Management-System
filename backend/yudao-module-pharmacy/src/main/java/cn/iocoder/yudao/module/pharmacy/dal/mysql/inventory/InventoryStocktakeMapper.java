package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InventoryStocktakeMapper {
    long count(@Param("scope") Scope scope, @Param("q") InventoryStocktakeQuery query);
    List<InventoryStocktakeVO.Summary> selectPage(@Param("scope") Scope scope,
                                                   @Param("q") InventoryStocktakeQuery query);
    InventoryStocktakeVO.Summary selectSummary(@Param("scope") Scope scope, @Param("id") long id);
    List<InventoryStocktakeVO.Line> selectLines(@Param("scope") Scope scope, @Param("id") long id);
    Header lockHeader(@Param("scope") Scope scope, @Param("id") long id);
    int countLines(@Param("scope") Scope scope, @Param("id") long id);
    int countUnrecorded(@Param("scope") Scope scope, @Param("id") long id);
    int insertHeader(@Param("scope") Scope scope, @Param("q") InventoryStocktakeCreateReqVO request,
                     @Param("stocktakeNo") String stocktakeNo, @Param("operator") long operator,
                     @Param("key") GeneratedKey key);
    int insertSnapshotLines(@Param("scope") Scope scope, @Param("stocktakeId") long stocktakeId,
                            @Param("warehouseId") long warehouseId, @Param("operator") long operator);
    int updateTotalItem(@Param("scope") Scope scope, @Param("id") long id, @Param("operator") long operator);
    int updateStatus(@Param("scope") Scope scope, @Param("id") long id,
                     @Param("from") int from, @Param("to") int to, @Param("operator") long operator);
    int updateRecord(@Param("scope") Scope scope, @Param("stocktakeId") long stocktakeId,
                     @Param("lineId") long lineId, @Param("realQty") int realQty,
                     @Param("diffQty") int diffQty, @Param("diffFlag") int diffFlag,
                     @Param("operator") long operator);
    int countRecorded(@Param("scope") Scope scope, @Param("id") long id);
    int updateDoneItem(@Param("scope") Scope scope, @Param("id") long id, @Param("done") int done,
                       @Param("operator") long operator);
    List<AdjustmentLine> selectAdjustmentLines(@Param("scope") Scope scope, @Param("id") long id);
    WarehouseLock lockWarehouse(@Param("scope") Scope scope, @Param("id") long id);
    BatchLock lockBatch(@Param("scope") Scope scope, @Param("id") long id);
    StockLock lockStock(@Param("scope") Scope scope, @Param("batchId") long batchId,
                        @Param("locationId") long locationId);
    LocationLock lockLocation(@Param("scope") Scope scope, @Param("warehouseId") long warehouseId,
                              @Param("id") long id);
    int updateBatch(@Param("scope") Scope scope, @Param("id") long id,
                    @Param("delta") int delta, @Param("operator") long operator);
    int updateStock(@Param("scope") Scope scope, @Param("id") long id,
                    @Param("delta") int delta, @Param("operator") long operator);
    int insertStock(@Param("scope") Scope scope, @Param("line") AdjustmentLine line,
                    @Param("operator") long operator);
    int insertFlow(@Param("scope") Scope scope, @Param("line") AdjustmentLine line,
                   @Param("balance") int balance, @Param("operator") long operator,
                   @Param("flowTime") java.time.LocalDateTime flowTime);
    int markAdjusted(@Param("scope") Scope scope, @Param("id") long id, @Param("operator") long operator);
    int cancelDraft(@Param("scope") Scope scope, @Param("id") long id, @Param("operator") long operator);

    @Data class GeneratedKey { private Long id; }

    @Data class Header {
        private Long id; private String stocktakeNo; private Long warehouseId; private Integer status;
        private Integer blindFlag; private Integer freezeFlag; private Long storeId;
    }
    @Data class WarehouseLock { private Long id; private Integer status; private Long storeId; }
    @Data class BatchLock {
        private Long id; private Long warehouseId; private Long storeId; private Long drugId;
        private Integer qtyTotal; private Integer qtyAvail; private Integer qtyFrozen; private Integer version;
        private java.math.BigDecimal costPrice;
    }
    @Data class StockLock {
        private Long id; private Long batchId; private Long locationId; private Long drugId;
        private Integer qty; private Integer qtyFrozen;
    }
    @Data class LocationLock {
        private Long id; private Integer status; private Long maxCapacity; private Long usedQty;
    }
    @Data class AdjustmentLine {
        private Long id; private Long stocktakeId; private Long batchId; private Long locationId;
        private Long drugId; private String batchNo; private Integer bookQty; private Integer realQty;
        private Integer diffQty; private Integer diffFlag; private java.time.LocalDate expiryDate;
        private java.math.BigDecimal costPrice; private Long warehouseId;
    }
}
