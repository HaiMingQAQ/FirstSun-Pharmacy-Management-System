package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.*;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryFacadeMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryFacadeAdapter;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryReservationServiceTest {
    private final InventoryReadAccess access = mock(InventoryReadAccess.class);
    private final InventoryFacadeMapper mapper = mock(InventoryFacadeMapper.class);
    private final DrugApi drugApi = mock(DrugApi.class);
    private final InventoryFacadeAdapter facade = new InventoryFacadeAdapter(access, mapper, drugApi);
    private final InventoryReadAccess.Scope scope = new InventoryReadAccess.Scope(1L, 7L);

    @BeforeEach void setup() {
        when(access.requireScope(7L)).thenReturn(scope);
        var user = new LoginUser(); user.setId(407L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var batch = new InventoryFacadeMapper.Batch();
        batch.setId(5L); batch.setWarehouseId(3L); batch.setDrugId(9L); batch.setBatchNo("B-1");
        batch.setQtyTotal(10); batch.setQtyAvail(10); batch.setQtyFrozen(0); batch.setQtySold(0);
        batch.setQualityStatus(0); batch.setExpiryDate(LocalDate.now().plusDays(10)); batch.setCostPrice(BigDecimal.ONE);
        when(mapper.lockBatch(scope, 5L)).thenReturn(batch);
        var warehouse = new InventoryFacadeMapper.Warehouse(); warehouse.setId(3L); warehouse.setStatus(1);
        when(mapper.lockWarehouse(scope, 3L)).thenReturn(warehouse);
        var location = new InventoryFacadeMapper.Location(); location.setId(10L); location.setStatus(1);
        when(mapper.lockLocation(scope, 3L, 10L)).thenReturn(location);
        var stock = new InventoryFacadeMapper.Stock(); stock.setId(100L); stock.setBatchId(5L); stock.setLocationId(10L); stock.setDrugId(9L); stock.setQty(10); stock.setQtyFrozen(4);
        when(mapper.lockStock(scope, 5L, 10L)).thenReturn(stock);
        var candidate = new InventoryFacadeMapper.FefoStock();
        candidate.setId(100L); candidate.setBatchId(5L); candidate.setLocationId(10L); candidate.setDrugId(9L); candidate.setQty(10); candidate.setQtyFrozen(0);
        when(mapper.lockFefoStocks(scope, 9L, LocalDate.now())).thenReturn(List.of(candidate));
        when(mapper.reserveStock(any(), anyLong(), anyInt(), anyString())).thenReturn(1);
        when(mapper.reserveBatch(any(), anyLong(), anyInt(), anyString())).thenReturn(1);
        when(mapper.releaseStock(any(), anyLong(), anyInt(), anyString())).thenReturn(1);
        when(mapper.releaseBatch(any(), anyLong(), anyInt(), anyString())).thenReturn(1);
        when(mapper.consumeReservedStock(any(), anyLong(), anyInt(), anyString())).thenReturn(1);
        when(mapper.consumeReservedBatch(any(), anyLong(), anyInt(), anyString())).thenReturn(1);
        when(mapper.insertFlow(any(), any())).thenReturn(1);
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void reserveUsesFefoAndWritesFrozenFlow() {
        ReserveItem item = reserveItem("WO-1", 11L, 4);
        var result = facade.reserve(7L, List.of(item));

        assertEquals(4, result.getAllocations().get(0).getQty());
        verify(mapper).reserveStock(scope, 100L, 4, "407");
        verify(mapper).reserveBatch(scope, 5L, 4, "407");
        var flow = flowCaptor();
        assertEquals(80, flow.getFlowType()); assertEquals(4, flow.getFrozenDelta()); assertEquals(0, flow.getOutQty());
    }

    @Test void releaseWritesNegativeFrozenFlow() {
        reservation(50L, 4); when(mapper.resolvedReservationQty(scope, 50L)).thenReturn(0);
        ReleaseItem item = new ReleaseItem(); item.setBizNo("WXC-1"); item.setBizLineId(101L); item.setOriginalBizNo("WO-1"); item.setOriginalBizLineId(11L);
        item.setDrugId(9L); item.setBatchId(5L); item.setLocationId(10L); item.setQty(2);
        facade.release(7L, List.of(item));

        verify(mapper).releaseStock(scope, 100L, 2, "407"); verify(mapper).releaseBatch(scope, 5L, 2, "407");
        var flow = flowCaptor(); assertEquals(81, flow.getFlowType()); assertEquals(-2, flow.getFrozenDelta()); assertEquals(50L, flow.getOriginalFlowId());
    }

    @Test void consumeKeepsAvailableLedgerAndWritesOutboundFlow() {
        reservation(50L, 4); when(mapper.resolvedReservationQty(scope, 50L)).thenReturn(0);
        ConsumeItem item = new ConsumeItem(); item.setBizNo("WO-1"); item.setBizLineId(101L); item.setOriginalBizNo("WO-1"); item.setOriginalBizLineId(11L);
        item.setDrugId(9L); item.setBatchId(5L); item.setLocationId(10L); item.setQty(2);
        var result = facade.consumeReservation(7L, List.of(item));

        assertEquals(2, result.getAllocations().get(0).getQty());
        verify(mapper).consumeReservedStock(scope, 100L, 2, "407"); verify(mapper).consumeReservedBatch(scope, 5L, 2, "407");
        var flow = flowCaptor(); assertEquals(82, flow.getFlowType()); assertEquals(2, flow.getOutQty()); assertEquals(-2, flow.getFrozenDelta());
    }

    @Test void availableQtyReturnsZeroForNoStockDrug() {
        var found = new InventoryFacadeMapper.Available(); found.setDrugId(9L); found.setQtyAvail(6);
        when(mapper.selectAvailableQty(scope, List.of(9L, 10L))).thenReturn(List.of(found));

        List<AvailableQty> results = facade.getAvailableQty(7L, List.of(9L, 10L));

        assertEquals(6, results.get(0).getQtyAvail()); assertEquals(0, results.get(1).getQtyAvail());
    }

    @Test void mapperXmlRegistersEveryReservationStatement() throws Exception {
        var configuration = new Configuration();
        String resource = "mapper/inventory/InventoryFacadeMapper.xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            org.junit.jupiter.api.Assertions.assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        for (var method : InventoryFacadeMapper.class.getDeclaredMethods()) {
            var statement = configuration.getMappedStatement(InventoryFacadeMapper.class.getName() + "." + method.getName());
            org.junit.jupiter.api.Assertions.assertNotNull(statement.getBoundSql(Map.of("scope", scope, "drugIds", List.of(9L))).getSql());
        }
    }

    private ReserveItem reserveItem(String bizNo, Long lineId, int qty) {
        ReserveItem item = new ReserveItem(); item.setBizNo(bizNo); item.setBizLineId(lineId); item.setDrugId(9L); item.setQty(qty); return item;
    }

    private void reservation(Long id, int qty) {
        var flow = new InventoryFacadeMapper.Flow(); flow.setId(id); flow.setBatchId(5L); flow.setLocationId(10L); flow.setDrugId(9L); flow.setFrozenDelta(qty);
        when(mapper.lockReservationFlow(scope, "WO-1", 11L, 5L, 10L)).thenReturn(flow);
    }

    private InventoryFacadeMapper.FlowCommand flowCaptor() {
        var captor = org.mockito.ArgumentCaptor.forClass(InventoryFacadeMapper.FlowCommand.class);
        verify(mapper).insertFlow(eq(scope), captor.capture());
        return captor.getValue();
    }
}
