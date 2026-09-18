-- B-2 采购订单驳回：补入驳回原因/驳回人/驳回时间，并注册「已驳回」字典项
--
-- 背景：采购订单原本只有「提交 → 审批」单一路径，缺少可用的驳回操作。
--   驳回需要把原因落库（审计要求），并记录驳回人与驳回时间。
--
-- 状态取值：已驳回 = 6（见 PurchaseOrderStatusEnum.REJECTED）
--   未选用 -2 之外的其它取值，是为了不与历史状态（-1/0/1/2/3/4/5）冲突。
--
-- 幂等：加列与字典项均先判断是否存在，重复执行不报错、不重复插入。
SET NAMES utf8mb4;

-- 1) ph_po_order 加三列（若不存在）
SET @has_reason := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ph_po_order' AND COLUMN_NAME = 'reject_reason'
);
SET @sql_reason := IF(@has_reason = 0,
  'ALTER TABLE `ph_po_order` ADD COLUMN `reject_reason` VARCHAR(500) NULL COMMENT ''驳回原因（仅已提交→已驳回时写入）'' AFTER `audit_at`',
  'DO 0');
PREPARE stmt FROM @sql_reason; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_by := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ph_po_order' AND COLUMN_NAME = 'reject_by'
);
SET @sql_by := IF(@has_by = 0,
  'ALTER TABLE `ph_po_order` ADD COLUMN `reject_by` BIGINT NULL COMMENT ''驳回人员工编号'' AFTER `reject_reason`',
  'DO 0');
PREPARE stmt FROM @sql_by; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_at := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ph_po_order' AND COLUMN_NAME = 'reject_at'
);
SET @sql_at := IF(@has_at = 0,
  'ALTER TABLE `ph_po_order` ADD COLUMN `reject_at` DATETIME NULL COMMENT ''驳回时间'' AFTER `reject_by`',
  'DO 0');
PREPARE stmt FROM @sql_at; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 放开状态检查约束：原约束为 status IN (-1,0,1,2,3,4,5)，不含「已驳回(6)」
--    不加这一步，驳回会直接撞 Check constraint 'ck_po_order_status' is violated。
--    幂等：仅当约束存在且尚未包含 6 时才重建。
SET @ck_clause := (
  SELECT CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'ck_po_order_status'
);
SET @need_fix := IF(@ck_clause IS NOT NULL AND @ck_clause NOT LIKE '%6%', 1, 0);
SET @sql_ck := IF(@need_fix = 1,
  'ALTER TABLE `ph_po_order` DROP CHECK `ck_po_order_status`',
  'DO 0');
PREPARE stmt FROM @sql_ck; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql_ck2 := IF(@need_fix = 1,
  'ALTER TABLE `ph_po_order` ADD CONSTRAINT `ck_po_order_status` CHECK (`status` IN (-1,0,1,2,3,4,5,6))',
  'DO 0');
PREPARE stmt FROM @sql_ck2; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 注册「已驳回」字典项（pharmacy_po_status，值 6）
--    先确保字典类型存在（本类型由 20260913_b_purchase_order_receipt_menu.sql 创建，这里兜底）
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT '采购订单状态', 'pharmacy_po_status', 0, 'B', NOW(), 'B', NOW(), b'0', NULL
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'pharmacy_po_status');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 8, '已驳回', '6', 'pharmacy_po_status', 0, 'danger', '', '采购订单被驳回，需修改后重新提交', 'B', NOW(), 'B', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_po_status' AND `value` = '6'
);
