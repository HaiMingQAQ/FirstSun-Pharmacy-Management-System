SET NAMES utf8mb4;

-- =============================================================
-- 迁移脚本：A 成员药店模块初始化（菜单 + 字典）
-- 文件：20260908_a_pharmacy_module_init.sql
-- 说明：
--   1. 仅新增 pharmacy 模块所需菜单与字典数据，不修改已有数据。
--   2. 使用 ON DUPLICATE KEY UPDATE / 先删后插，保证可重复执行。
--   3. 菜单 ID 段：22000+（system_menu AUTO_INCREMENT≈12732，避免冲突）
--   4. 字典类型 ID 段：300+（system_dict_type AUTO_INCREMENT≈298）
--   5. 字典数据 ID 段：1500+（system_dict_data AUTO_INCREMENT≈1409）
--   6. 字典 type 已与 backend DictTypeConstants、前端 utils/dict.ts 保持一致。
--   7. 状态值遵循真实建表脚本：pharmacy_status=1启用/0停用（与 CommonStatusEnum 方向相反）。
-- 作者：A 成员
-- 日期：2026-09-08
-- =============================================================

-- -------------------------------------------------------------
-- 1. 字典类型：pharmacy_status、pharmacy_category_type
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (300, '药店通用启停状态', 'pharmacy_status', 0, '药店业务通用启停状态：1=启用 / 0=停用（与 CommonStatusEnum 方向相反）', '1', NOW(), '1', NOW(), b'0'),
  (301, '药品分类类型', 'pharmacy_category_type', 0, '药品分类类型：0药品/1保健品/2医疗器械/3中药饮片/4日化/5其他', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 2. 字典数据：pharmacy_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1500, 1, '启用', '1', 'pharmacy_status', 0, 'success', '', '启用状态', '1', NOW(), '1', NOW(), b'0'),
  (1501, 2, '停用', '0', 'pharmacy_status', 0, 'info',     '', '停用状态', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 3. 字典数据：pharmacy_category_type
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_category_type';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1510, 1, '药品',     '0', 'pharmacy_category_type', 0, 'primary', '', '药品',     '1', NOW(), '1', NOW(), b'0'),
  (1511, 2, '保健品',   '1', 'pharmacy_category_type', 0, 'success', '', '保健品',   '1', NOW(), '1', NOW(), b'0'),
  (1512, 3, '医疗器械', '2', 'pharmacy_category_type', 0, 'warning', '', '医疗器械', '1', NOW(), '1', NOW(), b'0'),
  (1513, 4, '中药饮片', '3', 'pharmacy_category_type', 0, 'danger',  '', '中药饮片', '1', NOW(), '1', NOW(), b'0'),
  (1514, 5, '日化',     '4', 'pharmacy_category_type', 0, 'info',     '', '日化',     '1', NOW(), '1', NOW(), b'0'),
  (1515, 6, '其他',     '5', 'pharmacy_category_type', 0, 'info',     '', '其他',     '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 4. 菜单：药店业务（一级目录）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22000, '药店业务', '', 1, 30, 0, '/pharmacy-base', 'ep:shop', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 5. 菜单：基础资料（二级目录）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22010, '基础资料', '', 1, 10, 22000, '/base', 'ep:folder', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 6. 菜单：药品分类（菜单项 + 5 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22011, '药品分类', '', 2, 10, 22010, 'category', 'ep:collection', 'pharmacy/base/category/index', 'PharmacyBaseCategory', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22012, '分类查询', 'pharmacy:base:category:query',  3, 10, 22011, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22013, '分类创建', 'pharmacy:base:category:create', 3, 20, 22011, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22014, '分类更新', 'pharmacy:base:category:update', 3, 30, 22011, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22015, '分类删除', 'pharmacy:base:category:delete', 3, 40, 22011, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22016, '分类导出', 'pharmacy:base:category:export', 3, 50, 22011, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 7. 角色菜单关联：为超管角色（role_id=1）绑定以上菜单，便于联调验证
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` IN (22000, 22010, 22011, 22012, 22013, 22014, 22015, 22016);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, 22000, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22010, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22011, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22012, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22013, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22014, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22015, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22016, 1, '1', NOW(), '1', NOW(), b'0');
