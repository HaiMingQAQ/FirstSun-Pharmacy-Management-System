package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogUpdateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryCatalogUpdateMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryCatalogUpdateService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit and XML-binding tests only. Database locks, uniqueness and rollback need independent MySQL. */
class InventoryCatalogUpdateTest {
    private final InventoryReadAccess access = mock(InventoryReadAccess.class);
    private final InventoryCatalogUpdateMapper mapper = mock(InventoryCatalogUpdateMapper.class);
    private final InventoryCatalogUpdateService service = new InventoryCatalogUpdateService(access, mapper);
    private final InventoryReadAccess.Scope scope = new InventoryReadAccess.Scope(1, 7);
    private final InventoryReadVO.Warehouse warehouse = new InventoryReadVO.Warehouse();
    private final InventoryReadVO.Location location = new InventoryReadVO.Location();

    @BeforeEach void setup() {
        when(access.requireScope(null)).thenReturn(scope);
        warehouse.setId(3L); warehouse.setWhCode("A"); warehouse.setWhName("常温仓");
        warehouse.setTempZone(0); warehouse.setStatus(1); warehouse.setIsDefault(0);
        location.setId(4L); location.setWarehouseId(3L); location.setLocationCode("A01");
        location.setLocationType(0); location.setStatus(1); location.setMaxCapacity(null);
        when(mapper.lockWarehouse(scope, 3)).thenReturn(warehouse);
        when(mapper.lockLocation(scope, 3, 4)).thenReturn(location);
        var usage = new InventoryCatalogUpdateMapper.Usage(); usage.setQuantity(0L); usage.setInvalidRows(0L);
        when(mapper.locationUsage(scope, 3, 4)).thenReturn(usage);
        when(mapper.updateWarehouse(any(), any(), any())).thenReturn(1);
        when(mapper.updateLocation(any(), any(), any())).thenReturn(1);
        when(mapper.deleteWarehouse(any(), anyLong(), any())).thenReturn(1);
        when(mapper.deleteLocation(any(), anyLong(), anyLong(), any())).thenReturn(1);
        var login = new LoginUser(); login.setId(11L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }
    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        TransactionSynchronizationManager.clear();
    }
    private InventoryCatalogUpdateReqVO.Warehouse warehouseRequest() {
        var request = new InventoryCatalogUpdateReqVO.Warehouse(); request.setId(3L);
        var old = new InventoryCatalogUpdateReqVO.WarehouseFields();
        old.setWhCode("A"); old.setWhName("常温仓"); old.setTempZone(0); old.setStatus(1);
        var value = new InventoryCatalogUpdateReqVO.WarehouseFields();
        value.setWhCode("A"); value.setWhName("常温仓"); value.setTempZone(0); value.setStatus(1);
        request.setExpected(old); request.setValue(value); return request;
    }
    private InventoryCatalogUpdateReqVO.Location locationRequest() {
        var request = new InventoryCatalogUpdateReqVO.Location(); request.setId(4L); request.setWarehouseId(3L);
        var old = new InventoryCatalogUpdateReqVO.LocationFields();
        old.setLocationCode("A01"); old.setLocationType(0); old.setStatus(1);
        var value = new InventoryCatalogUpdateReqVO.LocationFields();
        value.setLocationCode("A01"); value.setLocationType(0); value.setStatus(1);
        request.setExpected(old); request.setValue(value); return request;
    }
    private void occupied(long quantity) {
        var usage = new InventoryCatalogUpdateMapper.Usage(); usage.setQuantity(quantity); usage.setInvalidRows(0L);
        when(mapper.locationUsage(scope, 3, 4)).thenReturn(usage);
    }
    private void noWarehouseWrite() { verify(mapper, never()).updateWarehouse(any(), any(), any()); }
    private void noLocationWrite() { verify(mapper, never()).updateLocation(any(), any(), any()); }

    @Test void incompatibleAmbientTransactionFailsBeforeScopeOrSql() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(Isolation.REPEATABLE_READ.value());
        assertThrows(IllegalStateException.class, () -> service.updateWarehouse(warehouseRequest()));
        verifyNoInteractions(access, mapper);
    }
    @Test void scopeDeniedBeforeLocks() {
        when(access.requireScope(null)).thenThrow(new AccessDeniedException("denied"));
        assertThrows(AccessDeniedException.class, () -> service.updateLocation(locationRequest()));
        verifyNoInteractions(mapper);
    }
    @Test void missingOrForeignWarehouseReturnsNotFound() {
        when(mapper.lockWarehouse(scope, 3)).thenReturn(null);
        assertEquals(404, assertThrows(ServiceException.class, () -> service.updateWarehouse(warehouseRequest())).getCode());
        noWarehouseWrite();
    }
    @Test void staleWarehouseSnapshotNeverOverwrites() {
        warehouse.setWhName("其他人刚改的名称");
        assertThrows(ServiceException.class, () -> service.updateWarehouse(warehouseRequest()));
        noWarehouseWrite();
    }
    @Test void unchangedFieldsDoNotDependOnJdbcAffectedRows() {
        service.updateWarehouse(warehouseRequest()); noWarehouseWrite();
    }
    @Test void renameWithStockIsAllowedAndUsesTrustedActor() {
        var request = warehouseRequest(); request.getValue().setWhName("新名称");
        service.updateWarehouse(request);
        verify(mapper).updateWarehouse(scope, request, "11");
        verify(mapper, never()).hasWarehouseStock(any(), anyLong());
    }
    @Test void defaultWarehouseCannotBeDisabled() {
        warehouse.setIsDefault(1);
        var request = warehouseRequest(); request.getValue().setStatus(0);
        assertThrows(ServiceException.class, () -> service.updateWarehouse(request)); noWarehouseWrite();
    }
    @Test void stockedWarehouseCannotBeDisabledOrChangeZone() {
        when(mapper.hasWarehouseStock(scope, 3)).thenReturn(true);
        var request = warehouseRequest(); request.getValue().setStatus(0);
        assertThrows(ServiceException.class, () -> service.updateWarehouse(request));
        request.getValue().setStatus(1); request.getValue().setTempZone(1);
        assertThrows(ServiceException.class, () -> service.updateWarehouse(request)); noWarehouseWrite();
    }
    @Test void openWorkBlocksOtherwiseEmptyWarehouse() {
        when(mapper.hasOpenWork(scope, 3)).thenReturn(true);
        var request = warehouseRequest(); request.getValue().setStatus(0);
        assertThrows(ServiceException.class, () -> service.updateWarehouse(request)); noWarehouseWrite();
    }
    @Test void emptyNonDefaultWarehouseCanBeDisabled() {
        var request = warehouseRequest(); request.getValue().setStatus(0);
        service.updateWarehouse(request);
        verify(mapper).updateWarehouse(scope, request, "11");
    }
    @Test void locationLocksParentBeforeChildAndRejectsStaleData() {
        location.setLocationCode("changed");
        assertThrows(ServiceException.class, () -> service.updateLocation(locationRequest()));
        var order = inOrder(mapper);
        order.verify(mapper).lockWarehouse(scope, 3); order.verify(mapper).lockLocation(scope, 3, 4);
        noLocationWrite();
    }
    @Test void wrongParentLocationDoesNotMoveOwnership() {
        when(mapper.lockLocation(scope, 3, 4)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.updateLocation(locationRequest())); noLocationWrite();
    }
    @Test void disabledParentBlocksLocationEdits() {
        warehouse.setStatus(0);
        var request = locationRequest(); request.getValue().setLocationCode("A02");
        assertThrows(ServiceException.class, () -> service.updateLocation(request)); noLocationWrite();
    }
    @Test void capacityIncludesAllStockAndAllowsExactOccupancy() {
        occupied(20);
        var request = locationRequest(); request.getValue().setMaxCapacity(19L);
        assertThrows(ServiceException.class, () -> service.updateLocation(request)); noLocationWrite();
        request.getValue().setMaxCapacity(20L);
        service.updateLocation(request); verify(mapper).updateLocation(scope, request, "11");
    }
    @Test void zeroCapacityRequiresEmptyLocation() {
        occupied(1);
        var request = locationRequest(); request.getValue().setMaxCapacity(0L);
        assertThrows(ServiceException.class, () -> service.updateLocation(request)); noLocationWrite();
        occupied(0);
        service.updateLocation(request); verify(mapper).updateLocation(scope, request, "11");
    }
    @Test void stockedLocationCannotDisableOrChangeType() {
        occupied(1);
        var request = locationRequest(); request.getValue().setStatus(0);
        assertThrows(ServiceException.class, () -> service.updateLocation(request));
        request.getValue().setStatus(1); request.getValue().setLocationType(1);
        assertThrows(ServiceException.class, () -> service.updateLocation(request)); noLocationWrite();
    }
    @Test void malformedLocationStockBlocksCapacityChange() {
        mapper.locationUsage(scope, 3, 4).setInvalidRows(1L);
        var request = locationRequest(); request.getValue().setMaxCapacity(100L);
        assertThrows(ServiceException.class, () -> service.updateLocation(request)); noLocationWrite();
    }
    @Test void duplicateCodeDoesNotExposeDatabaseDetailsOrRetry() {
        var request = locationRequest(); request.getValue().setLocationCode("used");
        when(mapper.updateLocation(scope, request, "11")).thenThrow(new DuplicateKeyException("private sql"));
        var error = assertThrows(ServiceException.class, () -> service.updateLocation(request));
        assertFalse(error.getMessage().contains("private"));
        verify(mapper, times(1)).updateLocation(scope, request, "11");
    }
    @Test void unexpectedAffectedRowCountFails() {
        var request = warehouseRequest(); request.getValue().setWhName("new");
        when(mapper.updateWarehouse(scope, request, "11")).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.updateWarehouse(request));
    }

    @Test void warehouseDeleteRequiresDisabledUnusedNonDefaultRow() {
        warehouse.setStatus(1);
        assertThrows(ServiceException.class, () -> service.deleteWarehouse(3L));
        verify(mapper, never()).hasWarehouseReferences(any(), anyLong());
        warehouse.setStatus(0);
        warehouse.setIsDefault(1);
        assertThrows(ServiceException.class, () -> service.deleteWarehouse(3L));
        verify(mapper, never()).deleteWarehouse(any(), anyLong(), any());
        warehouse.setIsDefault(0);
        when(mapper.hasWarehouseReferences(scope, 3L)).thenReturn(true);
        assertThrows(ServiceException.class, () -> service.deleteWarehouse(3L));
        verify(mapper, never()).deleteWarehouse(any(), anyLong(), any());
    }

    @Test void emptyDisabledWarehouseDeleteUsesStoreScopedLockAndActor() {
        warehouse.setStatus(0);
        when(mapper.hasWarehouseReferences(scope, 3L)).thenReturn(false);
        when(mapper.hasOpenWork(scope, 3L)).thenReturn(false);
        service.deleteWarehouse(3L);
        var order = inOrder(mapper);
        order.verify(mapper).lockWarehouse(scope, 3L);
        order.verify(mapper).hasWarehouseReferences(scope, 3L);
        order.verify(mapper).hasOpenWork(scope, 3L);
        order.verify(mapper).deleteWarehouse(scope, 3L, "11");
    }

    @Test void locationDeleteRequiresDisabledUnreferencedRowAndLocksParentFirst() {
        location.setStatus(1);
        assertThrows(ServiceException.class, () -> service.deleteLocation(3L, 4L));
        verify(mapper, never()).hasLocationReferences(any(), anyLong(), anyLong());
        location.setStatus(0);
        when(mapper.hasLocationReferences(scope, 3L, 4L)).thenReturn(true);
        assertThrows(ServiceException.class, () -> service.deleteLocation(3L, 4L));
        verify(mapper, never()).deleteLocation(any(), anyLong(), anyLong(), any());
        when(mapper.hasLocationReferences(scope, 3L, 4L)).thenReturn(false);
        service.deleteLocation(3L, 4L);
        var order = inOrder(mapper);
        order.verify(mapper).lockWarehouse(scope, 3L);
        order.verify(mapper).lockLocation(scope, 3L, 4L);
        order.verify(mapper).hasLocationReferences(scope, 3L, 4L);
        order.verify(mapper).deleteLocation(scope, 3L, 4L, "11");
    }
    @Test void allXmlStatementsResolveTheirActualBoundParameters() throws Exception {
        var configuration = new Configuration();
        String resource = "mapper/inventory/InventoryCatalogUpdateMapper.xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        for (var method : InventoryCatalogUpdateMapper.class.getDeclaredMethods()) {
            Object request = method.getName().equals("updateWarehouse") ? warehouseRequest() : locationRequest();
            var params = Map.of("scope", scope, "id", 4L, "warehouseId", 3L, "q", request, "actor", "11");
            var bound = configuration.getMappedStatement(InventoryCatalogUpdateMapper.class.getName() + "." + method.getName()).getBoundSql(params);
            assertTrue(bound.getSql().contains("tenant_id"));
            for (var mapping : bound.getParameterMappings()) {
                Object value = configuration.newMetaObject(params).getValue(mapping.getProperty());
                if (!mapping.getProperty().equals("q.value.maxCapacity")) assertNotNull(value, mapping.getProperty());
            }
            if (method.getName().startsWith("lock")) assertTrue(bound.getSql().contains("FOR UPDATE"));
        }
    }
}
