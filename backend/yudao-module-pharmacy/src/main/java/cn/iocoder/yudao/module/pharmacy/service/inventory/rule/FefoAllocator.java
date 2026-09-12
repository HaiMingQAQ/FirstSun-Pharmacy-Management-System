package cn.iocoder.yudao.module.pharmacy.service.inventory.rule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryRuleException.Reason.*;

/**
 * Pure FEFO calculation over trusted database snapshots. Does not reserve or deduct stock.
 * The application must authorize the scope, check drug/warehouse/location/quality states, verify
 * batch/location totals. Mutating callers must lock the snapshot and replay operation idempotency
 * BEFORE invoking this allocator. Read-only simulations use a consistent snapshot and cannot reserve stock.
 */
public final class FefoAllocator {
    /** No default: the product owner must explicitly decide the expiry-day boundary. */
    public enum ExpiryDayPolicy { ALLOW_ON_EXPIRY_DATE, BLOCK_ON_EXPIRY_DATE }

    private final Clock clock;
    private final ExpiryDayPolicy expiryDayPolicy;

    public FefoAllocator(Clock clock, ExpiryDayPolicy expiryDayPolicy) {
        this.clock = Objects.requireNonNull(clock, "clock").withZone(ZoneId.of("Asia/Shanghai"));
        this.expiryDayPolicy = Objects.requireNonNull(expiryDayPolicy, "expiryDayPolicy");
    }

    public List<Allocation> allocate(long tenantId, long storeId, long drugId,
            Set<Long> authorizedWarehouses, int quantity, List<Candidate> candidates) {
        InventoryQuantity.requirePositive(quantity);
        List<Candidate> eligible = eligibleCandidates(tenantId, storeId, drugId, authorizedWarehouses, candidates);
        eligible.sort(Comparator.comparing((Candidate candidate) -> candidate.expiryDate)
                .thenComparingLong(candidate -> candidate.batchId)
                .thenComparingLong(candidate -> candidate.locationId));
        int remaining = quantity;
        List<Allocation> result = new ArrayList<>();
        for (Candidate candidate : eligible) {
            int allocated = Math.min(remaining, candidate.available);
            result.add(new Allocation(candidate.batchId, candidate.locationId, allocated));
            remaining -= allocated;
            if (remaining == 0) {
                return Collections.unmodifiableList(result);
            }
        }
        throw new InventoryRuleException(INVENTORY_NOT_ENOUGH);
    }

    /**
     * Validate one source line's explicit selections against fresh trusted snapshots. Never
     * substitute another batch. Repeated batch/location selections are aggregated before checking
     * availability so splitting a request cannot oversell. The service must additionally account
     * for allocations consumed by other lines of the same operation before invoking this method.
     */
    public List<Allocation> validateExplicit(long tenantId, long storeId, long drugId,
            Set<Long> authorizedWarehouses, int quantity, List<Candidate> candidates,
            List<Selection> selections) {
        InventoryQuantity.requirePositive(quantity);
        if (selections == null || selections.isEmpty()) {
            throw new InventoryRuleException(INVALID_ALLOCATION);
        }
        Map<Long, Map<Long, Integer>> requested = new HashMap<>();
        long total = 0;
        for (Selection selection : selections) {
            if (selection == null) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
            total += selection.quantity;
            // Bounded on every iteration, before any narrowing conversion or map aggregation.
            if (total > quantity) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
            requested.computeIfAbsent(selection.batchId, ignored -> new HashMap<>())
                    .merge(selection.locationId, selection.quantity, Integer::sum);
        }
        if (total != quantity) {
            throw new InventoryRuleException(INVALID_ALLOCATION);
        }
        List<Candidate> eligible = eligibleCandidates(tenantId, storeId, drugId, authorizedWarehouses, candidates);
        List<Allocation> result = new ArrayList<>();
        for (Candidate candidate : eligible) {
            Map<Long, Integer> locations = requested.get(candidate.batchId);
            Integer selected = locations == null ? null : locations.remove(candidate.locationId);
            if (selected == null) {
                continue;
            }
            if (selected > candidate.available) {
                throw new InventoryRuleException(INVENTORY_NOT_ENOUGH);
            }
            result.add(new Allocation(candidate.batchId, candidate.locationId, selected));
        }
        if (requested.values().stream().anyMatch(locations -> !locations.isEmpty())) {
            // Missing, expired, disabled, or fully frozen selections cannot be replaced silently.
            throw new InventoryRuleException(INVENTORY_NOT_ENOUGH);
        }
        result.sort(Comparator.comparingLong(Allocation::getBatchId).thenComparingLong(Allocation::getLocationId));
        return Collections.unmodifiableList(result);
    }

    /**
     * Plan all lines of ONE drug in an operation against a shared residual snapshot. No database
     * mutation occurs. Stable source line IDs determine allocation order; duplicate IDs are rejected.
     * The application must group all lines by drug and plan every group before applying writes.
     * This does not replace operation-level idempotency, database locks, or transaction rollback.
     */
    public List<PlannedLine> allocateLines(long tenantId, long storeId, long drugId,
            Set<Long> authorizedWarehouses, List<Candidate> candidates, List<RequestLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new InventoryRuleException(INVALID_ALLOCATION);
        }
        Set<Long> sourceIds = new HashSet<>();
        List<RequestLine> ordered = new ArrayList<>(lines);
        for (RequestLine line : ordered) {
            if (line == null || !sourceIds.add(line.sourceLineId)) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
        }
        ordered.sort(Comparator.comparingLong(line -> line.sourceLineId));
        // Capture the clock once. A single operation cannot see different dates across midnight.
        FefoAllocator snapshotAllocator = new FefoAllocator(Clock.fixed(clock.instant(), clock.getZone()), expiryDayPolicy);
        snapshotAllocator.eligibleCandidates(tenantId, storeId, drugId, authorizedWarehouses, candidates);
        List<Candidate> remaining = new ArrayList<>(candidates);
        List<PlannedLine> result = new ArrayList<>();
        for (RequestLine line : ordered) {
            List<Allocation> allocations = line.selections == null
                    ? snapshotAllocator.allocate(tenantId, storeId, drugId, authorizedWarehouses, line.quantity, remaining)
                    : snapshotAllocator.validateExplicit(tenantId, storeId, drugId, authorizedWarehouses,
                            line.quantity, remaining, line.selections);
            Map<Long, Map<Long, Integer>> consumed = new HashMap<>();
            for (Allocation allocation : allocations) {
                consumed.computeIfAbsent(allocation.batchId, ignored -> new HashMap<>())
                        .put(allocation.locationId, allocation.quantity);
            }
            for (int index = 0; index < remaining.size(); index++) {
                Candidate candidate = remaining.get(index);
                Map<Long, Integer> locations = consumed.get(candidate.batchId);
                Integer quantity = locations == null ? null : locations.get(candidate.locationId);
                if (quantity != null) {
                    remaining.set(index, new Candidate(candidate, candidate.available - quantity));
                }
            }
            result.add(new PlannedLine(line.sourceLineId, allocations));
        }
        return Collections.unmodifiableList(result);
    }

    private List<Candidate> eligibleCandidates(long tenantId, long storeId, long drugId,
            Set<Long> authorizedWarehouses, List<Candidate> candidates) {
        if (tenantId < 0 || storeId <= 0 || drugId <= 0 || authorizedWarehouses == null
                || authorizedWarehouses.isEmpty() || authorizedWarehouses.stream()
                .anyMatch(id -> id == null || id <= 0)) {
            throw new InventoryRuleException(INVENTORY_SCOPE_DENIED);
        }
        if (candidates == null) {
            throw new InventoryRuleException(INVALID_SNAPSHOT);
        }
        LocalDate today = LocalDate.now(clock);
        List<Candidate> eligible = new ArrayList<>();
        Map<Long, Candidate> batches = new HashMap<>();
        Map<Long, Set<Long>> seenLocations = new HashMap<>();
        Map<Long, Long> locationWarehouses = new HashMap<>();
        for (Candidate candidate : candidates) {
            if (candidate == null) {
                throw new InventoryRuleException(INVALID_SNAPSHOT);
            }
            if (candidate.tenantId != tenantId || candidate.storeId != storeId
                    || candidate.drugId != drugId || !authorizedWarehouses.contains(candidate.warehouseId)) {
                throw new InventoryRuleException(INVENTORY_SCOPE_DENIED);
            }
            Candidate previous = batches.putIfAbsent(candidate.batchId, candidate);
            if (previous != null && (previous.warehouseId != candidate.warehouseId
                    || !previous.expiryDate.equals(candidate.expiryDate)
                    || previous.saleable != candidate.saleable)) {
                throw new InventoryRuleException(INVALID_SNAPSHOT);
            }
            if (!seenLocations.computeIfAbsent(candidate.batchId, ignored -> new HashSet<>())
                    .add(candidate.locationId)) {
                throw new InventoryRuleException(INVALID_SNAPSHOT);
            }
            Long locationWarehouse = locationWarehouses.putIfAbsent(candidate.locationId, candidate.warehouseId);
            if (locationWarehouse != null && locationWarehouse.longValue() != candidate.warehouseId) {
                throw new InventoryRuleException(INVALID_SNAPSHOT);
            }
            boolean expired = candidate.expiryDate.isBefore(today)
                    || (expiryDayPolicy == ExpiryDayPolicy.BLOCK_ON_EXPIRY_DATE
                    && candidate.expiryDate.equals(today));
            if (candidate.saleable && !expired && candidate.available > 0) {
                eligible.add(candidate);
            }
        }
        return eligible;
    }

    /**
     * saleable is the adapter's trusted batch/drug eligibility, never a client-provided flag.
     * Disabled locations must be removed by that adapter before constructing candidates.
     */
    public static final class Candidate {
        private final long tenantId;
        private final long storeId;
        private final long warehouseId;
        private final long drugId;
        private final long batchId;
        private final long locationId;
        private final LocalDate expiryDate;
        private final int available;
        private final boolean saleable;

        public Candidate(long tenantId, long storeId, long warehouseId, long drugId, long batchId,
                long locationId, LocalDate expiryDate, int quantity, int frozen, boolean saleable) {
            if (tenantId < 0 || storeId <= 0 || warehouseId <= 0 || drugId <= 0
                    || batchId <= 0 || locationId <= 0 || expiryDate == null) {
                throw new InventoryRuleException(INVALID_SNAPSHOT);
            }
            this.tenantId = tenantId;
            this.storeId = storeId;
            this.warehouseId = warehouseId;
            this.drugId = drugId;
            this.batchId = batchId;
            this.locationId = locationId;
            this.expiryDate = expiryDate;
            this.available = InventoryQuantity.locationAvailable(quantity, frozen);
            this.saleable = saleable;
        }

        private Candidate(Candidate source, int available) {
            this.tenantId = source.tenantId;
            this.storeId = source.storeId;
            this.warehouseId = source.warehouseId;
            this.drugId = source.drugId;
            this.batchId = source.batchId;
            this.locationId = source.locationId;
            this.expiryDate = source.expiryDate;
            this.available = available;
            this.saleable = source.saleable;
        }
    }

    /** Internal planning value; null selections is possible only via the explicit automatic factory. */
    public static final class RequestLine {
        private final long sourceLineId;
        private final int quantity;
        private final List<Selection> selections;

        private RequestLine(long sourceLineId, int quantity, List<Selection> selections) {
            if (sourceLineId <= 0) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
            InventoryQuantity.requirePositive(quantity);
            this.sourceLineId = sourceLineId;
            this.quantity = quantity;
            this.selections = selections;
        }

        public static RequestLine automatic(long sourceLineId, int quantity) {
            return new RequestLine(sourceLineId, quantity, null);
        }

        public static RequestLine explicit(long sourceLineId, int quantity, List<Selection> selections) {
            if (selections == null || selections.isEmpty() || selections.contains(null)) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
            return new RequestLine(sourceLineId, quantity, Collections.unmodifiableList(new ArrayList<>(selections)));
        }
    }

    public static final class PlannedLine {
        private final long sourceLineId;
        private final List<Allocation> allocations;

        private PlannedLine(long sourceLineId, List<Allocation> allocations) {
            this.sourceLineId = sourceLineId;
            this.allocations = allocations;
        }

        public long getSourceLineId() { return sourceLineId; }
        public List<Allocation> getAllocations() { return allocations; }
    }

    /** Internal selection value, not an approved cross-module or HTTP DTO. */
    public static final class Selection {
        private final long batchId;
        private final long locationId;
        private final int quantity;

        public Selection(long batchId, long locationId, int quantity) {
            if (batchId <= 0 || locationId <= 0) {
                throw new InventoryRuleException(INVALID_ALLOCATION);
            }
            InventoryQuantity.requirePositive(quantity);
            this.batchId = batchId;
            this.locationId = locationId;
            this.quantity = quantity;
        }
    }

    public static final class Allocation {
        private final long batchId;
        private final long locationId;
        private final int quantity;

        private Allocation(long batchId, long locationId, int quantity) {
            this.batchId = batchId;
            this.locationId = locationId;
            this.quantity = quantity;
        }

        public long getBatchId() { return batchId; }
        public long getLocationId() { return locationId; }
        public int getQuantity() { return quantity; }
    }
}
