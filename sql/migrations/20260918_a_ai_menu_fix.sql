SET NAMES utf8mb4;

-- =============================================================
-- 迁移脚本：A 纠正迁移——AI 助手菜单 ID 22300 冲突修复
-- 文件：20260918_a_ai_menu_fix.sql
-- 作者：A 成员（基础资料与公共平台 / AI 助手）
-- 日期：2026-09-18
--
-- 背景：
--   20260917_o_pharmacy_ai_prototype.sql 使用菜单 ID 22300 并通过
--   ON DUPLICATE KEY UPDATE 将 B 的 22300「采购管理」目录（type=1）
--   覆盖为「AI 助手使用」按钮（type=3），导致 22310~22348 失去父目录，
--   登录日志反复出现“找不到父资源(22300)”，管理端采购管理菜单消失。
--
-- 方案（不改写已执行的历史迁移，新增纠正迁移）：
--   1. 恢复 22300 为 B 的「采购管理」目录，字段与
--      20260912_b_purchase_supplier_menu.sql / 20260913_b_purchase_order_receipt_menu.sql
--      的原始定义完全一致（type=1、parent_id=22000、path='purchase' 等）。
--   2. 将 AI 助手权限迁移到新菜单 ID 22349：
--      22300~22348 已被 B 采购域占用；经全量 SQL 与数据库检索确认
--      22349 未在 system_menu、system_role_menu 或任何迁移脚本中使用。
--   3. 为超管（tenant 1 / role 1）与 FirstSun 租户管理员（tenant 163 / role 167）
--      增加 22349 菜单绑定，保持 AI 助手入口可见且权限有效；
--      不删除 22300 上原有的角色绑定（采购管理目录对角色可见，即 B 的采购权限）。
--   4. 刷新租户套餐 114 的 menu_ids（22349 落在 22000~22999 段，自动纳入）。
--
-- 幂等：全部使用 INSERT ... ON DUPLICATE KEY UPDATE 或 INSERT ... SELECT
--       WHERE NOT EXISTS，可重复执行，不产生重复菜单或重复关联。
-- =============================================================

-- -------------------------------------------------------------
-- 1. 恢复 22300 为 B 的「采购管理」目录（type=1）
--    若 22300 不存在（理论上不会，B 脚本先于 AI 脚本执行）则插入；
--    若存在则按 B 原始定义更新，纠正 AI 脚本的覆盖。
-- -------------------------------------------------------------
INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22300, '采购管理', '', 1, 20, 22000, 'purchase', 'ep:shopping-cart', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`),
  `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`),
  `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`),
  `updater` = VALUES(`updater`), `update_time` = NOW(), `deleted` = b'0';

-- -------------------------------------------------------------
-- 2. AI 助手权限菜单迁移到新 ID 22349
--    （permission=pharmacy:ai:chat 不变，仅菜单 ID 与父级关系调整）
-- -------------------------------------------------------------
INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22349, 'AI 助手使用', 'pharmacy:ai:chat', 3, 90, 22000, '', 'ep:chat-dot-round', '', '', 0, b'0', b'0', b'0', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`),
  `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`),
  `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`),
  `updater` = VALUES(`updater`), `update_time` = NOW(), `deleted` = b'0';

-- -------------------------------------------------------------
-- 3. 角色绑定：为超管与 FirstSun 租户管理员增加 22349（AI 助手入口）
--    保留 22300 上的既有绑定（采购管理目录可见），不误删角色采购权限。
-- -------------------------------------------------------------
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, 22349, 1, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_role_menu` WHERE `role_id` = 1 AND `menu_id` = 22349 AND `tenant_id` = 1 AND `deleted` = b'0');

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 167, 22349, 163, 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_role_menu` WHERE `role_id` = 167 AND `menu_id` = 22349 AND `tenant_id` = 163 AND `deleted` = b'0');

-- -------------------------------------------------------------
-- 4. 刷新 FirstSun 租户使用的药店套餐菜单范围（规则与历史脚本一致）
--    AI 新 ID 22349 位于 22000~22999 段，自动纳入套餐可用权限集合。
-- -------------------------------------------------------------
UPDATE `system_tenant_package`
SET `menu_ids` = (
    SELECT CAST(JSON_ARRAYAGG(`id`) AS CHAR)
    FROM `system_menu`
    WHERE `deleted` = b'0' AND (`id` BETWEEN 22000 AND 22999 OR `id` BETWEEN 61000 AND 61999)
  ),
  `updater` = 'system',
  `update_time` = NOW()
WHERE `id` = 114;
