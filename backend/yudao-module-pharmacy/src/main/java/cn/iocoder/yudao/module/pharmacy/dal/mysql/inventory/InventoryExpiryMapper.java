package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryExpiryQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryExpiryVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InventoryExpiryMapper {
    long countAlerts(@Param("scope") Scope scope, @Param("q") InventoryExpiryQuery query);
    List<InventoryExpiryVO.Alert> selectAlerts(@Param("scope") Scope scope,
                                                @Param("q") InventoryExpiryQuery query);
    int refreshDaily(@Param("scope") Scope scope, @Param("alertDate") LocalDate alertDate,
                     @Param("actor") String actor);
    Integer selectHandleType(@Param("scope") Scope scope, @Param("id") long id);
    int updateHandle(@Param("scope") Scope scope, @Param("id") long id,
                     @Param("handleType") int handleType, @Param("operator") long operator);
}
