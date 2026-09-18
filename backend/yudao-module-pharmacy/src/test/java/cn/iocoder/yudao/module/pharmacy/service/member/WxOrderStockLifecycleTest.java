package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.AvailableQty;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ConsumeItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReleaseItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReserveItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReserveResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacade;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineAllocDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderLineAllocMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderMapper;
import cn.iocoder.yudao.module.pharmacy.enums.WxOrderStatusEnum;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_WX_ORDER_STOCK_NOT_ENOUGH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WxOrderServiceImpl} 线上订单库存生命周期单元测试。
 *
 * 覆盖 F 与 C 的 InventoryFacade 契约：
 * - 支付成功 → 按 FEFO 正式出库，扣减请求必须携带 bizNo/bizLineId（C 的操作级幂等键）；
 * - 出库分配（批次 + 货位）落库，作为取消 / 退款回补「原批次、原货位」的依据；
 * - 重复支付、重复取消、重复退款均不得重复扣库或重复回补；
 * - 库存不足必须抛出明确的业务错误，且不写入任何出库分配。
 *
 * 说明：事务回滚由 {@code @Transactional} 保证，不在本单元测试内验证，由集成验收覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WxOrderStockLifecycleTest {

    private static final Long ORDER_ID = 100L;
    private static final String ORDER_NO = "WX-407-20260916-0001";
    private static final Long STORE_ID = 407L;
    private static final Long LINE_ID = 1001L;
    private static final Long ALLOC_ID = 9001L;
    private static final Long DRUG_ID = 163104L;
    private static final Long BATCH_ID = 2001L;
    private static final Long LOCATION_ID = 3001L;
    private static final Long PAY_ORDER_ID = 5001L;

    @Mock
    private WxOrderMapper wxOrderMapper;
    @Mock
    private InventoryFacade inventoryFacade;
    @Mock
    private PaymentFacade paymentFacade;
    @Mock
    private WxOrderLineService wxOrderLineService;
    @Mock
    private WxOrderLineMapper wxOrderLineMapper;
    @Mock
    private WxOrderLineAllocMapper wxOrderLineAllocMapper;
    @Mock
    private WxCartService wxCartService;
    @Mock
    private MemberAddressService memberAddressService;
    @Mock
    private DrugApi drugApi;
    /** F 的统一积分结算服务：库存生命周期用例只验证库存行为，积分用 mock 隔离 */
    @Mock
    private MemberPointSettlementService memberPointSettlementService;

    @InjectMocks
    private WxOrderServiceImpl wxOrderService;

    /**
     * 纯 Mockito 单测没有 Spring/MyBatis 上下文，这里显式注册实体的 TableInfo，
     * 让条件更新使用的 LambdaUpdateWrapper 能解析字段名。
     */
    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        for (Class<?> entity : List.of(WxOrderDO.class, WxOrderLineDO.class, WxOrderLineAllocDO.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), entity);
        }
    }

    // ========== 支付出库 ==========

    /** 支付成功：以订单号 + 明细行号为幂等键出库，并把 FEFO 实际分配落库 */
    @Test
    void testPayWxOrder_deductsStockAndRecordsAllocation() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0));
        when(wxOrderLineService.getWxOrderLineListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildLine(2)));
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        when(inventoryFacade.deduct(anyLong(), anyList())).thenReturn(buildDeductResult(1, 1));

        wxOrderService.payWxOrder(ORDER_ID, "PAY-0001");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeductItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryFacade).deduct(eq(STORE_ID), itemCaptor.capture());
        DeductItem item = itemCaptor.getValue().get(0);
        assertEquals(ORDER_NO, item.getBizNo(), "扣库必须携带业务单号，否则 C 无法做操作级幂等");
        assertEquals(LINE_ID, item.getBizLineId(), "扣库必须携带来源行号");
        assertEquals(DRUG_ID, item.getDrugId());
        assertEquals(2, item.getQty());

        // FEFO 拆分成两个批次时，两条分配都要落库，否则回补会漏批次
        ArgumentCaptor<WxOrderLineAllocDO> allocCaptor = ArgumentCaptor.forClass(WxOrderLineAllocDO.class);
        verify(wxOrderLineAllocMapper, times(2)).insert(allocCaptor.capture());
        List<WxOrderLineAllocDO> allocations = allocCaptor.getAllValues();
        assertEquals(2, allocations.size());
        assertEquals(BATCH_ID, allocations.get(0).getBatchId());
        assertEquals(LOCATION_ID, allocations.get(0).getLocationId());
        assertEquals(BATCH_ID + 1, allocations.get(1).getBatchId());
        assertEquals(ORDER_NO, allocations.get(0).getOrderNo());
        assertEquals(LINE_ID, allocations.get(0).getWxOrderLineId());
        assertEquals(0, allocations.get(0).getReturnedQty());
        assertEquals(WxOrderLineAllocDO.STATUS_OUT, allocations.get(0).getStatus());
        // 首个分配回填订单明细，便于拣货
        verify(wxOrderLineMapper).updateById(any(WxOrderLineDO.class));
    }

    /** 重复支付回调：订单已支付时直接返回，不再扣库 */
    @Test
    void testPayWxOrder_duplicateCallbackIsIdempotent() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PICK, 1));

        wxOrderService.payWxOrder(ORDER_ID, "PAY-0002");

        verify(inventoryFacade, never()).deduct(anyLong(), anyList());
        verify(wxOrderMapper, never()).update(any(), any());
    }

    /** 库存不足：抛出明确的业务错误，不写任何出库分配 */
    @Test
    void testPayWxOrder_stockNotEnough() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0));
        when(wxOrderLineService.getWxOrderLineListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildLine(2)));
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        when(inventoryFacade.deduct(anyLong(), anyList())).thenThrow(
                ServiceExceptionUtil.invalidParamException("符合效期、质量和货位状态的可用库存不足"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wxOrderService.payWxOrder(ORDER_ID, "PAY-0003"));

        assertEquals(PHARMACY_WX_ORDER_STOCK_NOT_ENOUGH.getCode(), ex.getCode());
        verify(wxOrderLineAllocMapper, never()).insert(any(WxOrderLineAllocDO.class));
    }

    /** 订单明细缺失：不允许出库，避免出现「订单已支付但库存无变化」 */
    @Test
    void testPayWxOrder_linesMissing() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0));
        when(wxOrderLineService.getWxOrderLineListByWxOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
        when(wxOrderMapper.update(any(), any())).thenReturn(1);

        assertThrows(ServiceException.class, () -> wxOrderService.payWxOrder(ORDER_ID, "PAY-0004"));
        verify(inventoryFacade, never()).deduct(anyLong(), anyList());
    }

    // ========== 取消 / 退款回补 ==========

    /** 取消已支付订单：按原批次、原货位回补，并标记分配已回补 */
    @Test
    void testCancelWxOrder_returnsStockToOriginalBatch() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PICK, 1));
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildAlloc(0)));

        wxOrderService.cancelWxOrder(ORDER_ID, "用户取消");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReturnBackItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryFacade).returnBack(eq(STORE_ID), itemCaptor.capture());
        ReturnBackItem item = itemCaptor.getValue().get(0);
        assertEquals("WXC-" + ORDER_NO, item.getBizNo(), "回补业务单号需唯一且可重复提交");
        assertEquals(ALLOC_ID, item.getBizLineId());
        assertEquals(ORDER_NO, item.getOriginalBizNo(), "必须携带原出库业务单号");
        assertEquals(LINE_ID, item.getOriginalBizLineId(), "必须携带原出库来源行");
        assertEquals(BATCH_ID, item.getBatchId(), "必须回到原批次");
        assertEquals(LOCATION_ID, item.getLocationId(), "必须回到原货位");
        assertEquals(2, item.getQty());

        ArgumentCaptor<WxOrderLineAllocDO> allocCaptor = ArgumentCaptor.forClass(WxOrderLineAllocDO.class);
        verify(wxOrderLineAllocMapper).updateById(allocCaptor.capture());
        assertEquals(2, allocCaptor.getValue().getReturnedQty());
        assertEquals(WxOrderLineAllocDO.STATUS_SETTLED, allocCaptor.getValue().getStatus());
        // 已支付订单取消同时退回款项
        verify(paymentFacade).refund(eq(PAY_ORDER_ID), anyString(), anyInt(), anyString());
        // 对接 F：取消即释放本单预扣的抵扣积分（幂等键 = 订单号）
        verify(memberPointSettlementService).releaseSalePoints(eq(1L), eq(ORDER_NO));
    }

    /** 重复取消：已取消订单直接返回，不再回补库存 */
    @Test
    void testCancelWxOrder_repeatIsIdempotent() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.CANCELED, 2));

        wxOrderService.cancelWxOrder(ORDER_ID, "重复取消");

        verify(inventoryFacade, never()).returnBack(anyLong(), anyList());
        verify(wxOrderMapper, never()).update(any(), any());
    }

    /** 已回补过的分配不再重复提交回补请求 */
    @Test
    void testCancelWxOrder_returnedAllocationIsSkipped() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PICK, 1));
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildAlloc(2)));

        wxOrderService.cancelWxOrder(ORDER_ID, "再次取消");

        verify(inventoryFacade, never()).returnBack(anyLong(), anyList());
    }

    /** 未出库订单取消：没有分配记录，不调用库存回补 */
    @Test
    void testCancelWxOrder_unpaidOrderHasNoStockAction() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0));
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

        wxOrderService.cancelWxOrder(ORDER_ID, "未支付取消");

        verify(inventoryFacade, never()).returnBack(anyLong(), anyList());
        verify(paymentFacade, never()).refund(any(), anyString(), anyInt(), anyString());
        // 对接 F：会员取消同样释放预扣积分（幂等，未预扣时为无操作）
        verify(memberPointSettlementService).releaseSalePoints(eq(1L), eq(ORDER_NO));
    }

    /** 退款：回补一次；重复退款回调不再回补 */
    @Test
    void testRefundWxOrder_returnsStockOnlyOnce() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PICK, 1));
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        // 冻结转出库产生的出库流水行号是分配记录编号
        WxOrderLineAllocDO consumed = buildAlloc(0);
        consumed.setOutBizLineId(ALLOC_ID);
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(List.of(consumed));

        wxOrderService.refundWxOrder(ORDER_ID, "用户申请退款");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReturnBackItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryFacade).returnBack(eq(STORE_ID), itemCaptor.capture());
        assertEquals("WXR-" + ORDER_NO, itemCaptor.getValue().get(0).getBizNo());
        assertEquals(ALLOC_ID, itemCaptor.getValue().get(0).getOriginalBizLineId(),
                "回补必须引用正式出库流水的行号，否则 C 找不到原出库流水");

        // 第二次退款（重复回调）：订单已是已退款状态，直接返回
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.CANCELED, 2));
        wxOrderService.refundWxOrder(ORDER_ID, "用户申请退款");

        verify(inventoryFacade, times(1)).returnBack(anyLong(), anyList());
        // 对接 F：退款释放预扣积分，重复退款回调不再重复返还
        verify(memberPointSettlementService, times(1)).releaseSalePoints(eq(1L), eq(ORDER_NO));
    }

    /** 已完成订单不允许退款，需走 D 的销售退货流程 */
    @Test
    void testRefundWxOrder_completedOrderRejected() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.COMPLETED, 1));

        assertThrows(ServiceException.class, () -> wxOrderService.refundWxOrder(ORDER_ID, "退款"));
        verify(inventoryFacade, never()).returnBack(anyLong(), anyList());
    }

    // ========== 门店节点：下单冻结 ==========

    /** 冻结：按 FEFO 分配并落库为「已冻结」，请求携带订单号 + 明细行号作为幂等键 */
    @Test
    void testReserveWxOrder_freezesStockAndRecordsFrozenAllocation() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0));
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
        when(wxOrderLineService.getWxOrderLineListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildLine(2)));
        when(inventoryFacade.getAvailableQty(eq(STORE_ID), anyList())).thenReturn(List.of(buildAvailableQty(30)));
        when(inventoryFacade.reserve(anyLong(), anyList())).thenReturn(buildReserveResult(2));

        wxOrderService.reserveWxOrder(ORDER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReserveItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryFacade).reserve(eq(STORE_ID), itemCaptor.capture());
        ReserveItem item = itemCaptor.getValue().get(0);
        assertEquals(ORDER_NO, item.getBizNo(), "冻结必须携带业务单号，否则 C 无法做操作级幂等");
        assertEquals(LINE_ID, item.getBizLineId());
        assertEquals(DRUG_ID, item.getDrugId());
        assertEquals(2, item.getQty());

        ArgumentCaptor<WxOrderLineAllocDO> allocCaptor = ArgumentCaptor.forClass(WxOrderLineAllocDO.class);
        verify(wxOrderLineAllocMapper).insert(allocCaptor.capture());
        assertEquals(WxOrderLineAllocDO.STATUS_FROZEN, allocCaptor.getValue().getStatus());
        assertEquals(BATCH_ID, allocCaptor.getValue().getBatchId());
        assertEquals(LOCATION_ID, allocCaptor.getValue().getLocationId());
    }

    /** 冻结前粗校验：门店可售量不足时直接拒绝，不产生任何冻结 */
    @Test
    void testReserveWxOrder_notEnoughAvailableRejected() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0));
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
        when(wxOrderLineService.getWxOrderLineListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildLine(2)));
        when(inventoryFacade.getAvailableQty(eq(STORE_ID), anyList())).thenReturn(List.of(buildAvailableQty(1)));

        ServiceException ex = assertThrows(ServiceException.class, () -> wxOrderService.reserveWxOrder(ORDER_ID));

        assertEquals(PHARMACY_WX_ORDER_STOCK_NOT_ENOUGH.getCode(), ex.getCode());
        verify(inventoryFacade, never()).reserve(anyLong(), anyList());
        verify(wxOrderLineAllocMapper, never()).insert(any(WxOrderLineAllocDO.class));
    }

    /** 冻结幂等：已存在分配记录时不重复冻结、不重复读可售量 */
    @Test
    void testReserveWxOrder_idempotent() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0));
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildFrozenAlloc()));

        wxOrderService.reserveWxOrder(ORDER_ID);

        verify(inventoryFacade, never()).reserve(anyLong(), anyList());
        verify(inventoryFacade, never()).getAvailableQty(anyLong(), anyList());
    }

    // ========== 支付：冻结转出库 ==========

    /** 已冻结订单支付：走冻结转出库，不再按 FEFO 重新扣库 */
    @Test
    void testPayWxOrder_consumesReservation() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0));
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildFrozenAlloc()));

        wxOrderService.payWxOrder(ORDER_ID, "PAY-0005");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ConsumeItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryFacade).consumeReservation(eq(STORE_ID), itemCaptor.capture());
        ConsumeItem item = itemCaptor.getValue().get(0);
        assertEquals(ORDER_NO, item.getBizNo());
        assertEquals(ALLOC_ID, item.getBizLineId());
        assertEquals(ORDER_NO, item.getOriginalBizNo(), "必须指向原冻结业务单号");
        assertEquals(LINE_ID, item.getOriginalBizLineId(), "必须指向原冻结来源行");
        assertEquals(BATCH_ID, item.getBatchId());
        assertEquals(LOCATION_ID, item.getLocationId());
        assertEquals(2, item.getQty());
        verify(inventoryFacade, never()).deduct(anyLong(), anyList());

        ArgumentCaptor<WxOrderLineAllocDO> allocCaptor = ArgumentCaptor.forClass(WxOrderLineAllocDO.class);
        verify(wxOrderLineAllocMapper).updateById(allocCaptor.capture());
        assertEquals(WxOrderLineAllocDO.STATUS_OUT, allocCaptor.getValue().getStatus());
        assertEquals(ALLOC_ID, allocCaptor.getValue().getOutBizLineId(),
                "必须记录出库流水的来源行号（分配记录编号），否则回补时找不到原出库流水");
    }

    // ========== 门店节点：超时关闭与释放冻结 ==========

    /** 未支付订单取消：释放冻结，不做回补 */
    @Test
    void testCancelWxOrder_releasesFrozenStock() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0));
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildFrozenAlloc()));

        wxOrderService.cancelWxOrder(ORDER_ID, "用户取消未支付订单");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReleaseItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryFacade).release(eq(STORE_ID), itemCaptor.capture());
        ReleaseItem item = itemCaptor.getValue().get(0);
        assertEquals("WXC-" + ORDER_NO, item.getBizNo(), "释放需使用独立业务号，避免与冻结 / 出库冲突");
        assertEquals(ALLOC_ID, item.getBizLineId());
        assertEquals(ORDER_NO, item.getOriginalBizNo());
        assertEquals(LINE_ID, item.getOriginalBizLineId());
        assertEquals(2, item.getQty());
        verify(inventoryFacade, never()).returnBack(anyLong(), anyList());
    }

    /** 会员取消：只关订单，不触发任何库存作业（小程序没有库存作业身份） */
    @Test
    void testCancelWxOrderByMember_hasNoStockAction() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildOrder(WxOrderStatusEnum.WAIT_PICK, 1));
        when(wxOrderMapper.update(any(), any())).thenReturn(1);

        wxOrderService.cancelWxOrderByMember(ORDER_ID, "会员取消");

        verify(inventoryFacade, never()).release(anyLong(), anyList());
        verify(inventoryFacade, never()).returnBack(anyLong(), anyList());
        verify(inventoryFacade, never()).consumeReservation(anyLong(), anyList());
        verify(inventoryFacade, never()).deduct(anyLong(), anyList());
        verify(paymentFacade, never()).refund(any(), anyString(), anyInt(), anyString());
    }

    /** 关闭超时未支付订单：逐单条件关闭并返回关闭数 */
    @Test
    void testCloseExpiredWxOrders() {
        when(wxOrderMapper.selectExpiredList(eq(STORE_ID), anyInt(), any(), anyInt()))
                .thenReturn(List.of(buildOrder(WxOrderStatusEnum.WAIT_PAY, 0), buildOrder(WxOrderStatusEnum.WAIT_PAY, 0)));
        when(wxOrderMapper.update(any(), any())).thenReturn(1);

        int closed = wxOrderService.closeExpiredWxOrders(STORE_ID, 50);

        assertEquals(2, closed);
        verify(wxOrderMapper, times(2)).update(any(), any());
        // 对接 F：支付超时 = 支付失败，逐单向 F 释放预扣积分（幂等键 = 订单号）
        verify(memberPointSettlementService, times(2)).releaseSalePoints(eq(1L), eq(ORDER_NO));
    }

    /** 批量释放：只释放仍冻结的分配，已出库的不动（避免未退款先回补） */
    @Test
    void testReleaseFrozenStockOfClosedOrders_releasesOnlyFrozen() {
        when(wxOrderMapper.selectListByStoreIdAndStatus(eq(STORE_ID), anyInt(), anyInt()))
                .thenReturn(List.of(buildOrder(WxOrderStatusEnum.CANCELED, 0)));
        WxOrderLineAllocDO out = buildAlloc(0);
        out.setId(ALLOC_ID + 1);
        when(wxOrderLineAllocMapper.selectListByWxOrderId(ORDER_ID)).thenReturn(List.of(buildFrozenAlloc(), out));

        int released = wxOrderService.releaseFrozenStockOfClosedOrders(STORE_ID, 50);

        assertEquals(1, released);
        verify(inventoryFacade).release(eq(STORE_ID), anyList());
        verify(inventoryFacade, never()).returnBack(anyLong(), anyList());
    }

    // ========== 测试辅助 ==========

    private WxOrderDO buildOrder(WxOrderStatusEnum status, int payStatus) {
        WxOrderDO order = new WxOrderDO();
        order.setId(ORDER_ID);
        order.setOrderNo(ORDER_NO);
        order.setMemberId(1L);
        order.setStoreId(STORE_ID);
        order.setStatus(status.getStatus());
        order.setPayStatus(payStatus);
        order.setPayableAmount(new BigDecimal("53.60"));
        if (payStatus >= 1) {
            order.setPayOrderId(PAY_ORDER_ID);
        }
        return order;
    }

    private WxOrderLineDO buildLine(int qty) {
        WxOrderLineDO line = new WxOrderLineDO();
        line.setId(LINE_ID);
        line.setWxOrderId(ORDER_ID);
        line.setDrugId(DRUG_ID);
        line.setQty(qty);
        return line;
    }

    private WxOrderLineAllocDO buildAlloc(int returnedQty) {
        WxOrderLineAllocDO alloc = new WxOrderLineAllocDO();
        alloc.setId(ALLOC_ID);
        alloc.setWxOrderId(ORDER_ID);
        alloc.setWxOrderLineId(LINE_ID);
        alloc.setOrderNo(ORDER_NO);
        alloc.setDrugId(DRUG_ID);
        alloc.setBatchId(BATCH_ID);
        alloc.setLocationId(LOCATION_ID);
        alloc.setQty(2);
        alloc.setReturnedQty(returnedQty);
        alloc.setStatus(returnedQty >= 2 ? WxOrderLineAllocDO.STATUS_SETTLED : WxOrderLineAllocDO.STATUS_OUT);
        return alloc;
    }

    /** 冻结阶段的分配记录 */
    private WxOrderLineAllocDO buildFrozenAlloc() {
        WxOrderLineAllocDO alloc = buildAlloc(0);
        alloc.setStatus(WxOrderLineAllocDO.STATUS_FROZEN);
        return alloc;
    }

    /** 构造 C 的出库结果，qtys 长度即为 FEFO 拆分出的批次数 */
    private DeductResult buildDeductResult(int... qtys) {
        DeductResult result = new DeductResult();
        result.setSuccess(true);
        List<DeductResult.Allocation> allocations = new ArrayList<>();
        long batchId = BATCH_ID;
        long locationId = LOCATION_ID;
        for (int qty : qtys) {
            DeductResult.Allocation allocation = new DeductResult.Allocation();
            allocation.setBizLineId(LINE_ID);
            allocation.setBatchId(batchId++);
            allocation.setLocationId(locationId++);
            allocation.setQty(qty);
            allocations.add(allocation);
        }
        result.setAllocations(allocations);
        return result;
    }

    /** 构造 C 的冻结结果，qtys 长度即为 FEFO 拆分出的批次数 */
    private ReserveResult buildReserveResult(int... qtys) {
        ReserveResult result = new ReserveResult();
        result.setSuccess(true);
        List<ReserveResult.Allocation> allocations = new ArrayList<>();
        long batchId = BATCH_ID;
        long locationId = LOCATION_ID;
        for (int qty : qtys) {
            ReserveResult.Allocation allocation = new ReserveResult.Allocation();
            allocation.setBizLineId(LINE_ID);
            allocation.setBatchId(batchId++);
            allocation.setLocationId(locationId++);
            allocation.setQty(qty);
            allocations.add(allocation);
        }
        result.setAllocations(allocations);
        return result;
    }

    private AvailableQty buildAvailableQty(int qtyAvail) {
        AvailableQty available = new AvailableQty();
        available.setDrugId(DRUG_ID);
        available.setQtyAvail(qtyAvail);
        return available;
    }

}
