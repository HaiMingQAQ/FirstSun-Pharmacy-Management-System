package cn.iocoder.yudao.module.pharmacy.service.inventory.rule;

import java.time.LocalDate;
import java.util.Objects;

import static cn.iocoder.yudao.module.pharmacy.service.inventory.rule.InventoryRuleException.Reason.*;

/**
 * Immutable production/expiry metadata for a receipt or existing batch. The service must locate the
 * existing row using the database unique key (tenant, warehouse, drug, batch_no), under its warehouse
 * lock. Java string equality must not emulate MySQL's case/accent-insensitive batch number collation.
 * Supplier changes, cost and expiry-day admission remain separate, unapproved integration policies.
 */
public final class ReceiptBatchMetadata {
    private final LocalDate manufactureDate;
    private final LocalDate expiryDate;

    public ReceiptBatchMetadata(LocalDate manufactureDate, LocalDate expiryDate) {
        if (expiryDate == null || (manufactureDate != null && manufactureDate.isAfter(expiryDate))) {
            throw new InventoryRuleException(BATCH_METADATA_CONFLICT);
        }
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
    }

    /** Null manufacture date is unknown, not a wildcard that permits replacing existing metadata. */
    public void verifyReuse(ReceiptBatchMetadata incoming) {
        if (incoming == null || !Objects.equals(manufactureDate, incoming.manufactureDate)
                || !expiryDate.equals(incoming.expiryDate)) {
            throw new InventoryRuleException(BATCH_METADATA_CONFLICT);
        }
    }
}
