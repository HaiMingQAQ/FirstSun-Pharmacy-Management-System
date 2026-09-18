-- F 线上订单库存分配表：补入「正式出库来源行号」（v3）
-- 背景：C 的 returnBack 需要 originalBizNo + originalBizLineId 精确指向「原出库流水」的业务单号与行号。
--   - 冻结转出库（consumeReservation，流水 82）为保证同一明细拆多批次时行号唯一，用的是「分配记录编号」；
--   - 未冻结订单的兼容路径（deduct，流水 20）用的是「订单明细编号」。
--   两种路径的行号不同，回补时无法从订单明细反推，因此显式记录本次正式出库所用的行号。
-- 幂等：加列用 information_schema 判断；回填用表注释 v3 标记判断，第二次执行不再改动数据。
SET NAMES utf8mb4;

-- 1) 加列（若不存在）
SET @has_col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ph_wx_order_line_alloc'
    AND COLUMN_NAME = 'out_biz_line_id'
);
SET @add_col := IF(@has_col = 0,
  'ALTER TABLE `ph_wx_order_line_alloc` ADD COLUMN `out_biz_line_id` BIGINT NULL COMMENT ''正式出库时的来源行号，回补时作为 originalBizLineId'' AFTER `order_no`',
  'DO 0');
PREPARE stmt FROM @add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 历史数据回填：直接从库存流水反查出库行号（20=直接扣库、82=冻结转出库），
--    取不到流水时退回订单明细编号，避免误标。
SET @already_v3 := (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ph_wx_order_line_alloc'
    AND TABLE_COMMENT LIKE '%v3%'
);
SET @backfill := IF(@already_v3 = 0,
  'UPDATE `ph_wx_order_line_alloc` a LEFT JOIN `ph_inv_flow` f ON f.`biz_no` = a.`order_no` AND f.`flow_type` IN (20, 82) AND f.`batch_id` = a.`batch_id` AND f.`location_id` = a.`location_id` SET a.`out_biz_line_id` = COALESCE(f.`biz_line_id`, a.`wx_order_line_id`) WHERE a.`status` >= 1 AND a.`out_biz_line_id` IS NULL',
  'DO 0');
PREPARE stmt FROM @backfill;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 打 v3 标记
SET @mark := IF(@already_v3 = 0,
  'ALTER TABLE `ph_wx_order_line_alloc` COMMENT = ''ph_wx_order_line_alloc v3: status 0已冻结/1已出库/2已释放或已回补, out_biz_line_id=正式出库来源行号''',
  'DO 0');
PREPARE stmt FROM @mark;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 说明：本迁移只改 F 自有表，不触碰 A/B/C/D/E 的表结构与数据。
