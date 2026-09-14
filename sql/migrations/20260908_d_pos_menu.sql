SET NAMES utf8mb4;

-- ============================================================
-- FirstSun 药店管理系统 | D 成员增量脚本
-- 20260908_d_pos_menu.sql
-- 说明：POS 销售域管理端菜单 + 按钮权限（收银台/销售单/退货/班次/支付/统计）
-- 幂等：INSERT IGNORE，重复执行安全；菜单 id 使用 61000+ 段，避免与框架冲突
-- 前置：已执行 sql/firstsun_pharmacy_init.sql（或 backend/sql/mysql/ruoyi-vue-pro.sql）
-- ============================================================

-- 1. 目录：药店 POS 管理
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61000, '药店POS管理', '', 1, 1000, 0, '/pharmacy-pos', 'ep:money', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 2. 收银台
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61001, '收银台', '', 2, 1, 61000, 'pos/index', 'ep:goods', 'pharmacy/pos/index', 'PharmacyPosIndex', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61002, '收银', 'pharmacy:pos-sale-order:create', 3, 1, 61001, '', NULL, NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 3. 销售单
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61003, '销售单', '', 2, 2, 61000, 'pos/orderList', 'ep:list', 'pharmacy/pos/orderList', 'PharmacyPosOrderList', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61004, '销售查询', 'pharmacy:pos-sale-order:query', 3, 1, 61003, '', NULL, NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
-- 销售单详情（隐藏菜单，仅用于路由跳转）
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61005, '销售单详情', '', 2, 3, 61000, 'pos/orderDetail', NULL, 'pharmacy/pos/orderDetail', 'PharmacyPosOrderDetail', 0, b'0', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 4. 退货单
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61006, '退货单', '', 2, 4, 61000, 'pos/returnList', 'ep:refresh-left', 'pharmacy/pos/returnList', 'PharmacyPosReturnList', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61007, '退货创建', 'pharmacy:pos-sale-return:create', 3, 1, 61006, '', NULL, NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61008, '退货查询', 'pharmacy:pos-sale-return:query', 3, 2, 61006, '', NULL, NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 5. 收银班次
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61009, '收银班次', '', 2, 5, 61000, 'pos/shiftList', 'ep:timer', 'pharmacy/pos/shiftList', 'PharmacyPosShiftList', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61010, '开台/交班', 'pharmacy:pos-shift:create', 3, 1, 61009, '', NULL, NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61011, '班次查询', 'pharmacy:pos-shift:query', 3, 2, 61009, '', NULL, NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 6. 支付明细
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61012, '支付明细', '', 2, 6, 61000, 'pos/paymentList', 'ep:wallet', 'pharmacy/pos/paymentList', 'PharmacyPosPaymentList', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61013, '支付查询', 'pharmacy:pos-payment:query', 3, 1, 61012, '', NULL, NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 7. 销售统计
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61014, '销售统计', '', 2, 7, 61000, 'pos/statistics', 'ep:data-analysis', 'pharmacy/pos/statistics', 'PharmacyPosStatistics', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (61015, '统计查询', 'pharmacy:pos-statistics:query', 3, 1, 61014, '', NULL, NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
