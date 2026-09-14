SET NAMES utf8mb4;

-- =============================================================
-- 迁移脚本：B 成员采购域（二）——采购订单 + 采购收货菜单与字典
-- 文件：20260913_b_purchase_order_receipt_menu.sql
-- 作者：B 成员（供应商与采购收货）
-- 日期：2026-09-13
-- 说明：
--   1. 仅新增采购订单/收货菜单与字典，不修改 A/C/D/E/F 已有数据，不改表结构。
--   2. 幂等可重复执行：菜单与字典类型 ON DUPLICATE KEY UPDATE，字典数据与角色菜单先删后插。
--   3. 菜单 ID 段：22130-22148（22100/22110/22120 已在
--      20260912_b_purchase_supplier_menu.sql 占用）。
--   4. 字典类型 ID：313-317；字典数据 ID：1630-1672。
--   5. 角色绑定：超管（tenant 1 / role 1）与 FirstSun 租户管理员（tenant 163 / role 167），
--      并刷新租户套餐 114 的 menu_ids，否则菜单在演示租户下不可见。
-- =============================================================

-- -------------------------------------------------------------
-- 1. 字典类型
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (313, '采购订单状态', 'pharmacy_po_status', 0, '采购订单状态：-1取消/0草稿/1提交/2审批/3发出/4部分到货/5完成', '1', NOW(), '1', NOW(), b'0'),
  (314, '采购收货单状态', 'pharmacy_receipt_status', 0, '采购收货单状态：0待提交/1已提交/2已入账/3已作废', '1', NOW(), '1', NOW(), b'0'),
  (315, '收货差异标记', 'pharmacy_receipt_diff_type', 0, '收货差异标记：0无/1数量差异/2价格差异', '1', NOW(), '1', NOW(), b'0'),
  (316, '收货质检结果', 'pharmacy_quality_status', 0, '收货单质检结果：0未检/1合格/2有异常', '1', NOW(), '1', NOW(), b'0'),
  (317, '收货明细质检标记', 'pharmacy_quality_flag', 0, '收货明细质检标记：0待检/1通过/2异常拒收', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 2. 字典数据：pharmacy_po_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_po_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1630, 1, '已取消',   '-1', 'pharmacy_po_status', 0, 'danger',  '', '已取消',     '1', NOW(), '1', NOW(), b'0'),
  (1631, 2, '草稿',     '0',  'pharmacy_po_status', 0, 'info',    '', '草稿',       '1', NOW(), '1', NOW(), b'0'),
  (1632, 3, '已提交',   '1',  'pharmacy_po_status', 0, 'warning', '', '待审批',     '1', NOW(), '1', NOW(), b'0'),
  (1633, 4, '已审批',   '2',  'pharmacy_po_status', 0, 'primary', '', '审批通过',   '1', NOW(), '1', NOW(), b'0'),
  (1634, 5, '已发出',   '3',  'pharmacy_po_status', 0, 'primary', '', '已发往供应商', '1', NOW(), '1', NOW(), b'0'),
  (1635, 6, '部分到货', '4',  'pharmacy_po_status', 0, 'warning', '', '部分收货完成', '1', NOW(), '1', NOW(), b'0'),
  (1636, 7, '已完成',   '5',  'pharmacy_po_status', 0, 'success', '', '全部收货完成', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 3. 字典数据：pharmacy_receipt_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_receipt_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1640, 1, '待提交', '0', 'pharmacy_receipt_status', 0, 'info',    '', '待提交',   '1', NOW(), '1', NOW(), b'0'),
  (1641, 2, '已提交', '1', 'pharmacy_receipt_status', 0, 'warning', '', '已提交待入账', '1', NOW(), '1', NOW(), b'0'),
  (1642, 3, '已入账', '2', 'pharmacy_receipt_status', 0, 'success', '', '库存已入账', '1', NOW(), '1', NOW(), b'0'),
  (1643, 4, '已作废', '3', 'pharmacy_receipt_status', 0, 'danger',  '', '已作废',   '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 4. 字典数据：pharmacy_receipt_diff_type
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_receipt_diff_type';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1650, 1, '无差异',   '0', 'pharmacy_receipt_diff_type', 0, 'info',    '', '与采购订单一致', '1', NOW(), '1', NOW(), b'0'),
  (1651, 2, '数量差异', '1', 'pharmacy_receipt_diff_type', 0, 'warning', '', '实收数量与订单不一致', '1', NOW(), '1', NOW(), b'0'),
  (1652, 3, '价格差异', '2', 'pharmacy_receipt_diff_type', 0, 'danger',  '', '实收金额与订单不一致', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 5. 字典数据：pharmacy_quality_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_quality_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1660, 1, '未检',   '0', 'pharmacy_quality_status', 0, 'info',    '', '未质检',   '1', NOW(), '1', NOW(), b'0'),
  (1661, 2, '合格',   '1', 'pharmacy_quality_status', 0, 'success', '', '质检合格', '1', NOW(), '1', NOW(), b'0'),
  (1662, 3, '有异常', '2', 'pharmacy_quality_status', 0, 'danger',  '', '质检异常', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 6. 字典数据：pharmacy_quality_flag
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_quality_flag';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1670, 1, '待检',     '0', 'pharmacy_quality_flag', 0, 'info',    '', '待质检',       '1', NOW(), '1', NOW(), b'0'),
  (1671, 2, '通过',     '1', 'pharmacy_quality_flag', 0, 'success', '', '质检通过',     '1', NOW(), '1', NOW(), b'0'),
  (1672, 3, '异常拒收', '2', 'pharmacy_quality_flag', 0, 'danger',  '', '质检异常，拒收', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 7. 菜单：采购订单（菜单项 + 9 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22130, '采购订单', '', 2, 30, 22100, 'order', 'ep:document-copy', 'pharmacy/purchase/order/index', 'PharmacyPurchaseOrder', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22131, '订单查询', 'pharmacy:purchase:order:query',   3, 10, 22130, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22132, '订单创建', 'pharmacy:purchase:order:create',  3, 20, 22130, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22133, '订单更新', 'pharmacy:purchase:order:update',  3, 30, 22130, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22134, '订单删除', 'pharmacy:purchase:order:delete',  3, 40, 22130, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22135, '订单导出', 'pharmacy:purchase:order:export',  3, 50, 22130, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22136, '订单提交', 'pharmacy:purchase:order:submit',  3, 60, 22130, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22137, '订单审批', 'pharmacy:purchase:order:approve', 3, 70, 22130, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22138, '标记发出', 'pharmacy:purchase:order:issue',   3, 80, 22130, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22139, '订单取消', 'pharmacy:purchase:order:cancel',  3, 90, 22130, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 8. 菜单：采购收货（菜单项 + 8 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22140, '采购收货', '', 2, 40, 22100, 'receipt', 'ep:box', 'pharmacy/purchase/receipt/index', 'PharmacyPurchaseReceipt', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22141, '收货查询', 'pharmacy:purchase:receipt:query',  3, 10, 22140, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22142, '收货创建', 'pharmacy:purchase:receipt:create', 3, 20, 22140, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22143, '收货更新', 'pharmacy:purchase:receipt:update', 3, 30, 22140, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22144, '收货删除', 'pharmacy:purchase:receipt:delete', 3, 40, 22140, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22145, '收货导出', 'pharmacy:purchase:receipt:export', 3, 50, 22140, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22146, '收货提交', 'pharmacy:purchase:receipt:submit', 3, 60, 22140, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22147, '收货入账', 'pharmacy:purchase:receipt:post',   3, 70, 22140, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22148, '收货作废', 'pharmacy:purchase:receipt:void',   3, 80, 22140, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 9. 角色菜单绑定：超管（tenant 1 / role 1）
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` BETWEEN 22130 AND 22148;
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, 22130, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22131, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22132, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22133, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22134, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22135, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22136, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22137, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22138, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22139, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22140, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22141, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22142, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22143, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22144, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22145, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22146, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22147, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22148, 1, '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 10. 角色菜单绑定：FirstSun 租户管理员（tenant 163 / role 167，共享账号 0407）
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 163 AND `role_id` = 167 AND `menu_id` BETWEEN 22130 AND 22148;
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (167, 22130, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22131, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22132, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22133, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22134, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22135, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22136, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22137, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22138, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22139, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22140, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22141, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22142, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22143, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22144, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22145, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22146, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22147, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22148, 163, 'system', NOW(), 'system', NOW(), b'0');

-- -------------------------------------------------------------
-- 11. 租户套餐 114「药店」菜单范围刷新
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
