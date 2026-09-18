package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.api.member.MemberPointFacade;
import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
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
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_POINT_NOT_ENOUGH;
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
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private MemberPointFacade memberPointFacade;

    @InjectMocks
    private SaleOrderServiceImpl saleOrderService;

    @BeforeEach
    void setUp() {
        // 订单号与支付幂等号默认不存在
        when(saleOrderMapper.selectByOrderNo(anyString())).thenReturn(null);
        when(salePaymentMapper.selectByPaymentNo(anyString())).thenReturn(null);
        // 积分服务默认：不抵扣、不赠送（具体用例按需覆盖）
        when(memberPointFacade.calcSalePoints(any(), any(), any()))
                .thenReturn(new SalePointCalcDTO(0, BigDecimal.ZERO, 0, 0, 0));
        when(memberPointFacade.settleSalePoints(any(), anyString(), any(), any())).thenReturn(0);
        // insert 回填主键，模拟数据库自增
        doAnswer(invocation -> {
            invocation.getArgument(0, PhSaleOrderDO.class).setId(1L);
            return 1;
        }).when(saleOrderMapper).insert(any(PhSaleOrderDO.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO.class).setId(11L);
            return 1;
        }).when(saleOrderLineMapper).insert(any(cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO.class));
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
        doAnswer(invocation -> {
            List<DeductItem> items = invocation.getArgument(1);
            assertEquals("SO-1-20260909101000-001", items.get(0).getBizNo());
            assertEquals(11L, items.get(0).getBizLineId());
            return null;
        }).when(inventoryFacade).deduct(anyLong(), anyList());
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

    // ========== 积分（F 的统一积分服务） ==========

    /** POS 销售正常赠送积分：赠送积分由 F 计算并回写订单 points_earned，D 不直接写积分流水 */
    @Test
    void testCreateSaleOrder_earnsPoints() {
        doAnswer(invocation -> null).when(inventoryFacade).deduct(anyLong(), anyList());
        when(memberPointFacade.calcSalePoints(any(), any(), any()))
                .thenReturn(new SalePointCalcDTO(0, BigDecimal.ZERO, 50, 500, 0));
        when(memberPointFacade.settleSalePoints(any(), eq("SO-1-20260909101000-001"), any(), any()))
                .thenReturn(50);

        saleOrderService.createSaleOrder(buildReqVO());

        ArgumentCaptor<PhSaleOrderDO> captor = ArgumentCaptor.forClass(PhSaleOrderDO.class);
        verify(saleOrderMapper).updateById(captor.capture());
        assertEquals(50, captor.getValue().getPointsEarned(), "订单 points_earned 必须等于 F 实际赠送的积分");
        verify(memberPointFacade).settleSalePoints(any(), eq("SO-1-20260909101000-001"), any(), any());
    }

    /** 前端传入的积分不被信任：抵扣金额一律取 F 的试算结果，并按该结果重算应付 */
    @Test
    void testCreateSaleOrder_pointDeductUsesFacadeResult() {
        doAnswer(invocation -> null).when(inventoryFacade).deduct(anyLong(), anyList());
        SaleOrderSaveReqVO reqVO = buildReqVO();
        reqVO.setPointDeduct(9999); // 前端想用 9999 分
        // F 按余额 / 抵扣比例 / 单笔上限只允许 1000 分，折合 10 元
        when(memberPointFacade.calcSalePoints(any(), any(), eq(9999)))
                .thenReturn(new SalePointCalcDTO(1000, new BigDecimal("10.00"), 40, 1000, 1000));
        when(memberPointFacade.settleSalePoints(any(), anyString(), any(), eq(1000))).thenReturn(40);

        saleOrderService.createSaleOrder(reqVO);

        ArgumentCaptor<PhSaleOrderDO> captor = ArgumentCaptor.forClass(PhSaleOrderDO.class);
        verify(saleOrderMapper).insert(captor.capture());
        assertEquals(0, new BigDecimal("10.00").compareTo(captor.getValue().getPointsDeduct()));
        // 应付 = 50.00 - 10.00
        assertEquals(0, new BigDecimal("40.00").compareTo(captor.getValue().getPayableAmount()));
        verify(memberPointFacade).settleSalePoints(any(), anyString(), any(), eq(1000));
    }

    /** 积分余额不足 / 超出抵扣上限：F 抛业务异常，销售整笔失败，不落单、不扣库存、不写积分 */
    @Test
    void testCreateSaleOrder_pointNotEnough_rollback() {
        when(memberPointFacade.calcSalePoints(any(), any(), any())).thenThrow(
                new ServiceException(PHARMACY_MEMBER_POINT_NOT_ENOUGH.getCode(), "会员积分不足"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleOrderService.createSaleOrder(buildReqVO()));

        assertEquals(PHARMACY_MEMBER_POINT_NOT_ENOUGH.getCode(), ex.getCode());
        verify(saleOrderMapper, never()).insert(any(PhSaleOrderDO.class));
        verify(inventoryFacade, never()).deduct(anyLong(), anyList());
        verify(memberPointFacade, never()).settleSalePoints(any(), anyString(), any(), any());
    }

    /** 库存失败时积分不变化：结算发生在扣库之后，扣库失败则该分支根本不会执行 */
    @Test
    void testCreateSaleOrder_inventoryFailure_keepsPointsUnchanged() {
        doThrow(new UnsupportedOperationException("not implemented"))
                .when(inventoryFacade).deduct(anyLong(), anyList());

        assertThrows(ServiceException.class, () -> saleOrderService.createSaleOrder(buildReqVO()));

        verify(memberPointFacade, never()).settleSalePoints(any(), anyString(), any(), any());
        verify(memberPointFacade, never()).deductPoints(any(), anyString(), any(), anyString());
        verify(memberPointFacade, never()).addPoints(any(), anyString(), any(), anyString());
    }

}
