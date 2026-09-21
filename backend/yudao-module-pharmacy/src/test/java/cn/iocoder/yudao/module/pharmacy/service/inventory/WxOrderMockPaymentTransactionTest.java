package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pay.dal.dataobject.app.PayAppDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.order.PayOrderDO;
import cn.iocoder.yudao.module.pay.enums.PayChannelEnum;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.service.app.PayAppService;
import cn.iocoder.yudao.module.pay.service.order.PayOrderService;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductResult;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.*;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.*;
import cn.iocoder.yudao.module.pharmacy.service.member.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Spring transactions + H2 row locks; payment provider and stock worker are JDBC-backed test doubles.
 * Exercises the production orchestration/authorization, NOT MySQL, payment-module integration or HTTP. */
class WxOrderMockPaymentTransactionTest {
    JdbcTemplate db;
    DataSourceTransactionManager transactions;
    WxOrderService service;
    PayOrderService payments;
    InventoryFacadeAdapter worker;
    InventoryFacade employeeInventory;
    MemberPointSettlementService points;
    WxOrderLineAllocMapper allocations;
    volatile boolean failStock;
    volatile boolean failAllocation;

    @BeforeAll static void metadata() {
        for (var entity : List.of(WxOrderDO.class, WxOrderLineDO.class, WxOrderLineAllocDO.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity);
    }
    @BeforeEach void setup() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        db = new JdbcTemplate(source); transactions = new DataSourceTransactionManager(source);
        db.execute("CREATE TABLE ph_wx_order(id BIGINT PRIMARY KEY, tenant_id BIGINT, deleted INT, member_id BIGINT,"
                + " store_id BIGINT, order_no VARCHAR(50), payable_amount DECIMAL(12,2), pay_no VARCHAR(50),"
                + " pay_status INT, status INT, expire_at TIMESTAMP)");
        db.execute("CREATE TABLE pay_order(id BIGINT PRIMARY KEY, tenant_id BIGINT, deleted INT, app_id BIGINT,"
                + " user_id BIGINT, user_type INT, merchant_order_id VARCHAR(50), price INT, status INT,"
                + " channel_code VARCHAR(50), success_time TIMESTAMP, refund_price INT)");
        db.execute("CREATE TABLE stock(id INT PRIMARY KEY, qty INT, frozen INT)");
        db.execute("CREATE TABLE effects(kind VARCHAR(30))");
        db.update("INSERT INTO ph_wx_order VALUES(100,7,0,1,4,'WX100',12.34,NULL,0,0,?)", LocalDateTime.now().plusHours(1));
        db.update("INSERT INTO stock VALUES(1,10,0)");
        var locks = mock(WxOrderPaymentMapper.class);
        // Execute the production mapper's exact explicit tenant/deleted predicates and FOR UPDATE.
        String orderSql = sql("lockOrder"), paymentSql = sql("lockPayment");
        when(locks.lockOrder(anyLong(), anyLong())).thenAnswer(a -> db.query(orderSql,
                new BeanPropertyRowMapper<>(WxOrderDO.class), a.getArgument(0), a.getArgument(1))
                .stream().findFirst().orElse(null));
        when(locks.lockPayment(anyLong(), anyLong())).thenAnswer(a -> db.query(paymentSql,
                new BeanPropertyRowMapper<>(PayOrderDO.class), a.getArgument(0), a.getArgument(1))
                .stream().findFirst().orElse(null));
        var apps = mock(PayAppService.class); var app = new PayAppDO(); app.setId(9L);
        when(apps.validPayApp("firstsun")).thenReturn(app);
        var access = new WxOrderPaymentAccess(locks, apps);
        ReflectionTestUtils.setField(access, "appKey", "firstsun");
        access = transactional(access);
        var orders = mock(WxOrderMapper.class);
        when(orders.update(any(), any())).thenAnswer(a -> {
            WxOrderDO update = a.getArgument(0);
            return db.update("UPDATE ph_wx_order SET pay_no=?,pay_status=?,status=? WHERE id=100 AND status=0",
                    update.getPayNo(), update.getPayStatus(), update.getStatus());
        });
        payments = mock(PayOrderService.class);
        when(payments.createOrder(any())).thenAnswer(a -> {
            cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO req = a.getArgument(0);
            assertEquals(1234, req.getPrice()); assertEquals(1L, req.getUserId());
            assertEquals(UserTypeEnum.MEMBER.getValue(), req.getUserType());
            db.update("INSERT INTO pay_order VALUES(50,7,0,9,?,?,?, ?,0,NULL,NULL,0)",
                    req.getUserId(), req.getUserType(), req.getMerchantOrderId(), req.getPrice());
            return 50L;
        });
        when(payments.submitOrder(any(), anyString())).thenAnswer(a -> {
            cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO req = a.getArgument(0);
            assertEquals(PayChannelEnum.MOCK.getCode(), req.getChannelCode()); assertEquals(50L, req.getId());
            db.update("UPDATE pay_order SET status=?,channel_code=?,success_time=? WHERE id=50",
                    PayOrderStatusEnum.SUCCESS.getStatus(), req.getChannelCode(), LocalDateTime.now());
            db.update("INSERT INTO effects VALUES('payment-notify')");
            return null;
        });
        var line = new WxOrderLineDO(); line.setId(1001L); line.setWxOrderId(100L);
        line.setDrugId(11L); line.setQty(2);
        var lines = mock(WxOrderLineMapper.class);
        when(lines.selectListByWxOrderId(100L)).thenReturn(List.of(line));
        var lineService = mock(WxOrderLineService.class);
        when(lineService.getWxOrderLineListByWxOrderId(100L)).thenReturn(List.of(line));
        allocations = mock(WxOrderLineAllocMapper.class);
        when(allocations.selectListByWxOrderId(100L)).thenReturn(List.of());
        when(allocations.insert(any(WxOrderLineAllocDO.class))).thenAnswer(a -> {
            db.update("INSERT INTO effects VALUES('allocation')");
            if (failAllocation) throw new IllegalStateException("allocation write failed");
            return 1;
        });
        worker = mock(InventoryFacadeAdapter.class);
        when(worker.deduct(any(InventoryReadAccess.Scope.class), anyList(), anyLong())).thenAnswer(a -> {
            assertEquals(new InventoryReadAccess.Scope(7,4), a.getArgument(0));
            assertEquals(0L, (Long) a.getArgument(2));
            assertEquals(UserTypeEnum.MEMBER.getValue(),
                    ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserType());
            db.update("UPDATE stock SET qty=qty-2 WHERE id=1");
            db.update("INSERT INTO effects VALUES('stock-flow')");
            if (failStock) throw new IllegalStateException("stock worker failed after write");
            var part = new DeductResult.Allocation(); part.setBizLineId(1001L);
            part.setBatchId(21L); part.setLocationId(31L); part.setQty(2);
            var result = new DeductResult(); result.setSuccess(true); result.setAllocations(List.of(part));
            return result;
        });
        var inventory = transactional(new PaidWxOrderInventoryService(access, lines, allocations, worker));
        employeeInventory = mock(InventoryFacade.class); points = mock(MemberPointSettlementService.class);
        var target = new WxOrderServiceImpl();
        ReflectionTestUtils.setField(target, "orderPaymentAccess", access);
        ReflectionTestUtils.setField(target, "payOrderService", payments);
        ReflectionTestUtils.setField(target, "paidOrderInventory", inventory);
        ReflectionTestUtils.setField(target, "wxOrderMapper", orders);
        ReflectionTestUtils.setField(target, "wxOrderLineMapper", lines);
        ReflectionTestUtils.setField(target, "wxOrderLineService", lineService);
        ReflectionTestUtils.setField(target, "wxOrderLineAllocMapper", allocations);
        ReflectionTestUtils.setField(target, "inventoryFacade", employeeInventory);
        ReflectionTestUtils.setField(target, "memberPointSettlementService", points);
        ReflectionTestUtils.setField(target, "mockPaymentEnabled", true);
        ReflectionTestUtils.setField(target, "environment", new MockEnvironment());
        ((MockEnvironment) ReflectionTestUtils.getField(target, "environment")).setActiveProfiles("test");
        service = transactional(target);
        login(1L,7L);
    }
    private String sql(String method) throws Exception {
        return WxOrderPaymentMapper.class.getMethod(method, Long.class, Long.class)
                .getAnnotation(Select.class).value()[0].replace("#{id}", "?").replace("#{tenantId}", "?");
    }
    @SuppressWarnings("unchecked") private <T> T transactional(T target) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (T) proxy.getProxy();
    }
    private static void login(Long member, Long tenant) {
        TenantContextHolder.setTenantId(tenant);
        var user = new LoginUser(); user.setId(member); user.setTenantId(tenant);
        user.setUserType(UserTypeEnum.MEMBER.getValue());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
    }
    @AfterEach void cleanup() {
        TenantContextHolder.clear(); SecurityContextHolder.clearContext(); db.execute("SHUTDOWN");
    }
    private int count(String table) { return db.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private void assertSuccess() {
        assertEquals(1, db.queryForObject("SELECT pay_status FROM ph_wx_order", Integer.class));
        assertEquals(1, db.queryForObject("SELECT status FROM ph_wx_order", Integer.class));
        assertEquals(8, db.queryForObject("SELECT qty FROM stock", Integer.class));
        assertEquals(1, count("pay_order")); assertEquals(3, count("effects"));
        verify(payments, times(1)).submitOrder(any(), anyString());
        verifyNoInteractions(points, employeeInventory);
    }
    private void assertRolledBack() {
        assertEquals(0, db.queryForObject("SELECT pay_status FROM ph_wx_order", Integer.class));
        assertEquals(0, db.queryForObject("SELECT status FROM ph_wx_order", Integer.class));
        assertNull(db.queryForObject("SELECT pay_no FROM ph_wx_order", String.class));
        assertEquals(10, db.queryForObject("SELECT qty FROM stock", Integer.class));
        assertEquals(0, count("pay_order")); assertEquals(0, count("effects"));
        verifyNoInteractions(points, employeeInventory);
    }
    @Test void successAndRepeatCommitStockOnceWithoutChangingPointsOrIdentity() {
        service.simulatePayWxOrderByMember(100L); service.simulatePayWxOrderByMember(100L); assertSuccess();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void paymentCreationPassesCompleteServerGeneratedDto(boolean missingExpiry) {
        var expiry = LocalDateTime.now().plusHours(1).withNano(0);
        db.update("UPDATE ph_wx_order SET expire_at=?", missingExpiry ? null : expiry);
        var before = LocalDateTime.now();
        service.simulatePayWxOrderByMember(100L);
        var request = org.mockito.ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO.class);
        verify(payments).createOrder(request.capture());
        var value = request.getValue();
        assertAll(
                () -> assertEquals("firstsun", value.getAppKey()),
                () -> assertEquals("WX100", value.getMerchantOrderId()),
                () -> assertEquals(1L, value.getUserId()),
                () -> assertEquals(UserTypeEnum.MEMBER.getValue(), value.getUserType()),
                () -> assertEquals(1234, value.getPrice()),
                () -> assertEquals("127.0.0.1", value.getUserIp()),
                () -> assertEquals("药店线上订单（开发模拟）", value.getSubject()),
                // Fixed server text, independent of patient, prescription or user-entered remarks.
                () -> assertEquals("药店线上订单（开发模拟）", value.getBody()),
                () -> assertNotNull(value.getExpireTime()));
        if (missingExpiry) {
            assertFalse(value.getExpireTime().isBefore(before.plusMinutes(30)));
            assertFalse(value.getExpireTime().isAfter(LocalDateTime.now().plusMinutes(30)));
        } else assertEquals(expiry, value.getExpireTime());
    }
    @Test void concurrentRequestsSerializeBeforeCreatingPaymentOrDeductingStock() throws Exception {
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        Callable<Void> request = () -> { login(1L,7L); try { start.await(); service.simulatePayWxOrderByMember(100L); }
            finally { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); } return null; };
        try {
            var first = pool.submit(request); var second = pool.submit(request); start.countDown();
            first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS); assertSuccess();
        } finally { pool.shutdownNow(); }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void reservedOrderCommitsOnceOrRollsBackIfAllocationUpdateMisses(boolean missingAllocation) {
        db.update("UPDATE stock SET frozen=2");
        var reserved = new WxOrderLineAllocDO(); reserved.setId(20L); reserved.setWxOrderId(100L);
        reserved.setWxOrderLineId(1001L); reserved.setOrderNo("WX100"); reserved.setDrugId(11L);
        reserved.setQty(2); reserved.setStatus(WxOrderLineAllocDO.STATUS_FROZEN);
        reserved.setBatchId(21L); reserved.setLocationId(31L);
        when(allocations.selectListByWxOrderId(100L)).thenReturn(List.of(reserved));
        when(worker.consumeReservation(any(InventoryReadAccess.Scope.class), anyList(), eq(0L))).thenAnswer(a -> {
            db.update("UPDATE stock SET qty=qty-2,frozen=frozen-2");
            db.update("INSERT INTO effects VALUES('stock-flow')"); return new DeductResult();
        });
        when(allocations.updateById(any(WxOrderLineAllocDO.class))).thenAnswer(a -> {
            WxOrderLineAllocDO update = a.getArgument(0);
            assertEquals(WxOrderLineAllocDO.STATUS_OUT, update.getStatus());
            assertEquals(20L, update.getOutBizLineId());
            db.update("INSERT INTO effects VALUES('allocation')"); return missingAllocation ? 0 : 1;
        });
        if (missingAllocation) {
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> service.simulatePayWxOrderByMember(100L));
            assertRolledBack(); assertEquals(2, db.queryForObject("SELECT frozen FROM stock", Integer.class));
            return;
        }
        service.simulatePayWxOrderByMember(100L); service.simulatePayWxOrderByMember(100L);
        assertSuccess(); assertEquals(0, db.queryForObject("SELECT frozen FROM stock", Integer.class));
        verify(worker, times(1)).consumeReservation(any(InventoryReadAccess.Scope.class), anyList(), eq(0L));
        verify(worker, never()).deduct(any(InventoryReadAccess.Scope.class), anyList(), anyLong());
    }
    @Test void internalAuthorizationCannotRunOutsideTransaction() {
        var access = transactional(new WxOrderPaymentAccess(mock(WxOrderPaymentMapper.class), mock(PayAppService.class)));
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> access.requirePaidOrder(100L,50L));
    }
    @Test void stockFailureRollsBackPaymentOrderAndPartialStockWrites() {
        failStock = true;
        assertThrows(IllegalStateException.class, () -> service.simulatePayWxOrderByMember(100L)); assertRolledBack();
        failStock = false; service.simulatePayWxOrderByMember(100L);
        assertEquals(8, db.queryForObject("SELECT qty FROM stock", Integer.class));
    }
    @Test void allocationFailureRollsBackEvenAfterSuccessfulStockWorker() {
        failAllocation = true;
        assertThrows(IllegalStateException.class, () -> service.simulatePayWxOrderByMember(100L)); assertRolledBack();
    }
    @Test void paymentThatNeverBecomesSuccessfulCannotTouchStock() {
        doReturn(null).when(payments).submitOrder(any(), anyString());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.simulatePayWxOrderByMember(100L));
        assertRolledBack(); verifyNoInteractions(worker);
    }
    @Test void otherMemberAndTenantAreRejectedBeforePayment() {
        for (var identity : List.of(new long[]{2,7}, new long[]{1,8})) {
            login(identity[0],identity[1]);
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> service.simulatePayWxOrderByMember(100L));
        }
        verifyNoInteractions(payments,worker); assertRolledBack();
    }
    @Test void cancelledExpiredAndInvalidAmountsCannotStartPayment() {
        db.update("UPDATE ph_wx_order SET status=-1");
        assertThrows(RuntimeException.class, () -> service.simulatePayWxOrderByMember(100L));
        db.update("UPDATE ph_wx_order SET status=0,expire_at=?", LocalDateTime.now().minusMinutes(1));
        assertThrows(RuntimeException.class, () -> service.simulatePayWxOrderByMember(100L));
        db.update("UPDATE ph_wx_order SET expire_at=?,payable_amount=0", LocalDateTime.now().plusHours(1));
        assertThrows(RuntimeException.class, () -> service.simulatePayWxOrderByMember(100L));
        verifyNoInteractions(payments,worker); assertRolledBack();
    }
}
