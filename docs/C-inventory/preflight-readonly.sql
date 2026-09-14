-- Review aid only. NOT executed. Use only after confirming the independent development schema.
-- No USE, DDL or DML. These checks are migration diagnostics, not the reconciliation service.
SELECT DATABASE() AS selected_schema, VERSION() AS mysql_version;

SELECT table_name, engine
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND (table_name LIKE 'ph\_inv\_%' OR table_name IN ('ph_warehouse', 'ph_location'))
ORDER BY table_name;

-- Default warehouse conflicts must be resolved by the owner before adding a unique constraint.
SELECT tenant_id, store_id, COUNT(*) AS active_default_count
FROM ph_warehouse
WHERE deleted = b'0' AND status = 1 AND is_default = 1
GROUP BY tenant_id, store_id
HAVING COUNT(*) > 1;

-- Batch/location totals; missing locations with nonzero batch balance are also reported.
SELECT b.tenant_id, b.store_id, b.id AS batch_id,
       b.qty_total, b.qty_avail, b.qty_frozen,
       COALESCE(s.qty, 0) AS location_qty, COALESCE(s.frozen, 0) AS location_frozen
FROM ph_inv_batch b
LEFT JOIN (
    SELECT tenant_id, batch_id, SUM(qty) AS qty, SUM(qty_frozen) AS frozen
    FROM ph_inv_location_stock
    WHERE deleted = b'0'
    GROUP BY tenant_id, batch_id
) s ON s.tenant_id = b.tenant_id AND s.batch_id = b.id
WHERE b.deleted = b'0'
  AND (b.qty_total <> b.qty_avail + b.qty_frozen
    OR b.qty_total <> COALESCE(s.qty, 0)
    OR b.qty_frozen <> COALESCE(s.frozen, 0));

-- Orphaned / cross-warehouse / redundant-drug mismatches omitted by a simple aggregate.
SELECT s.tenant_id, s.id AS location_stock_id, s.batch_id, s.location_id
FROM ph_inv_location_stock s
LEFT JOIN ph_inv_batch b
  ON b.id = s.batch_id AND b.tenant_id = s.tenant_id AND b.deleted = b'0'
LEFT JOIN ph_location l
  ON l.id = s.location_id AND l.tenant_id = s.tenant_id AND l.deleted = b'0'
WHERE s.deleted = b'0'
  AND (b.id IS NULL OR l.id IS NULL OR b.warehouse_id <> l.warehouse_id OR b.drug_id <> s.drug_id);

-- Existing trace columns, if any: do not rerun a partially applied migration blindly.
SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'ph_inv_flow' AND column_name IN ('operation_id', 'original_flow_id'))
    OR table_name = 'ph_inv_operation')
ORDER BY table_name, ordinal_position;
