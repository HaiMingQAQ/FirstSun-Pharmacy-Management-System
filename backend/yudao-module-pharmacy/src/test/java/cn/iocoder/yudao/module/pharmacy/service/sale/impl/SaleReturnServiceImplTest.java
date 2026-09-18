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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.MEMBER_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NOT_ALLOW;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_PRESC_NOT_CONFIRM;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_QTY_EXCEED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
        order.setOrderNo("SO-1-20260909101000-001");
        order.setStoreId(1L);
        order.setMemberId(1L); // 会员退货：积分回退必须落到该会员
        order.setStatus(1); // 已完成
        order.setReturnFlag(0);
        order.setCashierId(100L);
        order.setPointsDeduct(BigDecimal.ZERO);
        order.setSubtotal(new BigDecimal("50.00")); // 原单商品金额，用于按比例回退积分
        when(saleOrderMapper.selectById(1L)).thenReturn(order);

        line = new PhSaleOrderLineDO();
        line.setId(11L);
        line.setOrderId(1L);
        line.setDrugId(1L);
        line.setBatchId(2L);
        line.setLocationId(21L);
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
        doAnswer(invocation -> {
            invocation.getArgument(0, PhSaleReturnLineDO.class).setId(21L);
            return 1;
        }).when(saleReturnLineMapper).insert(any(PhSaleReturnLineDO.class));
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
        doAnswer(invocation -> {
            List<cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem> items = invocation.getArgument(1);
            assertEquals("SO-1-20260909101000-001", items.get(0).getOriginalBizNo());
            assertEquals(11L, items.get(0).getOriginalBizLineId());
            assertEquals(21L, items.get(0).getBizLineId());
            assertEquals(21L, items.get(0).getLocationId());
            return null;
        }).when(inventoryFacade).returnBack(anyLong(), anyList());
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

    // ========== 积分回退（F 的统一积分服务） ==========

    /** 部分退货：按实际退货金额 / 原单金额的比例回退积分，并把退货单号作为幂等键传给 F */
    @Test
    void testCreateReturn_refundsPointsByReturnedAmount() {
        doAnswer(invocation -> null).when(inventoryFacade).returnBack(anyLong(), anyList());

        saleReturnService.createReturn(buildReqVO());

        // 退货 2 件 × 10.00 = 20.00，原单 50.00，未构成整单全退
        verify(memberPointFacade).refundSalePoints(eq(1L), eq("SO-1-20260909101000-001"), anyString(),
                eq(new BigDecimal("50.00")), eq(new BigDecimal("20.00")), eq(false));
    }

    /** 全额退货：本次已构成整单全退，必须把 fullReturn 标记传给 F，由 F 结清剩余积分 */
    @Test
    void testCreateReturn_fullReturnPassesFullReturnFlag() {
        doAnswer(invocation -> null).when(inventoryFacade).returnBack(anyLong(), anyList());
        SaleReturnSaveReqVO reqVO = buildReqVO();
        reqVO.getItems().get(0).setQty(5); // 全部 5 件

        saleReturnService.createReturn(reqVO);

        verify(memberPointFacade).refundSalePoints(eq(1L), eq("SO-1-20260909101000-001"), anyString(),
                eq(new BigDecimal("50.00")), eq(new BigDecimal("50.00")), eq(true));
    }

    /** 重复退货：积分回退以退货单号为幂等键，同一退货单只会回退一次 */
    @Test
    void testCreateReturn_pointsRefundKeyedByReturnNo() {
        doAnswer(invocation -> null).when(inventoryFacade).returnBack(anyLong(), anyList());

        saleReturnService.createReturn(buildReqVO());

        ArgumentCaptor<String> returnNoCaptor = ArgumentCaptor.forClass(String.class);
        verify(memberPointFacade).refundSalePoints(any(), anyString(), returnNoCaptor.capture(),
                any(), any(), anyBoolean());
        // 幂等键非空：F 侧按「会员 + 退款冲回 + 该退货单号」保证只回退一次
        assertNotNull(returnNoCaptor.getValue());
    }

    /** 库存回补失败：积分不动（回退逻辑在扣库之后，且整个事务回滚） */
    @Test
    void testCreateReturn_inventoryFailure_pointsUnchanged() {
        doThrow(new UnsupportedOperationException("not implemented"))
                .when(inventoryFacade).returnBack(anyLong(), anyList());

        assertThrows(ServiceException.class, () -> saleReturnService.createReturn(buildReqVO()));

        verify(memberPointFacade, never()).refundSalePoints(any(), anyString(), anyString(),
                any(), any(), anyBoolean());
    }

    /** F 积分服务未就绪：抛 MEMBER_SERVICE_UNAVAILABLE，退货事务整体回滚 */
    @Test
    void testCreateReturn_memberPointServiceUnavailable_rollback() {
        doAnswer(invocation -> null).when(inventoryFacade).returnBack(anyLong(), anyList());
        doThrow(new UnsupportedOperationException("not implemented"))
                .when(memberPointFacade).refundSalePoints(any(), anyString(), anyString(), any(), any(), anyBoolean());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> saleReturnService.createReturn(buildReqVO()));

        assertEquals(MEMBER_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
    }

}
