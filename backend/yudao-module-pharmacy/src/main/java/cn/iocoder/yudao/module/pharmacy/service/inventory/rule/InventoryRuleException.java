package cn.iocoder.yudao.module.pharmacy.service.inventory.rule;

/** Internal rule failure. The application adapter must map this to A-approved error codes. */
public final class InventoryRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Reason {
        INVALID_QUANTITY, INVALID_SNAPSHOT, QUANTITY_OVERFLOW, INVENTORY_NOT_ENOUGH,
        RETURN_EXCEEDS_ISSUED, INVENTORY_SCOPE_DENIED, INVALID_ALLOCATION,
        LOCATION_CAPACITY_EXCEEDED, INVALID_ORIGINAL_ISSUE, BATCH_METADATA_CONFLICT
    }

    private final Reason reason;

    public InventoryRuleException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
