package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.PosShiftPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhPosShiftDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.PosShiftMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SalePaymentMapper;
import cn.iocoder.yudao.module.pharmacy.service.sale.PosShiftService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SHIFT_ALREADY_CLOSED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SHIFT_DIFF_REASON_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SHIFT_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SHIFT_OPENING_EXISTS;

/**
 * 班次服务实现：开台与交班。
 * <p>
 * 交班：汇总本班次销售（status=1 完成 / 3 部分退按应付款计），现金应收为本班次
 * 现金支付（payMethod=1）合计，实盘与应收差异非零时 diffReason 必填。
 */
@Service
public class PosShiftServiceImpl implements PosShiftService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Resource
    private PosShiftMapper posShiftMapper;
    @Resource
    private SaleOrderMapper saleOrderMapper;
    @Resource
    private SalePaymentMapper salePaymentMapper;

    @Override
    public Long openShift(Long storeId, String posNo, Long cashierId) {
        // 同一收银台已有进行中班次（status=0）时禁止重复开台，避免账目混乱
        Long openingCount = posShiftMapper.selectCount(new LambdaQueryWrapper<PhPosShiftDO>()
                .eq(PhPosShiftDO::getStoreId, storeId)
                .eq(PhPosShiftDO::getPosNo, posNo)
                .eq(PhPosShiftDO::getStatus, 0));
        if (openingCount != null && openingCount > 0) {
            throw ServiceExceptionUtil.exception(SHIFT_OPENING_EXISTS);
        }

        PhPosShiftDO shift = new PhPosShiftDO();
        shift.setShiftNo("SC-" + storeId + "-" + LocalDateTime.now().format(TIME_FMT)
                + "-" + String.format("%03d", SEQ.incrementAndGet() % 1000));
        shift.setStoreId(storeId);
        shift.setPosNo(posNo);
        shift.setCashierId(cashierId);
        shift.setOpenAt(LocalDateTime.now());
        shift.setSaleCount(0);
        shift.setSaleAmount(BigDecimal.ZERO);
        shift.setStatus(0);
        posShiftMapper.insert(shift);
        return shift.getId();
    }

    @Override
    public void closeShift(Long shiftId, BigDecimal cashActual, String diffReason) {
        PhPosShiftDO shift = posShiftMapper.selectById(shiftId);
        if (shift == null) {
            throw ServiceExceptionUtil.exception(SHIFT_NOT_EXISTS);
        }
        if (shift.getStatus() != null && shift.getStatus() == 1) {
            throw ServiceExceptionUtil.exception(SHIFT_ALREADY_CLOSED);
        }
        // 汇总本班次销售笔数与销售额
        List<PhSaleOrderDO> orders = saleOrderMapper.selectListByShiftId(shiftId);
        int saleCount = 0;
        BigDecimal saleAmount = BigDecimal.ZERO;
        BigDecimal cashExpected = BigDecimal.ZERO;
        for (PhSaleOrderDO order : orders) {
            if (order.getStatus() == 1 || order.getStatus() == 3) {
                saleCount++;
                saleAmount = saleAmount.add(order.getPayableAmount());
                // 现金应收：本单现金支付（payMethod=1）成功笔合计
                List<PhSalePaymentDO> payments = salePaymentMapper.selectListByOrderId(order.getId());
                for (PhSalePaymentDO payment : payments) {
                    if (payment.getPayMethod() != null && payment.getPayMethod() == 1
                            && payment.getStatus() != null && payment.getStatus() == 1) {
                        cashExpected = cashExpected.add(payment.getPayAmount());
                    }
                }
            }
        }
        // 差异校验
        BigDecimal actual = cashActual == null ? BigDecimal.ZERO : cashActual;
        BigDecimal diff = actual.subtract(cashExpected);
        if (diff.signum() != 0 && !StringUtils.hasText(diffReason)) {
            throw ServiceExceptionUtil.exception(SHIFT_DIFF_REASON_REQUIRED);
        }
        shift.setSaleCount(saleCount);
        shift.setSaleAmount(saleAmount);
        shift.setCashExpected(cashExpected);
        shift.setCashActual(actual);
        shift.setDiffAmount(diff);
        shift.setDiffReason(diffReason);
        shift.setCloseAt(LocalDateTime.now());
        shift.setStatus(1);
        posShiftMapper.updateById(shift);
    }

    @Override
    public PageResult<PhPosShiftDO> getShiftPage(PosShiftPageReqVO reqVO) {
        return posShiftMapper.selectPage(reqVO);
    }

    @Override
    public PhPosShiftDO getShift(Long id) {
        PhPosShiftDO shift = posShiftMapper.selectById(id);
        if (shift == null) {
            throw ServiceExceptionUtil.exception(SHIFT_NOT_EXISTS);
        }
        return shift;
    }
}
