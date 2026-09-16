package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryMovementReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryMovementMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryMovementService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Opt-in, rollback-only local Docker MySQL test for same-warehouse movement. */
@EnabledIfEnvironmentVariable(named = "INVENTORY_FACADE_MYSQL_URL", matches = "jdbc:mysql://127\\.0\\.0\\.1:3307/firstsun_pharmacy.*")
class InventoryMovementMysqlIT {
    private static final InventoryReadAccess.Scope SCOPE = new InventoryReadAccess.Scope(1L, 7L);
    private static JdbcTemplate jdbc;
    private static TransactionTemplate transaction;
    private static InventoryMovementService service;

    @BeforeAll static void setup() throws Exception {
        var dataSource = new DriverManagerDataSource(System.getenv("INVENTORY_FACADE_MYSQL_URL"),
                System.getenv("INVENTORY_FACADE_MYSQL_USERNAME"), System.getenv("INVENTORY_FACADE_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(dataSource);
        var factory = new SqlSessionFactoryBean(); factory.setDataSource(dataSource);
        var configuration = new Configuration(); configuration.setMapUnderscoreToCamelCase(true); factory.setConfiguration(configuration);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/inventory/*.xml"));
        var session = new SqlSessionTemplate(factory.getObject());
        var access = mock(InventoryReadAccess.class); when(access.requireScope(null)).thenReturn(SCOPE);
        service = new InventoryMovementService(access, session.getMapper(InventoryMovementMapper.class));
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }
    @BeforeEach void login() {
        var user = new LoginUser(); user.setId(407L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    @AfterEach void clearLogin() { SecurityContextHolder.clearContext(); }

    @Test void movesWithPairedFlowsAndReplayKeepsAllLedgers() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        transaction.executeWithoutResult(status -> {
            long warehouse = warehouse("MW" + token);
            long source = location(warehouse, "MS" + token, 10);
            long target = location(warehouse, "MT" + token, 10);
            long batch = batch(warehouse, "MB" + token);
            stock(batch, source, 8, 2);
            var request = request(warehouse, batch, source, target, "MV" + token, 4);
            service.execute(request);
            service.execute(request);
            assertEquals(10, intValue("SELECT qty_total FROM ph_inv_batch WHERE id=?", batch));
            assertEquals(8, intValue("SELECT qty_avail FROM ph_inv_batch WHERE id=?", batch));
            assertEquals(2, intValue("SELECT qty_frozen FROM ph_inv_batch WHERE id=?", batch));
            assertEquals(4, intValue("SELECT qty FROM ph_inv_location_stock WHERE batch_id=? AND location_id=?", batch, source));
            assertEquals(4, intValue("SELECT qty FROM ph_inv_location_stock WHERE batch_id=? AND location_id=?", batch, target));
            assertEquals(2, intValue("SELECT COUNT(*) FROM ph_inv_flow WHERE batch_id=? AND flow_type=60", batch));
            status.setRollbackOnly();
        });
        assertEquals(0, intValue("SELECT COUNT(*) FROM ph_warehouse WHERE wh_code=?", "MW" + token));
    }

    private long warehouse(String code) {
        jdbc.update("INSERT INTO ph_warehouse (tenant_id, store_id, wh_code, wh_name, temp_zone, status, creator, updater) VALUES (1,7,?,'移位测试仓',0,1,'407','407')", code);
        return jdbc.queryForObject("SELECT id FROM ph_warehouse WHERE wh_code=?", Long.class, code);
    }
    private long location(long warehouse, String code, int capacity) {
        jdbc.update("INSERT INTO ph_location (tenant_id, warehouse_id, location_code, location_type, max_capacity, status, creator, updater) VALUES (1,?,?,0,?,1,'407','407')", warehouse, code, capacity);
        return jdbc.queryForObject("SELECT id FROM ph_location WHERE warehouse_id=? AND location_code=?", Long.class, warehouse, code);
    }
    private long batch(long warehouse, String batchNo) {
        jdbc.update("INSERT INTO ph_inv_batch (tenant_id,store_id,warehouse_id,drug_id,batch_no,expiry_date,qty_total,qty_avail,qty_frozen,qty_sold,quality_status,expiry_status,cost_price,version,creator,updater) VALUES (1,7,?,900000002,?, ?,10,8,2,0,0,0,1,0,'407','407')", warehouse, batchNo, LocalDate.now().plusYears(1));
        return jdbc.queryForObject("SELECT id FROM ph_inv_batch WHERE warehouse_id=? AND batch_no=?", Long.class, warehouse, batchNo);
    }
    private void stock(long batch, long location, int qty, int frozen) {
        jdbc.update("INSERT INTO ph_inv_location_stock (tenant_id,batch_id,location_id,drug_id,qty,qty_frozen,creator,updater) VALUES (1,?,?,900000002,?,?,'407','407')", batch, location, qty, frozen);
    }
    private static InventoryMovementReqVO request(long warehouse, long batch, long source, long target, String no, int qty) {
        var line = new InventoryMovementReqVO.Line(); line.setBizLineId(1L); line.setBatchId(batch); line.setSourceLocationId(source); line.setTargetLocationId(target); line.setQty(qty);
        var request = new InventoryMovementReqVO(); request.setWarehouseId(warehouse); request.setBizNo(no); request.setLines(List.of(line)); return request;
    }
    private int intValue(String sql, Object... params) { return jdbc.queryForObject(sql, Integer.class, params); }
}
