package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InventoryMovementMapper {
    Warehouse lockWarehouse(@Param("scope") Scope scope, @Param("id") long id);
    Batch lockBatch(@Param("scope") Scope scope, @Param("warehouseId") long warehouseId, @Param("id") long id);
    Location lockLocation(@Param("scope") Scope scope, @Param("warehouseId") long warehouseId, @Param("id") long id);
    Stock lockStock(@Param("scope") Scope scope, @Param("batchId") long batchId, @Param("locationId") long locationId);
    long sumLocationQty(@Param("scope") Scope scope, @Param("locationId") long locationId);
    List<Flow> selectFlows(@Param("scope") Scope scope, @Param("bizNo") String bizNo, @Param("bizLineId") long bizLineId);
    int decreaseStock(@Param("scope") Scope scope, @Param("id") long id, @Param("qty") int qty, @Param("operator") String operator);
    int upsertStock(@Param("scope") Scope scope, @Param("batchId") long batchId, @Param("locationId") long locationId,
                    @Param("drugId") long drugId, @Param("operator") String operator);
    int increaseStock(@Param("scope") Scope scope, @Param("id") long id, @Param("qty") int qty, @Param("operator") String operator);
    int insertFlow(@Param("scope") Scope scope, @Param("flow") FlowCommand flow);

    @Data class Warehouse { private Long id; private Integer status; }
    @Data class Batch { private Long id; private Long drugId; private String batchNo; private Integer qtyTotal; private BigDecimal costPrice; }
    @Data class Location { private Long id; private Integer status; private Long maxCapacity; }
    @Data class Stock { private Long id; private Long batchId; private Long locationId; private Long drugId; private Integer qty; private Integer qtyFrozen; }
    @Data class Flow { private Long batchId; private Long locationId; private Integer inQty; private Integer outQty; }
    @Data class FlowCommand {
        private long batchId; private long locationId; private long drugId; private String batchNo;
        private int inQty; private int outQty; private int balanceQty; private String bizNo; private long bizLineId;
        private long operator; private LocalDateTime flowTime; private BigDecimal unitCost;
    }
}
