SET NAMES utf8mb4;

-- C-3: historical 20260915_c_inventory_facade_flow_ref.sql was executed
-- without an explicit connection character set. Reapply only its Chinese
-- column comment; this is safe for new and existing databases.
ALTER TABLE `ph_inv_flow`
  MODIFY COLUMN `original_flow_id` BIGINT NULL
  COMMENT '销售退货关联的原销售出库流水' AFTER `biz_line_id`;
