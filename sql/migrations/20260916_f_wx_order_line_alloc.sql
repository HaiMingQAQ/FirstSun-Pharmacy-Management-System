-- F 线上订单出库分配表（原批次 / 原货位回补依据）
-- 对应 docs/F-member/线上订单库存生命周期与接口需求.md
-- 可重复执行：CREATE TABLE IF NOT EXISTS，不修改任何既有表。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ph_wx_order_line_alloc` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `wx_order_id`      BIGINT       NOT NULL COMMENT '线上订单编号(ph_wx_order)',
  `wx_order_line_id` BIGINT       NOT NULL COMMENT '线上订单明细编号(ph_wx_order_line)',
  `order_no`         VARCHAR(32)  NOT NULL COMMENT '订单号，与库存流水 ph_inv_flow.biz_no 对齐',
  `drug_id`          BIGINT       NOT NULL COMMENT '商品编号(ph_drug)',
  `batch_id`         BIGINT       NOT NULL COMMENT '出库批次(ph_inv_batch)，C 的 FEFO 实际分配',
  `location_id`      BIGINT       NOT NULL COMMENT '出库货位(ph_inv_location)，C 的 FEFO 实际分配',
  `qty`              INT          NOT NULL COMMENT '本批次出库数量',
  `returned_qty`     INT          NOT NULL DEFAULT 0 COMMENT '已回补数量',
  `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '0已出库/1已回补',
  `creator`          VARCHAR(64)  NULL DEFAULT '' COMMENT '创建者',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`          VARCHAR(64)  NULL DEFAULT '' COMMENT '更新者',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`        BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wx_alloc_line` (`tenant_id`, `wx_order_line_id`, `batch_id`, `location_id`),
  KEY `idx_wx_alloc_order` (`wx_order_id`),
  KEY `idx_wx_alloc_biz` (`tenant_id`, `order_no`),
  CONSTRAINT `ck_wx_alloc_qty` CHECK (`qty` > 0 AND `returned_qty` >= 0 AND `returned_qty` <= `qty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ph_wx_order_line_alloc';

-- 字典：小程序订单退款状态（沿用既有 pharmacy_wx_pay_status，无需新增）
-- 说明：本迁移只新增 F 自有表，不触碰 A/B/C/D/E 的表结构与数据。
