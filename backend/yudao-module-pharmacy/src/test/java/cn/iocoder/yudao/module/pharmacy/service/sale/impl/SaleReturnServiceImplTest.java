package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.member.MemberPointFacade;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacade;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NOT_ALLOW;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_PRESC_NOT_CONFIRM;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_QTY_EXCEED;
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
 * {@link SaleReturnServiceImpl} 单元测试：可退数量校验、处方药复核、原单状态、依赖服务未就绪回滚。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaleReturnServiceImplTest {

    @Mock
    private SaleOrderMapper saleOrderMapper;
    @Mock
    private SaleOrderLineMapper saleOrderLineMapper;
    @Mock
    private SaleReturnMapper saleReturnMapper;
    @Mock
    private SaleReturnLineMapper saleReturnLineMapper;
    @Mock
    private InventoryFacade inventoryFacade;
    @Mock
    private PaymentFacade paymentFacade;
    @Mock
    private MemberPointFacade memberPointFacade;

    @InjectMocks
    private SaleReturnServiceImpl saleReturnService;

    private PhSaleOrderDO order;
    private PhSaleOrderLineDO line;

    @BeforeEach
    void setUp() {
        order = new PhSaleOrderDO();
        order.setId(1L);
        order.setStoreId(1L);
        order.setStatus(1); // 已完成
        order.setReturnFlag(0);
        order.setCashierId(100L);
        order.setPointsDeduct(BigDecimal.ZERO);
        when(saleOrderMapper.selectById(1L)).thenReturn(order);

        line = new PhSaleOrderLineDO();
        line.setId(11L);
        line.setOrderId(1L);
        line.setDrugId(1L);
        line.setBatchId(2L);
        line.setQty(5);
        line.setReturnedQty(0);
        line.setPrice(new BigDecimal("10.00"));
        line.setIsRx(0);
        when(saleOrderLineMapper.selectById(11L)).thenReturn(line);

        when(saleReturnLineMapper.selectSumQtyBySaleLineId(11L)).thenReturn(0);
        when(saleReturnMapper.selectByReturnNo(anyString())).thenReturn(null);
        // insert 回填主键，模拟数据库自增
        doAnswer(invocation -> {
            invocation.getArgument(0, PhSaleReturnDO.class).setId(1L);
            return 1;
        }).when(saleReturnMapper).insert(any(PhSaleReturnDO.class));
        when(saleReturnLineMapper.insert(any(PhSaleReturnLineDO.class))).thenReturn(1);
    }

    private SaleReturnSaveReqVO buildReqVO() {
        SaleReturnSaveReqVO reqVO = new SaleReturnSaveReqVO();
        reqVO.setSaleOrderId(1L);
        reqVO.setReturnType(0);
        reqVO.setReason(1);
        reqVO.setRefundMethod(1); // 现金退款，不依赖 E 支付
        reqVO.setPharmacistConfirm(0);
        reqVO.setCashierId(100L);
        SaleReturnSaveReqVO.Item item = new SaleReturnSaveReqVO.Item();
        item.setSaleOrderLineId(11L);
        item.setQty(2);
        item.setLocationId(21L);
        reqVO.setItems(Collections.singletonList(item));
        return reqVO;
    }

    @Test
    void testCreateReturn_success() {
        Long id = saleReturnService.createReturn(buildReqVO());
        assertEquals(1L, id);
        // 原批次回补库存；现金退款不触发电渠道
        verify(inventoryFacade).returnBack(anyLong(), anyList());
        verify(paymentFacade, never()).refund(any(), anyString(), any(), any());
        // 原销售行已退数量更新为 2
        assertEquals(2, line.getReturnedQty());
    }

    @Test
    void testCreateReturn_orderNotExists() {
        when(saleOrderMapper.selectById(1L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(buildReqVO()));
        assertEquals(SALE_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void testCreateReturn_orderStatusNotAllow() {
        order.setStatus(-1); // 已取消
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(buildReqVO()));
        assertEquals(SALE_RETURN_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    void testCreateReturn_lineNotMatchOrder() {
        PhSaleOrderLineDO otherLine = new PhSaleOrderLineDO();
        otherLine.setId(12L);
        otherLine.setOrderId(999L); // 不属于本销售单
        otherLine.setQty(1);
        otherLine.setReturnedQty(0);
        otherLine.setPrice(BigDecimal.TEN);
        when(saleOrderLineMapper.selectById(12L)).thenReturn(otherLine);
        SaleReturnSaveReqVO reqVO = buildReqVO();
        reqVO.getItems().get(0).setSaleOrderLineId(12L);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(reqVO));
        assertEquals(SALE_RETURN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void testCreateReturn_qtyExceed() {
        when(saleReturnLineMapper.selectSumQtyBySaleLineId(11L)).thenReturn(4); // 已退 4，剩可退 1
        SaleReturnSaveReqVO reqVO = buildReqVO();
        reqVO.getItems().get(0).setQty(2); // 超量
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(reqVO));
        assertEquals(SALE_RETURN_QTY_EXCEED.getCode(), ex.getCode());
        verify(inventoryFacade, never()).returnBack(anyLong(), anyList());
    }

    @Test
    void testCreateReturn_rxWithoutPharmacistConfirm() {
        line.setIsRx(1); // 处方药行
        SaleReturnSaveReqVO reqVO = buildReqVO();
        reqVO.setPharmacistConfirm(0); // 未药师复核
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(reqVO));
        assertEquals(SALE_RETURN_PRESC_NOT_CONFIRM.getCode(), ex.getCode());
    }

    @Test
    void testCreateReturn_inventoryUnavailable_rollback() {
        // C 库存回补未实现 → INV_SERVICE_UNAVAILABLE，事务整体回滚
        doThrow(new UnsupportedOperationException("not implemented"))
                .when(inventoryFacade).returnBack(anyLong(), anyList());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(buildReqVO()));
        assertEquals(INV_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
    }

}
