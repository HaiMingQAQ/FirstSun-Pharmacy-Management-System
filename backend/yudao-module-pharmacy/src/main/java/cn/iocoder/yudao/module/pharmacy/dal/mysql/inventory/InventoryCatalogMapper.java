package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryCatalogMapper {
    Integer lockWarehouseStatus(@Param("scope") Scope scope, @Param("id") long id);
    int insertWarehouse(@Param("scope") Scope scope,
                        @Param("q") InventoryCatalogCreateReqVO.Warehouse request,
                        @Param("actor") String actor, @Param("key") GeneratedKey key);
    int insertLocation(@Param("scope") Scope scope,
                       @Param("q") InventoryCatalogCreateReqVO.Location request,
                       @Param("actor") String actor, @Param("key") GeneratedKey key);

    @Data
    class GeneratedKey {
        private Long id;
    }
}
