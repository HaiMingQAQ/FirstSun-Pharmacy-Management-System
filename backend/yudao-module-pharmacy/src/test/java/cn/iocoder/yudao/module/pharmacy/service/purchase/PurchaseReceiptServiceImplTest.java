package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReceiveItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReceiveResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptLineSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseReceiptLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseReceiptMapper;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseReceiptStatusEnum;
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
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_RECEIPT_LINE_EMPTY;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_RECEIPT_LOCATION_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_RECEIPT_POST_DUP;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_RECEIPT_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PurchaseReceiptServiceImpl} 单元测试
 *
 * 覆盖：状态机拦截、入账防重复（CAS）、库存服务未就绪时的明确失败与"不推进订单"、
 * 入账前货位校验（这些是与事务一致性最相关的规则）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseReceiptServiceImplTest {

    @Mock
    private PurchaseReceiptMapper receiptMapper;
    @Mock
    private PurchaseReceiptLineMapper receiptLineMapper;
    @Mock
    private PurchaseOrderService purchaseOrderService;
    @Mock
    private SupplierLicenseService supplierLicenseService;
    @Mock
    private StoreService storeService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private DrugApi drugApi;
    @Mock
    private InventoryFacade inventoryFacade;

    @InjectMocks
    private PurchaseReceiptServiceImpl receiptService;

    @Test
    void submitReceipt_shouldRejectNonDraft() {
        when(receiptMapper.selectById(1L)).thenReturn(receipt(PurchaseReceiptStatusEnum.SUBMITTED.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.submitReceipt(1L));
        assertEquals(PURCHASE_RECEIPT_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void submitReceipt_shouldRejectEmptyLines() {
        when(receiptMapper.selectById(1L)).thenReturn(receipt(PurchaseReceiptStatusEnum.DRAFT.getStatus()));
        when(receiptLineMapper.selectListByReceiptId(1L)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.submitReceipt(1L));
        assertEquals(PURCHASE_RECEIPT_LINE_EMPTY.getCode(), ex.getCode());
    }

    @Test
    void submitReceipt_shouldMoveDraftToSubmitted() {
        when(receiptMapper.selectById(1L)).thenReturn(receipt(PurchaseReceiptStatusEnum.DRAFT.getStatus()));
        when(receiptLineMapper.selectListByReceiptId(1L)).thenReturn(List.of(receiptLine(11L, 1, null)));

        receiptService.submitReceipt(1L);

        ArgumentCaptor<PurchaseReceiptDO> captor = ArgumentCaptor.forClass(PurchaseReceiptDO.class);
        verify(receiptMapper).updateById(captor.capture());
        assertEquals(PurchaseReceiptStatusEnum.SUBMITTED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void postReceipt_shouldRejectWhenNotSubmitted() {
        when(receiptMapper.selectById(1L)).thenReturn(receipt(PurchaseReceiptStatusEnum.DRAFT.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.postReceipt(1L));
        assertEquals(PURCHASE_RECEIPT_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void postReceipt_shouldRejectDuplicatePost() {
        when(receiptMapper.selectById(1L)).thenReturn(receipt(PurchaseReceiptStatusEnum.POSTED.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.postReceipt(1L));
        assertEquals(PURCHASE_RECEIPT_POST_DUP.getCode(), ex.getCode());
    }

    @Test
    void postReceipt_shouldRejectMissingLocation() {
        mockPostableReceipt();
        when(receiptLineMapper.selectListByReceiptId(1L)).thenReturn(List.of(receiptLine(11L, 4, null)));

        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.postReceipt(1L));
        assertEquals(PURCHASE_RECEIPT_LOCATION_REQUIRED.getCode(), ex.getCode());
        // 货位校验在 CAS 之前，不应写状态、不应调用库存服务
        verify(receiptMapper, never()).update(any(), any());
        verify(inventoryFacade, never()).receive(anyLong(), anyString(), any());
    }

    @Test
    void postReceipt_shouldRejectWhenCasAffectsNoRow() {
        mockPostableReceipt();
        when(receiptLineMapper.selectListByReceiptId(1L)).thenReturn(List.of(receiptLine(11L, 4, 1L)));
        // 并发下状态已被别人改掉：CAS 影响 0 行
        when(receiptMapper.update(any(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.postReceipt(1L));
        assertEquals(PURCHASE_RECEIPT_POST_DUP.getCode(), ex.getCode());
        verify(inventoryFacade, never()).receive(anyLong(), anyString(), any());
    }

    @Test
    void postReceipt_shouldFailClearlyAndNotAdvanceOrderWhenInventoryUnavailable() {
        mockPostableReceipt();
        when(receiptLineMapper.selectListByReceiptId(1L)).thenReturn(List.of(receiptLine(11L, 4, 1L)));
        when(receiptMapper.update(any(), any())).thenReturn(1);
        when(inventoryFacade.receive(anyLong(), anyString(), any())).thenThrow(new UnsupportedOperationException("C 未实现"));

        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.postReceipt(1L));
        assertEquals(INV_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
        // 库存服务失败时绝不推进订单已收数量（由 @Transactional 回滚状态 CAS，保证单据与库存一致）
        verify(purchaseOrderService, never()).applyReceiptPosted(any(), any());
    }

    @Test
    void postReceipt_shouldApplyOrderReceiptWhenInventorySucceeds() {
        mockPostableReceipt();
        when(receiptLineMapper.selectListByReceiptId(1L)).thenReturn(List.of(receiptLine(11L, 4, 1L)));
        when(receiptMapper.update(any(), any())).thenReturn(1);
        when(inventoryFacade.receive(anyLong(), anyString(), any())).thenReturn(
                ReceiveResult.builder().lines(List.of(ReceiveResult.ReceiveLineResult.builder()
                        .bizLineId("12").batchId(88L).build())).build());

        receiptService.postReceipt(1L);

        // 成功后：回写批次号 + 累计订单已收数量
        ArgumentCaptor<PurchaseReceiptLineDO> lineCaptor = ArgumentCaptor.forClass(PurchaseReceiptLineDO.class);
        verify(receiptLineMapper).updateById(lineCaptor.capture());
        assertEquals(88L, lineCaptor.getValue().getCreateBatchId());
        verify(purchaseOrderService).applyReceiptPosted(any(), any());
        verify(inventoryFacade).receive(anyLong(), anyString(), any());
    }

    @Test
    void voidReceipt_shouldOnlyAllowDraft() {
        when(receiptMapper.selectById(1L)).thenReturn(receipt(PurchaseReceiptStatusEnum.SUBMITTED.getStatus()));
        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.voidReceipt(1L));
        assertEquals(PURCHASE_RECEIPT_STATUS_INVALID.getCode(), ex.getCode());

        when(receiptMapper.selectById(2L)).thenReturn(receipt(2L, PurchaseReceiptStatusEnum.DRAFT.getStatus()));
        receiptService.voidReceipt(2L);
        ArgumentCaptor<PurchaseReceiptDO> captor = ArgumentCaptor.forClass(PurchaseReceiptDO.class);
        verify(receiptMapper).updateById(captor.capture());
        assertEquals(PurchaseReceiptStatusEnum.VOIDED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void deleteReceipt_shouldRejectSubmitted() {
        when(receiptMapper.selectById(1L)).thenReturn(receipt(PurchaseReceiptStatusEnum.SUBMITTED.getStatus()));
        ServiceException ex = assertThrows(ServiceException.class, () -> receiptService.deleteReceipt(1L));
        assertEquals(PURCHASE_RECEIPT_STATUS_INVALID.getCode(), ex.getCode());
        verify(receiptMapper, never()).deleteById(anyLong());
    }

    /**
     * D6 回归：单号必须跳过「已被逻辑删除单据占用」的流水号。
     *
     * 背景：uk_receipt_no 唯一键不包含 deleted 列，逻辑删除的单据仍然占号；
     * 而 MyBatis-Plus 的 selectCount 会自动过滤 deleted=1，两者口径不一致时
     * 会算出一个已被占用的号，连续重试 5 次后报「收货单号已存在」，导致收货单完全无法创建。
     * 这里模拟「前缀内最大号是 0023（其中 0019~0023 有已删除单据）」，断言新号取 0024。
     */
    @Test
    void createReceipt_shouldSkipNumbersOccupiedBySoftDeletedReceipts() {
        mockCreatableReceipt();
        when(receiptMapper.selectMaxReceiptNo("GR407-20260911-")).thenReturn("GR407-20260911-0023");
        mockInsertAssigningId(101L);

        receiptService.createReceipt(createReqVO());

        ArgumentCaptor<PurchaseReceiptDO> captor = ArgumentCaptor.forClass(PurchaseReceiptDO.class);
        verify(receiptMapper).insert(captor.capture());
        assertEquals("GR407-20260911-0024", captor.getValue().getReceiptNo());
    }

    /**
     * D6 回归（并发分支）：取号后仍被别人抢占时，必须重算单号重试，而不是直接失败。
     */
    @Test
    void createReceipt_shouldRetryWithNextNumberWhenNumberTakenConcurrently() {
        mockCreatableReceipt();
        // 第一次取号时库里最大是 0023 → 生成 0024；被并发抢走后重取号，此时库里已有 0024 → 生成 0025
        String[] maxNo = {"GR407-20260911-0023", "GR407-20260911-0024"};
        final int[] pick = {0};
        when(receiptMapper.selectMaxReceiptNo("GR407-20260911-"))
                .thenAnswer(inv -> maxNo[Math.min(pick[0]++, maxNo.length - 1)]);
        // 第一次插入模拟被并发抢占（唯一键冲突），第二次成功。
        // 注意：这里不能用 ArgumentCaptor 记录单号——重试复用同一个 DO 对象，
        // captor 两次捕获到的是同一个引用，读出来都会是最后一次赋的值。
        final List<String> attemptedNos = new ArrayList<>();
        final int[] insertCall = {0};
        doAnswer(inv -> {
            insertCall[0]++;
            PurchaseReceiptDO d = inv.getArgument(0, PurchaseReceiptDO.class);
            attemptedNos.add(d.getReceiptNo());
            if (insertCall[0] == 1) {
                throw new DuplicateKeyException("uk_receipt_no");
            }
            d.setId(102L);
            return 1;
        }).when(receiptMapper).insert(any(PurchaseReceiptDO.class));

        Long id = receiptService.createReceipt(createReqVO());

        assertEquals(102L, id);
        assertEquals(2, insertCall[0]);
        assertEquals(List.of("GR407-20260911-0024", "GR407-20260911-0025"), attemptedNos);
    }

    // ==================== 辅助方法 ====================

    private void mockCreatableReceipt() {
        EmployeeDO receiver = new EmployeeDO();
        receiver.setId(407L);
        when(employeeService.getEmployeeByUserId(any())).thenReturn(receiver);
        when(drugApi.getDrugList(any())).thenReturn(List.of(new DrugRespDTO().setId(1L)));
        PurchaseOrderDO order = new PurchaseOrderDO();
        order.setId(5L);
        order.setStoreId(407L);
        order.setStatus(PurchaseOrderStatusEnum.ISSUED.getStatus());
        when(purchaseOrderService.validateOrderExists(5L)).thenReturn(order);
        PurchaseOrderLineDO orderLine = new PurchaseOrderLineDO();
        orderLine.setId(11L);
        orderLine.setOrderId(5L);
        orderLine.setDrugId(1L);
        orderLine.setOrderQty(3);
        orderLine.setReceivedQty(0);
        orderLine.setUnitPrice(new BigDecimal("5.00"));
        when(purchaseOrderService.getOrderLines(5L)).thenReturn(List.of(orderLine));
    }

    private void mockInsertAssigningId(long id) {
        when(receiptMapper.insert(any(PurchaseReceiptDO.class))).thenAnswer(inv -> {
            inv.getArgument(0, PurchaseReceiptDO.class).setId(id);
            return 1;
        });
    }

    private PurchaseReceiptSaveReqVO createReqVO() {
        PurchaseReceiptLineSaveReqVO line = new PurchaseReceiptLineSaveReqVO();
        line.setOrderLineId(11L);
        line.setDrugId(1L);
        line.setBatchNo("B20260911");
        line.setExpiryDate(LocalDate.now().plusYears(1));
        line.setQty(1);
        line.setUnitPrice(new BigDecimal("5.00"));
        PurchaseReceiptSaveReqVO reqVO = new PurchaseReceiptSaveReqVO();
        reqVO.setOrderId(5L);
        reqVO.setStoreId(407L);
        reqVO.setWarehouseId(1L);
        reqVO.setReceiveDate(LocalDateTime.of(2026, 9, 11, 10, 0));
        reqVO.setLines(List.of(line));
        return reqVO;
    }
    private void mockPostableReceipt() {
        PurchaseReceiptDO receipt = receipt(PurchaseReceiptStatusEnum.SUBMITTED.getStatus());
        when(receiptMapper.selectById(1L)).thenReturn(receipt);
        PurchaseOrderDO order = new PurchaseOrderDO();
        order.setId(5L);
        order.setSupplierId(9L);
        order.setStatus(PurchaseOrderStatusEnum.ISSUED.getStatus());
        when(purchaseOrderService.validateOrderExists(5L)).thenReturn(order);
    }

    private PurchaseReceiptDO receipt(Integer status) {
        return receipt(1L, status);
    }

    private PurchaseReceiptDO receipt(Long id, Integer status) {
        PurchaseReceiptDO receipt = new PurchaseReceiptDO();
        receipt.setId(id);
        receipt.setReceiptNo("GR407-20260911-0001");
        receipt.setOrderId(5L);
        receipt.setStoreId(407L);
        receipt.setWarehouseId(1L);
        receipt.setReceiveBy(407L);
        receipt.setReceiveDate(LocalDateTime.of(2026, 9, 11, 10, 0));
        receipt.setStatus(status);
        receipt.setTotalQty(4);
        receipt.setTotalAmount(new BigDecimal("20.00"));
        return receipt;
    }

    private PurchaseReceiptLineDO receiptLine(Long id, int qty, Long locationId) {
        PurchaseReceiptLineDO line = new PurchaseReceiptLineDO();
        line.setId(id);
        line.setReceiptId(1L);
        line.setLineNo(1);
        line.setOrderLineId(11L);
        line.setDrugId(1L);
        line.setBatchNo("B20260911");
        line.setManufactureDate(LocalDate.of(2026, 8, 1));
        line.setExpiryDate(LocalDate.of(2028, 8, 1));
        line.setQty(qty);
        line.setUnitPrice(new BigDecimal("5.00"));
        line.setAmount(new BigDecimal("20.00"));
        line.setLocationId(locationId);
        line.setQualityFlag(1);
        return line;
    }
}
