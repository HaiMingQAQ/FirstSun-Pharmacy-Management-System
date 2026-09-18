-- B 模块增量迁移：采购单号序列表（并发取号原子分配）
--
-- ============================ 升级说明 ============================
-- 执行顺序：本脚本应在既有采购域迁移之后执行
--   1) 20260912_b_purchase_supplier_menu.sql
--   2) 20260913_b_purchase_order_receipt_menu.sql
--   3) 20260917_b_purchase_order_reject.sql
--   4) 本脚本 20260917_b_purchase_doc_seq.sql
-- 依赖：无（仅新建一张 B 模块自有表，不改动任何既有表结构与数据）
-- 回滚：DROP TABLE ph_po_doc_seq;（会丢失当天已分配流水，回滚后单号将由 MAX 重新推算）
--
-- ============================ 背景与风险 ============================
-- 原取号方式为「SELECT MAX(单号) + 1」后插入，存在两个并发缺陷：
--   1) MySQL 默认隔离级别 REPEATABLE-READ 下，MAX() 是不加锁的一致性读，
--      同一事务内**重试时读到的快照恒定不变**，导致 5 次重试算出同一个号，
--      最终抛「单号已存在」；
--   2) 并发插入同一 receipt_no/order_no 会争抢 uk_receipt_no / uk_order_no 的索引锁，
--      产生死锁（DeadlockLoserDataAccessException），表现为 HTTP 500。
-- 实测：同门店同业务日并发创建 5 张收货单 → 仅 1 张成功，另 4 张为 2 个「单号已存在」+ 3 个 500。
--
-- 修复思路：用一张序列表把「取号」变成数据库层面的原子自增，
--   不再依赖 MAX 推算，因此与隔离级别和重试无关。
--   分配语句：INSERT ... ON DUPLICATE KEY UPDATE next_seq = LAST_INSERT_ID(next_seq + 1)
--   - 首次分配：插入 next_seq=1，LAST_INSERT_ID() 返回自增主键，故显式取 1；
--   - 后续分配：命中唯一键走 UPDATE，LAST_INSERT_ID(expr) 返回表达式结果（即分配到的号）。
--
-- 唯一约束：(store_id, biz_date, biz_type) 唯一 —— 保证每个「门店 + 业务日 + 单据类型」
--   拥有独立且唯一的流水序列。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS，重复执行不报错、不改数据。
-- 字符集：显式 utf8mb4，与库内既有表一致。
-- ===================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ph_po_doc_seq` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id`    BIGINT       NOT NULL COMMENT '门店编号',
  `biz_date`    DATE         NOT NULL COMMENT '业务日期（单号中的 yyyyMMdd）',
  `biz_type`    VARCHAR(16)  NOT NULL COMMENT '单据类型：ORDER=采购订单、RECEIPT=采购收货单',
  `next_seq`    INT          NOT NULL DEFAULT 0 COMMENT '已分配到的最大流水号',
  `creator`     VARCHAR(64)  NULL DEFAULT '' COMMENT '创建者(用户编号/系统标识)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     VARCHAR(64)  NULL DEFAULT '' COMMENT '更新者(用户编号/系统标识)',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除(逻辑删除)',
  `tenant_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_seq` (`store_id`, `biz_date`, `biz_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '采购单号序列表（并发取号原子分配，B 模块）';

-- ---------------------------------------------------------------
-- 历史数据衔接（可选但推荐）：把已存在单号的最大流水回填进序列表，
-- 避免迁移后新单号从 0001 重新开始而与历史单号冲突。
-- 说明：首次分配时若 next_seq=0 会返回 1，因此这里回填的是「历史最大流水」，
--      下一次分配得到「历史最大 + 1」，与既有单号自然衔接。
--
-- ⚠️ 关键口径：业务日期必须取自**单号自身的日期段**，不能取 receive_date / order_date。
--   原因：单号格式为 GR<门店>-<yyyyMMdd>-<流水>，运行时注册键用的就是单号里的日期段；
--   而收货日期与单号日期段并不总是一致（例如补录历史单据时 receive_date 是当天、
--   单号日期段却是原始业务日）。若按 receive_date 分组回填，会把 A 日期的流水算进 B 日期，
--   导致该组 next_seq 虚高、新单号凭空跳号。
--   实测样例：GR407-20260911-0042 的 receive_date 是 2026-09-17，
--   按 receive_date 分组会把 0917 组的 max 抬到 42（真实只有 10）。
--
-- 幂等：ON DUPLICATE KEY UPDATE 取 GREATEST（只增不减），重复执行不会把已推进的序列改小。
-- ---------------------------------------------------------------

-- 1) 采购订单：从 ph_po_order.order_no 解析 PO<门店>-<yyyyMMdd>-<流水>
SET @has_order_seq := (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ph_po_doc_seq'
);
SET @sql_backfill_order := IF(@has_order_seq = 1,
  'INSERT INTO `ph_po_doc_seq` (`store_id`, `biz_date`, `biz_type`, `next_seq`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
   SELECT o.store_id,
          STR_TO_DATE(SUBSTRING_INDEX(SUBSTRING_INDEX(o.order_no, ''-'', 2), ''-'', -1), ''%Y%m%d'') AS biz_date,
          ''ORDER'' AS biz_type,
          MAX(CAST(SUBSTRING_INDEX(o.order_no, ''-'', -1) AS UNSIGNED)) AS next_seq,
          ''B-migration'', NOW(), ''B-migration'', NOW(), b''0'', o.tenant_id
     FROM ph_po_order o
    WHERE o.order_no REGEXP ''^PO[0-9]+-[0-9]{8}-[0-9]+$''
    GROUP BY o.store_id, biz_date, o.tenant_id
   HAVING biz_date IS NOT NULL
   ON DUPLICATE KEY UPDATE `ph_po_doc_seq`.next_seq = GREATEST(`ph_po_doc_seq`.next_seq, VALUES(next_seq))',
  'DO 0');
PREPARE stmt FROM @sql_backfill_order; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 采购收货单：从 ph_po_receipt.receipt_no 解析 GR<门店>-<yyyyMMdd>-<流水>
SET @sql_backfill_receipt := IF(@has_order_seq = 1,
  'INSERT INTO `ph_po_doc_seq` (`store_id`, `biz_date`, `biz_type`, `next_seq`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
   SELECT r.store_id,
          STR_TO_DATE(SUBSTRING_INDEX(SUBSTRING_INDEX(r.receipt_no, ''-'', 2), ''-'', -1), ''%Y%m%d'') AS biz_date,
          ''RECEIPT'' AS biz_type,
          MAX(CAST(SUBSTRING_INDEX(r.receipt_no, ''-'', -1) AS UNSIGNED)) AS next_seq,
          ''B-migration'', NOW(), ''B-migration'', NOW(), b''0'', r.tenant_id
     FROM ph_po_receipt r
    WHERE r.receipt_no REGEXP ''^GR[0-9]+-[0-9]{8}-[0-9]+$''
    GROUP BY r.store_id, biz_date, r.tenant_id
   HAVING biz_date IS NOT NULL
   ON DUPLICATE KEY UPDATE `ph_po_doc_seq`.next_seq = GREATEST(`ph_po_doc_seq`.next_seq, VALUES(next_seq))',
  'DO 0');
PREPARE stmt FROM @sql_backfill_receipt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
