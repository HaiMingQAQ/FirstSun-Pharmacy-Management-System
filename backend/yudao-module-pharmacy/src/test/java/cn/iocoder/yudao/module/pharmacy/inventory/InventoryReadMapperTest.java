package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadQuery;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryReadMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Parses real mapper XML and resolves bound parameters; not SQL execution or a MySQL test. */
class InventoryReadMapperTest {
    private Configuration configuration() throws Exception {
        var configuration = new Configuration();
        String resource = "mapper/inventory/InventoryReadMapper.xml";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    @Test void everyStatementBindsRealScopeAndFilters() throws Exception {
        var configuration = configuration();
        var query = new InventoryReadQuery();
        query.setCode("x%' OR 1=1 --");
        query.setName("中心");
        query.setStatus(1);
        query.setWarehouseId(3L);
        query.setBatchId(4L);
        query.setLocationId(5L);
        query.setDrugId(6L);
        query.setBatchNo("B-001");
        query.setBizNo("SALE-001");
        query.setQualityStatus(0);
        query.setFlowId(10L);
        query.setFlowType(20);
        query.setBizType(2);
        query.setExpiryFrom(java.time.LocalDate.of(2026, 1, 1));
        query.setExpiryTo(java.time.LocalDate.of(2027, 1, 1));
        query.setFlowFrom(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        query.setFlowBefore(java.time.LocalDateTime.of(2027, 1, 1, 0, 0));
        query.setPageNo(2);
        Map<String, Object> parameters = Map.of("scope", new Scope(1, 2), "q", query, "id", 9L);
        for (var method : InventoryReadMapper.class.getDeclaredMethods()) {
            var statement = configuration.getMappedStatement(InventoryReadMapper.class.getName() + "." + method.getName());
            var bound = statement.getBoundSql(parameters);
            assertTrue(bound.getSql().contains("tenant_id"));
            assertTrue(bound.getSql().contains("store_id"));
            assertFalse(bound.getSql().contains(query.getCode()), "search must remain a bound parameter");
            for (var mapping : bound.getParameterMappings()) {
                assertNotNull(configuration.newMetaObject(parameters).getValue(mapping.getProperty()), mapping.getProperty());
            }
        }
    }

    @Test void unsetFiltersStillRetainScopeAndPagination() throws Exception {
        var configuration = configuration();
        var query = new InventoryReadQuery();
        var bound = configuration.getMappedStatement(InventoryReadMapper.class.getName() + ".selectWarehouses")
                .getBoundSql(Map.of("scope", new Scope(1, 2), "q", query));
        assertFalse(bound.getSql().contains("LOCATE"));
        assertTrue(bound.getSql().contains("OFFSET ?"));
        assertEquals(4, bound.getParameterMappings().size());
        assertEquals(0, query.getOffset());
    }
}
