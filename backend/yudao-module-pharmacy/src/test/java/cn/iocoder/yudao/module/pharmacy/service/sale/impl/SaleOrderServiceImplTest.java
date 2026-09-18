package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhPosShiftDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.PosShiftMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SalePaymentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_AMOUNT_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_CASHIER_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_GOODS_EMPTY;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_NO_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_PAYMENT_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_RX_PRESC_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_SHIFT_CONFLICT;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_SHIFT_NOT_OPEN;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_SHIFT_NOT_OWNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SaleOrderServiceImpl} 单元测试：销售幂等、金额校验、处方药校验、库存依赖回滚，
 * 以及 D-1 班次约束：未开班/已交班/跨员工拦截、不依赖前端 shiftId。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaleOrderServiceImplTest {

    @Mock
    private SaleOrderMapper saleOrderMapper;
    @Mock
    private SaleOrderLineMapper saleOrderLineMapper;
    @Mock
    private SalePaymentMapper salePaymentMapper;
    @Mock
    private InventoryFacade inventoryFacade;
    @Mock
    private PosShiftMapper posShiftMapper;

    @InjectMocks
    private SaleOrderServiceImpl saleOrderService;

    /** 默认营业中班次：门店 1 / POS-01 / 收银员 100 */
    private PhPosShiftDO openingShift;

    @BeforeEach
    void setUp() {
        // 订单号与支付幂等号默认不存在
        when(saleOrderMapper.selectByOrderNo(anyString())).thenReturn(null);
        when(salePaymentMapper.selectByPaymentNo(anyString())).thenReturn(null);
        // 默认存在营业中班次（status=0，归属当前收银员）
        openingShift = new PhPosShiftDO();
        openingShift.setId(10L);
        openingShift.setStoreId(1L);
        openingShift.setPosNo("POS-01");
        openingShift.setCashierId(100L);
        openingShift.setStatus(0);
        when(posShiftMapper.selectList(any())).thenReturn(Collections.singletonList(openingShift));
        // insert 回填主键，模拟数据库自增
        doAnswer(invocation -> {
            invocation.getArgument(0, PhSaleOrderDO.class).setId(1L);
            return 1;
        }).when(saleOrderMapper).insert(any(PhSaleOrderDO.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, PhSaleOrderLineDO.class).setId(11L);
            return 1;
        }).when(saleOrderLineMapper).insert(any(PhSaleOrderLineDO.class));
        when(salePaymentMapper.insert(any(PhSalePaymentDO.class))).thenReturn(1);
    }

    private SaleOrderSaveReqVO buildReqVO() {
        SaleOrderSaveReqVO reqVO = new SaleOrderSaveReqVO();
        reqVO.setOrderNo("SO-1-20260909101000-001");
        reqVO.setStoreId(1L);
        reqVO.setShiftId(10L);
        reqVO.setCashierId(100L);
        reqVO.setPosNo("POS-01");
        reqVO.setCustomerName("散客");

        SaleOrderSaveReqVO.Item item = new SaleOrderSaveReqVO.Item();
        item.setDrugId(1L);
        item.setBatchId(11L);
        item.setQty(2);
        item.setPrice(new BigDecimal("25.00"));
        item.setIsRx(0);
        item.setLocationId(21L);
        item.setDrugName("阿莫西林胶囊");
        item.setSpecification("0.25g*24粒");
        item.setUnit("盒");
        reqVO.setItems(Collections.singletonList(item));

        SaleOrderSaveReqVO.Payment payment = new SaleOrderSaveReqVO.Payment();
        payment.setPayMethod(1); // 现金
        payment.setPayAmount(new BigDecimal("50.00"));
        payment.setPaymentNo("PAY-20260909101000-001");
        reqVO.setPayments(Collections.singletonList(payment));
        return reqVO;
    }

    @Test
    void testCreateSaleOrder_success() {
        doAnswer(invocation -> {
            List<DeductItem> items = invocation.getArgument(1);
            assertEquals("SO-1-20260909101000-001", items.get(0).getBizNo());
            assertEquals(11L, items.get(0).getBizLineId());
            return null;
        }).when(inventoryFacade).deduct(anyLong(), anyList());
        Long id = saleOrderService.createSaleOrder(buildReqVO());
        assertEquals(1L, id);
        // 落库订单必须使用后端解析的营业中班次，而非前端传入值
        ArgumentCaptor<PhSaleOrderDO> orderCaptor = ArgumentCaptor.forClass(PhSaleOrderDO.class);
        verify(saleOrderMapper).insert(orderCaptor.capture());
        assertEquals(10L, orderCaptor.getValue().getShiftId());
        // 明细与支付均入库，库存按明细扣减
        verify(saleOrderLineMapper).insert(any(PhSaleOrderLineDO.class));
        verify(salePaymentMapper).insert(any(PhSalePaymentDO.class));
        verify(inventoryFacade).deduct(anyLong(), anyList());
    }

    @Test
    void testCreateSaleOrder_duplicateOrderNo() {
        when(saleOrderMapper.selectByOrderNo(anyString())).thenReturn(new PhSaleOrderDO());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(buildReqVO()));
        assertEquals(SALE_ORDER_NO_DUPLICATE.getCode(), ex.getCode());
        verify(inventoryFacade, never()).deduct(anyLong(), anyList());
    }

    @Test
    void testCreateSaleOrder_duplicatePaymentNo() {
        when(salePaymentMapper.selectByPaymentNo(anyString())).thenReturn(new PhSalePaymentDO());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(buildReqVO()));
        assertEquals(SALE_ORDER_PAYMENT_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void testCreateSaleOrder_goodsEmpty() {
        SaleOrderSaveReqVO reqVO = buildReqVO();
        reqVO.setItems(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(reqVO));
        assertEquals(SALE_ORDER_GOODS_EMPTY.getCode(), ex.getCode());
    }

    @Test
    void testCreateSaleOrder_qtyInvalid() {
        SaleOrderSaveReqVO reqVO = buildReqVO();
        reqVO.getItems().get(0).setQty(0);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(reqVO));
        assertEquals(SALE_ORDER_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    void testCreateSaleOrder_rxWithoutPrescription() {
        SaleOrderSaveReqVO reqVO = buildReqVO();
        reqVO.getItems().get(0).setIsRx(1);
        reqVO.getItems().get(0).setPrescId(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(reqVO));
        assertEquals(SALE_ORDER_RX_PRESC_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void testCreateSaleOrder_inventoryUnavailable_rollback() {
        // C 库存服务未实现时，deduct 抛 UnsupportedOperationException，应转 INV_SERVICE_UNAVAILABLE
        doThrow(new UnsupportedOperationException("not implemented"))
                .when(inventoryFacade).deduct(anyLong(), anyList());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(buildReqVO()));
        assertEquals(INV_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
        // 事务整体回滚：不返回订单号，调用方收不到成功
    }

    // ==================== D-1：班次约束 ====================

    @Test
    void testCreateSaleOrder_noOpeningShift_rejected() {
        // 未开班：门店/收银台无营业中班次 → 拒绝销售，不落销售单/支付明细/库存流水
        when(posShiftMapper.selectList(any())).thenReturn(Collections.emptyList());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(buildReqVO()));
        assertEquals(SALE_ORDER_SHIFT_NOT_OPEN.getCode(), ex.getCode());
        verify(saleOrderMapper, never()).insert(any(PhSaleOrderDO.class));
        verify(saleOrderLineMapper, never()).insert(any(PhSaleOrderLineDO.class));
        verify(salePaymentMapper, never()).insert(any(PhSalePaymentDO.class));
        verify(inventoryFacade, never()).deduct(anyLong(), anyList());
    }

    @Test
    void testCreateSaleOrder_closedShift_notUsable() {
        // 已交班（status=1）班次：查询仅取营业中班次，已交班/已关闭不可用于销售
        PhPosShiftDO closed = new PhPosShiftDO();
        closed.setId(10L);
        closed.setStoreId(1L);
        closed.setPosNo("POS-01");
        closed.setCashierId(100L);
        closed.setStatus(1); // 已交班
        when(posShiftMapper.selectList(any())).thenReturn(Collections.emptyList());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(buildReqVO()));
        assertEquals(SALE_ORDER_SHIFT_NOT_OPEN.getCode(), ex.getCode());
        verify(saleOrderMapper, never()).insert(any(PhSaleOrderDO.class));
        verify(inventoryFacade, never()).deduct(anyLong(), anyList());
    }

    @Test
    void testCreateSaleOrder_otherCashierShift_rejected() {
        // 跨员工：营业中班次属于其他收银员 → 拒绝销售
        PhPosShiftDO other = new PhPosShiftDO();
        other.setId(10L);
        other.setStoreId(1L);
        other.setPosNo("POS-01");
        other.setCashierId(999L);
        other.setStatus(0);
        when(posShiftMapper.selectList(any())).thenReturn(Collections.singletonList(other));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(buildReqVO()));
        assertEquals(SALE_ORDER_SHIFT_NOT_OWNER.getCode(), ex.getCode());
        verify(saleOrderMapper, never()).insert(any(PhSaleOrderDO.class));
        verify(inventoryFacade, never()).deduct(anyLong(), anyList());
    }

    @Test
    void testCreateSaleOrder_withoutShiftId_resolveByStoreAndCashier() {
        // 前端不传 shiftId：后端按门店+收银台解析营业中班次，仍正常落库
        SaleOrderSaveReqVO reqVO = buildReqVO();
        reqVO.setShiftId(null);
        doAnswer(invocation -> null).when(inventoryFacade).deduct(anyLong(), anyList());
        Long id = saleOrderService.createSaleOrder(reqVO);
        assertEquals(1L, id);
        ArgumentCaptor<PhSaleOrderDO> orderCaptor = ArgumentCaptor.forClass(PhSaleOrderDO.class);
        verify(saleOrderMapper).insert(orderCaptor.capture());
        assertEquals(10L, orderCaptor.getValue().getShiftId());
        verify(inventoryFacade).deduct(anyLong(), anyList());
    }

    @Test
    void testCreateSaleOrder_cashierRequired() {
        SaleOrderSaveReqVO reqVO = buildReqVO();
        reqVO.setCashierId(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(reqVO));
        assertEquals(SALE_ORDER_CASHIER_REQUIRED.getCode(), ex.getCode());
        verify(posShiftMapper, never()).selectList(any());
        verify(saleOrderMapper, never()).insert(any(PhSaleOrderDO.class));
    }

    @Test
    void testCreateSaleOrder_multipleOpeningShifts_conflict() {
        // 收银员存在多个营业中班次且未指定收银台 → 无法确定归属，拒绝
        PhPosShiftDO s1 = new PhPosShiftDO();
        s1.setId(10L); s1.setStoreId(1L); s1.setPosNo("POS-01"); s1.setCashierId(100L); s1.setStatus(0);
        PhPosShiftDO s2 = new PhPosShiftDO();
        s2.setId(11L); s2.setStoreId(1L); s2.setPosNo("POS-02"); s2.setCashierId(100L); s2.setStatus(0);
        SaleOrderSaveReqVO reqVO = buildReqVO();
        reqVO.setPosNo(null); // 未指定收银台 → 按收银员查询，命中两个班次
        when(posShiftMapper.selectList(any())).thenReturn(Arrays.asList(s1, s2));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(reqVO));
        assertEquals(SALE_ORDER_SHIFT_CONFLICT.getCode(), ex.getCode());
        verify(saleOrderMapper, never()).insert(any(PhSaleOrderDO.class));
    }
}
