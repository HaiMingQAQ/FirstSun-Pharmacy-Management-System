SET NAMES utf8mb4;

-- =============================================================
-- 迁移脚本：A 成员药店模块-药品条码字典 + 菜单
-- 文件：20260908_e_pharmacy_barcode_menu.sql
-- 说明：
--   1. 仅新增条码所需字典与菜单，不修改已有数据。
--   2. 幂等可重复执行。
--   3. 字典类型 ID：309；字典数据 ID：1590+。
--   4. 条码菜单 ID 段 22050+（阶段一 22000+、二 22020+、三 22030+、四 22040+）。
--   5. 条码菜单挂在父节点 22010（基础资料）下，与药品档案平级。
-- 作者：A 成员
-- 日期：2026-09-08
-- =============================================================

-- -------------------------------------------------------------
-- 1. 字典类型：条码类型
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (309, '条码类型', 'pharmacy_barcode_type', 0, '条码类型：0商品条码/1店内码/2追溯码', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 2. 字典数据：pharmacy_barcode_type
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_barcode_type';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1590, 1, '商品条码', '0', 'pharmacy_barcode_type', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1591, 2, '店内码',   '1', 'pharmacy_barcode_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1592, 3, '追溯码',   '2', 'pharmacy_barcode_type', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 3. 菜单：药品条码（菜单项 + 5 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22050, '药品条码', '', 2, 50, 22010, 'barcode', 'fa:barcode', 'pharmacy/base/barcode/index', 'PharmacyBaseBarcode', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22051, '条码查询', 'pharmacy:base:barcode:query',  3, 10, 22050, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22052, '条码创建', 'pharmacy:base:barcode:create', 3, 20, 22050, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22053, '条码更新', 'pharmacy:base:barcode:update', 3, 30, 22050, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22054, '条码删除', 'pharmacy:base:barcode:delete', 3, 40, 22050, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22055, '条码导出', 'pharmacy:base:barcode:export', 3, 50, 22050, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 4. 角色菜单关联：超管角色（role_id=1）绑定条码菜单
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` IN (22050, 22051, 22052, 22053, 22054, 22055);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, 22050, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22051, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22052, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22053, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22054, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22055, 1, '1', NOW(), '1', NOW(), b'0');
