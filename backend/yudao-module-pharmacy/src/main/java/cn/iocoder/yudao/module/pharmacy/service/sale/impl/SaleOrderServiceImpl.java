package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderDetailRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhPosShiftDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.PosShiftMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SalePaymentMapper;
import cn.iocoder.yudao.module.pharmacy.service.sale.SaleAmountCalculator;
import cn.iocoder.yudao.module.pharmacy.service.sale.SaleOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_AMOUNT_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_GOODS_EMPTY;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_NO_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_PAYMENT_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_CASHIER_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_RX_PRESC_REQUIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_SHIFT_CONFLICT;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_SHIFT_NOT_OPEN;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_SHIFT_NOT_OWNER;

/**
 * 销售单服务实现。
 * <p>
 * 1) 幂等：order_no 全局唯一 + paymentNo 幂等唯一，防重复提交重复扣库；
 * 2) 金额：服务端重算（前端仅供展示）；
 * 3) 库存：只经 InventoryFacade；库存拒绝时整单回滚。
 */
@Service
public class SaleOrderServiceImpl implements SaleOrderService {

    @Resource
    private SaleOrderMapper saleOrderMapper;
    @Resource
    private SaleOrderLineMapper saleOrderLineMapper;
    @Resource
    private SalePaymentMapper salePaymentMapper;
    @Resource
    private InventoryFacade inventoryFacade;
    @Resource
    private PosShiftMapper posShiftMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOrder(SaleOrderSaveReqVO reqVO) {
        // 0. 后端解析有效班次：根据当前操作员工与门店查找营业中班次，不依赖前端传入 shiftId
        PhPosShiftDO shift = resolveValidShift(reqVO);

        // 1. 幂等：订单号唯一
        if (saleOrderMapper.selectByOrderNo(reqVO.getOrderNo()) != null) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_NO_DUPLICATE);
        }
        // 1.1 支付明细幂等号唯一
        if (reqVO.getPayments() != null) {
            for (SaleOrderSaveReqVO.Payment payment : reqVO.getPayments()) {
                if (salePaymentMapper.selectByPaymentNo(payment.getPaymentNo()) != null) {
                    throw ServiceExceptionUtil.exception(SALE_ORDER_PAYMENT_DUPLICATE);
                }
            }
        }

        // 2. 校验明细并服务端重算金额
        if (reqVO.getItems() == null || reqVO.getItems().isEmpty()) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_GOODS_EMPTY);
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        List<PhSaleOrderLineDO> lines = new ArrayList<>();
        for (SaleOrderSaveReqVO.Item item : reqVO.getItems()) {
            if (item.getQty() == null || item.getQty() <= 0 || item.getPrice() == null) {
                throw ServiceExceptionUtil.exception(SALE_ORDER_AMOUNT_INVALID);
            }
            if (item.getIsRx() != null && item.getIsRx() == 1 && item.getPrescId() == null) {
                // TODO(D)：E 侧处方审方 Facade 提供后才做真实审方校验
                throw ServiceExceptionUtil.exception(SALE_ORDER_RX_PRESC_REQUIRED);
            }
            BigDecimal lineAmount = item.getPrice().multiply(BigDecimal.valueOf(item.getQty()));
            subtotal = subtotal.add(lineAmount);

            PhSaleOrderLineDO line = new PhSaleOrderLineDO();
            line.setDrugId(item.getDrugId());
            line.setBatchId(item.getBatchId());
            line.setQty(item.getQty());
            line.setPrice(item.getPrice());
            line.setNormalPrice(item.getPrice());
            line.setLineAmount(lineAmount);
            line.setDiscount(BigDecimal.ZERO);
            line.setIsRx(item.getIsRx() == null ? 0 : item.getIsRx());
            line.setPrescId(item.getPrescId());
            line.setIsGift(item.getIsGift() == null ? 0 : item.getIsGift());
            line.setLocationId(item.getLocationId());
            line.setReturnedQty(0);
            line.setDrugName(item.getDrugName());
            line.setSpecification(item.getSpecification());
            line.setUnit(item.getUnit());
            lines.add(line);
        }

        // 3. 金额：折扣/券/积分调用方传 0，服务端统一计算应付/实收/找零
        BigDecimal payable = SaleAmountCalculator.calcPayable(subtotal, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        if (payable == null) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_AMOUNT_INVALID);
        }
        BigDecimal paid = BigDecimal.ZERO;
        for (SaleOrderSaveReqVO.Payment payment : reqVO.getPayments()) {
            if (payment.getPayAmount() == null || payment.getPayAmount().signum() < 0) {
                throw ServiceExceptionUtil.exception(SALE_ORDER_AMOUNT_INVALID);
            }
            paid = paid.add(payment.getPayAmount());
        }
        BigDecimal change = SaleAmountCalculator.calcChange(payable, paid);
        if (change == null) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_AMOUNT_INVALID);
        }

        // 4. 落主单
        PhSaleOrderDO order = new PhSaleOrderDO();
        order.setOrderNo(reqVO.getOrderNo());
        order.setStoreId(reqVO.getStoreId());
        order.setPosNo(reqVO.getPosNo());
        order.setShiftId(shift.getId());
        order.setCashierId(reqVO.getCashierId());
        order.setMemberId(reqVO.getMemberId());
        order.setCustomerName(reqVO.getCustomerName());
        order.setSource(0);
        order.setSaleType(0);
        order.setReturnFlag(0);
        order.setTotalQty(lines.stream().mapToInt(PhSaleOrderLineDO::getQty).sum());
        order.setSubtotal(subtotal);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setCouponAmount(BigDecimal.ZERO);
        order.setPointsDeduct(BigDecimal.ZERO);
        order.setPayableAmount(payable);
        order.setPaidAmount(paid);
        order.setChangeAmount(change);
        order.setCostAmount(BigDecimal.ZERO);
        order.setPointsEarned(0);
        order.setStatus(1);
        order.setOfflineFlag(0);
        order.setSaleTime(LocalDateTime.now());
        saleOrderMapper.insert(order);

        // 5. 明细入库
        int lineNo = 1;
        for (PhSaleOrderLineDO line : lines) {
            line.setOrderId(order.getId());
            line.setLineNo(lineNo++);
            saleOrderLineMapper.insert(line);
        }

        // 6. 支付明细入库
        for (SaleOrderSaveReqVO.Payment payment : reqVO.getPayments()) {
            PhSalePaymentDO pm = new PhSalePaymentDO();
            pm.setOrderId(order.getId());
            pm.setPaymentNo(payment.getPaymentNo());
            pm.setPayMethod(payment.getPayMethod());
            pm.setPayAmount(payment.getPayAmount());
            pm.setChannel(payment.getChannel());
            pm.setPayNo(payment.getPayNo());
            pm.setStatus(1);
            pm.setPaidAt(order.getSaleTime());
            salePaymentMapper.insert(pm);
        }

        // 7. 扣库存。库存拒绝时事务整体回滚。
        List<DeductItem> deductItems = new ArrayList<>();
        for (PhSaleOrderLineDO line : lines) {
            DeductItem di = new DeductItem();
            di.setDrugId(line.getDrugId());
            di.setBatchId(line.getBatchId());
            di.setQty(line.getQty());
            di.setLocationId(line.getLocationId());
            // Persisted order/line identifiers are the inventory-flow idempotency key.
            di.setBizNo(order.getOrderNo());
            di.setBizLineId(line.getId());
            deductItems.add(di);
        }
        try {
            inventoryFacade.deduct(reqVO.getStoreId(), deductItems);
        } catch (UnsupportedOperationException ex) {
            throw ServiceExceptionUtil.exception(INV_SERVICE_UNAVAILABLE);
        }
        return order.getId();
    }

    @Override
    public PageResult<PhSaleOrderDO> getSaleOrderPage(SaleOrderPageReqVO reqVO) {
        return saleOrderMapper.selectPage(reqVO);
    }

    @Override
    public PhSaleOrderDO getSaleOrder(Long id) {
        PhSaleOrderDO order = saleOrderMapper.selectById(id);
        if (order == null) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public SaleOrderDetailRespVO getSaleOrderDetail(Long id) {
        PhSaleOrderDO order = getSaleOrder(id);
        SaleOrderDetailRespVO detail = new SaleOrderDetailRespVO();
        detail.setOrder(order);
        detail.setLines(saleOrderLineMapper.selectListByOrderId(id));
        detail.setPayments(salePaymentMapper.selectListByOrderId(id));
        return detail;
    }

    /**
     * 解析本次销售的有效班次（D-1）：
     * 1) 后端根据门店 + 收银台（无收银台时按收银员）查找营业中（status=0）班次，
     *    已交班、已关闭的班次自然被排除，不依赖前端传入 shiftId；
     * 2) 无有效班次时拒绝销售（未开班）；
     * 3) 其他员工的班次不能用于本次销售。
     */
    private PhPosShiftDO resolveValidShift(SaleOrderSaveReqVO reqVO) {
        if (reqVO.getCashierId() == null) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_CASHIER_REQUIRED);
        }
        LambdaQueryWrapper<PhPosShiftDO> wrapper = new LambdaQueryWrapper<PhPosShiftDO>()
                .eq(PhPosShiftDO::getStoreId, reqVO.getStoreId())
                .eq(PhPosShiftDO::getStatus, 0); // 0=营业中；已交班(1)不参与销售
        if (StringUtils.hasText(reqVO.getPosNo())) {
            wrapper.eq(PhPosShiftDO::getPosNo, reqVO.getPosNo());
        } else {
            wrapper.eq(PhPosShiftDO::getCashierId, reqVO.getCashierId());
        }
        List<PhPosShiftDO> shifts = posShiftMapper.selectList(wrapper);
        if (shifts.isEmpty()) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_SHIFT_NOT_OPEN);
        }
        if (shifts.size() > 1) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_SHIFT_CONFLICT);
        }
        PhPosShiftDO shift = shifts.get(0);
        // 其他员工的班次不能用于销售
        if (!reqVO.getCashierId().equals(shift.getCashierId())) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_SHIFT_NOT_OWNER);
        }
        return shift;
    }
}
