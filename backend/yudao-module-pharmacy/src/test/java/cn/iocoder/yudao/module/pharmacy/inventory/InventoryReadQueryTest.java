package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryReadMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryReadQueryTest {
    @Test void expiryRangeIncludesBothEndpointsAndRejectsReverse() {
        var query = new InventoryReadQuery();
        query.setExpiryFrom(LocalDate.of(2026, 9, 12)); query.setExpiryTo(query.getExpiryFrom());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(query).isEmpty());
            query.setExpiryTo(query.getExpiryFrom().minusDays(1));
            assertFalse(factory.getValidator().validate(query).isEmpty());
        }
    }
    @Test void flowWindowIsHalfOpenAndCannotBeEmpty() {
        var query = new InventoryReadQuery();
        query.setFlowFrom(LocalDateTime.of(2026, 9, 12, 0, 0)); query.setFlowBefore(query.getFlowFrom());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(query).isEmpty());
            query.setFlowBefore(query.getFlowFrom().plusDays(1));
            assertTrue(factory.getValidator().validate(query).isEmpty());
        }
    }
    @Test void qualityAndIdentifierBoundsAreValidated() {
        var query = new InventoryReadQuery(); query.setQualityStatus(3); query.setFlowId(0L);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertEquals(2, factory.getValidator().validate(query).size());
            query.setQualityStatus(0); query.setFlowId(1L);
            assertTrue(factory.getValidator().validate(query).isEmpty());
        }
    }
    @Test void allMissingOrForeignDetailsReturnSameNotFound() {
        var access = mock(InventoryReadAccess.class); var mapper = mock(InventoryReadMapper.class);
        when(access.requireScope(null)).thenReturn(new InventoryReadAccess.Scope(1, 7));
        var service = new InventoryReadService(access, mapper);
        assertEquals(404, assertThrows(ServiceException.class, () -> service.batch(5)).getCode());
        assertEquals(404, assertThrows(ServiceException.class, () -> service.location(5)).getCode());
        assertEquals(404, assertThrows(ServiceException.class, () -> service.flow(5)).getCode());
    }
    @Test void detailQueriesBindTrustedScopeAndExactId() {
        var access = mock(InventoryReadAccess.class); var mapper = mock(InventoryReadMapper.class);
        var scope = new InventoryReadAccess.Scope(1, 7);
        when(access.requireScope(null)).thenReturn(scope);
        var batch = new InventoryReadVO.Batch(); batch.setId(9L);
        when(mapper.selectBatches(eq(scope), argThat(q -> Long.valueOf(9).equals(q.getBatchId())))).thenReturn(List.of(batch));
        assertSame(batch, new InventoryReadService(access, mapper).batch(9));
    }
}
