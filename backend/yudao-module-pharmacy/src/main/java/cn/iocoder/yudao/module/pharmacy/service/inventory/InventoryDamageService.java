package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryDamageCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryDamageQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryDamageVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryDamageMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Report-loss workflow. It never accepts report-surplus and executes only available quantity. */
@Service
@Validated
@RequiredArgsConstructor
public class InventoryDamageService {
    private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final int REPORT_LOSS = 0;

    private final InventoryReadAccess access;
    private final InventoryDamageMapper mapper;

    @Transactional(readOnly = true)
    public PageResult<InventoryDamageVO.Summary> page(@Valid InventoryDamageQuery query) {
        var scope = access.requireScope(null);
        return new PageResult<>(mapper.selectPage(scope, query), mapper.count(scope, query));
    }

    @Transactional(readOnly = true)
    public InventoryDamageVO.Detail detail(long id) {
        var scope = access.requireScope(null);
        var summary = mapper.selectSummary(scope, id);
        if (summary == null) throw exception(NOT_FOUND);
        var result = new InventoryDamageVO.Detail();
        result.setSummary(summary);
        result.setLines(mapper.selectLines(scope, id));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public long create(@NotNull @Valid InventoryDamageCreateReqVO request) {
        var scope = access.requireScope(null);
        if (request.getDamageType() != REPORT_LOSS) throw invalidParamException("一期只支持报损，报溢请走盘点");
        long operator = operator();
        List<InventoryDamageCreateReqVO.Line> lines = request.getLines().stream()
                .sorted(Comparator.comparing(InventoryDamageCreateReqVO.Line::getBatchId)
                        .thenComparing(InventoryDamageCreateReqVO.Line::getLocationId)).toList();
        Set<String> unique = new HashSet<>();
        Map<String, InventoryDamageMapper.BatchLock> batches = new HashMap<>();
        Map<String, InventoryDamageMapper.StockLock> stocks = new HashMap<>();
        for (var line : lines) {
            String key = line.getBatchId() + ":" + line.getLocationId();
            if (!unique.add(key)) throw invalidParamException("同一批次货位不能重复报损");
            var batch = mapper.lockBatch(scope, line.getBatchId());
            if (batch == null) throw invalidParamException("报损批次不存在或不属于当前门店");
            var stock = mapper.lockStock(scope, line.getBatchId(), line.getLocationId());
            if (stock == null) throw invalidParamException("报损货位没有该批次库存");
            int available = Math.subtractExact(stock.getQty(), stock.getQtyFrozen());
            if (line.getQty() > available) throw invalidParamException("报损数量不能超过货位可用库存");
            batches.put(key, batch);
            stocks.put(key, stock);
        }
        var key = new InventoryDamageMapper.GeneratedKey();
        try {
            if (mapper.insertHeader(scope, request, number(scope.storeId()), operator, key) != 1
                    || key.getId() == null || key.getId() <= 0) {
                throw new IllegalStateException("报损单新增未返回唯一有效主键");
            }
            for (var line : lines) {
                var batch = batches.get(line.getBatchId() + ":" + line.getLocationId());
                BigDecimal cost = money(batch.getCostPrice());
                BigDecimal amount = cost.multiply(BigDecimal.valueOf(line.getQty())).setScale(2, RoundingMode.HALF_UP);
                batch.setCostPrice(cost);
                if (mapper.insertLine(scope, key.getId(), line, batch, amount, operator) != 1) {
                    throw new IllegalStateException("报损明细新增失败");
                }
            }
            if (mapper.updateTotals(scope, key.getId(), operator) != 1) {
                throw new IllegalStateException("报损汇总更新失败");
            }
            return key.getId();
        } catch (DuplicateKeyException ex) {
            throw invalidParamException("报损单号已存在，请重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void submit(long id) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, id);
        if (header.getStatus() == null || header.getStatus() != 0) throw invalidParamException("只有草稿报损可以提交");
        if (mapper.countLines(scope, id) == 0) throw invalidParamException("报损单没有明细");
        transition(scope, id, 0, 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(long id) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, id);
        if (header.getStatus() == null || header.getStatus() != 1) throw invalidParamException("只有待审批报损可以审批");
        if (mapper.approve(scope, id, operator()) != 1) throw invalidParamException("报损状态已变化，请刷新后重试");
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(long id) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, id);
        if (header.getStatus() == null || header.getStatus() != 1) throw invalidParamException("只有待审批报损可以驳回");
        if (mapper.reject(scope, id, operator()) != 1) throw invalidParamException("报损状态已变化，请刷新后重试");
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(long id) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, id);
        long operator = operator();
        if (header.getStatus() == null || header.getStatus() != 2) throw invalidParamException("只有已审批报损可以复核");
        if (header.getAuditBy() != null && header.getAuditBy() == operator) {
            throw invalidParamException("审批人与复核人必须是不同登录用户");
        }
        if (header.getReviewBy() != null) return;
        if (mapper.review(scope, id, operator) != 1) throw invalidParamException("报损已被其他人复核");
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(long id) {
        var scope = access.requireScope(null);
        var header = lockHeader(scope, id);
        long operator = operator();
        if (header.getStatus() == null || header.getStatus() != 2) throw invalidParamException("只有已审批报损可以执行");
        if (header.getReviewBy() == null) throw invalidParamException("报损必须先由其他用户复核");
        if (header.getReviewBy() == operator) throw invalidParamException("执行人与复核人必须是不同登录用户");
        if (header.getDamageType() == null || header.getDamageType() != REPORT_LOSS) {
            throw invalidParamException("报溢不允许走报损执行");
        }
        List<InventoryDamageMapper.ExecuteLine> lines = mapper.selectExecuteLines(scope, id);
        if (lines.isEmpty()) throw invalidParamException("报损没有明细");
        Map<Long, InventoryDamageMapper.BatchLock> batches = new HashMap<>();
        Map<String, InventoryDamageMapper.StockLock> stocks = new HashMap<>();
        for (Long batchId : lines.stream().map(InventoryDamageMapper.ExecuteLine::getBatchId).distinct().sorted().toList()) {
            var batch = mapper.lockBatch(scope, batchId);
            if (batch == null) throw invalidParamException("报损批次已不存在或门店归属已变化");
            batches.put(batchId, batch);
        }
        for (var line : lines) {
            var stock = mapper.lockStock(scope, line.getBatchId(), line.getLocationId());
            if (stock == null) throw invalidParamException("报损货位库存已不存在");
            stocks.put(stockKey(line.getBatchId(), line.getLocationId()), stock);
        }
        LocalDateTime flowTime = LocalDateTime.now();
        for (var line : lines) {
            var batch = batches.get(line.getBatchId());
            var stock = stocks.get(stockKey(line.getBatchId(), line.getLocationId()));
            int available = Math.subtractExact(stock.getQty(), stock.getQtyFrozen());
            if (line.getQty() > available || batch.getQtyAvail() < line.getQty()
                    || batch.getQtyTotal() < line.getQty()) {
                throw invalidParamException("执行时可用库存不足，整单不出账");
            }
            if (mapper.updateStock(scope, stock.getId(), line.getQty(), operator) != 1
                    || mapper.updateBatch(scope, batch.getId(), line.getQty(), operator) != 1) {
                throw new IllegalStateException("报损未影响唯一库存记录");
            }
            stock.setQty(Math.subtractExact(stock.getQty(), line.getQty()));
            batch.setQtyTotal(Math.subtractExact(batch.getQtyTotal(), line.getQty()));
            batch.setQtyAvail(Math.subtractExact(batch.getQtyAvail(), line.getQty()));
            if (mapper.insertFlow(scope, line, batch.getQtyTotal(), operator, flowTime) != 1) {
                throw new IllegalStateException("报损流水写入失败");
            }
        }
        if (mapper.execute(scope, id, operator) != 1) throw invalidParamException("报损状态已变化，请刷新后重试");
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelDraft(long id) {
        var scope = access.requireScope(null);
        lockHeader(scope, id);
        if (mapper.cancelDraft(scope, id, operator()) != 1) throw invalidParamException("只有草稿报损可以取消");
    }

    private InventoryDamageMapper.Header lockHeader(InventoryReadAccess.Scope scope, long id) {
        var header = mapper.lockHeader(scope, id);
        if (header == null) throw exception(NOT_FOUND);
        return header;
    }
    private void transition(InventoryReadAccess.Scope scope, long id, int from, int to) {
        if (mapper.updateStatus(scope, id, from, to, operator()) != 1) throw invalidParamException("报损状态已变化，请刷新后重试");
    }
    private static String stockKey(long batchId, long locationId) { return batchId + ":" + locationId; }
    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
    private static long operator() {
        Long id = SecurityFrameworkUtils.getLoginUserId();
        if (id == null || id <= 0) throw invalidParamException("缺少有效操作人");
        return id;
    }
    private static String number(long storeId) {
        int sequence = Math.floorMod(SEQUENCE.incrementAndGet(), 1000);
        return "LS-" + storeId + "-" + LocalDateTime.now().format(NO_TIME) + "-" + String.format("%03d", sequence);
    }
}
