package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhPosShiftDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.PosShiftMapper;
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
import java.util.Collections;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SHIFT_ALREADY_CLOSED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SHIFT_DIFF_REASON_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SHIFT_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SHIFT_OPENING_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * {@link PosShiftServiceImpl} 单元测试：开台/交班、现金差异必填原因、重复交班拦截。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PosShiftServiceImplTest {

    @Mock
    private PosShiftMapper posShiftMapper;
    @Mock
    private SaleOrderMapper saleOrderMapper;
    @Mock
    private SalePaymentMapper salePaymentMapper;

    @InjectMocks
    private PosShiftServiceImpl posShiftService;

    private PhPosShiftDO shift;

    @BeforeEach
    void setUp() {
        shift = new PhPosShiftDO();
        shift.setId(1L);
        shift.setStoreId(1L);
        shift.setPosNo("POS-01");
        shift.setCashierId(100L);
        shift.setStatus(0); // 营业中
        when(posShiftMapper.selectById(1L)).thenReturn(shift);
    }

    @Test
    void testOpenShift() {
        doAnswer(invocation -> {
            invocation.getArgument(0, PhPosShiftDO.class).setId(1L);
            return 1;
        }).when(posShiftMapper).insert(any(PhPosShiftDO.class));
        Long id = posShiftService.openShift(1L, "POS-01", 100L);
        assertEquals(1L, id);
    }

    @Test
    void testCloseShift_success() {
        // 本班次 1 笔完成销售（应付款 100），其中现金 80
        PhSaleOrderDO order = new PhSaleOrderDO();
        order.setId(10L);
        order.setStatus(1);
        order.setPayableAmount(new BigDecimal("100.00"));
        when(saleOrderMapper.selectListByShiftId(1L)).thenReturn(Collections.singletonList(order));
        PhSalePaymentDO payment = new PhSalePaymentDO();
        payment.setPayMethod(1); // 现金
        payment.setStatus(1); // 成功
        payment.setPayAmount(new BigDecimal("80.00"));
        when(salePaymentMapper.selectListByOrderId(10L)).thenReturn(Collections.singletonList(payment));

        posShiftService.closeShift(1L, new BigDecimal("80.00"), null);
        assertEquals(1, shift.getSaleCount());
        assertEquals(new BigDecimal("100.00"), shift.getSaleAmount());
        assertEquals(new BigDecimal("80.00"), shift.getCashExpected());
        assertEquals(new BigDecimal("80.00"), shift.getCashActual());
        assertEquals(0, shift.getDiffAmount().compareTo(BigDecimal.ZERO));
        assertEquals(1, shift.getStatus());
    }

    @Test
    void testCloseShift_shiftNotExists() {
        when(posShiftMapper.selectById(1L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> posShiftService.closeShift(1L, BigDecimal.ZERO, null));
        assertEquals(SHIFT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void testCloseShift_alreadyClosed() {
        shift.setStatus(1);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> posShiftService.closeShift(1L, BigDecimal.ZERO, null));
        assertEquals(SHIFT_ALREADY_CLOSED.getCode(), ex.getCode());
    }

    @Test
    void testCloseShift_diffRequiresReason() {
        when(saleOrderMapper.selectListByShiftId(1L)).thenReturn(Collections.emptyList());
        // 实盘 100，应收 0，差异 100 且未填原因 → 拒绝
        ServiceException ex = assertThrows(ServiceException.class,
                () -> posShiftService.closeShift(1L, new BigDecimal("100.00"), null));
        assertEquals(SHIFT_DIFF_REASON_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void testCloseShift_diffWithReason_success() {
        when(saleOrderMapper.selectListByShiftId(1L)).thenReturn(Collections.emptyList());
        posShiftService.closeShift(1L, new BigDecimal("100.00"), "备用金");
        assertEquals(new BigDecimal("100.00"), shift.getDiffAmount());
        assertEquals("备用金", shift.getDiffReason());
        assertEquals(1, shift.getStatus());
    }


    @Test
    void testOpenShift_duplicateOpeningRejected() {
        // 同一收银台已存在进行中班次（status=0）→ 禁止重复开台
        when(posShiftMapper.selectCount(any())).thenReturn(1L);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> posShiftService.openShift(1L, "POS-01", 100L));
        assertEquals(SHIFT_OPENING_EXISTS.getCode(), ex.getCode());
    }


}
