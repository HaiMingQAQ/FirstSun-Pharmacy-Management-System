-- C inventory facade repair (ACC-20260915-001)
-- Run once after the existing 20260914 inventory migrations.
-- A return flow points to the exact original sales-out flow, allowing a locked original flow
-- to serialize cumulative return validation without guessing a batch or location.
DROP PROCEDURE IF EXISTS add_c_inventory_facade_flow_ref;

DELIMITER $$
CREATE PROCEDURE add_c_inventory_facade_flow_ref()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ph_inv_flow'
          AND COLUMN_NAME = 'original_flow_id'
    ) THEN
        ALTER TABLE ph_inv_flow
            ADD COLUMN original_flow_id BIGINT NULL COMMENT '销售退货关联的原销售出库流水' AFTER biz_line_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ph_inv_flow'
          AND INDEX_NAME = 'idx_inv_flow_original'
    ) THEN
        ALTER TABLE ph_inv_flow
            ADD KEY idx_inv_flow_original (tenant_id, store_id, original_flow_id);
    END IF;
END $$
DELIMITER ;

CALL add_c_inventory_facade_flow_ref();
DROP PROCEDURE add_c_inventory_facade_flow_ref;
