package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryPreviewReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryPreviewMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryReadMapper;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryQuantity;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryRuleException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.*;

@Service
@Validated
@RequiredArgsConstructor
public class InventoryPreviewService {
    private final InventoryReadAccess access;
    private final InventoryReadMapper reads;
    private final InventoryPreviewMapper mapper;
    private final DrugApi drugs;

    public record Preview(Instant evaluatedAt, FefoAllocator.ExpiryDayPolicy expiryDayPolicy,
                          boolean reserved, List<FefoAllocator.Allocation> allocations) { }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Preview preview(@NotNull @Valid InventoryPreviewReqVO request) {
        var scope = access.requireScope(null);
        var warehouse = reads.selectWarehouse(scope, request.getWarehouseId());
        if (warehouse == null) throw exception(NOT_FOUND);
        if (!Objects.equals(warehouse.getStatus(), 1)) throw invalidParamException("仓库未启用");
        drugs.validateDrugList(List.of(request.getDrugId()));
        var rows = mapper.selectSnapshot(scope, request.getWarehouseId(), request.getDrugId());
        if (rows.size() > 10000) throw invalidParamException("库存明细超过单仓预览上限，请联系管理员核查");
        // One captured instant for all candidate expiry checks and the returned timestamp.
        Instant now = Instant.now();
        try {
            List<FefoAllocator.Candidate> candidates = candidates(scope, request, rows);
            var allocator = new FefoAllocator(Clock.fixed(now, ZoneId.of("Asia/Shanghai")), request.getExpiryDayPolicy());
            return new Preview(now, request.getExpiryDayPolicy(), false,
                    allocator.allocate(scope.tenantId(), scope.storeId(), request.getDrugId(),
                            Set.of(request.getWarehouseId()), request.getQuantity(), candidates));
        } catch (InventoryRuleException ex) {
            if (ex.getReason() == InventoryRuleException.Reason.INVENTORY_NOT_ENOUGH) {
                throw invalidParamException("符合本次效期边界、质量和启用条件的可用库存不足");
            }
            throw invalidParamException("库存快照异常，请先核对批次与货位账");
        }
    }

    private static List<FefoAllocator.Candidate> candidates(InventoryReadAccess.Scope scope,
            InventoryPreviewReqVO request, List<InventoryPreviewMapper.Row> rows) {
        var groups = new LinkedHashMap<Long, List<InventoryPreviewMapper.Row>>();
        for (var row : rows) groups.computeIfAbsent(row.getBatchId(), ignored -> new ArrayList<>()).add(row);
        var result = new ArrayList<FefoAllocator.Candidate>();
        for (var group : groups.values()) {
            var batch = group.get(0);
            var quantity = new InventoryQuantity(batch.getQtyTotal(), batch.getQtyAvail(), batch.getQtyFrozen(), batch.getQtySold());
            long total = 0;
            long frozen = 0;
            for (var row : group) {
                if (row.getStockId() == null) continue;
                if (!Objects.equals(row.getValidLocation(), 1)) {
                    throw invalidParamException("货位库存归属异常，请先核对库存账");
                }
                InventoryQuantity.locationAvailable(row.getQuantity(), row.getFrozen());
                total += row.getQuantity();
                frozen += row.getFrozen();
                if (Objects.equals(row.getLocationStatus(), 1)) {
                    result.add(new FefoAllocator.Candidate(scope.tenantId(), scope.storeId(), request.getWarehouseId(),
                            request.getDrugId(), row.getBatchId(), row.getLocationId(), row.getExpiryDate(),
                            row.getQuantity(), row.getFrozen(), Objects.equals(row.getQualityStatus(), 0)));
                }
            }
            quantity.verifyLocationTotals(total, frozen);
        }
        return result;
    }
}
