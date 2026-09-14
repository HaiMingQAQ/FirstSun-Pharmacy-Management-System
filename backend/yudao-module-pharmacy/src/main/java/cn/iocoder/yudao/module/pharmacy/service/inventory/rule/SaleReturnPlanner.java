package cn.iocoder.yudao.module.pharmacy.service.inventory.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryRuleException.Reason.*;

/**
 * Plans a complete return event against locked original issue snapshots. No SQL or transaction.
 * The service must authorize the original sale and store, replay operation idempotency first,
 * lock the warehouses/original flows, and obtain alreadyReturned from committed return flows.
 * Normal return quality, original location usability, capacity and net-sold checks remain required
 * before applying the plan. Expiry or current drug saleability must not erase historical references.
 */
public final class SaleReturnPlanner {

    public List<ReturnAllocation> plan(long tenantId, long storeId, List<OriginalIssue> originals,
            List<ReturnLine> lines) {
        if (tenantId < 0 || storeId <= 0) {
            throw new InventoryRuleException(INVENTORY_SCOPE_DENIED);
        }
        if (originals == null || lines == null || lines.isEmpty()) {
            throw new InventoryRuleException(INVALID_ALLOCATION);
        }
        Map<Long, OriginalIssue> byId = new HashMap<>();
        for (OriginalIssue original : originals) {
            if (original == null) {
                throw new InventoryRuleException(INVALID_ORIGINAL_ISSUE);
            }
            if (original.tenantId != tenantId || original.storeId != storeId) {
                throw new InventoryRuleException(INVENTORY_SCOPE_DENIED);
            }
            if (byId.putIfAbsent(original.flowId, original) != null) {
                throw new InventoryRuleException(INVALID_ORIGINAL_ISSUE);
            }
        }
        // One return source line may reference several original allocations of the same drug.
        Map<Long, Long> sourceDrugs = new HashMap<>();
        Map<Long, Set<Long>> sourceFlows = new HashMap<>();
        Map<Long, Map<Long, Set<Long>>> sourceDestinations = new HashMap<>();
        Map<Long, Long> sourceQuantities = new HashMap<>();
        Map<Long, Long> requestedByFlow = new HashMap<>();
        List<ReturnAllocation> result = new ArrayList<>();
        for (ReturnLine line : lines) {
            if (line == null) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
            OriginalIssue original = byId.get(line.originalFlowId);
            if (original == null) {
                throw new InventoryRuleException(INVALID_ORIGINAL_ISSUE);
            }
            // Existing uk_flow_event permits only one return flow for a source line + batch/location.
            // Keep distinct original references: require caller-owned line IDs instead of merging them.
            if (!sourceDestinations.computeIfAbsent(line.sourceLineId, ignored -> new HashMap<>())
                    .computeIfAbsent(original.batchId, ignored -> new HashSet<>()).add(original.locationId)) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
            long sourceQuantity = sourceQuantities.getOrDefault(line.sourceLineId, 0L) + line.quantity;
            if (sourceQuantity > Integer.MAX_VALUE) {
                throw new InventoryRuleException(QUANTITY_OVERFLOW);
            }
            sourceQuantities.put(line.sourceLineId, sourceQuantity);
            Long drugId = sourceDrugs.putIfAbsent(line.sourceLineId, original.drugId);
            if ((drugId != null && drugId.longValue() != original.drugId)
                    || !sourceFlows.computeIfAbsent(line.sourceLineId, ignored -> new HashSet<>())
                    .add(line.originalFlowId)) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
            long requested = requestedByFlow.getOrDefault(original.flowId, 0L) + line.quantity;
            // Each accepted cumulative value is at most INT_MAX, so the next addition cannot overflow long.
            if (requested > original.issuedQuantity - original.alreadyReturned) {
                throw new InventoryRuleException(RETURN_EXCEEDS_ISSUED);
            }
            requestedByFlow.put(original.flowId, requested);
            result.add(new ReturnAllocation(line, original));
        }
        result.sort(Comparator.comparingLong(ReturnAllocation::getSourceLineId)
                .thenComparingLong(ReturnAllocation::getOriginalFlowId));
        return Collections.unmodifiableList(result);
    }

    /** Internal request value: no client-selected replacement batch, location, drug or cost. */
    public static final class ReturnLine {
        private final long sourceLineId;
        private final long originalFlowId;
        private final int quantity;

        public ReturnLine(long sourceLineId, long originalFlowId, int quantity) {
            if (sourceLineId <= 0 || originalFlowId <= 0) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
            InventoryQuantity.requirePositive(quantity);
            this.sourceLineId = sourceLineId;
            this.originalFlowId = originalFlowId;
            this.quantity = quantity;
        }
    }

    /** Must be constructed from an original, non-deleted sales issue flow, never request JSON. */
    public static final class OriginalIssue {
        private final long tenantId;
        private final long storeId;
        private final long flowId;
        private final long drugId;
        private final long batchId;
        private final long locationId;
        private final int issuedQuantity;
        private final long alreadyReturned;
        private final BigDecimal unitCost;

        public OriginalIssue(long tenantId, long storeId, long flowId, long drugId, long batchId,
                long locationId, int flowType, int inQuantity, int issuedQuantity,
                long alreadyReturned, BigDecimal unitCost) {
            if (tenantId < 0 || storeId <= 0 || flowId <= 0 || drugId <= 0 || batchId <= 0
                    || locationId <= 0 || (flowType != 20 && flowType != 82) || inQuantity != 0
                    || issuedQuantity <= 0 || alreadyReturned < 0 || alreadyReturned > issuedQuantity
                    || unitCost == null || unitCost.signum() < 0) {
                throw new InventoryRuleException(INVALID_ORIGINAL_ISSUE);
            }
            BigDecimal exactCost;
            try {
                exactCost = unitCost.setScale(4, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException exception) {
                throw new InventoryRuleException(INVALID_ORIGINAL_ISSUE);
            }
            if (exactCost.precision() > 18) {
                throw new InventoryRuleException(INVALID_ORIGINAL_ISSUE);
            }
            this.tenantId = tenantId;
            this.storeId = storeId;
            this.flowId = flowId;
            this.drugId = drugId;
            this.batchId = batchId;
            this.locationId = locationId;
            this.issuedQuantity = issuedQuantity;
            this.alreadyReturned = alreadyReturned;
            this.unitCost = exactCost;
        }
    }

    public static final class ReturnAllocation {
        private final long sourceLineId;
        private final long originalFlowId;
        private final long drugId;
        private final long batchId;
        private final long locationId;
        private final int quantity;
        private final BigDecimal unitCost;

        private ReturnAllocation(ReturnLine line, OriginalIssue original) {
            this.sourceLineId = line.sourceLineId;
            this.originalFlowId = original.flowId;
            this.drugId = original.drugId;
            this.batchId = original.batchId;
            this.locationId = original.locationId;
            this.quantity = line.quantity;
            this.unitCost = original.unitCost;
        }

        public long getSourceLineId() { return sourceLineId; }
        public long getOriginalFlowId() { return originalFlowId; }
        public long getDrugId() { return drugId; }
        public long getBatchId() { return batchId; }
        public long getLocationId() { return locationId; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitCost() { return unitCost; }
    }
}
