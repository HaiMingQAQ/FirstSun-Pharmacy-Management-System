package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryPreviewReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryPreviewMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryReadMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryPreviewService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator.ExpiryDayPolicy;
import jakarta.validation.Validation;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Mock snapshots and real XML parsing only; no MySQL or sale integration is certified. */
class InventoryPreviewTest {
    private final InventoryReadAccess access = mock(InventoryReadAccess.class);
    private final InventoryReadMapper reads = mock(InventoryReadMapper.class);
    private final InventoryPreviewMapper mapper = mock(InventoryPreviewMapper.class);
    private final DrugApi drugs = mock(DrugApi.class);
    private final InventoryPreviewService service = new InventoryPreviewService(access, reads, mapper, drugs);
    private final InventoryReadAccess.Scope scope = new InventoryReadAccess.Scope(1, 7);
    private final InventoryPreviewReqVO request = new InventoryPreviewReqVO();

    @BeforeEach void setup() {
        request.setWarehouseId(3L); request.setDrugId(8L); request.setQuantity(4);
        request.setExpiryDayPolicy(ExpiryDayPolicy.BLOCK_ON_EXPIRY_DATE);
        when(access.requireScope(null)).thenReturn(scope);
        var warehouse = new InventoryReadVO.Warehouse(); warehouse.setStatus(1);
        when(reads.selectWarehouse(scope, 3)).thenReturn(warehouse);
    }

    private InventoryPreviewMapper.Row row(long batch, long location, int days, int quantity, int frozen) {
        var row = new InventoryPreviewMapper.Row();
        row.setBatchId(batch); row.setLocationId(location); row.setStockId(location);
        row.setExpiryDate(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(days));
        row.setQualityStatus(0); row.setQtyTotal(quantity); row.setQtyAvail(quantity - frozen);
        row.setQtyFrozen(frozen); row.setQtySold(20); row.setQuantity(quantity); row.setFrozen(frozen);
        row.setLocationStatus(1); row.setValidLocation(1);
        return row;
    }

    private void snapshot(InventoryPreviewMapper.Row... rows) {
        when(mapper.selectSnapshot(scope, 3, 8)).thenReturn(List.of(rows));
    }

    @Test void fefoUsesExpiryAndExcludesFrozenWithoutReserving() {
        snapshot(row(1, 11, 20, 10, 0), row(2, 12, 10, 5, 3));
        var result = service.preview(request);
        assertFalse(result.reserved());
        assertEquals(request.getExpiryDayPolicy(), result.expiryDayPolicy());
        assertEquals(List.of(2L, 1L), result.allocations().stream().map(a -> a.getBatchId()).toList());
        assertEquals(List.of(2, 2), result.allocations().stream().map(a -> a.getQuantity()).toList());
        verify(drugs).validateDrugList(List.of(8L));
    }

    @Test void foreignWarehouseDeniedBeforeDrugAndStockLookup() {
        when(reads.selectWarehouse(scope, 3)).thenReturn(null);
        assertEquals(404, assertThrows(ServiceException.class, () -> service.preview(request)).getCode());
        verifyNoInteractions(drugs, mapper);
    }

    @Test void drugRejectionNeverQueriesStock() {
        doThrow(new ServiceException(400, "stopped")).when(drugs).validateDrugList(List.of(8L));
        assertThrows(ServiceException.class, () -> service.preview(request));
        verifyNoInteractions(mapper);
    }

    @Test void batchLocationMismatchCannotProducePartialPreview() {
        var row = row(1, 11, 10, 10, 0); row.setQtyTotal(11); row.setQtyAvail(11);
        snapshot(row);
        assertThrows(ServiceException.class, () -> service.preview(request));
    }

    @Test void disabledLocationStillCountsForConservationButCannotBeAllocated() {
        var enabled = row(1, 11, 10, 5, 0);
        var disabled = row(1, 12, 10, 6, 0); disabled.setLocationStatus(0);
        for (var row : List.of(enabled, disabled)) { row.setQtyTotal(11); row.setQtyAvail(11); }
        snapshot(enabled, disabled);
        assertEquals(11, service.preview(request).allocations().get(0).getLocationId());
        request.setQuantity(6);
        assertThrows(ServiceException.class, () -> service.preview(request));
    }

    @Test void expiredAndQualityStoppedStockCannotBeAllocated() {
        var stopped = row(1, 11, 10, 10, 0); stopped.setQualityStatus(1);
        snapshot(stopped, row(2, 12, -1, 10, 0));
        assertThrows(ServiceException.class, () -> service.preview(request));
    }

    @Test void malformedOwnershipFailsRatherThanDroppingTheRow() {
        var row = row(1, 11, 10, 10, 0); row.setValidLocation(0);
        snapshot(row);
        assertThrows(ServiceException.class, () -> service.preview(request));
    }

    @Test void rowLimitNeverTruncatesAndAllocates() {
        when(mapper.selectSnapshot(scope, 3, 8)).thenReturn(Collections.nCopies(10001, row(1, 11, 10, 10, 0)));
        assertThrows(ServiceException.class, () -> service.preview(request));
    }

    @Test void batchWithoutLocationsCannotHidePositiveStock() {
        var row = row(1, 11, 10, 10, 0);
        row.setStockId(null); row.setLocationId(null); row.setQuantity(null); row.setFrozen(null);
        snapshot(row);
        var error = assertThrows(ServiceException.class, () -> service.preview(request));
        assertTrue(error.getMessage().contains("快照异常"));
    }

    @Test void expiryPolicyAndPositiveQuantityAreMandatory() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
            request.setExpiryDayPolicy(null);
            assertFalse(factory.getValidator().validate(request).isEmpty());
            request.setExpiryDayPolicy(ExpiryDayPolicy.ALLOW_ON_EXPIRY_DATE);
            request.setQuantity(0);
            assertFalse(factory.getValidator().validate(request).isEmpty());
        }
    }

    @Test void xmlKeepsBrokenRowsVisibleAndBindsScope() throws Exception {
        var configuration = new Configuration();
        String resource = "mapper/inventory/InventoryPreviewMapper.xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        var parameters = Map.of("scope", scope, "warehouseId", 3L, "drugId", 8L);
        var bound = configuration.getMappedStatement(InventoryPreviewMapper.class.getName() + ".selectSnapshot").getBoundSql(parameters);
        assertTrue(bound.getSql().contains("LEFT JOIN ph_inv_location_stock"));
        assertTrue(bound.getSql().contains("LEFT JOIN ph_location"));
        assertTrue(bound.getSql().contains("LIMIT 10001"));
        assertFalse(bound.getSql().contains("FOR UPDATE"));
        for (var mapping : bound.getParameterMappings()) {
            assertNotNull(configuration.newMetaObject(parameters).getValue(mapping.getProperty()));
        }
    }
}
