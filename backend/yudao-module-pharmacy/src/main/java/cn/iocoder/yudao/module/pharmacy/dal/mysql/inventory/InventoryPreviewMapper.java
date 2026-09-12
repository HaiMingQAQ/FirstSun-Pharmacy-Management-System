package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InventoryPreviewMapper {
    List<Row> selectSnapshot(@Param("scope") Scope scope, @Param("warehouseId") long warehouseId,
                             @Param("drugId") long drugId);

    @Data
    class Row {
        private Long batchId;
        private LocalDate expiryDate;
        private Integer qualityStatus;
        private Integer qtyTotal;
        private Integer qtyAvail;
        private Integer qtyFrozen;
        private Integer qtySold;
        private Long stockId;
        private Long locationId;
        private Integer locationStatus;
        private Integer quantity;
        private Integer frozen;
        private Integer validLocation;
    }
}
