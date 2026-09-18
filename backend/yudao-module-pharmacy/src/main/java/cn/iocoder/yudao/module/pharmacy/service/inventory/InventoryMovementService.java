package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryMovementReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryMovementMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryMovementMapper.Batch;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryMovementMapper.FlowCommand;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryMovementMapper.Stock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Same-warehouse movement. Batch total and frozen quantity stay unchanged. */
@Service
@Validated
@RequiredArgsConstructor
public class InventoryMovementService {
    static final int FLOW_MOVEMENT = 60;
    static final int BIZ_MOVEMENT = 8;

    private final InventoryReadAccess access;
    private final InventoryMovementMapper mapper;

    @Transactional(rollbackFor = Exception.class)
    public void execute(@NotNull @Valid InventoryMovementReqVO request) {
        var scope = access.requireScope(null);
        validate(request);
        if (isCompletedReplay(scope, request)) return;

        var warehouse = mapper.lockWarehouse(scope, request.getWarehouseId());
        if (warehouse == null || warehouse.getStatus() == null || warehouse.getStatus() != 1) {
            throw invalidParamException("仓库不存在或未启用");
        }
        Map<Long, Batch> batches = lockBatches(scope, request);
        Map<Long, InventoryMovementMapper.Location> locations = lockLocations(scope, request);
        Map<StockKey, Stock> sourceStocks = lockAndValidateSources(scope, request, batches);
        validateCapacity(scope, request, locations);

        long operator = operator();
        Map<StockKey, Stock> targetStocks = new HashMap<>();
        for (InventoryMovementReqVO.Line line : ordered(request)) {
            Batch batch = batches.get(line.getBatchId());
            Stock source = sourceStocks.get(new StockKey(line.getBatchId(), line.getSourceLocationId()));
            if (mapper.decreaseStock(scope, source.getId(), line.getQty(), String.valueOf(operator)) != 1) {
                throw invalidParamException("源货位可移动库存不足");
            }
            StockKey targetKey = new StockKey(line.getBatchId(), line.getTargetLocationId());
            Stock target = targetStocks.get(targetKey);
            if (target == null) {
                mapper.upsertStock(scope, line.getBatchId(), line.getTargetLocationId(), batch.getDrugId(), String.valueOf(operator));
                target = mapper.lockStock(scope, line.getBatchId(), line.getTargetLocationId());
                if (target == null || !batch.getDrugId().equals(target.getDrugId())) {
                    throw new IllegalStateException("目标货位库存创建失败");
                }
                targetStocks.put(targetKey, target);
            }
            if (mapper.increaseStock(scope, target.getId(), line.getQty(), String.valueOf(operator)) != 1) {
                throw new IllegalStateException("目标货位库存增加失败");
            }
            LocalDateTime now = LocalDateTime.now();
            insertFlow(scope, batch, line.getSourceLocationId(), 0, line.getQty(), request.getBizNo(), line.getBizLineId(), operator, now);
            insertFlow(scope, batch, line.getTargetLocationId(), line.getQty(), 0, request.getBizNo(), line.getBizLineId(), operator, now);
            source.setQty(Math.subtractExact(source.getQty(), line.getQty()));
            target.setQty(Math.addExact(target.getQty(), line.getQty()));
        }
    }

    private boolean isCompletedReplay(InventoryReadAccess.Scope scope, InventoryMovementReqVO request) {
        int completed = 0;
        for (InventoryMovementReqVO.Line line : request.getLines()) {
            List<InventoryMovementMapper.Flow> flows = mapper.selectFlows(scope, request.getBizNo(), line.getBizLineId());
            if (flows.isEmpty()) continue;
            if (flows.size() != 2 || !matchesPair(flows, line)) {
                throw invalidParamException("移位业务单存在不完整或冲突流水，禁止重复执行");
            }
            completed++;
        }
        if (completed == 0) return false;
        if (completed != request.getLines().size()) {
            throw invalidParamException("移位业务单存在部分流水，禁止重复执行");
        }
        return true;
    }

    private static boolean matchesPair(List<InventoryMovementMapper.Flow> flows, InventoryMovementReqVO.Line line) {
        boolean source = false;
        boolean target = false;
        for (InventoryMovementMapper.Flow flow : flows) {
            if (!line.getBatchId().equals(flow.getBatchId())) return false;
            source |= line.getSourceLocationId().equals(flow.getLocationId())
                    && flow.getInQty() == 0 && line.getQty().equals(flow.getOutQty());
            target |= line.getTargetLocationId().equals(flow.getLocationId())
                    && line.getQty().equals(flow.getInQty()) && flow.getOutQty() == 0;
        }
        return source && target;
    }

    private Map<Long, Batch> lockBatches(InventoryReadAccess.Scope scope, InventoryMovementReqVO request) {
        Map<Long, Batch> result = new HashMap<>();
        for (Long id : request.getLines().stream().map(InventoryMovementReqVO.Line::getBatchId).distinct().sorted().toList()) {
            Batch batch = mapper.lockBatch(scope, request.getWarehouseId(), id);
            if (batch == null || batch.getQtyTotal() == null || batch.getQtyTotal() < 0) {
                throw invalidParamException("批次不存在或不属于当前仓库");
            }
            result.put(id, batch);
        }
        return result;
    }

    private Map<Long, InventoryMovementMapper.Location> lockLocations(InventoryReadAccess.Scope scope, InventoryMovementReqVO request) {
        Set<Long> ids = new HashSet<>();
        request.getLines().forEach(line -> { ids.add(line.getSourceLocationId()); ids.add(line.getTargetLocationId()); });
        Map<Long, InventoryMovementMapper.Location> result = new HashMap<>();
        for (Long id : ids.stream().sorted().toList()) {
            var location = mapper.lockLocation(scope, request.getWarehouseId(), id);
            if (location == null || location.getStatus() == null || location.getStatus() != 1) {
                throw invalidParamException("货位不存在、不属于当前仓库或未启用");
            }
            result.put(id, location);
        }
        return result;
    }

    private Map<StockKey, Stock> lockAndValidateSources(InventoryReadAccess.Scope scope, InventoryMovementReqVO request,
                                                         Map<Long, Batch> batches) {
        Map<StockKey, Integer> required = new HashMap<>();
        for (InventoryMovementReqVO.Line line : request.getLines()) {
            required.merge(new StockKey(line.getBatchId(), line.getSourceLocationId()), line.getQty(), Math::addExact);
        }
        Map<StockKey, Stock> result = new HashMap<>();
        for (StockKey key : required.keySet().stream().sorted().toList()) {
            Stock stock = mapper.lockStock(scope, key.batchId(), key.locationId());
            Batch batch = batches.get(key.batchId());
            if (stock == null || !batch.getDrugId().equals(stock.getDrugId()) || stock.getQty() == null || stock.getQtyFrozen() == null
                    || stock.getQty() < stock.getQtyFrozen() || stock.getQty() - stock.getQtyFrozen() < required.get(key)) {
                throw invalidParamException("源货位可移动库存不足，冻结库存不可移位");
            }
            result.put(key, stock);
        }
        return result;
    }

    private void validateCapacity(InventoryReadAccess.Scope scope, InventoryMovementReqVO request,
                                  Map<Long, InventoryMovementMapper.Location> locations) {
        Map<Long, Integer> delta = new HashMap<>();
        for (InventoryMovementReqVO.Line line : request.getLines()) {
            delta.merge(line.getSourceLocationId(), -line.getQty(), Math::addExact);
            delta.merge(line.getTargetLocationId(), line.getQty(), Math::addExact);
        }
        for (Map.Entry<Long, Integer> entry : delta.entrySet()) {
            long after = Math.addExact(mapper.sumLocationQty(scope, entry.getKey()), entry.getValue());
            if (after < 0 || (locations.get(entry.getKey()).getMaxCapacity() != null && after > locations.get(entry.getKey()).getMaxCapacity())) {
                throw invalidParamException("移位后货位库存将超过容量或为负数");
            }
        }
    }

    private void insertFlow(InventoryReadAccess.Scope scope, Batch batch, long locationId, int inQty, int outQty,
                            String bizNo, long bizLineId, long operator, LocalDateTime now) {
        FlowCommand flow = new FlowCommand();
        flow.setBatchId(batch.getId()); flow.setLocationId(locationId); flow.setDrugId(batch.getDrugId());
        flow.setBatchNo(batch.getBatchNo()); flow.setInQty(inQty); flow.setOutQty(outQty); flow.setBalanceQty(batch.getQtyTotal());
        flow.setBizNo(bizNo); flow.setBizLineId(bizLineId); flow.setOperator(operator); flow.setFlowTime(now); flow.setUnitCost(batch.getCostPrice());
        if (mapper.insertFlow(scope, flow) != 1) throw new IllegalStateException("移位流水写入失败");
    }

    private static List<InventoryMovementReqVO.Line> ordered(InventoryMovementReqVO request) {
        return request.getLines().stream().sorted(Comparator.comparingLong(InventoryMovementReqVO.Line::getBizLineId)).toList();
    }

    private static void validate(InventoryMovementReqVO request) {
        Set<Long> lineIds = new HashSet<>();
        for (InventoryMovementReqVO.Line line : request.getLines()) {
            if (!lineIds.add(line.getBizLineId())) throw invalidParamException("移位业务明细编号重复");
            if (line.getSourceLocationId().equals(line.getTargetLocationId())) throw invalidParamException("源货位和目标货位不能相同");
        }
    }

    private static long operator() {
        Long id = SecurityFrameworkUtils.getLoginUserId();
        if (id == null || id <= 0) throw invalidParamException("缺少有效操作人");
        return id;
    }

    private record StockKey(long batchId, long locationId) implements Comparable<StockKey> {
        @Override public int compareTo(StockKey other) {
            int batch = Long.compare(batchId, other.batchId);
            return batch != 0 ? batch : Long.compare(locationId, other.locationId);
        }
    }
}
