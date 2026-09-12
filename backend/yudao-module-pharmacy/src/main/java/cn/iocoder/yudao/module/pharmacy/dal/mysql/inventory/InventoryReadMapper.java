package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** All SQL scopes every participating table explicitly; this mapper has no mutation methods. */
@Mapper
public interface InventoryReadMapper {
    long countWarehouses(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
    List<InventoryReadVO.Warehouse> selectWarehouses(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
    InventoryReadVO.Warehouse selectWarehouse(@Param("scope") Scope scope, @Param("id") long id);
    long countLocations(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
    List<InventoryReadVO.Location> selectLocations(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
    long countBatches(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
    List<InventoryReadVO.Batch> selectBatches(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
    long countLocationStock(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
    List<InventoryReadVO.LocationStock> selectLocationStock(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
    long countFlows(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
    List<InventoryReadVO.Flow> selectFlows(@Param("scope") Scope scope, @Param("q") InventoryReadQuery query);
}
