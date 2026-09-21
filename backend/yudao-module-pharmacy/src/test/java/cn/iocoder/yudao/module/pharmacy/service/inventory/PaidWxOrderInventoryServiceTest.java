package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.*;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.*;
import cn.iocoder.yudao.module.pharmacy.service.member.WxOrderPaymentAccess;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaidWxOrderInventoryServiceTest {
    WxOrderPaymentAccess access;
    WxOrderLineMapper lines;
    WxOrderLineAllocMapper allocations;
    InventoryFacadeAdapter inventory;
    PaidWxOrderInventoryService service;
    WxOrderLineAllocDO allocation;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        access = mock(WxOrderPaymentAccess.class); lines = mock(WxOrderLineMapper.class);
        allocations = mock(WxOrderLineAllocMapper.class); inventory = mock(InventoryFacadeAdapter.class);
        service = new PaidWxOrderInventoryService(access, lines, allocations, inventory);
        var order = new WxOrderDO(); order.setId(100L); order.setStoreId(4L); order.setOrderNo("WX100");
        when(access.requirePaidOrder(100L,50L)).thenReturn(order);
        var line = new WxOrderLineDO(); line.setId(10L); line.setWxOrderId(100L); line.setDrugId(11L); line.setQty(2);
        when(lines.selectListByWxOrderId(100L)).thenReturn(List.of(line));
        allocation = new WxOrderLineAllocDO(); allocation.setId(20L); allocation.setWxOrderId(100L);
        allocation.setWxOrderLineId(10L); allocation.setOrderNo("WX100"); allocation.setDrugId(11L);
        allocation.setQty(2); allocation.setStatus(WxOrderLineAllocDO.STATUS_FROZEN);
        allocation.setBatchId(30L); allocation.setLocationId(40L);
        when(allocations.selectListByWxOrderId(100L)).thenReturn(List.of(allocation));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void consumesOnlyPersistedReservationUsingOriginalIdempotencyKeys() {
        service.consumeReservation(100L,50L);
        verify(inventory).consumeReservation(eq(new InventoryReadAccess.Scope(7,4)), argThat(items -> {
            var item = items.get(0);
            return items.size() == 1 && item.getBizNo().equals("WX100") && item.getBizLineId().equals(20L)
                    && item.getOriginalBizLineId().equals(10L) && item.getOriginalBizNo().equals("WX100")
                    && item.getDrugId().equals(11L) && item.getQty() == 2
                    && item.getBatchId().equals(30L) && item.getLocationId().equals(40L);
        }), eq(0L));
    }
    @Test void cannotDeductDirectlyWhenReservationExists() {
        assertThrows(RuntimeException.class, () -> service.deduct(100L,50L)); verifyNoInteractions(inventory);
    }
    @Test void rejectsReleasedOrMismatchedReservation() {
        allocation.setStatus(WxOrderLineAllocDO.STATUS_SETTLED);
        assertThrows(RuntimeException.class, () -> service.consumeReservation(100L,50L));
        allocation.setStatus(WxOrderLineAllocDO.STATUS_FROZEN); allocation.setQty(1);
        assertThrows(RuntimeException.class, () -> service.consumeReservation(100L,50L));
        allocation.setQty(2); allocation.setDrugId(99L);
        assertThrows(RuntimeException.class, () -> service.consumeReservation(100L,50L));
        allocation.setDrugId(11L); allocation.setWxOrderLineId(99L);
        assertThrows(RuntimeException.class, () -> service.consumeReservation(100L,50L));
        verifyNoInteractions(inventory);
    }
    @Test void deniedProofCannotReadLinesOrRunStockWork() {
        when(access.requirePaidOrder(100L,50L)).thenThrow(new AccessDeniedException("untrusted payment"));
        assertThrows(AccessDeniedException.class, () -> service.deduct(100L,50L));
        assertThrows(AccessDeniedException.class, () -> service.consumeReservation(100L,50L));
        verifyNoInteractions(lines,allocations,inventory);
    }
}
