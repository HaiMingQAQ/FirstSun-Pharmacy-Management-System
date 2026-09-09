package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;
import cn.iocoder.yudao.module.pharmacy.api.member.MemberPointFacade;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacade;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnDetailRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleReturnMapper;
import cn.iocoder.yudao.module.pharmacy.service.sale.SaleReturnService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.MEMBER_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PAY_SERVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NO_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NOT_ALLOW;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_PRESC_NOT_CONFIRM;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.SALE_RETURN_QTY_EXCEED;

/**
 * 退货服务实现。
 * <p>
 * 退货依赖：渠道退款（E PaymentFacade）、积分回退（F MemberPointFacade）、
 * 库存回补（C InventoryFacade）。任一依赖未实现即抛对应“服务未就绪”错误码，
 * 整个事务回滚；本实现不写库存/支付/积分数据。
 */
@Service
public class SaleReturnServiceImpl implements SaleReturnService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Resource
    private SaleOrderMapper saleOrderMapper;
    @Resource
    private SaleOrderLineMapper saleOrderLineMapper;
    @Resource
    private SaleReturnMapper saleReturnMapper;
    @Resource
    private SaleReturnLineMapper saleReturnLineMapper;
    @Resource
    private InventoryFacade inventoryFacade;
    @Resource
    private PaymentFacade paymentFacade;
    @Resource
    private MemberPointFacade memberPointFacade;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReturn(SaleReturnSaveReqVO reqVO) {
        // 1. 原单校验
        PhSaleOrderDO order = saleOrderMapper.selectById(reqVO.getSaleOrderId());
        if (order == null) {
            throw ServiceExceptionUtil.exception(SALE_ORDER_NOT_EXISTS);
        }
        if (order.getStatus() != null && (order.getStatus() == 2 || order.getStatus() == -1)) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_NOT_ALLOW);
        }

        // 2. 明细校验与金额汇总
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PhSaleReturnLineDO> lines = new ArrayList<>();
        for (SaleReturnSaveReqVO.Item item : reqVO.getItems()) {
            PhSaleOrderLineDO line = saleOrderLineMapper.selectById(item.getSaleOrderLineId());
            if (line == null || !line.getOrderId().equals(order.getId())) {
                throw ServiceExceptionUtil.exception(SALE_RETURN_NOT_EXISTS);
            }
            int returned = saleReturnLineMapper.selectSumQtyBySaleLineId(line.getId());
            if (returned + item.getQty() > line.getQty()) {
                throw ServiceExceptionUtil.exception(SALE_RETURN_QTY_EXCEED);
            }
            if (line.getIsRx() != null && line.getIsRx() == 1
                    && (reqVO.getPharmacistConfirm() == null || reqVO.getPharmacistConfirm() != 1)) {
                throw ServiceExceptionUtil.exception(SALE_RETURN_PRESC_NOT_CONFIRM);
            }
            PhSaleReturnLineDO rl = new PhSaleReturnLineDO();
            rl.setSaleLineId(line.getId());
            rl.setBatchId(line.getBatchId());
            rl.setDrugId(line.getDrugId());
            rl.setQty(item.getQty());
            rl.setPrice(line.getPrice());
            rl.setAmount(line.getPrice().multiply(BigDecimal.valueOf(item.getQty())));
            rl.setPointsDeduct(0);
            rl.setLocationId(item.getLocationId());
            totalAmount = totalAmount.add(rl.getAmount());
            lines.add(rl);
        }
        if (totalAmount.signum() <= 0) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_QTY_EXCEED);
        }

        // 3. 渠道退款（refundMethod=0 原路，依赖 E 支付；未实现整体回滚；幂等键后续改为 returnNo）
        Integer refundMethod = reqVO.getRefundMethod() == null ? 0 : reqVO.getRefundMethod();
        try {
            if (refundMethod == 0) {
                paymentFacade.refund(null, "REFUND-" + System.currentTimeMillis(), totalAmount.movePointRight(2).intValue(), null);
            }
        } catch (UnsupportedOperationException ex) {
            throw ServiceExceptionUtil.exception(PAY_SERVICE_UNAVAILABLE);
        }

        // 4. 积分回退（F 服务；本期 pointsDeduct=0 不触发）
        try {
            // 有积分抵扣的销售退货应回退积分；本期销售 pointsDeduct=0
            if (order.getPointsDeduct() != null && order.getPointsDeduct().signum() > 0) {
                memberPointFacade.backPoints(order.getMemberId(),
                        order.getOrderNo(), order.getPointsDeduct().intValue());
            }
        } catch (UnsupportedOperationException ex) {
            throw ServiceExceptionUtil.exception(MEMBER_SERVICE_UNAVAILABLE);
        }

        // 5. 库存回补（C 服务；未实现整体回滚）
        List<ReturnBackItem> returnItems = new ArrayList<>();
        for (PhSaleReturnLineDO rl : lines) {
            ReturnBackItem rbi = new ReturnBackItem();
            rbi.setDrugId(rl.getDrugId());
            rbi.setBatchId(rl.getBatchId());
            rbi.setQty(rl.getQty());
            rbi.setLocationId(rl.getLocationId());
            returnItems.add(rbi);
        }
        try {
            inventoryFacade.returnBack(order.getStoreId(), returnItems);
        } catch (UnsupportedOperationException ex) {
            throw ServiceExceptionUtil.exception(INV_SERVICE_UNAVAILABLE);
        }

        // 6. 写退货单（本期流程直接置“已完成”，审批流由后续迭代接入）
        String returnNo = genReturnNo(order.getStoreId());
        if (saleReturnMapper.selectByReturnNo(returnNo) != null) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_NO_DUPLICATE);
        }
        PhSaleReturnDO ret = new PhSaleReturnDO();
        ret.setReturnNo(returnNo);
        ret.setSaleOrderId(order.getId());
        ret.setStoreId(order.getStoreId());
        ret.setReturnType(reqVO.getReturnType());
        ret.setReason(reqVO.getReason());
        ret.setTotalAmount(totalAmount);
        ret.setRefundMethod(refundMethod);
        ret.setStatus(1); // 1 待审批（一期直接完成审批，简化流程）
        ret.setCashierId(reqVO.getCashierId() != null ? reqVO.getCashierId() : order.getCashierId());
        ret.setPharmacistConfirm(reqVO.getPharmacistConfirm());
        ret.setRefundStatus(0);
        saleReturnMapper.insert(ret);
        for (PhSaleReturnLineDO l : lines) {
            l.setReturnId(ret.getId());
            saleReturnLineMapper.insert(l);
        }

        // 7. 更新原单行已退数量与整单退货标志
        boolean allReturned = true;
        for (PhSaleReturnLineDO rl : lines) {
            PhSaleOrderLineDO line = saleOrderLineMapper.selectById(rl.getSaleLineId());
            int newReturned = line.getReturnedQty() + rl.getQty();
            line.setReturnedQty(newReturned);
            saleOrderLineMapper.updateById(line);
            if (newReturned < line.getQty()) {
                allReturned = false;
            }
        }
        order.setReturnFlag(allReturned ? 2 : 1);
        order.setStatus(allReturned ? 2 : 3);
        saleOrderMapper.updateById(order);
        return ret.getId();
    }

    @Override
    public PageResult<PhSaleReturnDO> getReturnPage(SaleReturnPageReqVO reqVO) {
        return saleReturnMapper.selectPage(reqVO);
    }

    @Override
    public PhSaleReturnDO getReturn(Long id) {
        PhSaleReturnDO ret = saleReturnMapper.selectById(id);
        if (ret == null) {
            throw ServiceExceptionUtil.exception(SALE_RETURN_NOT_EXISTS);
        }
        return ret;
    }

    @Override
    public SaleReturnDetailRespVO getReturnDetail(Long id) {
        PhSaleReturnDO ret = getReturn(id);
        SaleReturnDetailRespVO detail = new SaleReturnDetailRespVO();
        detail.setReturnOrder(ret);
        detail.setLines(saleReturnLineMapper.selectListByReturnId(id));
        return detail;
    }

    private String genReturnNo(Long storeId) {
        return "SR-" + storeId + "-" + LocalDateTime.now().format(TIME_FMT) + "-"
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
