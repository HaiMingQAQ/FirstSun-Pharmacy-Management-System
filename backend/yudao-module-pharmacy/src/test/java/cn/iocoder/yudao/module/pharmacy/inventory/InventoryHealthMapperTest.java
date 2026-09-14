package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryExpiryQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReconciliationQuery;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryDamageMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryExpiryMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryReconciliationMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryStocktakeMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess.Scope;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** XML binding checks only; no expiry writes or reconciliation queries reach a database. */
class InventoryHealthMapperTest {
    private Configuration configuration(String resource) throws Exception {
        var configuration = new Configuration();
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    @Test
    void expiryStatementsKeepStoreScopeAndBoundFilters() throws Exception {
        var config = configuration("mapper/inventory/InventoryExpiryMapper.xml");
        var query = new InventoryExpiryQuery();
        query.setWarehouseId(3L);
        query.setDrugId(8L);
        query.setBatchId(9L);
        query.setAlertLevel(1);
        query.setHandleType(0);
        query.setAlertDate(LocalDate.of(2026, 9, 12));
        query.setPageNo(2);
        var params = Map.of("scope", new Scope(1, 7), "q", query, "id", 9L,
                "alertDate", query.getAlertDate(), "actor", "operator", "handleType", 1,
                "operator", 11L);
        var page = config.getMappedStatement(InventoryExpiryMapper.class.getName() + ".selectAlerts")
                .getBoundSql(params);
        assertTrue(page.getSql().contains("tenant_id"));
        assertTrue(page.getSql().contains("store_id"));
        assertTrue(page.getSql().contains("OFFSET ?"));
        assertFalse(page.getSql().contains("3L"));
        var refresh = config.getMappedStatement(InventoryExpiryMapper.class.getName() + ".refreshDaily")
                .getBoundSql(params);
        assertTrue(refresh.getSql().contains("ON DUPLICATE KEY UPDATE"));
        assertTrue(refresh.getSql().contains("DATEDIFF"));
    }

    @Test
    void reconciliationIncludesAllFourLedgersAndOptionalDifferenceFilter() throws Exception {
        var config = configuration("mapper/inventory/InventoryReconciliationMapper.xml");
        var query = new InventoryReconciliationQuery();
        query.setWarehouseId(3L);
        query.setDrugId(8L);
        query.setOnlyDifference(true);
        query.setPageNo(1);
        var bound = config.getMappedStatement(InventoryReconciliationMapper.class.getName() + ".selectRows")
                .getBoundSql(Map.of("scope", new Scope(1, 7), "q", query));
        String sql = bound.getSql();
        assertTrue(sql.contains("ph_inv_location_stock"));
        assertTrue(sql.contains("ph_inv_flow"));
        assertTrue(sql.contains("ph_inv_lock"));
        assertTrue(sql.contains("opening_flow_count"));
        assertTrue(sql.contains("<>"));
        assertTrue(sql.contains("LIMIT ? OFFSET ?"));
    }

    @Test
    void stocktakeXmlBindsSnapshotAndAdjustmentStatements() throws Exception {
        var config = configuration("mapper/inventory/InventoryStocktakeMapper.xml");
        var scope = new Scope(1, 7);
        var query = Map.of("warehouseId", 3L, "status", 1, "pageSize", 10, "offset", 0);
        var page = config.getMappedStatement(InventoryStocktakeMapper.class.getName() + ".selectPage")
                .getBoundSql(Map.of("scope", scope, "q", query));
        assertTrue(page.getSql().contains("ph_inv_stocktake"));
        assertTrue(page.getSql().contains("tenant_id"));
        assertTrue(page.getSql().contains("LIMIT ? OFFSET ?"));
        var adjustment = config.getMappedStatement(InventoryStocktakeMapper.class.getName() + ".selectAdjustmentLines")
                .getBoundSql(Map.of("scope", scope, "id", 12L));
        assertTrue(adjustment.getSql().contains("ph_inv_stocktake_line"));
        assertTrue(adjustment.getSql().contains("ph_inv_batch"));
        assertTrue(adjustment.getSql().contains("tenant_id"));
        var location = config.getMappedStatement(InventoryStocktakeMapper.class.getName() + ".lockLocation")
                .getBoundSql(Map.of("scope", scope, "warehouseId", 3L, "id", 8L));
        assertTrue(location.getSql().contains("ph_warehouse"));
        assertTrue(location.getSql().contains("store_id"));
    }

    @Test
    void damageXmlBindsPageAndExecuteStatements() throws Exception {
        var config = configuration("mapper/inventory/InventoryDamageMapper.xml");
        var scope = new Scope(1, 7);
        var query = Map.of("warehouseId", 3L, "status", 1, "damageType", 0,
                "pageSize", 10, "offset", 0);
        var page = config.getMappedStatement(InventoryDamageMapper.class.getName() + ".selectPage")
                .getBoundSql(Map.of("scope", scope, "q", query));
        assertTrue(page.getSql().contains("ph_inv_damage"));
        assertTrue(page.getSql().contains("ph_inv_batch"));
        assertTrue(page.getSql().contains("LIMIT ? OFFSET ?"));
        var execute = config.getMappedStatement(InventoryDamageMapper.class.getName() + ".selectExecuteLines")
                .getBoundSql(Map.of("scope", scope, "id", 12L));
        assertTrue(execute.getSql().contains("damage_no"));
        assertTrue(execute.getSql().contains("ph_inv_damage_line"));
        assertTrue(execute.getSql().contains("tenant_id"));
    }
}
