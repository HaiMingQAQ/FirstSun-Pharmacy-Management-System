package cn.iocoder.yudao.module.pharmacy.service.inventory.rule;

import static cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryRuleException.Reason.*;

/**
 * Immutable quantities for one ph_inv_location_stock row. Capacity applies to the whole location,
 * across batches and drugs, including frozen units. The application must read that aggregate under
 * the warehouse write lock, authorize the location and persist batch + location + flow atomically.
 */
public final class LocationQuantity {
    private static final long MAX_UNSIGNED_INT = 4294967295L;
    private final int quantity;
    private final int frozen;

    public LocationQuantity(int quantity, int frozen) {
        InventoryQuantity.locationAvailable(quantity, frozen);
        this.quantity = quantity;
        this.frozen = frozen;
    }

    /** Null capacity means no configured limit; a zero capacity is an actual zero limit. */
    public LocationQuantity receive(int incoming, long occupiedAcrossLocation, Long maximumCapacity) {
        InventoryQuantity.requirePositive(incoming);
        if (occupiedAcrossLocation < quantity || (maximumCapacity != null
                && (maximumCapacity < 0 || maximumCapacity > MAX_UNSIGNED_INT))) {
            throw new InventoryRuleException(INVALID_SNAPSHOT);
        }
        if (occupiedAcrossLocation > Long.MAX_VALUE - incoming
                || (long) quantity + incoming > Integer.MAX_VALUE) {
            throw new InventoryRuleException(QUANTITY_OVERFLOW);
        }
        if (maximumCapacity != null && occupiedAcrossLocation + incoming > maximumCapacity) {
            throw new InventoryRuleException(LOCATION_CAPACITY_EXCEEDED);
        }
        return new LocationQuantity(quantity + incoming, frozen);
    }

    /** Normal sale only uses available units. Frozen transfer is intentionally not exposed (P2). */
    public LocationQuantity sell(int outgoing) {
        InventoryQuantity.requirePositive(outgoing);
        if (outgoing > getAvailable()) {
            throw new InventoryRuleException(INVENTORY_NOT_ENOUGH);
        }
        return new LocationQuantity(quantity - outgoing, frozen);
    }

    public int getQuantity() { return quantity; }
    public int getFrozen() { return frozen; }
    public int getAvailable() { return quantity - frozen; }
}
