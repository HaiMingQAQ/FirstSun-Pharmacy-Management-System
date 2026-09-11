SET NAMES utf8mb4;

-- =============================================================
-- 迁移脚本：B 成员采购域（一）——采购管理目录 + 供应商 + 供应商证照
-- 文件：20260912_b_purchase_supplier_menu.sql
-- 作者：B 成员（供应商与采购收货）
-- 日期：2026-09-12
-- 说明：
--   1. 仅新增采购域菜单、字典与角色绑定，不修改 A/C/D/E/F 已有数据。
--   2. 幂等可重复执行：菜单与字典类型 ON DUPLICATE KEY UPDATE，字典数据与角色菜单先删后插。
--   3. 菜单 ID 段：22100+（A 已占 22000-22055；D 的 POS 使用 61000+）。
--   4. 字典类型 ID：310-312；字典数据 ID：1600-1649。
--   5. 表结构无需变更：ph_supplier、ph_supplier_license 已在
--      sql/firstsun_pharmacy_init.sql 中定义，本脚本只补菜单与字典。
--   6. 角色绑定：超管角色（tenant 1 / role 1）与 FirstSun 租户管理员
--      （tenant 163 / role 167，即共享账号 0407 所属角色）都要绑定，
--      否则菜单在演示租户下不可见；同时刷新租户套餐 114 的 menu_ids。
-- =============================================================

-- -------------------------------------------------------------
-- 1. 字典类型：供应商首营审核状态 / 证照类型 / 证照状态
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (310, '供应商首营审核状态', 'pharmacy_supplier_approve_status', 0, '供应商首营审核状态：0待审/1通过/2驳回', '1', NOW(), '1', NOW(), b'0'),
  (311, '供应商证照类型', 'pharmacy_license_type', 0, '供应商证照类型：0经营许可证/1生产许可证/2GSP证/3营业执照/4其他', '1', NOW(), '1', NOW(), b'0'),
  (312, '供应商证照状态', 'pharmacy_license_status', 0, '供应商证照状态：1有效/0过期（按到期日自动计算）', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 2. 字典数据：pharmacy_supplier_approve_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_supplier_approve_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1600, 1, '待审', '0', 'pharmacy_supplier_approve_status', 0, 'warning', '', '待首营审核', '1', NOW(), '1', NOW(), b'0'),
  (1601, 2, '通过', '1', 'pharmacy_supplier_approve_status', 0, 'success', '', '首营审核通过', '1', NOW(), '1', NOW(), b'0'),
  (1602, 3, '驳回', '2', 'pharmacy_supplier_approve_status', 0, 'danger',  '', '首营审核驳回', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 3. 字典数据：pharmacy_license_type
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_license_type';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1610, 1, '经营许可证', '0', 'pharmacy_license_type', 0, 'primary', '', '药品经营许可证', '1', NOW(), '1', NOW(), b'0'),
  (1611, 2, '生产许可证', '1', 'pharmacy_license_type', 0, 'success', '', '药品生产许可证', '1', NOW(), '1', NOW(), b'0'),
  (1612, 3, 'GSP证',      '2', 'pharmacy_license_type', 0, 'success', '', '药品经营质量管理规范认证', '1', NOW(), '1', NOW(), b'0'),
  (1613, 4, '营业执照',   '3', 'pharmacy_license_type', 0, 'info',    '', '营业执照', '1', NOW(), '1', NOW(), b'0'),
  (1614, 5, '其他',       '4', 'pharmacy_license_type', 0, 'info',    '', '其他资质文件', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 4. 字典数据：pharmacy_license_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_license_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1620, 1, '有效', '1', 'pharmacy_license_status', 0, 'success', '', '证照在有效期内', '1', NOW(), '1', NOW(), b'0'),
  (1621, 2, '过期', '0', 'pharmacy_license_status', 0, 'danger',  '', '证照已过期', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 5. 菜单：采购管理（二级目录，挂在 22000 药店业务下）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22100, '采购管理', '', 1, 20, 22000, 'purchase', 'ep:shopping-cart', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 6. 菜单：供应商（菜单项 + 6 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22110, '供应商',   '', 2, 10, 22100, 'supplier', 'ep:shop', 'pharmacy/purchase/supplier/index', 'PharmacyPurchaseSupplier', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22111, '供应商查询', 'pharmacy:purchase:supplier:query',   3, 10, 22110, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22112, '供应商创建', 'pharmacy:purchase:supplier:create',  3, 20, 22110, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22113, '供应商更新', 'pharmacy:purchase:supplier:update',  3, 30, 22110, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22114, '供应商删除', 'pharmacy:purchase:supplier:delete',  3, 40, 22110, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22115, '供应商导出', 'pharmacy:purchase:supplier:export',  3, 50, 22110, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22116, '供应商审核', 'pharmacy:purchase:supplier:approve', 3, 60, 22110, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 7. 菜单：供应商证照（菜单项 + 5 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22120, '供应商证照', '', 2, 20, 22100, 'license', 'ep:document', 'pharmacy/purchase/license/index', 'PharmacyPurchaseLicense', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22121, '证照查询', 'pharmacy:purchase:license:query',  3, 10, 22120, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22122, '证照创建', 'pharmacy:purchase:license:create', 3, 20, 22120, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22123, '证照更新', 'pharmacy:purchase:license:update', 3, 30, 22120, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22124, '证照删除', 'pharmacy:purchase:license:delete', 3, 40, 22120, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22125, '证照导出', 'pharmacy:purchase:license:export', 3, 50, 22120, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 8. 角色菜单绑定：超管角色（tenant 1 / role 1）
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` BETWEEN 22100 AND 22125;
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, 22100, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22110, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22111, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22112, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22113, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22114, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22115, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22116, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22120, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22121, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22122, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22123, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22124, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22125, 1, '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 9. 角色菜单绑定：FirstSun 租户管理员（tenant 163 / role 167，共享账号 0407）
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 163 AND `role_id` = 167 AND `menu_id` BETWEEN 22100 AND 22125;
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (167, 22100, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22110, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22111, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22112, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22113, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22114, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22115, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22116, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22120, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22121, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22122, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22123, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22124, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22125, 163, 'system', NOW(), 'system', NOW(), b'0');

-- -------------------------------------------------------------
-- 10. 租户套餐 114「药店」菜单范围刷新（与 20260910_i 的规则保持一致）
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
