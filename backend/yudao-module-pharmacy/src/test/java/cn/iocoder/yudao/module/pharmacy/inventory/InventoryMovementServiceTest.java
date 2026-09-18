package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryMovementReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryMovementMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryMovementService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryMovementServiceTest {
    private final InventoryReadAccess access = mock(InventoryReadAccess.class);
    private final InventoryMovementMapper mapper = mock(InventoryMovementMapper.class);
    private final InventoryReadAccess.Scope scope = new InventoryReadAccess.Scope(1, 7);
    private final InventoryMovementService service = new InventoryMovementService(access, mapper);

    @BeforeEach void setup() {
        when(access.requireScope(null)).thenReturn(scope);
        var login = new LoginUser(); login.setId(11L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
        var warehouse = new InventoryMovementMapper.Warehouse(); warehouse.setId(3L); warehouse.setStatus(1);
        when(mapper.lockWarehouse(scope, 3L)).thenReturn(warehouse);
        var batch = new InventoryMovementMapper.Batch(); batch.setId(5L); batch.setDrugId(9L); batch.setBatchNo("B-1"); batch.setQtyTotal(10); batch.setCostPrice(BigDecimal.ONE);
        when(mapper.lockBatch(scope, 3L, 5L)).thenReturn(batch);
        var sourceLocation = new InventoryMovementMapper.Location(); sourceLocation.setId(10L); sourceLocation.setStatus(1); sourceLocation.setMaxCapacity(20L);
        var targetLocation = new InventoryMovementMapper.Location(); targetLocation.setId(11L); targetLocation.setStatus(1); targetLocation.setMaxCapacity(20L);
        when(mapper.lockLocation(scope, 3L, 10L)).thenReturn(sourceLocation);
        when(mapper.lockLocation(scope, 3L, 11L)).thenReturn(targetLocation);
        var source = new InventoryMovementMapper.Stock(); source.setId(100L); source.setBatchId(5L); source.setLocationId(10L); source.setDrugId(9L); source.setQty(8); source.setQtyFrozen(2);
        var target = new InventoryMovementMapper.Stock(); target.setId(101L); target.setBatchId(5L); target.setLocationId(11L); target.setDrugId(9L); target.setQty(1); target.setQtyFrozen(0);
        when(mapper.lockStock(scope, 5L, 10L)).thenReturn(source);
        when(mapper.lockStock(scope, 5L, 11L)).thenReturn(target);
        when(mapper.sumLocationQty(scope, 10L)).thenReturn(8L);
        when(mapper.sumLocationQty(scope, 11L)).thenReturn(1L);
        when(mapper.decreaseStock(any(), anyLong(), anyInt(), anyString())).thenReturn(1);
        when(mapper.upsertStock(any(), anyLong(), anyLong(), anyLong(), anyString())).thenReturn(1);
        when(mapper.increaseStock(any(), anyLong(), anyInt(), anyString())).thenReturn(1);
        when(mapper.insertFlow(any(), any())).thenReturn(1);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void movesOnlyAvailableQuantityAndWritesPairedFlows() {
        assertDoesNotThrow(() -> service.execute(request(3)));
        verify(mapper).decreaseStock(scope, 100L, 3, "11");
        verify(mapper).increaseStock(scope, 101L, 3, "11");
        verify(mapper, times(2)).insertFlow(eq(scope), any());
    }

    @Test void completedReplayDoesNotLockOrMoveAgain() {
        var out = new InventoryMovementMapper.Flow(); out.setBatchId(5L); out.setLocationId(10L); out.setInQty(0); out.setOutQty(3);
        var in = new InventoryMovementMapper.Flow(); in.setBatchId(5L); in.setLocationId(11L); in.setInQty(3); in.setOutQty(0);
        when(mapper.selectFlows(scope, "MV-1", 1L)).thenReturn(List.of(out, in));
        service.execute(request(3));
        verify(mapper, never()).lockWarehouse(any(), anyLong());
        verify(mapper, never()).decreaseStock(any(), anyLong(), anyInt(), anyString());
    }

    @Test void frozenQuantityCannotMove() {
        assertThrows(ServiceException.class, () -> service.execute(request(7)));
        verify(mapper, never()).decreaseStock(any(), anyLong(), anyInt(), anyString());
    }

    @Test void targetCapacityRejectsWholeMove() {
        when(mapper.sumLocationQty(scope, 11L)).thenReturn(19L);
        assertThrows(ServiceException.class, () -> service.execute(request(3)));
        verify(mapper, never()).decreaseStock(any(), anyLong(), anyInt(), anyString());
    }

    @Test void sameLocationAndDuplicateLineAreRejectedBeforeSql() {
        var request = request(1); request.getLines().get(0).setTargetLocationId(10L);
        assertThrows(ServiceException.class, () -> service.execute(request));
        verifyNoInteractions(mapper);
    }

    @Test void mapperXmlBindsEveryStatementWithScopedParameters() throws Exception {
        var configuration = new Configuration();
        String resource = "mapper/inventory/InventoryMovementMapper.xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            org.junit.jupiter.api.Assertions.assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        var flow = new InventoryMovementMapper.FlowCommand();
        flow.setBatchId(5L); flow.setLocationId(11L); flow.setDrugId(9L); flow.setBatchNo("B-1");
        flow.setInQty(3); flow.setOutQty(0); flow.setBalanceQty(8); flow.setBizNo("MV-1");
        flow.setBizLineId(1L); flow.setOperator(11L); flow.setFlowTime(java.time.LocalDateTime.now()); flow.setUnitCost(BigDecimal.ONE);
        Map<String, Object> params = new HashMap<>();
        params.put("scope", scope); params.put("id", 3L); params.put("warehouseId", 3L); params.put("batchId", 5L);
        params.put("locationId", 11L); params.put("bizNo", "MV-1"); params.put("bizLineId", 1L); params.put("qty", 3);
        params.put("drugId", 9L); params.put("operator", "11"); params.put("flow", flow);
        for (var method : InventoryMovementMapper.class.getDeclaredMethods()) {
            var sql = configuration.getMappedStatement(InventoryMovementMapper.class.getName() + "." + method.getName())
                    .getBoundSql(params).getSql();
            org.junit.jupiter.api.Assertions.assertTrue(sql.contains("tenant_id"), method.getName());
        }
    }

    private static InventoryMovementReqVO request(int qty) {
        var line = new InventoryMovementReqVO.Line();
        line.setBizLineId(1L); line.setBatchId(5L); line.setSourceLocationId(10L); line.setTargetLocationId(11L); line.setQty(qty);
        var request = new InventoryMovementReqVO(); request.setWarehouseId(3L); request.setBizNo("MV-1"); request.setLines(List.of(line));
        return request;
    }
}
