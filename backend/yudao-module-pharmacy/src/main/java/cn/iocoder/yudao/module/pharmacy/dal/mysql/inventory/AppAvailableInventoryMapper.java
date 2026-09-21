package cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo.AppInventoryAvailableRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/** Anonymous catalogue projection; never exposes batch, cost or internal location data. */
@Mapper
public interface AppAvailableInventoryMapper {
    @Select("""
        <script>
        SELECT b.drug_id, SUM(GREATEST(s.qty - s.qty_frozen, 0)) AS qty_avail
        FROM ph_inv_location_stock s
        JOIN ph_inv_batch b ON b.id = s.batch_id AND b.tenant_id = s.tenant_id
        JOIN ph_location l ON l.id = s.location_id AND l.tenant_id = s.tenant_id AND l.warehouse_id = b.warehouse_id
        JOIN ph_warehouse w ON w.id = b.warehouse_id AND w.tenant_id = b.tenant_id AND w.store_id = b.store_id
        JOIN ph_store st ON st.id = b.store_id AND st.tenant_id = b.tenant_id
        JOIN ph_drug d ON d.id = b.drug_id AND d.tenant_id = b.tenant_id
        WHERE b.tenant_id = #{tenantId} AND b.store_id = #{storeId}
          AND s.deleted = 0 AND b.deleted = 0 AND l.deleted = 0 AND w.deleted = 0 AND st.deleted = 0 AND d.deleted = 0
          AND st.status = 1 AND w.status = 1 AND l.status = 1
          AND d.status = 1 AND d.approve_status = 1 AND d.saleable_online = 1
          AND b.quality_status = 0 AND b.expiry_date &gt;= CURRENT_DATE
          AND b.drug_id IN
          <foreach collection="drugIds" item="id" open="(" separator="," close=")">#{id}</foreach>
        GROUP BY b.drug_id
        </script>
        """)
    List<AppInventoryAvailableRespVO> selectAvailable(@Param("tenantId") Long tenantId,
            @Param("storeId") Long storeId, @Param("drugIds") List<Long> drugIds);
}
