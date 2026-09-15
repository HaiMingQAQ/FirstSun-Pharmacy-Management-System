package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReceiveItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryFacadeMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryFacadeAdapter;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Opt-in local Docker MySQL test for ACC-20260915-001.
 *
 * <p>Every fixture is created inside one transaction and rolled back. It accepts only the
 * developer's loopback Docker URL supplied through environment variables, never a team or remote
 * database. Drug validity is mocked deliberately: this test verifies C's physical inventory
 * transaction after A's DrugApi boundary has accepted the drugs.</p>
 */
@EnabledIfEnvironmentVariable(named = "INVENTORY_FACADE_MYSQL_URL", matches = "jdbc:mysql://127\\.0\\.0\\.1:3307/firstsun_pharmacy.*")
class InventoryFacadeAdapterMysqlIT {

    private static final InventoryReadAccess.Scope SCOPE = new InventoryReadAccess.Scope(1L, 7L);
    private static InventoryFacadeAdapter facade;
    private static JdbcTemplate jdbc;
    private static TransactionTemplate transaction;

    @BeforeAll
    static void setUp() throws Exception {
        String url = System.getenv("INVENTORY_FACADE_MYSQL_URL");
        String username = System.getenv("INVENTORY_FACADE_MYSQL_USERNAME");
        String password = System.getenv("INVENTORY_FACADE_MYSQL_PASSWORD");
        if (blank(username) || blank(password)) {
            throw new IllegalArgumentException("本机 MySQL 测试缺少用户名或密码环境变量");
        }
        var dataSource = new DriverManagerDataSource(url, username, password);
        jdbc = new JdbcTemplate(dataSource);
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        var configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/inventory/*.xml"));
        var session = new SqlSessionTemplate(factory.getObject());
        var access = mock(InventoryReadAccess.class);
        when(access.requireScope(7L)).thenReturn(SCOPE);
        DrugApi drugApi = mock(DrugApi.class);
        doNothing().when(drugApi).validateDrugList(org.mockito.ArgumentMatchers.anyCollection());
        facade = new InventoryFacadeAdapter(access, session.getMapper(InventoryFacadeMapper.class), drugApi);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void login() {
        var user = new LoginUser();
        user.setId(407L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void receiveDeductReturnAreIdempotentAndKeepThreeLedgersConsistent() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        transaction.executeWithoutResult(status -> {
            long warehouseId = insertWarehouse("IT" + token);
            long locationId = insertLocation(warehouseId, "L" + token);
            ReceiveItem receive = ReceiveItem.builder()
                    .bizLineId("1001").storeId(7L).drugId(900000001L).batchNo("B" + token)
                    .manufactureDate(LocalDate.now().minusMonths(1)).expiryDate(LocalDate.now().plusYears(1))
                    .qty(10).unitPrice(new BigDecimal("12.50")).warehouseId(warehouseId).locationId(locationId).build();

            long batchId = facade.receive(7L, "RC" + token, List.of(receive)).getLines().get(0).getBatchId();
            assertEquals(batchId, facade.receive(7L, "RC" + token, List.of(receive)).getLines().get(0).getBatchId());
            assertLedger(batchId, locationId, 10, 10, 0, 0, 1);

            DeductItem deduct = new DeductItem();
            deduct.setBizNo("SO" + token); deduct.setBizLineId(2001L); deduct.setDrugId(900000001L); deduct.setQty(4);
            var firstDeduct = facade.deduct(7L, List.of(deduct));
            assertEquals(1, firstDeduct.getAllocations().size());
            assertEquals(batchId, firstDeduct.getAllocations().get(0).getBatchId());
            assertEquals(locationId, firstDeduct.getAllocations().get(0).getLocationId());
            assertEquals(4, firstDeduct.getAllocations().get(0).getQty());
            assertEquals(1, facade.deduct(7L, List.of(deduct)).getAllocations().size());
            assertLedger(batchId, locationId, 6, 6, 0, 4, 2);

            ReturnBackItem returned = new ReturnBackItem();
            returned.setBizNo("SR" + token); returned.setBizLineId(3001L); returned.setOriginalBizNo("SO" + token);
            returned.setOriginalBizLineId(2001L); returned.setDrugId(900000001L); returned.setBatchId(batchId);
            returned.setLocationId(locationId); returned.setQty(4);
            facade.returnBack(7L, List.of(returned));
            facade.returnBack(7L, List.of(returned));
            assertLedger(batchId, locationId, 10, 10, 0, 0, 3);

            returned.setBizNo("SRX" + token); returned.setBizLineId(3002L); returned.setQty(1);
            assertThrows(ServiceException.class, () -> facade.returnBack(7L, List.of(returned)));
            assertLedger(batchId, locationId, 10, 10, 0, 0, 3);
            status.setRollbackOnly();
        });
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ph_warehouse WHERE wh_code=?", Integer.class, "IT" + token));
    }

    private long insertWarehouse(String code) {
        jdbc.update("INSERT INTO ph_warehouse (tenant_id, store_id, wh_code, wh_name, temp_zone, status, creator, updater) "
                + "VALUES (1, 7, ?, '库存门面测试仓', 0, 1, '407', '407')", code);
        return jdbc.queryForObject("SELECT id FROM ph_warehouse WHERE tenant_id=1 AND store_id=7 AND wh_code=?", Long.class, code);
    }

    private long insertLocation(long warehouseId, String code) {
        jdbc.update("INSERT INTO ph_location (tenant_id, warehouse_id, location_code, location_type, max_capacity, status, creator, updater) "
                + "VALUES (1, ?, ?, 0, 100, 1, '407', '407')", warehouseId, code);
        return jdbc.queryForObject("SELECT id FROM ph_location WHERE warehouse_id=? AND location_code=?", Long.class, warehouseId, code);
    }

    private void assertLedger(long batchId, long locationId, int total, int available, int frozen, int sold, int flows) {
        Integer totalDb = jdbc.queryForObject("SELECT qty_total FROM ph_inv_batch WHERE id=?", Integer.class, batchId);
        Integer availableDb = jdbc.queryForObject("SELECT qty_avail FROM ph_inv_batch WHERE id=?", Integer.class, batchId);
        Integer frozenDb = jdbc.queryForObject("SELECT qty_frozen FROM ph_inv_batch WHERE id=?", Integer.class, batchId);
        Integer soldDb = jdbc.queryForObject("SELECT qty_sold FROM ph_inv_batch WHERE id=?", Integer.class, batchId);
        Integer stockDb = jdbc.queryForObject("SELECT qty FROM ph_inv_location_stock WHERE batch_id=? AND location_id=?", Integer.class, batchId, locationId);
        assertEquals(total, totalDb); assertEquals(available, availableDb); assertEquals(frozen, frozenDb); assertEquals(sold, soldDb);
        assertEquals(total, stockDb);
        assertEquals(total, available + frozen);
        assertEquals(flows, jdbc.queryForObject("SELECT COUNT(*) FROM ph_inv_flow WHERE batch_id=?", Integer.class, batchId));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
