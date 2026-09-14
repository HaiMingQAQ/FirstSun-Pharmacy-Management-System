package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReconciliationQuery;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import lombok.Data;

import java.util.List;

@Mapper
public interface InventoryReconciliationMapper {
    long countRows(@Param("scope") Scope scope, @Param("q") InventoryReconciliationQuery query);
    List<Row> selectRows(@Param("scope") Scope scope, @Param("q") InventoryReconciliationQuery query);

    @Data
    class Row {
        private Long batchId;
        private Long warehouseId;
        private Long drugId;
        private String batchNo;
        private Long batchTotal;
        private Long batchAvail;
        private Long batchFrozen;
        private Long locationTotal;
        private Long locationFrozen;
        private Long invalidLocationRows;
        private Long flowNet;
        private Long openingFlowCount;
        private Long activeLockQty;
        private Long lastFlowId;
    }
}
