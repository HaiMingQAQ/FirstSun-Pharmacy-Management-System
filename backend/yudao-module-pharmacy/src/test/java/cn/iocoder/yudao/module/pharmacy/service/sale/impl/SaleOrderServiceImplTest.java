package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SalePaymentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_GOODS_EMPTY;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_NO_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_PAYMENT_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_RX_PRESC_REQUIRED;
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
 * {@link SaleOrderServiceImpl} 单元测试：销售幂等、金额校验、处方药校验、库存依赖回滚。
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

    @InjectMocks
    private SaleOrderServiceImpl saleOrderService;

    @BeforeEach
    void setUp() {
        // 订单号与支付幂等号默认不存在
        when(saleOrderMapper.selectByOrderNo(anyString())).thenReturn(null);
        when(salePaymentMapper.selectByPaymentNo(anyString())).thenReturn(null);
        // insert 回填主键，模拟数据库自增
        doAnswer(invocation -> {
            invocation.getArgument(0, PhSaleOrderDO.class).setId(1L);
            return 1;
        }).when(saleOrderMapper).insert(any(PhSaleOrderDO.class));
        when(saleOrderLineMapper.insert(any(cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO.class))).thenReturn(1);
        when(salePaymentMapper.insert(any(cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO.class))).thenReturn(1);
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
        Long id = saleOrderService.createSaleOrder(buildReqVO());
        assertEquals(1L, id);
        // 明细与支付均入库，库存按明细扣减
        verify(saleOrderLineMapper).insert(any(cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO.class));
        verify(salePaymentMapper).insert(any(cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO.class));
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
        when(salePaymentMapper.selectByPaymentNo(anyString())).thenReturn(new cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO());
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

}
