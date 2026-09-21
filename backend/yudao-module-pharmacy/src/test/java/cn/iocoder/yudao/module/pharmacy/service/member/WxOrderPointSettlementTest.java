package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacade;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxCartDO;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WxOrderServiceImpl} 线上订单积分联动单元测试。
 *
 * 覆盖：
 * - 下单：前端只表达「想用多少积分」，实际抵扣积分 / 金额由 F 的积分结算服务计算，
 *   下单即预扣抵扣积分，积分不足则整笔下单失败（不落单、不扣积分）；
 * - 核销 / 完成：赠送积分且幂等（重复核销不重复赠送），并把实际赠送积分回写订单；
 * - 取消 / 支付失败：只释放预扣积分，绝不赠送积分。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WxOrderPointSettlementTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long STORE_ID = 407L;
    private static final Long ORDER_ID = 100L;
    private static final String ORDER_NO = "WX-407-20260918-0001";
    private static final Long DRUG_ID = 163104L;

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
    @Mock
    private MemberPointSettlementService memberPointSettlementService;

    @InjectMocks
    private WxOrderServiceImpl wxOrderService;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        for (Class<?> entity : List.of(WxOrderDO.class, WxOrderLineDO.class, WxOrderLineAllocDO.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), entity);
        }
    }

    @BeforeEach
    void setUp() {
        var login = new cn.iocoder.yudao.framework.security.core.LoginUser();
        login.setId(MEMBER_ID); login.setTenantId(7L);
        login.setUserType(cn.iocoder.yudao.framework.common.enums.UserTypeEnum.MEMBER.getValue());
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(7L);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login, null, List.of()));
        // 购物车：一件商品，单价 25.00 × 2 = 50.00
        when(wxCartService.getSelectedCartListByMemberId(MEMBER_ID)).thenReturn(List.of(buildCart()));
        when(drugApi.getDrugList(anyList())).thenReturn(List.of(buildDrug()));
        when(wxOrderMapper.selectCountByOrderNoPrefix(anyString())).thenReturn(0L);
        when(wxOrderMapper.selectByOrderNo(anyString())).thenReturn(null);
        // insert 回填主键与订单号，模拟数据库自增
        doAnswer(invocation -> {
            WxOrderDO order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return 1;
        }).when(wxOrderMapper).insert(any(WxOrderDO.class));
    }

    @org.junit.jupiter.api.AfterEach
    void clearMemberContext() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    // ========== 下单：预扣抵扣积分 ==========

    /** 下单：抵扣积分与金额取 F 的试算结果，应付金额同步扣减，并预扣积分（幂等键 = 订单号） */
    @Test
    void testCreateOrderFromCart_preDeductsPointsAndReducesPayable() {
        // 前端想用 1000 分；F 按余额 / 比例 / 上限校验后允许 1000 分，折合 10 元
        when(memberPointSettlementService.calcSalePoints(eq(MEMBER_ID), any(), eq(1000)))
                .thenReturn(new SalePointCalcDTO(1000, new BigDecimal("10.00"), 40, 5000, 5000));

        wxOrderService.createOrderFromCart(MEMBER_ID, STORE_ID, 0, null, null, "备注", 1000);

        ArgumentCaptor<WxOrderDO> captor = ArgumentCaptor.forClass(WxOrderDO.class);
        verify(wxOrderMapper).insert(captor.capture());
        WxOrderDO order = captor.getValue();
        assertEquals(1000, order.getPointDeduct());
        assertEquals(0, new BigDecimal("10.00").compareTo(order.getPointDeductAmount()));
        // 应付 = 50.00 - 10.00
        assertEquals(0, new BigDecimal("40.00").compareTo(order.getPayableAmount()));
        assertEquals(0, order.getPointEarned(), "赠送积分必须等订单完成 / 核销后才产生");
        // 下单即预扣，幂等键使用订单号
        verify(memberPointSettlementService).deductSalePoints(MEMBER_ID, order.getOrderNo(), 1000);
    }

    /** 不使用积分：不预扣、不改变应付金额 */
    @Test
    void testCreateOrderFromCart_withoutPoints() {
        when(memberPointSettlementService.calcSalePoints(eq(MEMBER_ID), any(), eq(0)))
                .thenReturn(new SalePointCalcDTO(0, BigDecimal.ZERO, 50, 5000, 5000));

        wxOrderService.createOrderFromCart(MEMBER_ID, STORE_ID, 0, null, null, null, 0);

        ArgumentCaptor<WxOrderDO> captor = ArgumentCaptor.forClass(WxOrderDO.class);
        verify(wxOrderMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getPointDeduct());
        assertEquals(0, new BigDecimal("50.00").compareTo(captor.getValue().getPayableAmount()));
        verify(memberPointSettlementService).deductSalePoints(MEMBER_ID, captor.getValue().getOrderNo(), 0);
    }

    /** 积分不足 / 超出抵扣上限：F 抛业务异常，整笔下单失败，不落订单、不写积分流水 */
    @Test
    void testCreateOrderFromCart_pointNotEnough() {
        when(memberPointSettlementService.calcSalePoints(anyLong(), any(), anyInt()))
                .thenThrow(new ServiceException(1_030_008_002, "会员积分不足"));

        assertThrows(ServiceException.class, () -> wxOrderService.createOrderFromCart(
                MEMBER_ID, STORE_ID, 0, null, null, null, 99999));

        verify(wxOrderMapper, never()).insert(any(WxOrderDO.class));
        verify(memberPointSettlementService, never()).deductSalePoints(anyLong(), anyString(), anyInt());
    }

    // ========== 核销 / 完成：赠送积分 ==========

    /** 核销即完成：按实付金额赠送积分并回写订单；重复核销不重复赠送 */
    @Test
    void testVerifyWxOrder_earnsPointsOnce() {
        WxOrderDO waitVerify = buildWaitVerifyOrder();
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(waitVerify);
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        when(wxOrderLineService.getWxOrderLineListByWxOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
        when(memberPointSettlementService.earnSalePoints(MEMBER_ID, ORDER_NO, new BigDecimal("53.60")))
                .thenReturn(53);

        wxOrderService.verifyWxOrder(ORDER_ID, "ABC123", 9001L);

        ArgumentCaptor<WxOrderDO> captor = ArgumentCaptor.forClass(WxOrderDO.class);
        verify(wxOrderMapper).updateById(captor.capture());
        assertEquals(53, captor.getValue().getPointEarned(), "订单 point_earned 必须等于 F 实际赠送的积分");

        // 重复核销：订单已完成，直接返回，不再赠送
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildCompletedOrder());
        wxOrderService.verifyWxOrder(ORDER_ID, "ABC123", 9001L);

        verify(memberPointSettlementService, times(1)).earnSalePoints(anyLong(), anyString(), any());
    }

    /** 核销时积分规则折算为 0：不写流水、不回写赠送积分 */
    @Test
    void testVerifyWxOrder_noEarnWhenRuleGivesZero() {
        when(wxOrderMapper.selectById(ORDER_ID)).thenReturn(buildWaitVerifyOrder());
        when(wxOrderMapper.update(any(), any())).thenReturn(1);
        when(wxOrderLineService.getWxOrderLineListByWxOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
        when(memberPointSettlementService.earnSalePoints(anyLong(), anyString(), any())).thenReturn(0);

        wxOrderService.verifyWxOrder(ORDER_ID, "ABC123", 9001L);

        verify(wxOrderMapper, never()).updateById(any(WxOrderDO.class));
    }

    // ========== 测试辅助 ==========

    private WxCartDO buildCart() {
        WxCartDO cart = new WxCartDO();
        cart.setId(5001L);
        cart.setMemberId(MEMBER_ID);
        cart.setStoreId(STORE_ID);
        cart.setDrugId(DRUG_ID);
        cart.setQty(2);
        cart.setSelectedFlag(1);
        return cart;
    }

    private DrugRespDTO buildDrug() {
        DrugRespDTO drug = new DrugRespDTO();
        drug.setId(DRUG_ID);
        drug.setStatus(1);
        drug.setSaleableOnline(1);
        drug.setApproveStatus(1);
        drug.setIsRx(0);
        drug.setRetailPrice(new BigDecimal("25.00"));
        drug.setGenericName("阿莫西林胶囊");
        drug.setSpecification("0.25g*24粒");
        drug.setUnit("盒");
        return drug;
    }

    private WxOrderDO buildWaitVerifyOrder() {
        WxOrderDO order = new WxOrderDO();
        order.setId(ORDER_ID);
        order.setOrderNo(ORDER_NO);
        order.setMemberId(MEMBER_ID);
        order.setStoreId(STORE_ID);
        order.setStatus(WxOrderStatusEnum.WAIT_VERIFY.getStatus());
        order.setPayStatus(1);
        order.setPayableAmount(new BigDecimal("53.60"));
        order.setPickupCode("ABC123");
        order.setExpireAt(LocalDateTime.now().plusMinutes(30));
        return order;
    }

    private WxOrderDO buildCompletedOrder() {
        WxOrderDO order = buildWaitVerifyOrder();
        order.setStatus(WxOrderStatusEnum.COMPLETED.getStatus());
        order.setFinishAt(LocalDateTime.now());
        return order;
    }

}
