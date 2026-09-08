-- =============================================================
-- 迁移脚本：A 成员药店模块-门店菜单 + 字典 pharmacy_yes_no
-- 文件：20260908_b_pharmacy_store_menu.sql
-- 说明：
--   1. 仅新增门店所需菜单与 pharmacy_yes_no 字典，不修改已有数据。
--   2. 幂等可重复执行。
--   3. 菜单 ID 段：22020+（阶段一占 22000-22016）。
--   4. 字典类型 ID：302；字典数据 ID 段：1520+。
--   5. 门店菜单挂在父节点 22010（基础资料）下，与药品分类平级。
-- 作者：A 成员
-- 日期：2026-09-08
-- =============================================================

-- -------------------------------------------------------------
-- 1. 字典类型：pharmacy_yes_no（0否/1是）
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (302, '药店通用是否', 'pharmacy_yes_no', 0, '药店业务通用是否类型：1=是 / 0=否', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 2. 字典数据：pharmacy_yes_no
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_yes_no';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1520, 1, '是', '1', 'pharmacy_yes_no', 0, 'success', '', '是', '1', NOW(), '1', NOW(), b'0'),
  (1521, 2, '否', '0', 'pharmacy_yes_no', 0, 'info',     '', '否', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 3. 菜单：门店（菜单项 + 5 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22020, '门店管理', '', 2, 20, 22010, 'store', 'ep:shop', 'pharmacy/base/store/index', 'PharmacyBaseStore', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22021, '门店查询', 'pharmacy:base:store:query',  3, 10, 22020, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22022, '门店创建', 'pharmacy:base:store:create', 3, 20, 22020, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22023, '门店更新', 'pharmacy:base:store:update', 3, 30, 22020, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22024, '门店删除', 'pharmacy:base:store:delete', 3, 40, 22020, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22025, '门店导出', 'pharmacy:base:store:export', 3, 50, 22020, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 4. 角色菜单关联：超管角色（role_id=1）绑定门店菜单
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` IN (22020, 22021, 22022, 22023, 22024, 22025);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, 22020, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22021, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22022, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22023, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22024, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22025, 1, '1', NOW(), '1', NOW(), b'0');
