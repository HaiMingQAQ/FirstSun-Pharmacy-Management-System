package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryStocktakeMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadAccess;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryStocktakeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InventoryStocktakeServiceTest {
    private final InventoryReadAccess access = mock(InventoryReadAccess.class);
    private final InventoryStocktakeMapper mapper = mock(InventoryStocktakeMapper.class);
    private final InventoryReadAccess.Scope scope = new InventoryReadAccess.Scope(1L, 7L);
    private final InventoryStocktakeService service = new InventoryStocktakeService(access, mapper);

    @BeforeEach
    void setup() {
        when(access.requireScope(null)).thenReturn(scope);
        var login = new LoginUser();
        login.setId(407L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void frozenStocktakeIsExplicitlyDeferredBeforeAnyInventoryWrite() {
        var ex = assertThrows(ServiceException.class, () -> service.create(request(1)));

        assertEquals("冻结式盘点本期延期：ph_inv_lock 归属仍待确认", ex.getMessage());
        verifyNoInteractions(mapper);
    }

    @Test
    void normalStocktakeCreationRemainsAvailable() {
        var warehouse = new InventoryStocktakeMapper.WarehouseLock();
        warehouse.setId(3L);
        warehouse.setStatus(1);
        warehouse.setStoreId(7L);
        when(mapper.lockWarehouse(scope, 3L)).thenReturn(warehouse);
        when(mapper.insertHeader(eq(scope), any(), any(), eq(407L), any())).thenAnswer(invocation -> {
            invocation.getArgument(4, InventoryStocktakeMapper.GeneratedKey.class).setId(88L);
            return 1;
        });
        when(mapper.updateTotalItem(scope, 88L, 407L)).thenReturn(1);

        assertEquals(88L, service.create(request(0)));
        verify(mapper).insertSnapshotLines(scope, 88L, 3L, 407L);
        verify(mapper).updateTotalItem(scope, 88L, 407L);
    }

    private static InventoryStocktakeCreateReqVO request(int freezeFlag) {
        var request = new InventoryStocktakeCreateReqVO();
        request.setWarehouseId(3L);
        request.setStocktakeType(0);
        request.setBlindFlag(0);
        request.setFreezeFlag(freezeFlag);
        return request;
    }
}
