package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryDamageCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryDamageQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryDamageVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface InventoryDamageMapper {
    long count(@Param("scope") Scope scope, @Param("q") InventoryDamageQuery query);
    List<InventoryDamageVO.Summary> selectPage(@Param("scope") Scope scope,
                                                @Param("q") InventoryDamageQuery query);
    InventoryDamageVO.Summary selectSummary(@Param("scope") Scope scope, @Param("id") long id);
    List<InventoryDamageVO.Line> selectLines(@Param("scope") Scope scope, @Param("id") long id);
    Header lockHeader(@Param("scope") Scope scope, @Param("id") long id);
    int countLines(@Param("scope") Scope scope, @Param("id") long id);
    int insertHeader(@Param("scope") Scope scope, @Param("q") InventoryDamageCreateReqVO request,
                     @Param("damageNo") String damageNo, @Param("operator") long operator,
                     @Param("key") GeneratedKey key);
    int insertLine(@Param("scope") Scope scope, @Param("damageId") long damageId,
                   @Param("line") InventoryDamageCreateReqVO.Line line, @Param("batch") BatchLock batch,
                   @Param("amount") BigDecimal amount, @Param("operator") long operator);
    int updateTotals(@Param("scope") Scope scope, @Param("id") long id, @Param("operator") long operator);
    BatchLock lockBatch(@Param("scope") Scope scope, @Param("id") long id);
    StockLock lockStock(@Param("scope") Scope scope, @Param("batchId") long batchId,
                        @Param("locationId") long locationId);
    List<ExecuteLine> selectExecuteLines(@Param("scope") Scope scope, @Param("id") long id);
    int updateStatus(@Param("scope") Scope scope, @Param("id") long id,
                     @Param("from") int from, @Param("to") int to, @Param("operator") long operator);
    int approve(@Param("scope") Scope scope, @Param("id") long id, @Param("operator") long operator);
    int reject(@Param("scope") Scope scope, @Param("id") long id, @Param("operator") long operator);
    int review(@Param("scope") Scope scope, @Param("id") long id, @Param("operator") long operator);
    int updateStock(@Param("scope") Scope scope, @Param("id") long id,
                    @Param("qty") int qty, @Param("operator") long operator);
    int updateBatch(@Param("scope") Scope scope, @Param("id") long id,
                    @Param("qty") int qty, @Param("operator") long operator);
    int insertFlow(@Param("scope") Scope scope, @Param("line") ExecuteLine line,
                   @Param("balance") int balance, @Param("operator") long operator,
                   @Param("flowTime") java.time.LocalDateTime flowTime);
    int execute(@Param("scope") Scope scope, @Param("id") long id, @Param("operator") long operator);
    int cancelDraft(@Param("scope") Scope scope, @Param("id") long id, @Param("operator") long operator);

    @Data class GeneratedKey { private Long id; }
    @Data class Header {
        private Long id; private String damageNo; private Long storeId; private Integer damageType;
        private Integer status; private Long auditBy; private Long reviewBy;
    }
    @Data class BatchLock {
        private Long id; private Long storeId; private Long warehouseId; private Long drugId;
        private String batchNo; private Integer qtyTotal; private Integer qtyAvail; private Integer qtyFrozen;
        private BigDecimal costPrice;
    }
    @Data class StockLock {
        private Long id; private Long batchId; private Long locationId; private Integer qty; private Integer qtyFrozen;
    }
    @Data class ExecuteLine {
        private Long id; private Long damageId; private Long batchId; private Long locationId;
        private Long drugId; private String batchNo; private String damageNo; private Integer qty; private BigDecimal costPrice;
    }
}
