package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderLineSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseReceiptMapper;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import cn.iocoder.yudao.module.pharmacy.service.base.StoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_ORDER_DISCOUNT_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_ORDER_HAS_RECEIPT;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_ORDER_LINE_EMPTY;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_ORDER_QTY_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_ORDER_STATUS_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_RECEIPT_QTY_EXCEED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PurchaseOrderServiceImpl} 单元测试
 *
 * 覆盖：金额服务端重算、药品 id 去重（D5 回归）、状态机拦截、收货入账累计的原子性与订单状态推进（D4/B2 相关）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseOrderServiceImplTest {

    @Mock
    private PurchaseOrderMapper orderMapper;
    @Mock
    private PurchaseOrderLineMapper orderLineMapper;
    @Mock
    private PurchaseReceiptMapper receiptMapper;
    @Mock
    private StoreService storeService;
    @Mock
    private SupplierService supplierService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private DrugApi drugApi;

    @InjectMocks
    private PurchaseOrderServiceImpl orderService;

    // ==================== 创建订单 ====================

    @Test
    void createOrder_shouldRecomputeAmountsAndLineAmounts() {
        mockCreateDependencies();

        PurchaseOrderSaveReqVO reqVO = buildReqVO(List.of(
                line(1L, 20, "6.00", "1.00"),
                line(2L, 5, "4.00", "0.50")));

        orderService.createOrder(reqVO);

        ArgumentCaptor<PurchaseOrderDO> orderCaptor = ArgumentCaptor.forClass(PurchaseOrderDO.class);
        verify(orderMapper).insert(orderCaptor.capture());
        PurchaseOrderDO saved = orderCaptor.getValue();
        // 20×6.00 = 120.00；5×4.00×0.5 = 10.00 → 含税 140.00 / 优惠 10.00 / 应付 130.00
        assertEquals(new BigDecimal("140.00"), saved.getTotalAmount());
        assertEquals(new BigDecimal("10.00"), saved.getDiscountAmount());
        assertEquals(new BigDecimal("130.00"), saved.getPayableAmount());
        assertEquals(25, saved.getTotalQty());
        assertEquals(PurchaseOrderStatusEnum.DRAFT.getStatus(), saved.getStatus());
        assertNotNull(saved.getOrderNo());
        assertTrue(saved.getOrderNo().startsWith("PO"));

        ArgumentCaptor<PurchaseOrderLineDO> lineCaptor = ArgumentCaptor.forClass(PurchaseOrderLineDO.class);
        verify(orderLineMapper, times(2)).insert(lineCaptor.capture());
        List<PurchaseOrderLineDO> lines = lineCaptor.getAllValues();
        assertEquals(1, lines.get(0).getLineNo());
        assertEquals(new BigDecimal("120.00"), lines.get(0).getLineAmount());
        assertEquals(2, lines.get(1).getLineNo());
        assertEquals(new BigDecimal("10.00"), lines.get(1).getLineAmount());
        // 行号由服务端生成、新建明细已收数量初始化为 0
        assertEquals(0, lines.get(0).getReceivedQty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createOrder_shouldDedupeDrugIdsBeforeValidate() {
        mockCreateDependencies();

        // 同一药品出现在两行（同药不同价格）是合法场景：D5 回归——不能因为重复 id 被误判"药品不存在"
        orderService.createOrder(buildReqVO(List.of(
                line(1L, 10, "5.00", "1.00"),
                line(1L, 2, "6.00", "1.00"))));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(drugApi).validateDrugList(captor.capture());
        assertEquals(1, captor.getValue().size(), "传给药品校验的 id 必须去重");
        assertEquals(1L, captor.getValue().get(0));
    }

    @Test
    void createOrder_shouldRejectEmptyLines() {
        PurchaseOrderSaveReqVO reqVO = buildReqVO(Collections.emptyList());
        ServiceException ex = assertThrows(ServiceException.class, () -> orderService.createOrder(reqVO));
        assertEquals(PURCHASE_ORDER_LINE_EMPTY.getCode(), ex.getCode());
    }

    @Test
    void createOrder_shouldRejectInvalidQty() {
        mockCreateDependencies();
        PurchaseOrderSaveReqVO reqVO = buildReqVO(List.of(line(1L, 0, "5.00", "1.00")));
        ServiceException ex = assertThrows(ServiceException.class, () -> orderService.createOrder(reqVO));
        assertEquals(PURCHASE_ORDER_QTY_INVALID.getCode(), ex.getCode());
    }

    @Test
    void createOrder_shouldRejectInvalidDiscount() {
        mockCreateDependencies();
        PurchaseOrderSaveReqVO reqVO = buildReqVO(List.of(line(1L, 1, "5.00", "1.50")));
        ServiceException ex = assertThrows(ServiceException.class, () -> orderService.createOrder(reqVO));
        assertEquals(PURCHASE_ORDER_DISCOUNT_INVALID.getCode(), ex.getCode());
    }

    // ==================== 更新订单 ====================

    @Test
    void updateOrder_shouldRejectNonDraft() {
        PurchaseOrderDO submitted = order(1L, PurchaseOrderStatusEnum.SUBMITTED.getStatus());
        when(orderMapper.selectById(1L)).thenReturn(submitted);

        PurchaseOrderSaveReqVO reqVO = buildReqVO(List.of(line(1L, 1, "5.00", "1.00")));
        reqVO.setId(1L);
        ServiceException ex = assertThrows(ServiceException.class, () -> orderService.updateOrder(reqVO));
        assertEquals(PURCHASE_ORDER_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void updateOrder_shouldPhysicallyRebuildLines() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, PurchaseOrderStatusEnum.DRAFT.getStatus()));
        mockCreateDependencies();

        PurchaseOrderSaveReqVO reqVO = buildReqVO(List.of(line(1L, 3, "2.00", "1.00")));
        reqVO.setId(1L);
        orderService.updateOrder(reqVO);

        // D4 回归：必须先物理删除旧明细，否则逻辑删除残留行会让重插行号撞唯一键
        verify(orderLineMapper).physicalDeleteByOrderId(1L);
        verify(orderLineMapper, never()).deleteByOrderId(any());
    }

    // ==================== 取消订单 ====================

    @Test
    void cancelOrder_shouldRejectWhenReceiptExists() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, PurchaseOrderStatusEnum.APPROVED.getStatus()));
        when(receiptMapper.countValidByOrderId(1L)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> orderService.cancelOrder(1L));
        assertEquals(PURCHASE_ORDER_HAS_RECEIPT.getCode(), ex.getCode());
    }

    @Test
    void cancelOrder_shouldRejectIssuedOrder() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, PurchaseOrderStatusEnum.ISSUED.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class, () -> orderService.cancelOrder(1L));
        assertEquals(PURCHASE_ORDER_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void cancelOrder_shouldSucceedForDraft() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, PurchaseOrderStatusEnum.DRAFT.getStatus()));
        when(receiptMapper.countValidByOrderId(1L)).thenReturn(0L);

        orderService.cancelOrder(1L);

        ArgumentCaptor<PurchaseOrderDO> captor = ArgumentCaptor.forClass(PurchaseOrderDO.class);
        verify(orderMapper).updateById(captor.capture());
        assertEquals(PurchaseOrderStatusEnum.CANCEL.getStatus(), captor.getValue().getStatus());
    }

    // ==================== 收货入账累计 ====================

    @Test
    void applyReceiptPosted_shouldFinishOrderWhenAllReceived() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, PurchaseOrderStatusEnum.ISSUED.getStatus()));
        // 第一次读：已收 0；原子累加后第二次读：已收 10 = 订购 10
        when(orderLineMapper.selectListByOrderId(1L)).thenReturn(
                List.of(orderLine(11L, 10, 0)),
                List.of(orderLine(11L, 10, 10)));
        when(orderLineMapper.update(any(), any())).thenReturn(1);

        Map<Long, Integer> delta = new HashMap<>();
        delta.put(11L, 10);
        orderService.applyReceiptPosted(1L, delta);

        verify(orderLineMapper).update(isNull(), any());
        ArgumentCaptor<PurchaseOrderDO> captor = ArgumentCaptor.forClass(PurchaseOrderDO.class);
        verify(orderMapper).updateById(captor.capture());
        assertEquals(PurchaseOrderStatusEnum.FINISHED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void applyReceiptPosted_shouldMarkPartialReceived() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, PurchaseOrderStatusEnum.ISSUED.getStatus()));
        when(orderLineMapper.selectListByOrderId(1L)).thenReturn(
                List.of(orderLine(11L, 10, 0)),
                List.of(orderLine(11L, 10, 4)));
        when(orderLineMapper.update(any(), any())).thenReturn(1);

        Map<Long, Integer> delta = new HashMap<>();
        delta.put(11L, 4);
        orderService.applyReceiptPosted(1L, delta);

        ArgumentCaptor<PurchaseOrderDO> captor = ArgumentCaptor.forClass(PurchaseOrderDO.class);
        verify(orderMapper).updateById(captor.capture());
        assertEquals(PurchaseOrderStatusEnum.PARTIAL_RECEIVED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void applyReceiptPosted_shouldRejectOverReceipt() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, PurchaseOrderStatusEnum.ISSUED.getStatus()));
        when(orderLineMapper.selectListByOrderId(1L)).thenReturn(List.of(orderLine(11L, 10, 0)));
        // 原子更新的 where 条件（received_qty + delta <= order_qty）不满足 → 影响 0 行
        when(orderLineMapper.update(any(), any())).thenReturn(0);

        Map<Long, Integer> delta = new HashMap<>();
        delta.put(11L, 100);
        ServiceException ex = assertThrows(ServiceException.class, () -> orderService.applyReceiptPosted(1L, delta));
        assertEquals(PURCHASE_RECEIPT_QTY_EXCEED.getCode(), ex.getCode());
    }

    @Test
    void applyReceiptPosted_shouldRejectLineNotBelongToOrder() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, PurchaseOrderStatusEnum.ISSUED.getStatus()));
        when(orderLineMapper.selectListByOrderId(1L)).thenReturn(List.of(orderLine(11L, 10, 0)));

        Map<Long, Integer> delta = new HashMap<>();
        delta.put(999L, 1);
        assertThrows(ServiceException.class, () -> orderService.applyReceiptPosted(1L, delta));
        verify(orderLineMapper, never()).update(any(), any());
    }

    // ==================== 辅助方法 ====================

    private void mockCreateDependencies() {
        when(storeService.validateStoreExistsAndOpen(anyLong())).thenReturn(new StoreDO());
        when(supplierService.validateSupplierPurchasable(anyLong())).thenReturn(new SupplierDO());
        when(orderMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            PurchaseOrderDO order = invocation.getArgument(0);
            order.setId(100L);
            return 1;
        }).when(orderMapper).insert(any(PurchaseOrderDO.class));
    }

    private PurchaseOrderSaveReqVO buildReqVO(List<PurchaseOrderLineSaveReqVO> lines) {
        PurchaseOrderSaveReqVO reqVO = new PurchaseOrderSaveReqVO();
        reqVO.setStoreId(407L);
        reqVO.setSupplierId(1L);
        reqVO.setOrderDate(LocalDate.of(2026, 9, 11));
        reqVO.setLines(lines);
        return reqVO;
    }

    private PurchaseOrderLineSaveReqVO line(Long drugId, int qty, String price, String discount) {
        PurchaseOrderLineSaveReqVO vo = new PurchaseOrderLineSaveReqVO();
        vo.setDrugId(drugId);
        vo.setOrderQty(qty);
        vo.setUnitPrice(new BigDecimal(price));
        vo.setDiscountRate(new BigDecimal(discount));
        return vo;
    }

    private PurchaseOrderDO order(Long id, Integer status) {
        PurchaseOrderDO order = new PurchaseOrderDO();
        order.setId(id);
        order.setStatus(status);
        order.setStoreId(407L);
        order.setSupplierId(1L);
        order.setOrderNo("PO407-20260911-0001");
        order.setOrderDate(LocalDate.of(2026, 9, 11));
        return order;
    }

    private PurchaseOrderLineDO orderLine(Long id, int orderQty, int receivedQty) {
        PurchaseOrderLineDO line = new PurchaseOrderLineDO();
        line.setId(id);
        line.setOrderId(1L);
        line.setDrugId(1L);
        line.setOrderQty(orderQty);
        line.setReceivedQty(receivedQty);
        line.setUnitPrice(new BigDecimal("5.00"));
        line.setDiscountRate(BigDecimal.ONE);
        line.setLineAmount(new BigDecimal("50.00"));
        return line;
    }
}
