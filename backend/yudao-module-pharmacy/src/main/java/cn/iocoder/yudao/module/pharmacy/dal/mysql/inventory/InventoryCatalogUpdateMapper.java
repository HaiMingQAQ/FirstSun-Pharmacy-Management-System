package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogUpdateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryCatalogUpdateMapper {
    InventoryReadVO.Warehouse lockWarehouse(@Param("scope") Scope scope, @Param("id") long id);
    InventoryReadVO.Location lockLocation(@Param("scope") Scope scope, @Param("warehouseId") long warehouseId,
                                         @Param("id") long id);
    boolean hasWarehouseStock(@Param("scope") Scope scope, @Param("id") long id);
    boolean hasOpenWork(@Param("scope") Scope scope, @Param("id") long id);
    Usage locationUsage(@Param("scope") Scope scope, @Param("warehouseId") long warehouseId, @Param("id") long id);
    int updateWarehouse(@Param("scope") Scope scope, @Param("q") InventoryCatalogUpdateReqVO.Warehouse request,
                        @Param("actor") String actor);
    int updateLocation(@Param("scope") Scope scope, @Param("q") InventoryCatalogUpdateReqVO.Location request,
                       @Param("actor") String actor);

    @Data
    class Usage {
        private Long quantity;
        private Long invalidRows;
    }
}
