package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.*;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineAllocDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderLineAllocMapper;
import cn.iocoder.yudao.module.pharmacy.service.member.WxOrderPaymentAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Internal paid-order contract. No caller-supplied store, tenant, amount, actor or stock lines. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
public class PaidWxOrderInventoryService {
    private final WxOrderPaymentAccess paymentAccess;
    private final WxOrderLineMapper lineMapper;
    private final WxOrderLineAllocMapper allocationMapper;
    private final InventoryFacadeAdapter inventory;
    // System payment event, not an employee. Flow bizNo/bizLineId link to order and its verified payNo.
    private static final long PAYMENT_EVENT_OPERATOR = 0L;

    public DeductResult deduct(Long orderId, Long paymentId) {
        var order = paymentAccess.requirePaidOrder(orderId, paymentId);
        if (!allocationMapper.selectListByWxOrderId(orderId).isEmpty()) {
            throw invalidParamException("已有预留分配的订单不能走直接扣库");
        }
        var lines = lineMapper.selectListByWxOrderId(orderId);
        if (lines.isEmpty()) throw invalidParamException("订单缺少明细");
        List<DeductItem> items = new ArrayList<>();
        for (var line : lines) {
            var item = new DeductItem();
            item.setBizNo(order.getOrderNo()); item.setBizLineId(line.getId());
            item.setDrugId(line.getDrugId()); item.setQty(line.getQty()); items.add(item);
        }
        return inventory.deduct(new InventoryReadAccess.Scope(WxOrderPaymentAccess.tenantId(), order.getStoreId()),
                items, PAYMENT_EVENT_OPERATOR);
    }

    public DeductResult consumeReservation(Long orderId, Long paymentId) {
        var order = paymentAccess.requirePaidOrder(orderId, paymentId);
        var allocations = allocationMapper.selectListByWxOrderId(orderId);
        var lines = lineMapper.selectListByWxOrderId(orderId);
        if (allocations.isEmpty() || lines.isEmpty()) throw invalidParamException("订单缺少预留或明细");
        Map<Long, Integer> quantities = new HashMap<>();
        List<ConsumeItem> items = new ArrayList<>();
        for (var allocation : allocations) {
            var line = lines.stream().filter(value -> Objects.equals(value.getId(), allocation.getWxOrderLineId()))
                    .findFirst().orElseThrow(() -> invalidParamException("预留引用未知订单明细"));
            if (!Objects.equals(allocation.getStatus(), WxOrderLineAllocDO.STATUS_FROZEN)
                    || !Objects.equals(allocation.getOrderNo(), order.getOrderNo())
                    || !Objects.equals(allocation.getDrugId(), line.getDrugId())
                    || allocation.getQty() == null || allocation.getQty() <= 0) {
                throw invalidParamException("订单预留分配状态或药品不一致");
            }
            quantities.merge(line.getId(), allocation.getQty(), Math::addExact);
            var item = new ConsumeItem();
            item.setBizNo(order.getOrderNo()); item.setBizLineId(allocation.getId());
            item.setOriginalBizNo(order.getOrderNo()); item.setOriginalBizLineId(line.getId());
            item.setDrugId(line.getDrugId()); item.setQty(allocation.getQty());
            item.setBatchId(allocation.getBatchId()); item.setLocationId(allocation.getLocationId());
            items.add(item);
        }
        for (var line : lines) {
            if (!Objects.equals(quantities.get(line.getId()), line.getQty())) {
                throw invalidParamException("预留数量与订单不一致");
            }
        }
        return inventory.consumeReservation(
                new InventoryReadAccess.Scope(WxOrderPaymentAccess.tenantId(), order.getStoreId()),
                items, PAYMENT_EVENT_OPERATOR);
    }
}
