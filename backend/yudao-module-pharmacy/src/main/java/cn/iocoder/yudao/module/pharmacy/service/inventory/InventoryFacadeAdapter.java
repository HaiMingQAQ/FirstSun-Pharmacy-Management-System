package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.*;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryFacadeMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryFacadeMapper.Batch;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryFacadeMapper.Flow;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryFacadeMapper.FlowCommand;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryFacadeMapper.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * Real write-side inventory facade. Source rows are the idempotency boundary: flow rows are
 * checked before any selection/locking, then the same unique event key is written in one local
 * transaction with the caller's document state.
 */
@Service
@RequiredArgsConstructor
public class InventoryFacadeAdapter implements InventoryFacade {

    private static final int FLOW_RECEIVE = 10;
    private static final int FLOW_SALE = 20;
    private static final int FLOW_RETURN = 21;
    private static final int FLOW_RESERVE = 80;
    private static final int FLOW_RELEASE = 81;
    private static final int FLOW_CONSUME = 82;
    private static final int BIZ_RECEIPT = 1;
    private static final int BIZ_SALE = 2;
    private static final int BIZ_RETURN = 3;
    private static final int BIZ_ONLINE_ORDER = 6;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final InventoryReadAccess access;
    private final InventoryFacadeMapper mapper;
    private final DrugApi drugApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceiveResult receive(Long storeId, String receiptNo, List<ReceiveItem> items) {
        var scope = access.requireScope(storeId);
        validateReceipt(receiptNo, items, scope.storeId());
        Map<Long, List<Flow>> existing = flows(scope, BIZ_RECEIPT, receiptNo, receiveLineIds(items));
        if (!existing.isEmpty()) return existingReceipt(items, existing);

        drugApi.validateDrugList(items.stream().map(ReceiveItem::getDrugId).distinct().toList());
        long operator = operator();
        Map<Long, Long> usedByLocation = new HashMap<>();
        List<ReceiveItem> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparingLong(item -> parseLineId(item.getBizLineId())));
        List<ReceiveResult.ReceiveLineResult> result = new ArrayList<>(ordered.size());
        for (ReceiveItem item : ordered) {
            var warehouse = mapper.lockWarehouse(scope, item.getWarehouseId());
            if (warehouse == null || !Objects.equals(warehouse.getStatus(), 1)) {
                throw invalidParamException("收货仓库不存在或未启用");
            }
            var location = mapper.lockLocation(scope, item.getWarehouseId(), item.getLocationId());
            if (location == null || !Objects.equals(location.getStatus(), 1)) {
                throw invalidParamException("收货货位不存在或未启用");
            }
            long used = usedByLocation.computeIfAbsent(item.getLocationId(), id -> mapper.sumLocationQty(scope, id));
            long nextUsed = Math.addExact(used, item.getQty());
            if (location.getMaxCapacity() != null && nextUsed > location.getMaxCapacity()) {
                throw invalidParamException("收货数量超过货位容量");
            }
            usedByLocation.put(item.getLocationId(), nextUsed);

            mapper.upsertBatch(scope, item.getWarehouseId(), item.getDrugId(), item.getBatchNo(),
                    item.getManufactureDate(), item.getExpiryDate(), String.valueOf(operator));
            Batch batch = mapper.lockBatchByNaturalKey(scope, item.getWarehouseId(), item.getDrugId(), item.getBatchNo());
            if (batch == null || !Objects.equals(batch.getExpiryDate(), item.getExpiryDate())
                    || !Objects.equals(batch.getManufactureDate(), item.getManufactureDate())) {
                throw invalidParamException("批次信息与已有库存不一致");
            }
            mapper.insertStock(scope, batch.getId(), item.getLocationId(), item.getDrugId(), String.valueOf(operator));
            Stock stock = mapper.lockStock(scope, batch.getId(), item.getLocationId());
            if (stock == null || !Objects.equals(stock.getDrugId(), item.getDrugId())) {
                throw invalidParamException("货位库存归属异常");
            }
            require(mapper.increaseStock(scope, stock.getId(), item.getQty(), String.valueOf(operator)), "货位库存入账失败");
            require(mapper.increaseBatchForReceipt(scope, batch.getId(), item.getQty(), item.getUnitPrice(), String.valueOf(operator)), "批次库存入账失败");
            insertFlow(scope, batch, item.getLocationId(), FLOW_RECEIVE, item.getQty(), 0,
                    Math.addExact(batch.getQtyTotal(), item.getQty()), BIZ_RECEIPT, receiptNo,
                    parseLineId(item.getBizLineId()), null, operator, item.getUnitPrice());
            result.add(ReceiveResult.ReceiveLineResult.builder().bizLineId(item.getBizLineId())
                    .batchId(batch.getId()).locationStockId(stock.getId()).build());
        }
        return ReceiveResult.builder().lines(result).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeductResult deduct(Long storeId, List<DeductItem> items) {
        return deduct(access.requireScope(storeId), items, operator());
    }

    // Package-private: only the verified paid-order entry in this package may use a system actor.
    DeductResult deduct(InventoryReadAccess.Scope scope, List<DeductItem> items, long operator) {
        validateDeduct(items);
        Map<Long, List<Flow>> existing = flows(scope, BIZ_SALE, items.get(0).getBizNo(), deductLineIds(items));
        if (!existing.isEmpty()) return existingDeduct(items, existing);

        drugApi.validateDrugList(items.stream().map(DeductItem::getDrugId).distinct().toList());
        List<DeductResult.Allocation> allocations = new ArrayList<>();
        for (DeductItem item : items.stream().sorted(Comparator.comparingLong(DeductItem::getBizLineId)).toList()) {
            if (item.getBatchId() != null || item.getLocationId() != null) {
                if (item.getBatchId() == null || item.getLocationId() == null) {
                    throw invalidParamException("销售扣库必须同时指定批次和货位");
                }
                deductOne(scope, item, item.getBatchId(), item.getLocationId(), item.getQty(), operator, allocations);
            } else {
                int remaining = item.getQty();
                List<InventoryFacadeMapper.FefoStock> candidates = mapper.lockFefoStocks(scope, item.getDrugId(), LocalDate.now(BUSINESS_ZONE));
                for (var candidate : candidates) {
                    int available = Math.subtractExact(candidate.getQty(), candidate.getQtyFrozen());
                    int quantity = Math.min(remaining, available);
                    if (quantity > 0) {
                        deductOne(scope, item, candidate.getBatchId(), candidate.getLocationId(), quantity, operator, allocations);
                        remaining -= quantity;
                    }
                    if (remaining == 0) break;
                }
                if (remaining != 0) throw invalidParamException("符合效期、质量和货位状态的可用库存不足");
            }
        }
        DeductResult result = new DeductResult();
        result.setSuccess(true);
        result.setAllocations(allocations);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReserveResult reserve(Long storeId, List<ReserveItem> items) {
        var scope = access.requireScope(storeId);
        validateReserve(items);
        Map<Long, List<Flow>> existing = flowsByType(scope, BIZ_ONLINE_ORDER, items.get(0).getBizNo(), reserveLineIds(items), FLOW_RESERVE);
        if (!existing.isEmpty()) return existingReserve(items, existing);

        drugApi.validateDrugList(items.stream().map(ReserveItem::getDrugId).distinct().toList());
        long operator = operator();
        List<ReserveResult.Allocation> allocations = new ArrayList<>();
        for (ReserveItem item : items.stream().sorted(Comparator.comparingLong(ReserveItem::getBizLineId)).toList()) {
            if (item.getBatchId() != null || item.getLocationId() != null) {
                if (item.getBatchId() == null || item.getLocationId() == null) throw invalidParamException("冻结库存必须同时指定批次和货位");
                reserveOne(scope, item, item.getBatchId(), item.getLocationId(), item.getQty(), operator, allocations);
            } else {
                int remaining = item.getQty();
                for (var candidate : mapper.lockFefoStocks(scope, item.getDrugId(), LocalDate.now(BUSINESS_ZONE))) {
                    int quantity = Math.min(remaining, Math.subtractExact(candidate.getQty(), candidate.getQtyFrozen()));
                    if (quantity > 0) {
                        reserveOne(scope, item, candidate.getBatchId(), candidate.getLocationId(), quantity, operator, allocations);
                        remaining -= quantity;
                    }
                    if (remaining == 0) break;
                }
                if (remaining != 0) throw invalidParamException("符合效期、质量和货位状态的可用库存不足");
            }
        }
        ReserveResult result = new ReserveResult(); result.setSuccess(true); result.setAllocations(allocations); return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long storeId, List<ReleaseItem> items) {
        var scope = access.requireScope(storeId);
        validateRelease(items);
        Map<Long, List<Flow>> existing = flowsByType(scope, BIZ_ONLINE_ORDER, items.get(0).getBizNo(), releaseLineIds(items), FLOW_RELEASE);
        if (!existing.isEmpty()) { for (ReleaseItem item : items) requireFrozenQuantity(existing.get(item.getBizLineId()), item.getQty(), "释放幂等记录不完整"); return; }
        long operator = operator();
        for (ReleaseItem item : items.stream().sorted(Comparator.comparingLong(ReleaseItem::getBizLineId)).toList()) {
            resolveReservation(scope, item.getOriginalBizNo(), item.getOriginalBizLineId(), item.getBatchId(), item.getLocationId(), item.getDrugId(), item.getQty(), operator, false, item.getBizNo(), item.getBizLineId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeductResult consumeReservation(Long storeId, List<ConsumeItem> items) {
        return consumeReservation(access.requireScope(storeId), items, operator());
    }

    DeductResult consumeReservation(InventoryReadAccess.Scope scope, List<ConsumeItem> items, long operator) {
        validateConsume(items);
        Map<Long, List<Flow>> existing = flowsByType(scope, BIZ_ONLINE_ORDER, items.get(0).getBizNo(), consumeLineIds(items), FLOW_CONSUME);
        if (!existing.isEmpty()) return existingConsumed(items, existing);
        List<DeductResult.Allocation> allocations = new ArrayList<>();
        for (ConsumeItem item : items.stream().sorted(Comparator.comparingLong(ConsumeItem::getBizLineId)).toList()) {
            resolveReservation(scope, item.getOriginalBizNo(), item.getOriginalBizLineId(), item.getBatchId(), item.getLocationId(), item.getDrugId(), item.getQty(), operator, true, item.getBizNo(), item.getBizLineId());
            DeductResult.Allocation allocation = new DeductResult.Allocation(); allocation.setBizLineId(item.getBizLineId()); allocation.setBatchId(item.getBatchId()); allocation.setLocationId(item.getLocationId()); allocation.setQty(item.getQty()); allocations.add(allocation);
        }
        DeductResult result = new DeductResult(); result.setSuccess(true); result.setAllocations(allocations); return result;
    }

    @Override
    public List<AvailableQty> getAvailableQty(Long storeId, List<Long> drugIds) {
        var scope = access.requireScope(storeId);
        if (drugIds == null || drugIds.isEmpty() || drugIds.stream().anyMatch(id -> id == null || id <= 0) || new HashSet<>(drugIds).size() != drugIds.size()) {
            throw invalidParamException("可售量查询药品编号非法");
        }
        Map<Long, Integer> quantities = new HashMap<>();
        mapper.selectAvailableQty(scope, drugIds).forEach(row -> quantities.put(row.getDrugId(), row.getQtyAvail()));
        return drugIds.stream().map(id -> { AvailableQty result = new AvailableQty(); result.setDrugId(id); result.setQtyAvail(quantities.getOrDefault(id, 0)); return result; }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnBack(Long storeId, List<ReturnBackItem> items) {
        var scope = access.requireScope(storeId);
        validateReturn(items);
        Map<Long, List<Flow>> existing = flows(scope, BIZ_RETURN, items.get(0).getBizNo(), returnLineIds(items));
        if (!existing.isEmpty()) {
            for (ReturnBackItem item : items) requireQuantity(existing.get(item.getBizLineId()), item.getQty(), true, "退货幂等记录不完整");
            return;
        }

        long operator = operator();
        for (ReturnBackItem item : items.stream().sorted(Comparator.comparingLong(ReturnBackItem::getBizLineId)).toList()) {
            Flow original = mapper.lockOriginalOutboundFlow(scope, item.getOriginalBizNo(), item.getOriginalBizLineId(),
                    item.getBatchId(), item.getLocationId());
            if (original == null || original.getOutQty() == null) throw invalidParamException("未找到原销售出库流水");
            int returned = mapper.returnedQty(scope, original.getId());
            if (Math.addExact(returned, item.getQty()) > original.getOutQty()) {
                throw invalidParamException("累计退货数量超过原销售出库数量");
            }
            Batch batch = mapper.lockBatch(scope, item.getBatchId());
            if (batch == null || !Objects.equals(batch.getDrugId(), item.getDrugId())) throw invalidParamException("退货批次不存在或药品不匹配");
            var location = mapper.lockLocation(scope, batch.getWarehouseId(), item.getLocationId());
            Stock stock = mapper.lockStock(scope, item.getBatchId(), item.getLocationId());
            if (location == null || stock == null || !Objects.equals(stock.getDrugId(), item.getDrugId())) {
                throw invalidParamException("退货货位不存在或库存归属异常");
            }
            long used = mapper.sumLocationQty(scope, item.getLocationId());
            if (location.getMaxCapacity() != null && Math.addExact(used, item.getQty()) > location.getMaxCapacity()) {
                throw invalidParamException("退货回补超过货位容量");
            }
            require(mapper.returnStock(scope, stock.getId(), item.getQty(), String.valueOf(operator)), "货位库存回补失败");
            require(mapper.returnBatch(scope, batch.getId(), item.getQty(), String.valueOf(operator)), "批次库存回补失败");
            insertFlow(scope, batch, item.getLocationId(), FLOW_RETURN, item.getQty(), 0,
                    Math.addExact(batch.getQtyTotal(), item.getQty()), BIZ_RETURN, item.getBizNo(), item.getBizLineId(),
                    original.getId(), operator, batch.getCostPrice());
        }
    }

    private void reserveOne(InventoryReadAccess.Scope scope, ReserveItem item, long batchId, long locationId, int qty,
                            long operator, List<ReserveResult.Allocation> allocations) {
        Batch batch = mapper.lockBatch(scope, batchId);
        if (batch == null || !Objects.equals(batch.getDrugId(), item.getDrugId()) || !Objects.equals(batch.getQualityStatus(), 0)
                || batch.getExpiryDate() == null || batch.getExpiryDate().isBefore(LocalDate.now(BUSINESS_ZONE))) {
            throw invalidParamException("冻结批次不存在、停售或已过期");
        }
        var warehouse = mapper.lockWarehouse(scope, batch.getWarehouseId());
        var location = mapper.lockLocation(scope, batch.getWarehouseId(), locationId);
        Stock stock = mapper.lockStock(scope, batchId, locationId);
        if (warehouse == null || !Objects.equals(warehouse.getStatus(), 1) || location == null || !Objects.equals(location.getStatus(), 1)
                || stock == null || !Objects.equals(stock.getDrugId(), item.getDrugId()) || stock.getQty() - stock.getQtyFrozen() < qty
                || batch.getQtyAvail() < qty) {
            throw invalidParamException("可冻结库存不足或仓库、货位状态非法");
        }
        require(mapper.reserveStock(scope, stock.getId(), qty, String.valueOf(operator)), "货位库存冻结失败");
        require(mapper.reserveBatch(scope, batch.getId(), qty, String.valueOf(operator)), "批次库存冻结失败");
        insertFlow(scope, batch, locationId, FLOW_RESERVE, 0, 0, qty, batch.getQtyTotal(), BIZ_ONLINE_ORDER,
                item.getBizNo(), item.getBizLineId(), null, operator, batch.getCostPrice());
        ReserveResult.Allocation allocation = new ReserveResult.Allocation();
        allocation.setBizLineId(item.getBizLineId()); allocation.setBatchId(batchId); allocation.setLocationId(locationId); allocation.setQty(qty);
        allocations.add(allocation);
    }

    private void resolveReservation(InventoryReadAccess.Scope scope, String originalBizNo, long originalBizLineId,
                                    long batchId, long locationId, long drugId, int qty, long operator, boolean consume,
                                    String bizNo, long bizLineId) {
        Flow reservation = mapper.lockReservationFlow(scope, originalBizNo, originalBizLineId, batchId, locationId);
        if (reservation == null || reservation.getFrozenDelta() == null || reservation.getFrozenDelta() <= 0) {
            throw invalidParamException("未找到原库存冻结流水");
        }
        if (Math.addExact(mapper.resolvedReservationQty(scope, reservation.getId()), qty) > reservation.getFrozenDelta()) {
            throw invalidParamException("释放或转出数量超过原冻结数量");
        }
        Batch batch = mapper.lockBatch(scope, batchId);
        Stock stock = mapper.lockStock(scope, batchId, locationId);
        if (batch == null || stock == null || !Objects.equals(batch.getDrugId(), drugId) || !Objects.equals(stock.getDrugId(), drugId)) {
            throw invalidParamException("冻结批次、货位或药品不匹配");
        }
        if (consume) {
            require(mapper.consumeReservedStock(scope, stock.getId(), qty, String.valueOf(operator)), "货位冻结转出库失败");
            require(mapper.consumeReservedBatch(scope, batch.getId(), qty, String.valueOf(operator)), "批次冻结转出库失败");
            insertFlow(scope, batch, locationId, FLOW_CONSUME, 0, qty, -qty, Math.subtractExact(batch.getQtyTotal(), qty),
                    BIZ_ONLINE_ORDER, bizNo, bizLineId, reservation.getId(), operator, batch.getCostPrice());
        } else {
            require(mapper.releaseStock(scope, stock.getId(), qty, String.valueOf(operator)), "货位库存释放失败");
            require(mapper.releaseBatch(scope, batch.getId(), qty, String.valueOf(operator)), "批次库存释放失败");
            insertFlow(scope, batch, locationId, FLOW_RELEASE, 0, 0, -qty, batch.getQtyTotal(),
                    BIZ_ONLINE_ORDER, bizNo, bizLineId, reservation.getId(), operator, batch.getCostPrice());
        }
    }

    private ReserveResult existingReserve(List<ReserveItem> items, Map<Long, List<Flow>> existing) {
        List<ReserveResult.Allocation> allocations = new ArrayList<>();
        for (ReserveItem item : items) {
            List<Flow> values = existing.get(item.getBizLineId()); requireFrozenQuantity(values, item.getQty(), "冻结幂等记录不完整");
            for (Flow flow : values) {
                ReserveResult.Allocation allocation = new ReserveResult.Allocation(); allocation.setBizLineId(item.getBizLineId());
                allocation.setBatchId(flow.getBatchId()); allocation.setLocationId(flow.getLocationId()); allocation.setQty(flow.getFrozenDelta()); allocations.add(allocation);
            }
        }
        ReserveResult result = new ReserveResult(); result.setSuccess(true); result.setAllocations(allocations); return result;
    }

    private DeductResult existingConsumed(List<ConsumeItem> items, Map<Long, List<Flow>> existing) {
        List<DeductResult.Allocation> allocations = new ArrayList<>();
        for (ConsumeItem item : items) {
            List<Flow> values = existing.get(item.getBizLineId()); requireQuantity(values, item.getQty(), false, "冻结转出库幂等记录不完整");
            for (Flow flow : values) { DeductResult.Allocation allocation = new DeductResult.Allocation(); allocation.setBizLineId(item.getBizLineId()); allocation.setBatchId(flow.getBatchId()); allocation.setLocationId(flow.getLocationId()); allocation.setQty(flow.getOutQty()); allocations.add(allocation); }
        }
        DeductResult result = new DeductResult(); result.setSuccess(true); result.setAllocations(allocations); return result;
    }

    private void deductOne(InventoryReadAccess.Scope scope, DeductItem item, long batchId, long locationId, int qty,
                           long operator, List<DeductResult.Allocation> allocations) {
        Batch batch = mapper.lockBatch(scope, batchId);
        if (batch == null || !Objects.equals(batch.getDrugId(), item.getDrugId()) || !Objects.equals(batch.getQualityStatus(), 0)
                || batch.getExpiryDate() == null || batch.getExpiryDate().isBefore(LocalDate.now(BUSINESS_ZONE))) {
            throw invalidParamException("批次不存在、停售或已过期");
        }
        var warehouse = mapper.lockWarehouse(scope, batch.getWarehouseId());
        var location = mapper.lockLocation(scope, batch.getWarehouseId(), locationId);
        Stock stock = mapper.lockStock(scope, batchId, locationId);
        if (warehouse == null || !Objects.equals(warehouse.getStatus(), 1) || location == null || !Objects.equals(location.getStatus(), 1)
                || stock == null || !Objects.equals(stock.getDrugId(), item.getDrugId()) || stock.getQty() - stock.getQtyFrozen() < qty
                || batch.getQtyAvail() < qty || batch.getQtyTotal() - qty < batch.getQtyFrozen()) {
            throw invalidParamException("库存不足或仓库、货位状态非法");
        }
        require(mapper.deductStock(scope, stock.getId(), qty, String.valueOf(operator)), "货位库存扣减失败");
        require(mapper.deductBatch(scope, batch.getId(), qty, String.valueOf(operator)), "批次库存扣减失败");
        insertFlow(scope, batch, locationId, FLOW_SALE, 0, qty, Math.subtractExact(batch.getQtyTotal(), qty),
                BIZ_SALE, item.getBizNo(), item.getBizLineId(), null, operator, batch.getCostPrice());
        DeductResult.Allocation allocation = new DeductResult.Allocation();
        allocation.setBizLineId(item.getBizLineId()); allocation.setBatchId(batchId); allocation.setLocationId(locationId); allocation.setQty(qty);
        allocations.add(allocation);
    }

    private ReceiveResult existingReceipt(List<ReceiveItem> items, Map<Long, List<Flow>> existing) {
        List<ReceiveResult.ReceiveLineResult> lines = new ArrayList<>();
        for (ReceiveItem item : items) {
            List<Flow> flows = existing.get(parseLineId(item.getBizLineId()));
            requireQuantity(flows, item.getQty(), true, "收货幂等记录不完整");
            if (flows.size() != 1) throw invalidParamException("收货明细存在异常重复流水");
            Flow flow = flows.get(0);
            lines.add(ReceiveResult.ReceiveLineResult.builder().bizLineId(item.getBizLineId())
                    .batchId(flow.getBatchId()).locationStockId(null).build());
        }
        return ReceiveResult.builder().lines(lines).build();
    }

    private DeductResult existingDeduct(List<DeductItem> items, Map<Long, List<Flow>> existing) {
        List<DeductResult.Allocation> allocations = new ArrayList<>();
        for (DeductItem item : items) {
            List<Flow> flows = existing.get(item.getBizLineId());
            requireQuantity(flows, item.getQty(), false, "销售幂等记录不完整");
            for (Flow flow : flows) {
                DeductResult.Allocation allocation = new DeductResult.Allocation();
                allocation.setBizLineId(item.getBizLineId()); allocation.setBatchId(flow.getBatchId());
                allocation.setLocationId(flow.getLocationId()); allocation.setQty(flow.getOutQty()); allocations.add(allocation);
            }
        }
        DeductResult result = new DeductResult(); result.setSuccess(true); result.setAllocations(allocations); return result;
    }

    private Map<Long, List<Flow>> flows(InventoryReadAccess.Scope scope, int bizType, String bizNo, Set<Long> ids) {
        Map<Long, List<Flow>> result = new HashMap<>();
        for (Long id : ids) {
            List<Flow> values = mapper.selectFlows(scope, bizType, bizNo, id);
            if (!values.isEmpty()) result.put(id, values);
        }
        if (!result.isEmpty() && result.size() != ids.size()) throw invalidParamException("业务操作存在部分库存流水，禁止重新执行");
        return result;
    }

    private Map<Long, List<Flow>> flowsByType(InventoryReadAccess.Scope scope, int bizType, String bizNo, Set<Long> ids, int flowType) {
        Map<Long, List<Flow>> result = new HashMap<>();
        for (Long id : ids) {
            List<Flow> values = mapper.selectFlowsByType(scope, bizType, bizNo, id, flowType);
            if (!values.isEmpty()) result.put(id, values);
        }
        if (!result.isEmpty() && result.size() != ids.size()) throw invalidParamException("业务操作存在部分库存流水，禁止重新执行");
        return result;
    }

    private void insertFlow(InventoryReadAccess.Scope scope, Batch batch, long locationId, int flowType, int inQty, int outQty,
                            int balance, int bizType, String bizNo, long bizLineId, Long originalFlowId,
                            long operator, BigDecimal unitCost) {
        insertFlow(scope, batch, locationId, flowType, inQty, outQty, 0, balance, bizType, bizNo, bizLineId, originalFlowId, operator, unitCost);
    }

    private void insertFlow(InventoryReadAccess.Scope scope, Batch batch, long locationId, int flowType, int inQty, int outQty,
                            int frozenDelta, int balance, int bizType, String bizNo, long bizLineId, Long originalFlowId,
                            long operator, BigDecimal unitCost) {
        FlowCommand flow = new FlowCommand();
        flow.setBatchId(batch.getId()); flow.setLocationId(locationId); flow.setDrugId(batch.getDrugId()); flow.setBatchNo(batch.getBatchNo());
        flow.setFlowType(flowType); flow.setInQty(inQty); flow.setOutQty(outQty); flow.setFrozenDelta(frozenDelta); flow.setBalanceQty(balance);
        flow.setBizType(bizType); flow.setBizNo(bizNo); flow.setBizLineId(bizLineId); flow.setOriginalFlowId(originalFlowId);
        flow.setUnitCost(unitCost == null ? BigDecimal.ZERO : unitCost); flow.setOperator(operator); flow.setFlowTime(LocalDateTime.now());
        require(mapper.insertFlow(scope, flow), "库存流水写入失败");
    }

    private static void validateReceipt(String receiptNo, List<ReceiveItem> items, long storeId) {
        if (blank(receiptNo) || items == null || items.isEmpty()) throw invalidParamException("收货单号和明细不能为空");
        Set<Long> ids = new HashSet<>();
        for (ReceiveItem item : items) {
            long lineId = item == null ? 0 : parseLineId(item.getBizLineId());
            if (!ids.add(lineId) || item.getDrugId() == null || item.getDrugId() <= 0 || item.getWarehouseId() == null || item.getWarehouseId() <= 0
                    || item.getLocationId() == null || item.getLocationId() <= 0 || item.getQty() == null || item.getQty() <= 0
                    || blank(item.getBatchNo()) || item.getExpiryDate() == null || item.getUnitPrice() == null || item.getUnitPrice().signum() < 0
                    || (item.getStoreId() != null && item.getStoreId() != storeId)
                    || (item.getManufactureDate() != null && item.getManufactureDate().isAfter(item.getExpiryDate()))) {
                throw invalidParamException("收货明细参数非法");
            }
        }
    }

    private static void validateDeduct(List<DeductItem> items) {
        if (items == null || items.isEmpty()) throw invalidParamException("销售扣库明细不能为空");
        String bizNo = items.get(0) == null ? null : items.get(0).getBizNo(); Set<Long> ids = new HashSet<>();
        for (DeductItem item : items) {
            if (item == null || blank(item.getBizNo()) || !Objects.equals(bizNo, item.getBizNo()) || item.getBizLineId() == null || item.getBizLineId() <= 0
                    || !ids.add(item.getBizLineId()) || item.getDrugId() == null || item.getDrugId() <= 0 || item.getQty() == null || item.getQty() <= 0) {
                throw invalidParamException("销售扣库缺少来源单据、来源行或药品数量");
            }
        }
    }

    private static void validateReturn(List<ReturnBackItem> items) {
        if (items == null || items.isEmpty()) throw invalidParamException("销售退货明细不能为空");
        String bizNo = items.get(0) == null ? null : items.get(0).getBizNo(); Set<Long> ids = new HashSet<>();
        for (ReturnBackItem item : items) {
            if (item == null || blank(item.getBizNo()) || !Objects.equals(bizNo, item.getBizNo()) || item.getBizLineId() == null || item.getBizLineId() <= 0
                    || !ids.add(item.getBizLineId()) || blank(item.getOriginalBizNo()) || item.getOriginalBizLineId() == null || item.getOriginalBizLineId() <= 0
                    || item.getDrugId() == null || item.getDrugId() <= 0 || item.getBatchId() == null || item.getBatchId() <= 0
                    || item.getLocationId() == null || item.getLocationId() <= 0 || item.getQty() == null || item.getQty() <= 0) {
                throw invalidParamException("销售退货缺少来源、原出库引用、批次、货位或数量");
            }
        }
    }

    private static void validateReserve(List<ReserveItem> items) {
        if (items == null || items.isEmpty()) throw invalidParamException("库存冻结明细不能为空");
        String bizNo = items.get(0) == null ? null : items.get(0).getBizNo(); Set<Long> ids = new HashSet<>();
        for (ReserveItem item : items) {
            if (item == null || blank(item.getBizNo()) || !Objects.equals(bizNo, item.getBizNo()) || item.getBizLineId() == null || item.getBizLineId() <= 0
                    || !ids.add(item.getBizLineId()) || item.getDrugId() == null || item.getDrugId() <= 0 || item.getQty() == null || item.getQty() <= 0
                    || (item.getBatchId() == null) != (item.getLocationId() == null)) throw invalidParamException("库存冻结缺少来源单据、来源行、药品、数量或批次货位");
        }
    }

    private static void validateRelease(List<ReleaseItem> items) {
        if (items == null || items.isEmpty()) throw invalidParamException("库存释放明细不能为空");
        String bizNo = items.get(0) == null ? null : items.get(0).getBizNo(); Set<Long> ids = new HashSet<>();
        for (ReleaseItem item : items) {
            if (item == null || blank(item.getBizNo()) || !Objects.equals(bizNo, item.getBizNo()) || item.getBizLineId() == null || item.getBizLineId() <= 0 || !ids.add(item.getBizLineId())
                    || blank(item.getOriginalBizNo()) || item.getOriginalBizLineId() == null || item.getOriginalBizLineId() <= 0 || item.getDrugId() == null || item.getDrugId() <= 0
                    || item.getBatchId() == null || item.getBatchId() <= 0 || item.getLocationId() == null || item.getLocationId() <= 0 || item.getQty() == null || item.getQty() <= 0) {
                throw invalidParamException("库存释放缺少来源、原冻结引用、批次、货位或数量");
            }
        }
    }

    private static void validateConsume(List<ConsumeItem> items) {
        if (items == null || items.isEmpty()) throw invalidParamException("冻结转出库明细不能为空");
        String bizNo = items.get(0) == null ? null : items.get(0).getBizNo(); Set<Long> ids = new HashSet<>();
        for (ConsumeItem item : items) {
            if (item == null || blank(item.getBizNo()) || !Objects.equals(bizNo, item.getBizNo()) || item.getBizLineId() == null || item.getBizLineId() <= 0 || !ids.add(item.getBizLineId())
                    || blank(item.getOriginalBizNo()) || item.getOriginalBizLineId() == null || item.getOriginalBizLineId() <= 0 || item.getDrugId() == null || item.getDrugId() <= 0
                    || item.getBatchId() == null || item.getBatchId() <= 0 || item.getLocationId() == null || item.getLocationId() <= 0 || item.getQty() == null || item.getQty() <= 0) {
                throw invalidParamException("冻结转出库缺少来源、原冻结引用、批次、货位或数量");
            }
        }
    }

    private static Set<Long> receiveLineIds(List<ReceiveItem> items) { return items.stream().map(item -> parseLineId(item.getBizLineId())).collect(java.util.stream.Collectors.toSet()); }
    private static Set<Long> deductLineIds(List<DeductItem> items) { return items.stream().map(DeductItem::getBizLineId).collect(java.util.stream.Collectors.toSet()); }
    private static Set<Long> returnLineIds(List<ReturnBackItem> items) { return items.stream().map(ReturnBackItem::getBizLineId).collect(java.util.stream.Collectors.toSet()); }
    private static Set<Long> reserveLineIds(List<ReserveItem> items) { return items.stream().map(ReserveItem::getBizLineId).collect(java.util.stream.Collectors.toSet()); }
    private static Set<Long> releaseLineIds(List<ReleaseItem> items) { return items.stream().map(ReleaseItem::getBizLineId).collect(java.util.stream.Collectors.toSet()); }
    private static Set<Long> consumeLineIds(List<ConsumeItem> items) { return items.stream().map(ConsumeItem::getBizLineId).collect(java.util.stream.Collectors.toSet()); }
    private static long parseLineId(String value) { try { long id = Long.parseLong(value); if (id <= 0) throw new NumberFormatException(); return id; } catch (RuntimeException ex) { throw invalidParamException("来源明细编号必须是正整数"); } }
    private static void requireQuantity(List<Flow> flows, int expected, boolean inbound, String message) { if (flows == null || flows.stream().mapToInt(flow -> inbound ? flow.getInQty() : flow.getOutQty()).sum() != expected) throw invalidParamException(message); }
    private static void requireFrozenQuantity(List<Flow> flows, int expected, String message) { if (flows == null || flows.stream().mapToInt(Flow::getFrozenDelta).map(Math::abs).sum() != expected) throw invalidParamException(message); }
    private static void require(int affected, String message) { if (affected != 1) throw invalidParamException(message); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static long operator() { Long id = SecurityFrameworkUtils.getLoginUserId(); if (id == null || id <= 0) throw invalidParamException("缺少有效库存操作人"); return id; }
}
