-- F 会员模块：线上订单（ph_wx_order）积分字段
-- 背景：接通积分赠送 / 抵扣 / 退货回退后，线上订单同样需要记录
--       「本单抵扣了多少积分、抵扣了多少钱、本单赠送了多少积分」，
--       否则无法与 member_point_record 流水对账，也无法在退款 / 取消时按比例返还。
-- 做法：为 ph_wx_order 增加 3 个积分字段，全部幂等（已存在则跳过），可重复执行。
-- 说明：本迁移只改 F 自有表 ph_wx_order，不触碰 A/B/C/D/E 的表结构与数据。
SET NAMES utf8mb4;

-- 1) 抵扣使用的积分
SET @has_col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ph_wx_order' AND COLUMN_NAME = 'point_deduct'
);
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `ph_wx_order` ADD COLUMN `point_deduct` INT NOT NULL DEFAULT 0 COMMENT ''积分抵扣使用的积分'' AFTER `discount_amount`',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 积分抵扣金额（元）
SET @has_col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ph_wx_order' AND COLUMN_NAME = 'point_deduct_amount'
);
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `ph_wx_order` ADD COLUMN `point_deduct_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT ''积分抵扣金额'' AFTER `point_deduct`',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 赠送积分（完成 / 核销后由 F 的积分结算服务写入）
SET @has_col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ph_wx_order' AND COLUMN_NAME = 'point_earned'
);
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `ph_wx_order` ADD COLUMN `point_earned` INT NOT NULL DEFAULT 0 COMMENT ''赠送积分'' AFTER `point_deduct_amount`',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) 金额约束：应付 / 抵扣金额不得为负，防止历史脏数据与新增字段冲突
SET @has_ck := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'ph_wx_order'
    AND CONSTRAINT_NAME = 'ck_wx_order_point'
);
SET @ddl := IF(@has_ck = 0,
  'ALTER TABLE `ph_wx_order` ADD CONSTRAINT `ck_wx_order_point` CHECK (`point_deduct` >= 0 AND `point_deduct_amount` >= 0 AND `point_earned` >= 0)',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
