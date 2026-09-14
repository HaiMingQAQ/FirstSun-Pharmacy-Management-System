SET NAMES utf8mb4;

-- =============================================================
-- 迁移脚本：A 成员药店模块-员工字典 + 员工菜单
-- 文件：20260908_c_pharmacy_employee_menu.sql
-- 说明：
--   1. 仅新增员工所需字典与菜单，不修改已有数据。
--   2. 幂等可重复执行。
--   3. 字典类型 ID：303-304；字典数据 ID：1530+。
--   4. 员工菜单 ID 段 22030+（阶段一 22000+、阶段二 22020+）。
--   5. 员工菜单挂在父节点 22010（基础资料）下，与药品分类/门店平级。
-- 作者：A 成员
-- 日期：2026-09-08
-- =============================================================

-- -------------------------------------------------------------
-- 1. 字典类型：pharmacy_employee_status（1在职/0离职/2休假）
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (303, '员工在职状态', 'pharmacy_employee_status', 0, '药店员工在职状态：1=在职/0=离职/2=休假', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 2. 字典类型：pharmacy_employee_position（1店长/2药师/3收银员/4库管员/5采购/6财务/9系统管理员）
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (304, '员工岗位', 'pharmacy_employee_position', 0, '药店员工岗位：1店长/2药师/3收银员/4库管员/5采购/6财务/9系统管理员', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 3. 字典数据：pharmacy_employee_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_employee_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1530, 1, '离职', '0', 'pharmacy_employee_status', 0, 'info',     '', '', '1', NOW(), '1', NOW(), b'0'),
  (1531, 2, '在职', '1', 'pharmacy_employee_status', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1532, 3, '休假', '2', 'pharmacy_employee_status', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 4. 字典数据：pharmacy_employee_position
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_employee_position';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1540, 1, '店长',         '1', 'pharmacy_employee_position', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1541, 2, '药师',         '2', 'pharmacy_employee_position', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1542, 3, '收银员',       '3', 'pharmacy_employee_position', 0, 'info',     '', '', '1', NOW(), '1', NOW(), b'0'),
  (1543, 4, '库管员',       '4', 'pharmacy_employee_position', 0, 'info',     '', '', '1', NOW(), '1', NOW(), b'0'),
  (1544, 5, '采购',         '5', 'pharmacy_employee_position', 0, 'info',     '', '', '1', NOW(), '1', NOW(), b'0'),
  (1545, 6, '财务',         '6', 'pharmacy_employee_position', 0, 'info',     '', '', '1', NOW(), '1', NOW(), b'0'),
  (1546, 9, '系统管理员',   '9', 'pharmacy_employee_position', 0, 'danger',  '', '', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 5. 菜单：员工管理（菜单项 + 5 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22030, '员工管理', '', 2, 30, 22010, 'employee', 'ep:user', 'pharmacy/base/employee/index', 'PharmacyBaseEmployee', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22031, '员工查询', 'pharmacy:base:employee:query',  3, 10, 22030, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22032, '员工创建', 'pharmacy:base:employee:create', 3, 20, 22030, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22033, '员工更新', 'pharmacy:base:employee:update', 3, 30, 22030, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22034, '员工删除', 'pharmacy:base:employee:delete', 3, 40, 22030, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22035, '员工导出', 'pharmacy:base:employee:export', 3, 50, 22030, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 6. 角色菜单关联：超管角色（role_id=1）绑定员工菜单
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` IN (22030, 22031, 22032, 22033, 22034, 22035);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, 22030, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22031, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22032, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22033, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22034, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22035, 1, '1', NOW(), '1', NOW(), b'0');
