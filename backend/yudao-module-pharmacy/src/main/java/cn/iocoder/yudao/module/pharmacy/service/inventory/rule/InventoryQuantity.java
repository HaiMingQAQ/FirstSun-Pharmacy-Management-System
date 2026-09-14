package cn.iocoder.yudao.module.pharmacy.service.inventory.rule;

import static cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryRuleException.Reason.*;

/**
 * Immutable batch quantities, matching ph_inv_batch. No persistence or transaction semantics.
 * A caller must validate and lock the original flow before supplying cumulative return amounts.
 */
public final class InventoryQuantity {
    private final int total;
    private final int available;
    private final int frozen;
    private final int sold;

    public InventoryQuantity(int total, int available, int frozen, int sold) {
        if (total < 0 || available < 0 || frozen < 0 || sold < 0
                || (long) available + frozen != total) {
            throw new InventoryRuleException(INVALID_SNAPSHOT);
        }
        this.total = total;
        this.available = available;
        this.frozen = frozen;
        this.sold = sold;
    }

    public InventoryQuantity receive(int quantity) {
        requirePositive(quantity);
        return new InventoryQuantity(add(total, quantity), add(available, quantity), frozen, sold);
    }

    public InventoryQuantity sell(int quantity) {
        requirePositive(quantity);
        if (quantity > available) {
            throw new InventoryRuleException(INVENTORY_NOT_ENOUGH);
        }
        return new InventoryQuantity(total - quantity, available - quantity, frozen, add(sold, quantity));
    }

    public InventoryQuantity returnSale(int quantity, int originalIssued, long alreadyReturned) {
        requirePositive(quantity);
        if (originalIssued <= 0 || alreadyReturned < 0 || alreadyReturned > originalIssued) {
            throw new InventoryRuleException(INVALID_SNAPSHOT);
        }
        if (quantity > originalIssued - alreadyReturned) {
            throw new InventoryRuleException(RETURN_EXCEEDS_ISSUED);
        }
        if (quantity > sold) {
            throw new InventoryRuleException(INVALID_SNAPSHOT);
        }
        return new InventoryQuantity(add(total, quantity), add(available, quantity), frozen, sold - quantity);
    }

    /** Compare all live locations of one batch; sold is deliberately excluded. */
    public void verifyLocationTotals(long locationQuantity, long locationFrozen) {
        if (locationQuantity != total || locationFrozen != frozen) {
            throw new InventoryRuleException(INVALID_SNAPSHOT);
        }
    }

    public static int locationAvailable(int quantity, int frozen) {
        if (quantity < 0 || frozen < 0 || frozen > quantity) {
            throw new InventoryRuleException(INVALID_SNAPSHOT);
        }
        return quantity - frozen;
    }

    static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new InventoryRuleException(INVALID_QUANTITY);
        }
    }

    private static int add(int left, int right) {
        long result = (long) left + right;
        if (result > Integer.MAX_VALUE) {
            throw new InventoryRuleException(QUANTITY_OVERFLOW);
        }
        return (int) result;
    }

    public int getTotal() { return total; }
    public int getAvailable() { return available; }
    public int getFrozen() { return frozen; }
    public int getSold() { return sold; }
}
