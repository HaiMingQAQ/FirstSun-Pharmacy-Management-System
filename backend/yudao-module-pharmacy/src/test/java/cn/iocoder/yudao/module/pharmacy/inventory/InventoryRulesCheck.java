package cn.iocoder.yudao.module.pharmacy.inventory;

import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator.Allocation;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator.Candidate;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator.Selection;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator.RequestLine;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator.PlannedLine;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryQuantity;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryRuleException;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.LocationQuantity;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.ReceiptBatchMetadata;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.SaleReturnPlanner;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.SaleReturnPlanner.OriginalIssue;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.SaleReturnPlanner.ReturnLine;
import cn.iocoder.yudao.module.pharmacy.service.inventory.rule.SaleReturnPlanner.ReturnAllocation;

import java.math.BigDecimal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashSet;

import static cn.iocoder.yudao.module.pharmacy.service.inventory.rule.FefoAllocator.ExpiryDayPolicy.*;
import static cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryRuleException.Reason.*;

/** Dependency-free executable rule checks until A integrates the Maven module. Not MySQL tests. */
public final class InventoryRulesCheck {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 10);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-09T16:00:00Z"), ZoneOffset.UTC);
    private static final FefoAllocator FEFO = new FefoAllocator(CLOCK, ALLOW_ON_EXPIRY_DATE);
    private static int passed;

    public static void main(String[] args) {
        check("receive-sale-return conservation", () -> {
            InventoryQuantity empty = new InventoryQuantity(0, 0, 0, 0);
            InventoryQuantity result = empty.receive(18).sell(12).returnSale(3, 10, 0);
            equal(9, result.getTotal());
            equal(9, result.getAvailable());
            equal(9, result.getSold());
            equal(0, empty.getTotal());
            result.verifyLocationTotals(9, 0);
        });
        check("frozen quantity cannot be sold", () -> {
            InventoryQuantity stock = new InventoryQuantity(5, 1, 4, 0);
            expect(INVENTORY_NOT_ENOUGH, () -> stock.sell(2));
            InventoryQuantity result = stock.sell(1);
            equal(4, result.getTotal());
            equal(4, result.getFrozen());
        });
        check("reject invalid conservation", () -> {
            expect(INVALID_SNAPSHOT, () -> new InventoryQuantity(5, 5, 1, 0));
            expect(INVALID_SNAPSHOT, () -> new InventoryQuantity(0, 0, 0, -1));
            expect(INVALID_SNAPSHOT, () -> new InventoryQuantity(0, Integer.MAX_VALUE, 1, 0));
        });
        check("reject zero and negative writes", () -> {
            InventoryQuantity stock = new InventoryQuantity(1, 1, 0, 1);
            expect(INVALID_QUANTITY, () -> stock.receive(0));
            expect(INVALID_QUANTITY, () -> stock.sell(-1));
            expect(INVALID_QUANTITY, () -> stock.returnSale(0, 1, 0));
        });
        check("reject quantity and sold overflow", () -> {
            expect(QUANTITY_OVERFLOW, () -> new InventoryQuantity(Integer.MAX_VALUE,
                    Integer.MAX_VALUE, 0, 0).receive(1));
            expect(QUANTITY_OVERFLOW, () -> new InventoryQuantity(1, 1, 0, Integer.MAX_VALUE).sell(1));
            expect(QUANTITY_OVERFLOW, () -> new InventoryQuantity(Integer.MAX_VALUE,
                    Integer.MAX_VALUE, 0, 1).returnSale(1, 1, 0));
        });
        check("partial return cap", () -> {
            InventoryQuantity stock = new InventoryQuantity(0, 0, 0, 5);
            InventoryQuantity returned = stock.returnSale(2, 5, 0).returnSale(3, 5, 2);
            equal(5, returned.getTotal());
            equal(0, returned.getSold());
            expect(RETURN_EXCEEDS_ISSUED, () -> returned.returnSale(1, 5, 5));
        });
        check("reject corrupt return history", () -> {
            InventoryQuantity stock = new InventoryQuantity(0, 0, 0, 5);
            expect(INVALID_SNAPSHOT, () -> stock.returnSale(1, 5, Long.MAX_VALUE));
            expect(INVALID_SNAPSHOT, () -> stock.returnSale(1, 5, -1));
            expect(INVALID_SNAPSHOT, () -> stock.returnSale(1, 0, 0));
            expect(INVALID_SNAPSHOT, () -> new InventoryQuantity(0, 0, 0, 0).returnSale(1, 5, 0));
        });
        check("location invariants", () -> {
            equal(2, InventoryQuantity.locationAvailable(5, 3));
            expect(INVALID_SNAPSHOT, () -> InventoryQuantity.locationAvailable(3, 4));
            expect(INVALID_SNAPSHOT, () -> InventoryQuantity.locationAvailable(-1, 0));
            expect(INVALID_SNAPSHOT, () -> new InventoryQuantity(5, 2, 3, 9).verifyLocationTotals(5, 2));
        });
        check("earliest expiry first independent of insertion and batch ID", () -> {
            List<Allocation> result = allocate(12, candidate(1, 2, 8, 0, 30), candidate(2, 1, 10, 0, 10));
            allocation(result.get(0), 2, 1, 10);
            allocation(result.get(1), 1, 2, 2);
        });
        check("same expiry sorts batch then location", () -> {
            List<Allocation> result = allocate(5, candidate(2, 1, 2, 0, 10),
                    candidate(1, 3, 2, 0, 10), candidate(1, 2, 2, 0, 10));
            allocation(result.get(0), 1, 2, 2);
            allocation(result.get(1), 1, 3, 2);
            allocation(result.get(2), 2, 1, 1);
        });
        check("allocation excludes frozen quantity", () -> {
            allocation(allocate(2, candidate(1, 1, 5, 4, 10), candidate(2, 2, 2, 0, 20)).get(1), 2, 2, 1);
            expect(INVENTORY_NOT_ENOUGH, () -> allocate(2, candidate(1, 1, 5, 4, 10)));
        });
        check("expired and non-saleable candidates excluded", () -> {
            Candidate stopped = new Candidate(1, 1, 1, 1, 2, 2, TODAY.plusDays(1), 100, 0, false);
            List<Allocation> result = allocate(1, candidate(1, 1, 100, 0, -1), stopped,
                    candidate(3, 3, 1, 0, 10));
            allocation(result.get(0), 3, 3, 1);
        });
        check("Shanghai midnight and explicit expiry-day policy", () -> {
            Candidate today = candidate(1, 1, 2, 0, 0);
            equal(2, allocate(2, today).get(0).getQuantity());
            FefoAllocator strict = new FefoAllocator(CLOCK, BLOCK_ON_EXPIRY_DATE);
            expect(INVENTORY_NOT_ENOUGH, () -> strict.allocate(1, 1, 1,
                    Collections.singleton(1L), 1, Collections.singletonList(today)));
            expect(INVENTORY_NOT_ENOUGH, () -> allocate(1, candidate(1, 1, 1, 0, -1)));
        });
        check("cross-tenant store drug and warehouse rejected", () -> {
            expect(INVENTORY_SCOPE_DENIED, () -> allocate(1, new Candidate(2, 1, 1, 1, 1, 1, TODAY, 1, 0, true)));
            expect(INVENTORY_SCOPE_DENIED, () -> allocate(1, new Candidate(1, 2, 1, 1, 1, 1, TODAY, 1, 0, true)));
            expect(INVENTORY_SCOPE_DENIED, () -> allocate(1, new Candidate(1, 1, 2, 1, 1, 1, TODAY, 1, 0, true)));
            expect(INVENTORY_SCOPE_DENIED, () -> allocate(1, new Candidate(1, 1, 1, 2, 1, 1, TODAY, 1, 0, true)));
        });
        check("duplicate candidates cannot inflate stock", () -> {
            Candidate first = candidate(1, 1, 2, 0, 1);
            expect(INVALID_SNAPSHOT, () -> allocate(3, first, first));
        });
        check("contradictory batch metadata rejected", () -> {
            expect(INVALID_SNAPSHOT, () -> allocate(1, candidate(1, 1, 2, 0, 1), candidate(1, 2, 2, 0, 2)));
        });
        check("empty null and invalid requests rejected", () -> {
            expect(INVENTORY_NOT_ENOUGH, () -> allocate(1));
            expect(INVALID_QUANTITY, () -> allocate(0));
            expect(INVALID_SNAPSHOT, () -> allocate(1, (Candidate) null));
            expect(INVENTORY_SCOPE_DENIED, () -> FEFO.allocate(1, 1, 1,
                    Collections.emptySet(), 1, Collections.emptyList()));
            expect(INVALID_SNAPSHOT, () -> candidate(1, 1, 1, 2, 0));
        });
        check("large aggregate does not overflow", () -> {
            List<Allocation> result = allocate(Integer.MAX_VALUE,
                    candidate(1, 1, Integer.MAX_VALUE - 1, 0, 1),
                    candidate(2, 2, Integer.MAX_VALUE, 0, 2));
            equal(1, result.get(1).getQuantity());
        });
        check("result immutable and input order untouched", () -> {
            Candidate late = candidate(2, 2, 2, 0, 2);
            List<Candidate> input = new ArrayList<>(Arrays.asList(late, candidate(1, 1, 2, 0, 1)));
            List<Allocation> result = FEFO.allocate(1, 1, 1, Collections.singleton(1L), 1, input);
            if (input.get(0) != late) { throw new AssertionError("input was reordered"); }
            try {
                result.clear();
                throw new AssertionError("mutable allocation result");
            } catch (UnsupportedOperationException expected) {
                // Required immutable result.
            }
        });
        check("500 seeded allocation and round-trip cases", () -> {
            Random random = new Random(20260910L);
            for (int iteration = 0; iteration < 500; iteration++) {
                int early = random.nextInt(1000) + 1;
                int late = random.nextInt(1000) + 1;
                int frozen = random.nextInt(100);
                int request = random.nextInt(early + late) + 1;
                List<Allocation> result = allocate(request, candidate(2, 2, late, 0, 2),
                        candidate(1, 1, early + frozen, frozen, 1));
                equal(Math.min(early, request), result.get(0).getQuantity());
                equal(request, result.stream().mapToLong(Allocation::getQuantity).sum());
                InventoryQuantity initial = new InventoryQuantity(early + late + frozen, early + late, frozen, 0);
                InventoryQuantity restored = initial.sell(request).returnSale(request, request, 0);
                equal(initial.getTotal(), restored.getTotal());
                equal(frozen, restored.getFrozen());
                equal(0, restored.getSold());
            }
        });
        check("explicit allocation preserves chosen batch instead of FEFO substitution", () -> {
            List<Allocation> result = explicit(2, Arrays.asList(candidate(1, 1, 10, 0, 1),
                    candidate(2, 2, 10, 0, 10)), new Selection(2, 2, 2));
            equal(1, result.size());
            allocation(result.get(0), 2, 2, 2);
        });
        check("explicit duplicates aggregate before availability check", () -> {
            List<Candidate> stock = Collections.singletonList(candidate(1, 1, 5, 1, 1));
            List<Allocation> result = explicit(4, stock, new Selection(1, 1, 2), new Selection(1, 1, 2));
            equal(1, result.size());
            allocation(result.get(0), 1, 1, 4);
            expect(INVENTORY_NOT_ENOUGH, () -> explicit(5, stock,
                    new Selection(1, 1, 2), new Selection(1, 1, 3)));
        });
        check("explicit invalid totals and overflowing sums rejected", () -> {
            List<Candidate> stock = Collections.singletonList(candidate(1, 1, 5, 0, 1));
            expect(INVALID_ALLOCATION, () -> explicit(3, stock, new Selection(1, 1, 2)));
            expect(INVALID_ALLOCATION, () -> explicit(1, stock, new Selection(1, 1, 2)));
            expect(INVALID_ALLOCATION, () -> explicit(Integer.MAX_VALUE, stock,
                    new Selection(1, 1, Integer.MAX_VALUE), new Selection(1, 1, 1)));
        });
        check("explicit stale selection cannot switch to available batch", () -> {
            List<Candidate> stock = Arrays.asList(candidate(1, 1, 1, 1, 1), candidate(2, 2, 10, 0, 2));
            expect(INVENTORY_NOT_ENOUGH, () -> explicit(1, stock, new Selection(1, 1, 1)));
            expect(INVENTORY_NOT_ENOUGH, () -> explicit(1, stock, new Selection(9, 9, 1)));
        });
        check("explicit expired and stopped selections rejected", () -> {
            List<Candidate> stock = Arrays.asList(candidate(1, 1, 5, 0, -1),
                    new Candidate(1, 1, 1, 1, 2, 2, TODAY.plusDays(1), 5, 0, false),
                    candidate(3, 3, 10, 0, 2));
            expect(INVENTORY_NOT_ENOUGH, () -> explicit(1, stock, new Selection(1, 1, 1)));
            expect(INVENTORY_NOT_ENOUGH, () -> explicit(1, stock, new Selection(2, 2, 1)));
        });
        check("explicit scope and malformed input rejected", () -> {
            List<Candidate> stock = Collections.singletonList(candidate(1, 1, 5, 0, 1));
            expect(INVALID_ALLOCATION, () -> explicit(1, stock));
            expect(INVALID_ALLOCATION, () -> explicit(1, stock, (Selection) null));
            expect(INVALID_ALLOCATION, () -> new Selection(0, 1, 1));
            expect(INVALID_QUANTITY, () -> new Selection(1, 1, 0));
            expect(INVENTORY_SCOPE_DENIED, () -> explicit(1,
                    Collections.singletonList(new Candidate(1, 2, 1, 1, 1, 1, TODAY, 1, 0, true)),
                    new Selection(1, 1, 1)));
        });
        check("explicit output deterministic and immutable", () -> {
            List<Candidate> stock = Arrays.asList(candidate(2, 2, 5, 0, 1), candidate(1, 1, 5, 0, 2));
            List<Allocation> result = explicit(4, stock, new Selection(2, 2, 2), new Selection(1, 1, 2));
            allocation(result.get(0), 1, 1, 2);
            allocation(result.get(1), 2, 2, 2);
            try {
                result.clear();
                throw new AssertionError("mutable explicit result");
            } catch (UnsupportedOperationException expected) {
                // No partial or mutable result can escape.
            }
        });
        check("same location cannot belong to two warehouses", () -> {
            List<Candidate> stock = Arrays.asList(candidate(1, 1, 5, 0, 1),
                    new Candidate(1, 1, 2, 1, 2, 1, TODAY, 5, 0, true));
            expect(INVALID_SNAPSHOT, () -> FEFO.allocate(1, 1, 1,
                    new HashSet<>(Arrays.asList(1L, 2L)), 1, stock));
        });
        check("capacity counts other batches and frozen units", () -> {
            LocationQuantity stock = new LocationQuantity(5, 4);
            expect(LOCATION_CAPACITY_EXCEEDED, () -> stock.receive(2, 9, 10L));
            LocationQuantity received = stock.receive(1, 9, 10L);
            equal(6, received.getQuantity());
            equal(4, received.getFrozen());
            equal(5, stock.getQuantity());
            expect(INVENTORY_NOT_ENOUGH, () -> stock.sell(2));
            equal(4, stock.sell(1).getQuantity());
        });
        check("capacity null zero and unsigned INT boundary", () -> {
            LocationQuantity empty = new LocationQuantity(0, 0);
            equal(1, empty.receive(1, 4294967295L, null).getQuantity());
            equal(1, empty.receive(1, 4294967294L, 4294967295L).getQuantity());
            expect(LOCATION_CAPACITY_EXCEEDED, () -> empty.receive(1, 4294967295L, 4294967295L));
            expect(LOCATION_CAPACITY_EXCEEDED, () -> empty.receive(1, 0, 0L));
            expect(INVALID_SNAPSHOT, () -> empty.receive(1, 0, 4294967296L));
            expect(INVALID_SNAPSHOT, () -> empty.receive(1, 0, -1L));
        });
        check("location aggregate corruption and overflow rejected", () -> {
            expect(INVALID_SNAPSHOT, () -> new LocationQuantity(5, 1).receive(1, 4, null));
            expect(QUANTITY_OVERFLOW, () -> new LocationQuantity(0, 0).receive(1, Long.MAX_VALUE, null));
            expect(QUANTITY_OVERFLOW, () -> new LocationQuantity(Integer.MAX_VALUE, 0)
                    .receive(1, Integer.MAX_VALUE, null));
            expect(INVALID_QUANTITY, () -> new LocationQuantity(1, 0).sell(0));
            expect(INVALID_QUANTITY, () -> new LocationQuantity(1, 0).receive(-1, 1, null));
        });
        check("batch and location receive-sale-return agree", () -> {
            InventoryQuantity batch = new InventoryQuantity(8, 6, 2, 0);
            LocationQuantity first = new LocationQuantity(5, 2);
            LocationQuantity second = new LocationQuantity(3, 0);
            batch = batch.receive(2);
            first = first.receive(2, 5, 10L);
            batch.verifyLocationTotals((long) first.getQuantity() + second.getQuantity(),
                    (long) first.getFrozen() + second.getFrozen());
            batch = batch.sell(6);
            first = first.sell(5);
            second = second.sell(1);
            batch.verifyLocationTotals((long) first.getQuantity() + second.getQuantity(), 2);
            batch = batch.returnSale(3, 5, 0);
            first = first.receive(3, first.getQuantity(), 10L);
            batch.verifyLocationTotals((long) first.getQuantity() + second.getQuantity(), 2);
            equal(3, batch.getSold());
        });
        check("multiple automatic lines share residual availability", () -> {
            List<PlannedLine> result = plan(Arrays.asList(candidate(1, 1, 5, 1, 1), candidate(2, 2, 4, 0, 2)),
                    RequestLine.automatic(20, 3), RequestLine.automatic(10, 3));
            equal(10, result.get(0).getSourceLineId());
            allocation(result.get(0).getAllocations().get(0), 1, 1, 3);
            allocation(result.get(1).getAllocations().get(0), 1, 1, 1);
            allocation(result.get(1).getAllocations().get(1), 2, 2, 2);
        });
        check("individually valid lines cannot jointly oversell", () -> {
            List<Candidate> stock = Collections.singletonList(candidate(1, 1, 5, 0, 1));
            expect(INVENTORY_NOT_ENOUGH, () -> plan(stock,
                    RequestLine.automatic(1, 4), RequestLine.automatic(2, 4)));
            expect(INVENTORY_NOT_ENOUGH, () -> plan(stock,
                    RequestLine.explicit(1, 4, Collections.singletonList(new Selection(1, 1, 4))),
                    RequestLine.explicit(2, 4, Collections.singletonList(new Selection(1, 1, 4)))));
            // A failed plan leaves the original snapshot intact; there is no partial mutation.
            equal(5, allocate(5, stock.get(0)).get(0).getQuantity());
        });
        check("mixed explicit and automatic lines share the same stock", () -> {
            List<PlannedLine> result = plan(Arrays.asList(candidate(1, 1, 3, 0, 1), candidate(2, 2, 3, 0, 2)),
                    RequestLine.explicit(1, 2, Collections.singletonList(new Selection(1, 1, 2))),
                    RequestLine.automatic(2, 3));
            allocation(result.get(1).getAllocations().get(0), 1, 1, 1);
            allocation(result.get(1).getAllocations().get(1), 2, 2, 2);
        });
        check("operation source line identity required and unique", () -> {
            List<Candidate> stock = Collections.singletonList(candidate(1, 1, 5, 0, 1));
            expect(INVALID_ALLOCATION, () -> plan(stock, RequestLine.automatic(1, 1), RequestLine.automatic(1, 1)));
            expect(INVALID_ALLOCATION, () -> plan(stock, (RequestLine) null));
            expect(INVALID_ALLOCATION, () -> plan(stock));
            expect(INVALID_ALLOCATION, () -> RequestLine.automatic(0, 1));
            expect(INVALID_ALLOCATION, () -> RequestLine.explicit(1, 1, null));
        });
        check("line selections defensively copied", () -> {
            List<Selection> mutable = new ArrayList<>(Collections.singletonList(new Selection(1, 1, 2)));
            RequestLine request = RequestLine.explicit(1, 2, mutable);
            mutable.clear();
            allocation(plan(Collections.singletonList(candidate(1, 1, 5, 0, 1)), request)
                    .get(0).getAllocations().get(0), 1, 1, 2);
        });
        check("operation captures one business date across midnight", () -> {
            int[] reads = {0};
            Clock moving = new Clock() {
                @Override public ZoneId getZone() { return ZoneId.of("Asia/Shanghai"); }
                @Override public Clock withZone(ZoneId zone) { return this; }
                @Override public Instant instant() {
                    return reads[0]++ == 0 ? Instant.parse("2026-09-10T15:59:59Z")
                            : Instant.parse("2026-09-10T16:00:00Z");
                }
            };
            FefoAllocator allocator = new FefoAllocator(moving, ALLOW_ON_EXPIRY_DATE);
            List<PlannedLine> result = allocator.allocateLines(1, 1, 1, Collections.singleton(1L),
                    Collections.singletonList(candidate(1, 1, 2, 0, 0)),
                    Arrays.asList(RequestLine.automatic(1, 1), RequestLine.automatic(2, 1)));
            equal(2, result.size());
            equal(1, reads[0]);
        });
        check("return restores original allocation and exact cost", () -> {
            List<ReturnAllocation> result = returns(Collections.singletonList(issue(1, 5, 2)), new ReturnLine(10, 1, 3));
            ReturnAllocation allocation = result.get(0);
            equal(10, allocation.getSourceLineId());
            equal(1, allocation.getOriginalFlowId());
            equal(7, allocation.getDrugId());
            equal(8, allocation.getBatchId());
            equal(9, allocation.getLocationId());
            equal(3, allocation.getQuantity());
            if (!new BigDecimal("1.2345").equals(allocation.getUnitCost())) {
                throw new AssertionError("original four-place cost lost");
            }
            try {
                result.clear();
                throw new AssertionError("mutable return plan");
            } catch (UnsupportedOperationException expected) {
                // Plans are immutable.
            }
        });
        check("return lines share remaining original issue quantity", () -> {
            List<OriginalIssue> originals = Collections.singletonList(issue(1, 5, 2));
            List<ReturnAllocation> result = returns(originals, new ReturnLine(20, 1, 1), new ReturnLine(10, 1, 2));
            equal(10, result.get(0).getSourceLineId());
            equal(3, result.stream().mapToLong(ReturnAllocation::getQuantity).sum());
            expect(RETURN_EXCEEDS_ISSUED, () -> returns(originals, new ReturnLine(10, 1, 2), new ReturnLine(20, 1, 2)));
            equal(3, returns(originals, new ReturnLine(10, 1, 3)).get(0).getQuantity());
        });
        check("return rejects missing duplicate or foreign original flow", () -> {
            expect(INVALID_ORIGINAL_ISSUE, () -> returns(Collections.emptyList(), new ReturnLine(1, 1, 1)));
            OriginalIssue original = issue(1, 5, 0);
            expect(INVALID_ORIGINAL_ISSUE, () -> returns(Arrays.asList(original, original), new ReturnLine(1, 1, 1)));
            OriginalIssue foreign = new OriginalIssue(2, 1, 1, 7, 8, 9, 20, 0, 5, 0, BigDecimal.ONE);
            expect(INVENTORY_SCOPE_DENIED, () -> returns(Collections.singletonList(foreign), new ReturnLine(1, 1, 1)));
            OriginalIssue otherStore = new OriginalIssue(1, 2, 1, 7, 8, 9, 20, 0, 5, 0, BigDecimal.ONE);
            expect(INVENTORY_SCOPE_DENIED, () -> returns(Collections.singletonList(otherStore), new ReturnLine(1, 1, 1)));
        });
        check("return requires a valid sales outflow", () -> {
            expect(INVALID_ORIGINAL_ISSUE, () -> new OriginalIssue(1, 1, 1, 7, 8, 9, 10, 0, 5, 0, BigDecimal.ONE));
            expect(INVALID_ORIGINAL_ISSUE, () -> new OriginalIssue(1, 1, 1, 7, 8, 9, 20, 1, 5, 0, BigDecimal.ONE));
            expect(INVALID_ORIGINAL_ISSUE, () -> issue(1, 5, 6));
            expect(INVALID_ORIGINAL_ISSUE, () -> issue(1, 5, -1));
            OriginalIssue consumed = new OriginalIssue(1, 1, 1, 7, 8, 9, 82, 0, 5, 0, BigDecimal.ONE);
            equal(1, returns(Collections.singletonList(consumed), new ReturnLine(1, 1, 1)).size());
        });
        check("return cost must fit database precision without rounding", () -> {
            expect(INVALID_ORIGINAL_ISSUE, () -> new OriginalIssue(1, 1, 1, 7, 8, 9, 20, 0, 5, 0, new BigDecimal("0.00001")));
            expect(INVALID_ORIGINAL_ISSUE, () -> new OriginalIssue(1, 1, 1, 7, 8, 9, 20, 0, 5, 0, new BigDecimal("100000000000000")));
            expect(INVALID_ORIGINAL_ISSUE, () -> new OriginalIssue(1, 1, 1, 7, 8, 9, 20, 0, 5, 0, new BigDecimal("-1")));
            expect(INVALID_ORIGINAL_ISSUE, () -> new OriginalIssue(1, 1, 1, 7, 8, 9, 20, 0, 5, 0, null));
        });
        check("return duplicate source-flow pair rejected", () -> {
            expect(INVALID_ALLOCATION, () -> returns(Collections.singletonList(issue(1, 5, 0)),
                    new ReturnLine(10, 1, 1), new ReturnLine(10, 1, 1)));
            expect(INVALID_ALLOCATION, () -> new ReturnLine(0, 1, 1));
            expect(INVALID_QUANTITY, () -> new ReturnLine(1, 1, 0));
        });
        check("return preserves flow uniqueness for same source and destination", () -> {
            List<OriginalIssue> originals = Arrays.asList(issue(1, 5, 0), issue(2, 5, 0));
            expect(INVALID_ALLOCATION, () -> returns(originals, new ReturnLine(10, 1, 1), new ReturnLine(10, 2, 1)));
            equal(2, returns(originals, new ReturnLine(10, 1, 1), new ReturnLine(20, 2, 1)).size());
        });
        check("return source line may span original locations but not drugs", () -> {
            OriginalIssue otherLocation = new OriginalIssue(1, 1, 2, 7, 8, 10, 20, 0, 5, 0, BigDecimal.ONE);
            equal(2, returns(Arrays.asList(issue(1, 5, 0), otherLocation),
                    new ReturnLine(10, 1, 1), new ReturnLine(10, 2, 1)).size());
            OriginalIssue otherDrug = new OriginalIssue(1, 1, 2, 10, 11, 12, 20, 0, 5, 0, BigDecimal.ONE);
            expect(INVALID_ALLOCATION, () -> returns(Arrays.asList(issue(1, 5, 0), otherDrug),
                    new ReturnLine(10, 1, 1), new ReturnLine(10, 2, 1)));
        });
        check("return source quantity overflow rejected", () -> {
            OriginalIssue other = new OriginalIssue(1, 1, 2, 7, 8, 10, 20, 0, Integer.MAX_VALUE, 0, BigDecimal.ONE);
            expect(QUANTITY_OVERFLOW, () -> returns(Arrays.asList(issue(1, Integer.MAX_VALUE, 0), other),
                    new ReturnLine(10, 1, Integer.MAX_VALUE), new ReturnLine(10, 2, 1)));
        });
        check("receipt batch metadata can be reused without replacement", () -> {
            ReceiptBatchMetadata existing = new ReceiptBatchMetadata(TODAY.minusDays(10), TODAY.plusDays(100));
            existing.verifyReuse(new ReceiptBatchMetadata(TODAY.minusDays(10), TODAY.plusDays(100)));
            expect(BATCH_METADATA_CONFLICT, () -> existing.verifyReuse(
                    new ReceiptBatchMetadata(TODAY.minusDays(9), TODAY.plusDays(100))));
            expect(BATCH_METADATA_CONFLICT, () -> existing.verifyReuse(
                    new ReceiptBatchMetadata(TODAY.minusDays(10), TODAY.plusDays(101))));
        });
        check("unknown manufacture date is not a wildcard", () -> {
            ReceiptBatchMetadata unknown = new ReceiptBatchMetadata(null, TODAY);
            unknown.verifyReuse(new ReceiptBatchMetadata(null, TODAY));
            expect(BATCH_METADATA_CONFLICT, () -> unknown.verifyReuse(new ReceiptBatchMetadata(TODAY.minusDays(1), TODAY)));
            expect(BATCH_METADATA_CONFLICT, () -> new ReceiptBatchMetadata(TODAY.minusDays(1), TODAY).verifyReuse(unknown));
        });
        check("invalid receipt dates rejected", () -> {
            expect(BATCH_METADATA_CONFLICT, () -> new ReceiptBatchMetadata(TODAY, null));
            expect(BATCH_METADATA_CONFLICT, () -> new ReceiptBatchMetadata(TODAY.plusDays(1), TODAY));
            expect(BATCH_METADATA_CONFLICT, () -> new ReceiptBatchMetadata(null, TODAY).verifyReuse(null));
        });
        System.out.println("PASS " + passed + " rule checks (including 500 seeded cases); MySQL tests: 0");
    }

    private static Candidate candidate(long batch, long location, int quantity, int frozen, int days) {
        return new Candidate(1, 1, 1, 1, batch, location, TODAY.plusDays(days), quantity, frozen, true);
    }

    private static OriginalIssue issue(long flowId, int issued, long returned) {
        return new OriginalIssue(1, 1, flowId, 7, 8, 9, 20, 0, issued, returned, new BigDecimal("1.2345"));
    }

    private static List<ReturnAllocation> returns(List<OriginalIssue> originals, ReturnLine... lines) {
        return new SaleReturnPlanner().plan(1, 1, originals, Arrays.asList(lines));
    }

    private static List<Allocation> allocate(int quantity, Candidate... candidates) {
        return FEFO.allocate(1, 1, 1, Collections.singleton(1L), quantity, Arrays.asList(candidates));
    }

    private static List<Allocation> explicit(int quantity, List<Candidate> stock, Selection... selections) {
        return FEFO.validateExplicit(1, 1, 1, Collections.singleton(1L), quantity, stock, Arrays.asList(selections));
    }

    private static List<PlannedLine> plan(List<Candidate> stock, RequestLine... lines) {
        return FEFO.allocateLines(1, 1, 1, Collections.singleton(1L), stock, Arrays.asList(lines));
    }

    private static void allocation(Allocation actual, long batch, long location, int quantity) {
        equal(batch, actual.getBatchId());
        equal(location, actual.getLocationId());
        equal(quantity, actual.getQuantity());
    }

    private static void equal(long expected, long actual) {
        if (expected != actual) { throw new AssertionError("expected " + expected + ", got " + actual); }
    }

    private static void expect(InventoryRuleException.Reason expected, Runnable action) {
        try {
            action.run();
        } catch (InventoryRuleException exception) {
            if (exception.getReason() != expected) { throw new AssertionError(exception); }
            return;
        }
        throw new AssertionError("expected " + expected);
    }

    private static void check(String name, Runnable action) {
        action.run();
        passed++;
        System.out.println("PASS " + name);
    }
}
