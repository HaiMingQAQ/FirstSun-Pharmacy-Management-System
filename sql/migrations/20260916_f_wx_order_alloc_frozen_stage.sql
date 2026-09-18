-- F 线上订单库存分配表：补入「已冻结」阶段的语义（v2）
-- 背景：接入 C 的 reserve/release/consumeReservation 后，同一张分配表的生命周期变为
--       0 已冻结 → 1 已出库 → 2 已释放/已回补（v1 只有 0 已出库 / 1 已回补）。
-- 做法：把 v1 的历史状态整体 +1（0→1、1→2），再用表注释打上 v2 标记，保证可重复执行。
-- 幂等：靠表注释标记判断是否已迁移，第二次执行不再改动数据。
SET NAMES utf8mb4;

SET @already_v2 := (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ph_wx_order_line_alloc'
    AND TABLE_COMMENT LIKE '%v2%'
);

-- 1) v1 → v2 的历史数据迁移（只执行一次）
SET @migrate := IF(@already_v2 = 0,
  'UPDATE `ph_wx_order_line_alloc` SET `status` = `status` + 1',
  'DO 0');
PREPARE stmt FROM @migrate;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 表注释打 v2 标记，作为上面这步的幂等依据
SET @mark := IF(@already_v2 = 0,
  'ALTER TABLE `ph_wx_order_line_alloc` COMMENT = ''ph_wx_order_line_alloc v2: status 0已冻结/1已出库/2已释放或已回补''',
  'DO 0');
PREPARE stmt FROM @mark;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 冻结阶段需要按「订单 + 状态」批量清理超时订单的冻结量
SET @has_idx := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ph_wx_order_line_alloc'
    AND INDEX_NAME = 'idx_wx_alloc_status'
);
SET @add_idx := IF(@has_idx = 0,
  'ALTER TABLE `ph_wx_order_line_alloc` ADD KEY `idx_wx_alloc_status` (`tenant_id`,`status`,`wx_order_id`)',
  'DO 0');
PREPARE stmt FROM @add_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 说明：本迁移只改 F 自有表，不触碰 A/B/C/D/E 的表结构与数据。
