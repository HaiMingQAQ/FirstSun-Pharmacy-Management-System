package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryCatalogMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryCatalogService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess;
import jakarta.validation.Validation;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Mock/validation/XML tests only: does not certify MySQL locks, generated keys or rollback. */
class InventoryCatalogTest {
    private final InventoryReadAccess access = mock(InventoryReadAccess.class);
    private final InventoryCatalogMapper mapper = mock(InventoryCatalogMapper.class);
    private final InventoryCatalogService service = new InventoryCatalogService(access, mapper);
    private final InventoryReadAccess.Scope scope = new InventoryReadAccess.Scope(1, 7);

    @BeforeEach void setup() {
        when(access.requireScope(null)).thenReturn(scope);
        var login = new LoginUser();
        login.setId(11L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private InventoryCatalogCreateReqVO.Location location() {
        var request = new InventoryCatalogCreateReqVO.Location();
        request.setWarehouseId(9L);
        request.setLocationCode("A-01");
        request.setLocationType(0);
        return request;
    }

    @Test void deniedScopeNeverTouchesCatalog() {
        when(access.requireScope(null)).thenThrow(new AccessDeniedException("denied"));
        assertThrows(AccessDeniedException.class, () -> service.createLocation(location()));
        verifyNoInteractions(mapper);
    }

    @Test void foreignOrMissingWarehouseDoesNotInsert() {
        when(mapper.lockWarehouseStatus(scope, 9)).thenReturn(null);
        var error = assertThrows(ServiceException.class, () -> service.createLocation(location()));
        assertEquals(404, error.getCode());
        verify(mapper).lockWarehouseStatus(scope, 9);
        verifyNoMoreInteractions(mapper);
    }

    @Test void inactiveWarehouseDoesNotInsert() {
        when(mapper.lockWarehouseStatus(scope, 9)).thenReturn(0);
        assertThrows(ServiceException.class, () -> service.createLocation(location()));
        verify(mapper).lockWarehouseStatus(scope, 9);
        verifyNoMoreInteractions(mapper);
    }

    @Test void locationLocksParentBeforeInsertAndUsesTrustedActor() {
        var request = location();
        when(mapper.lockWarehouseStatus(scope, 9)).thenReturn(1);
        when(mapper.insertLocation(eq(scope), same(request), eq("11"), any())).thenAnswer(call -> {
            call.<InventoryCatalogMapper.GeneratedKey>getArgument(3).setId(30L);
            return 1;
        });
        assertEquals(30, service.createLocation(request));
        var order = inOrder(mapper);
        order.verify(mapper).lockWarehouseStatus(scope, 9);
        order.verify(mapper).insertLocation(eq(scope), same(request), eq("11"), any());
    }

    @Test void duplicateFailureDoesNotRetryOrPretendSuccess() {
        var request = new InventoryCatalogCreateReqVO.Warehouse();
        when(mapper.insertWarehouse(eq(scope), same(request), eq("11"), any()))
                .thenThrow(new DuplicateKeyException("private database details"));
        var error = assertThrows(ServiceException.class, () -> service.createWarehouse(request));
        assertFalse(error.getMessage().contains("private"));
        verify(mapper).insertWarehouse(eq(scope), same(request), eq("11"), any());
        verifyNoMoreInteractions(mapper);
    }

    @Test void missingGeneratedKeyFailsInsteadOfSuccess() {
        when(mapper.insertWarehouse(any(), any(), any(), any())).thenReturn(1);
        assertThrows(IllegalStateException.class,
                () -> service.createWarehouse(new InventoryCatalogCreateReqVO.Warehouse()));
    }

    @Test void requestValidationMatchesUnsignedCapacityAndEnumerations() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = location();
            assertTrue(validator.validate(request).isEmpty());
            request.setMaxCapacity(4294967295L);
            assertTrue(validator.validate(request).isEmpty());
            request.setMaxCapacity(4294967296L);
            assertFalse(validator.validate(request).isEmpty());
            request.setMaxCapacity(0L);
            assertTrue(validator.validate(request).isEmpty());
            request.setLocationType(5);
            assertFalse(validator.validate(request).isEmpty());
            var warehouse = new InventoryCatalogCreateReqVO.Warehouse();
            warehouse.setWhCode(" "); warehouse.setWhName("中心"); warehouse.setTempZone(0);
            assertFalse(validator.validate(warehouse).isEmpty());
            warehouse.setWhCode("A01");
            assertTrue(validator.validate(warehouse).isEmpty());
        }
    }

    @Test void realXmlBindsScopeAuditAndNullableCapacity() throws Exception {
        var configuration = new Configuration();
        String resource = "mapper/inventory/InventoryCatalogMapper.xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        var request = location();
        request.setLocationCode("x'--");
        var parameters = Map.of("scope", scope, "id", 9L, "q", request, "actor", "11",
                "key", new InventoryCatalogMapper.GeneratedKey());
        var insert = configuration.getMappedStatement(InventoryCatalogMapper.class.getName() + ".insertLocation");
        assertArrayEquals(new String[]{"key.id"}, insert.getKeyProperties());
        var bound = insert.getBoundSql(parameters);
        assertFalse(bound.getSql().contains(request.getLocationCode()));
        for (var mapping : bound.getParameterMappings()) {
            Object value = configuration.newMetaObject(parameters).getValue(mapping.getProperty());
            if (!mapping.getProperty().equals("q.maxCapacity")) assertNotNull(value);
        }
        String lock = configuration.getMappedStatement(InventoryCatalogMapper.class.getName() + ".lockWarehouseStatus")
                .getBoundSql(parameters).getSql();
        assertTrue(lock.contains("FOR UPDATE"));
        assertTrue(lock.contains("store_id") && lock.contains("tenant_id") && lock.contains("deleted"));
    }
}
