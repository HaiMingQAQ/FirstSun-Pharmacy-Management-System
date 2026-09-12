package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeRecordReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryStocktakeMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Stocktake state machine and one-transaction physical adjustment. */
@Service
@Validated
@RequiredArgsConstructor
public class InventoryStocktakeService {
    private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final InventoryReadAccess access;
    private final InventoryStocktakeMapper mapper;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResult<InventoryStocktakeVO.Summary> page(@Valid InventoryStocktakeQuery query) {
        var scope = access.requireScope(null);
        return new PageResult<>(mapper.selectPage(scope, query), mapper.count(scope, query));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public InventoryStocktakeVO.Detail detail(long id) {
        var scope = access.requireScope(null);
        var summary = mapper.selectSummary(scope, id);
        if (summary == null) throw exception(NOT_FOUND);
        var detail = new InventoryStocktakeVO.Detail();
        detail.setSummary(summary);
        detail.setLines(mapper.selectLines(scope, id));
        if (summary.getBlindFlag() != null && summary.getBlindFlag() == 1
                && summary.getStatus() != null && summary.getStatus() == 1) {
            detail.getLines().forEach(line -> line.setBookQty(null));
        }
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public long create(@NotNull @Valid InventoryStocktakeCreateReqVO request) {
        var scope = access.requireScope(null);
        if (request.getFreezeFlag() != null && request.getFreezeFlag() == 1) {
            throw invalidParamException("盘点冻结策略尚未完成 ph_inv_lock 归属确认");
        }
        var warehouse = mapper.lockWarehouse(scope, request.getWarehouseId());
        if (warehouse == null) throw exception(NOT_FOUND);
        if (warehouse.getStatus() == null || warehouse.getStatus() != 1) {
            throw invalidParamException("仓库未启用，不能发起盘点");
        }
        long operator = operator();
        var key = new InventoryStocktakeMapper.GeneratedKey();
        try {
            int inserted = mapper.insertHeader(scope, request, number(scope.storeId()), operator, key);
            if (inserted != 1 || key.getId() == null || key.getId() <= 0) {
                throw new IllegalStateException("盘点单新增未返回唯一有效主键");
            }
            mapper.insertSnapshotLines(scope, key.getId(), request.getWarehouseId(), operator);
            if (mapper.updateTotalItem(scope, key.getId(), operator) != 1) {
                throw new IllegalStateException("盘点项数更新失败");
            }
            return key.getId();
        } catch (DuplicateKeyException ex) {
            throw invalidParamException("盘点单号已存在，请重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void start(long id) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, id);
        if (header.getFreezeFlag() != null && header.getFreezeFlag() == 1) {
            throw invalidParamException("盘点冻结策略尚未完成 ph_inv_lock 归属确认");
        }
        if (mapper.countLines(scope, id) <= 0) throw invalidParamException("盘点范围没有可盘库存");
        transition(scope, id, 0, 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public void record(@NotNull @Valid InventoryStocktakeRecordReqVO request) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, request.getStocktakeId());
        if (header.getStatus() == null || header.getStatus() != 1) {
            throw invalidParamException("只有进行中的盘点单可以录入实盘数");
        }
        InventoryStocktakeVO.Line line = mapper.selectLines(scope, request.getStocktakeId()).stream()
                .filter(item -> request.getLineId().equals(item.getId())).findFirst().orElse(null);
        if (line == null) throw exception(NOT_FOUND);
        int diff = Math.subtractExact(request.getRealQty(), line.getBookQty());
        int flag = diff > 0 ? 1 : diff < 0 ? 2 : 0;
        if (mapper.updateRecord(scope, request.getStocktakeId(), request.getLineId(), request.getRealQty(),
                diff, flag, operator()) != 1) {
            throw invalidParamException("盘点明细已变化，请刷新后重试");
        }
        int done = mapper.countRecorded(scope, request.getStocktakeId());
        mapper.updateDoneItem(scope, request.getStocktakeId(), done, operator());
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(long id) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, id);
        if (header.getStatus() == null || header.getStatus() != 1) {
            throw invalidParamException("只有进行中的盘点单可以完成盘点");
        }
        if (mapper.countUnrecorded(scope, id) != 0) throw invalidParamException("仍有盘点明细未录入实盘数");
        transition(scope, id, 1, 2);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveAndAdjust(long id) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, id);
        if (header.getStatus() == null || header.getStatus() != 2) {
            throw invalidParamException("只有已完成盘点可以审批调整");
        }
        if (header.getFreezeFlag() != null && header.getFreezeFlag() == 1) {
            throw invalidParamException("盘点冻结策略尚未完成 ph_inv_lock 归属确认");
        }
        var warehouse = mapper.lockWarehouse(scope, header.getWarehouseId());
        if (warehouse == null || warehouse.getStatus() == null) throw exception(NOT_FOUND);
        List<InventoryStocktakeMapper.AdjustmentLine> lines = mapper.selectAdjustmentLines(scope, id);
        if (lines.isEmpty()) throw invalidParamException("盘点没有明细，不能审批");
        Map<Long, Integer> expectedTotals = new HashMap<>();
        for (var line : lines) {
            if (line.getRealQty() == null) throw invalidParamException("盘点明细未全部录入");
            expectedTotals.merge(line.getBatchId(), line.getBookQty(), Math::addExact);
        }
        Map<Long, InventoryStocktakeMapper.BatchLock> batches = new HashMap<>();
        Map<Long, Integer> locationDeltas = new HashMap<>();
        for (var line : lines) {
            int diff = Math.subtractExact(line.getRealQty(), line.getBookQty());
            locationDeltas.merge(line.getLocationId(), diff, Math::addExact);
        }
        for (Long batchId : expectedTotals.keySet().stream().sorted().toList()) {
            var batch = mapper.lockBatch(scope, batchId);
            if (batch == null || !header.getWarehouseId().equals(batch.getWarehouseId())) {
                throw invalidParamException("盘点批次归属已变化，请重新盘点");
            }
            if (!expectedTotals.get(batchId).equals(batch.getQtyTotal())) {
                throw invalidParamException("盘点期间库存已变化，请重新盘点");
            }
            batches.put(batchId, batch);
        }
        for (Long locationId : locationDeltas.keySet().stream().sorted().toList()) {
            var location = mapper.lockLocation(scope, header.getWarehouseId(), locationId);
            if (location == null || location.getStatus() == null || location.getStatus() != 1) {
                throw invalidParamException("盘点货位已停用或不存在");
            }
            long usedAfter = Math.addExact(location.getUsedQty() == null ? 0 : location.getUsedQty(),
                    locationDeltas.get(locationId));
            if (usedAfter < 0 || (location.getMaxCapacity() != null && usedAfter > location.getMaxCapacity())) {
                throw invalidParamException("盘点调整会超过货位容量");
            }
        }
        LocalDateTime flowTime = LocalDateTime.now();
        for (var line : lines) {
            var batch = batches.get(line.getBatchId());
            var location = mapper.lockStock(scope, line.getBatchId(), line.getLocationId());
            if (location == null || !line.getBookQty().equals(location.getQty())) {
                throw invalidParamException("盘点期间货位库存已变化，请重新盘点");
            }
            int diff = Math.subtractExact(line.getRealQty(), line.getBookQty());
            if (diff == 0) continue;
            if (diff < 0 && (location.getQtyFrozen() == null || line.getRealQty() < location.getQtyFrozen())) {
                throw invalidParamException("实盘数不能低于冻结量");
            }
            if (batch.getQtyAvail() == null || batch.getQtyFrozen() == null
                    || Math.addExact(batch.getQtyAvail(), diff) < 0
                    || Math.addExact(batch.getQtyTotal(), diff) < batch.getQtyFrozen()) {
                throw invalidParamException("盘点调整会造成库存数量非法");
            }
            line.setDiffQty(diff);
            if (mapper.updateStock(scope, location.getId(), diff, operator()) != 1
                    || mapper.updateBatch(scope, batch.getId(), diff, operator()) != 1) {
                throw new IllegalStateException("盘点调整未影响唯一库存记录");
            }
            batch.setQtyTotal(Math.addExact(batch.getQtyTotal(), diff));
            batch.setQtyAvail(Math.addExact(batch.getQtyAvail(), diff));
            if (mapper.insertFlow(scope, line, batch.getQtyTotal(), operator(), flowTime) != 1) {
                throw new IllegalStateException("盘点流水写入失败");
            }
        }
        if (mapper.markAdjusted(scope, id, operator()) != 1) {
            throw invalidParamException("盘点状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelDraft(long id) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, id);
        if (header.getStatus() == null || header.getStatus() != 0) {
            throw invalidParamException("只有草稿盘点可以取消");
        }
        if (mapper.cancelDraft(scope, id, operator()) != 1) {
            throw invalidParamException("盘点状态已变化，请刷新后重试");
        }
    }

    private InventoryStocktakeMapper.Header lockHeader(InventoryReadAccess.Scope scope, long id) {
        var header = mapper.lockHeader(scope, id);
        if (header == null) throw exception(NOT_FOUND);
        return header;
    }

    private void transition(InventoryReadAccess.Scope scope, long id, int from, int to) {
        if (mapper.updateStatus(scope, id, from, to, operator()) != 1) {
            throw invalidParamException("盘点状态已变化，请刷新后重试");
        }
    }

    private static long operator() {
        Long id = SecurityFrameworkUtils.getLoginUserId();
        if (id == null || id <= 0) throw invalidParamException("缺少有效操作人");
        return id;
    }

    private static String number(long storeId) {
        int sequence = Math.floorMod(SEQUENCE.incrementAndGet(), 1000);
        return "PD-" + storeId + "-" + LocalDateTime.now().format(NO_TIME) + "-" + String.format("%03d", sequence);
    }
}
