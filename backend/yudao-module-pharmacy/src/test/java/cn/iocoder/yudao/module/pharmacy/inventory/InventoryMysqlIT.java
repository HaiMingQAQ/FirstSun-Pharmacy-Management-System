package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogUpdateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadQuery;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.*;
import cn.iocoder.yudao.module.pharmacy.service.inventory.*;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Opt-in real MySQL persistence tests. Never connects without a dedicated local config.
 * Authentication and A employee lookup are deliberately outside this persistence test scope.
 * Every run creates a NEW schema in a verified dedicated data directory; no DROP/TRUNCATE.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "inventory.mysql.config", matches = ".+")
class InventoryMysqlIT {
    private final InventoryReadAccess.Scope scope = new InventoryReadAccess.Scope(1, 7);
    private InventoryCatalogService creates;
    private InventoryCatalogUpdateService updates;
    private InventoryReadMapper reads;
    private InventoryCatalogUpdateMapper edits;
    private TransactionTemplate transaction;
    private JdbcTemplate jdbc;

    @BeforeAll void initializeDedicatedSchema() throws Exception {
        Path module = Path.of(System.getProperty("basedir")).toRealPath();
        Path dedicatedRoot = module.resolve("target/inventory-mysql").toRealPath();
        if (!dedicatedRoot.equals(module.resolve("target/inventory-mysql").normalize())) {
            throw new IllegalArgumentException("C 测试目录不能通过链接重定向到其他目录");
        }
        Path configPath = Path.of(System.getProperty("inventory.mysql.config")).toRealPath();
        if (!configPath.startsWith(dedicatedRoot)) throw new IllegalArgumentException("配置必须位于 C 专属测试目录");
        var properties = new Properties();
        try (var reader = java.nio.file.Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        if (!"C-local-disposable".equals(properties.getProperty("owner"))) {
            throw new IllegalArgumentException("缺少 C 独立测试实例归属标记");
        }
        int port = Integer.parseInt(properties.getProperty("port"));
        if (port < 1024 || port > 65535 || port == 3306 || port == 3307) {
            throw new IllegalArgumentException("禁止使用默认/团队数据库端口");
        }
        Path expectedData = Path.of(properties.getProperty("dataDir")).toRealPath();
        if (!expectedData.startsWith(dedicatedRoot) || expectedData.equals(dedicatedRoot)) {
            throw new IllegalArgumentException("数据目录必须位于 C 专属测试目录内");
        }
        String user = properties.getProperty("user");
        String password = properties.getProperty("password");
        if (user == null || password == null || password.isBlank()) throw new IllegalArgumentException("缺少独立实例凭据");
        String host = "jdbc:mysql://127.0.0.1:" + port + "/";
        String options = "?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000";
        String schema = "c_inventory_it_" + UUID.randomUUID().toString().replace("-", "");
        // First connection performs only the data-directory ownership check before any mutation.
        try (var connection = DriverManager.getConnection(host + "mysql" + options, user, password);
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT @@datadir, @@version")) {
                assertTrue(result.next());
                var version = java.util.regex.Pattern.compile("^8\\.(\\d+)\\.(\\d+)").matcher(result.getString(2));
                boolean supported = version.find() && (Integer.parseInt(version.group(1)) > 0
                        || Integer.parseInt(version.group(2)) >= 28);
                if (!Path.of(result.getString(1)).toRealPath().equals(expectedData)
                        || !supported) {
                    throw new IllegalStateException("数据库实例目录或版本与 C 独立配置不符，拒绝写入");
                }
            }
            statement.executeUpdate("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        var dataSource = new DriverManagerDataSource(host + schema + options, user, password);
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new FileSystemResource(module.getParent().getParent().resolve("sql/firstsun_pharmacy_init.sql")));
        }
        jdbc = new JdbcTemplate(dataSource);
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        var configuration = new Configuration(); configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/inventory/*.xml"));
        var session = new SqlSessionTemplate(java.util.Objects.requireNonNull(factory.getObject()));
        reads = session.getMapper(InventoryReadMapper.class);
        edits = session.getMapper(InventoryCatalogUpdateMapper.class);
        var access = mock(InventoryReadAccess.class);
        when(access.requireScope(null)).thenReturn(scope);
        creates = new InventoryCatalogService(access, session.getMapper(InventoryCatalogMapper.class));
        updates = new InventoryCatalogUpdateService(access, edits);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setTimeout(15);
    }

    @BeforeEach void login() {
        var user = new LoginUser(); user.setId(11L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    @AfterEach void clearLogin() { SecurityContextHolder.clearContext(); }
    private InventoryCatalogCreateReqVO.Warehouse warehouseRequest() {
        var request = new InventoryCatalogCreateReqVO.Warehouse();
        request.setWhCode("T" + UUID.randomUUID().toString().substring(0, 12));
        request.setWhName("独立测试仓"); request.setTempZone(0); return request;
    }
    private long createWarehouse() { return transaction.execute(status -> creates.createWarehouse(warehouseRequest())); }
    private InventoryCatalogUpdateReqVO.Warehouse rename(long id, String name) {
        var row = reads.selectWarehouse(scope, id);
        var old = new InventoryCatalogUpdateReqVO.WarehouseFields();
        old.setWhCode(row.getWhCode()); old.setWhName(row.getWhName()); old.setTempZone(row.getTempZone()); old.setStatus(row.getStatus());
        var value = new InventoryCatalogUpdateReqVO.WarehouseFields();
        value.setWhCode(row.getWhCode()); value.setWhName(name); value.setTempZone(row.getTempZone()); value.setStatus(row.getStatus());
        var request = new InventoryCatalogUpdateReqVO.Warehouse();
        request.setId(id); request.setExpected(old); request.setValue(value); return request;
    }

    @Test void generatedIdsUnsignedCapacityAndStoreScopeUseActualSql() {
        long warehouse = createWarehouse();
        var request = new InventoryCatalogCreateReqVO.Location();
        request.setWarehouseId(warehouse); request.setLocationCode("L01"); request.setLocationType(0);
        request.setMaxCapacity(4294967295L);
        long location = transaction.execute(status -> creates.createLocation(request));
        var query = new InventoryReadQuery(); query.setWarehouseId(warehouse); query.setLocationId(location);
        var rows = reads.selectLocations(scope, query);
        assertEquals(1, rows.size()); assertEquals(4294967295L, rows.get(0).getMaxCapacity());
        assertNull(reads.selectWarehouse(new InventoryReadAccess.Scope(1, 8), warehouse));
        assertNull(reads.selectWarehouse(new InventoryReadAccess.Scope(2, 7), warehouse));
        assertEquals("11", jdbc.queryForObject("SELECT creator FROM ph_location WHERE id=?", String.class, location));
    }

    @Test void uniqueCodeIncludesDeletedRowsAndDoesNotCreateDuplicate() {
        var request = warehouseRequest();
        long id = transaction.execute(status -> creates.createWarehouse(request));
        assertThrows(ServiceException.class, () -> transaction.execute(status -> creates.createWarehouse(request)));
        jdbc.update("UPDATE ph_warehouse SET deleted=b'1' WHERE id=?", id);
        assertThrows(ServiceException.class, () -> transaction.execute(status -> creates.createWarehouse(request)));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ph_warehouse WHERE wh_code=? AND tenant_id=1 AND store_id=7", Integer.class, request.getWhCode()));
    }

    @Test void exceptionAfterRealInsertRollsBack() {
        var request = warehouseRequest();
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            creates.createWarehouse(request);
            throw new IllegalStateException("deliberate rollback verification");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ph_warehouse WHERE wh_code=?", Integer.class, request.getWhCode()));
    }

    @Test void exceptionAfterRealUpdateRestoresOldValues() {
        long id = createWarehouse(); var request = rename(id, "应回滚");
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            updates.updateWarehouse(request); throw new IllegalStateException("deliberate rollback verification");
        }));
        assertEquals(request.getExpected().getWhName(), reads.selectWarehouse(scope, id).getWhName());
    }

    @Test void concurrentEditorActuallyWaitsForLockThenRejectsStaleSnapshot() throws Exception {
        long id = createWarehouse(); var first = rename(id, "首个修改"); var second = rename(id, "旧页面修改");
        var executor = Executors.newSingleThreadExecutor();
        var connectionId = new AtomicLong();
        var ready = new CountDownLatch(1);
        var future = new java.util.concurrent.atomic.AtomicReference<Future<ServiceException>>();
        try {
            transaction.executeWithoutResult(status -> {
                edits.lockWarehouse(scope, id);
                future.set(executor.submit(() -> {
                    login();
                    try {
                        transaction.executeWithoutResult(tx -> {
                            connectionId.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                            ready.countDown(); updates.updateWarehouse(second);
                        });
                        return null;
                    } catch (ServiceException error) { return error; }
                    finally { clearLogin(); }
                }));
                assertTimeoutPreemptively(Duration.ofSeconds(8), () -> {
                    assertTrue(ready.await(3, TimeUnit.SECONDS));
                    while (jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w "
                            + "JOIN performance_schema.threads t ON t.THREAD_ID=w.REQUESTING_THREAD_ID "
                            + "WHERE t.PROCESSLIST_ID=?", Integer.class, connectionId.get()) == 0) {
                        Thread.sleep(30);
                    }
                });
                updates.updateWarehouse(first);
            });
            assertNotNull(future.get().get(10, TimeUnit.SECONDS));
            assertEquals("首个修改", reads.selectWarehouse(scope, id).getWhName());
        } finally { executor.shutdownNow(); }
    }
}
