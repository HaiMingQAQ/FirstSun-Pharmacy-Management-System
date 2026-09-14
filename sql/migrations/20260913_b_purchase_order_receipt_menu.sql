SET NAMES utf8mb4;

-- =============================================================
-- 迁移脚本：B 成员采购域（二）——采购订单 + 采购收货菜单与字典
-- 文件：20260913_b_purchase_order_receipt_menu.sql
-- 作者：B 成员（供应商与采购收货）
-- 日期：2026-09-13
-- 说明：
--   1. 仅新增采购订单/收货菜单与字典，不修改 A/C/D/E/F 已有数据，不改表结构。
--   2. 幂等可重复执行：菜单与字典类型 ON DUPLICATE KEY UPDATE，字典数据与角色菜单先删后插。
--   3. 菜单 ID 段：22330-22348（22300/22310/22320 已在
--      20260912_b_purchase_supplier_menu.sql 占用）。
--   4. 字典类型 ID：323-327；字典数据 ID：1730-1772。
--   5. 角色绑定：超管（tenant 1 / role 1）与 FirstSun 租户管理员（tenant 163 / role 167），
--      并刷新租户套餐 114 的 menu_ids，否则菜单在演示租户下不可见。
-- =============================================================

-- -------------------------------------------------------------
-- 0. 清理旧版冲突编号（旧版误用了会员模块的 221xx / 313+ / 1630+）
-- -------------------------------------------------------------
DELETE FROM `system_role_menu`
WHERE `menu_id` IN (
  SELECT `id` FROM `system_menu`
  WHERE `permission` LIKE 'pharmacy:purchase:order:%'
     OR `permission` LIKE 'pharmacy:purchase:receipt:%'
     OR `component` IN ('pharmacy/purchase/order/index', 'pharmacy/purchase/receipt/index')
     OR (`id` IN (22130, 22140) AND `name` IN ('采购订单', '采购收货'))
);
DELETE FROM `system_menu`
WHERE `permission` LIKE 'pharmacy:purchase:order:%'
   OR `permission` LIKE 'pharmacy:purchase:receipt:%'
   OR `component` IN ('pharmacy/purchase/order/index', 'pharmacy/purchase/receipt/index')
   OR (`id` IN (22130, 22140) AND `name` IN ('采购订单', '采购收货'));
DELETE FROM `system_dict_data`
WHERE `dict_type` IN ('pharmacy_po_status', 'pharmacy_receipt_status', 'pharmacy_receipt_diff_type', 'pharmacy_quality_status', 'pharmacy_quality_flag');
DELETE FROM `system_dict_type`
WHERE `type` IN ('pharmacy_po_status', 'pharmacy_receipt_status', 'pharmacy_receipt_diff_type', 'pharmacy_quality_status', 'pharmacy_quality_flag');

-- -------------------------------------------------------------
-- 1. 字典类型
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (323, '采购订单状态', 'pharmacy_po_status', 0, '采购订单状态：-1取消/0草稿/1提交/2审批/3发出/4部分到货/5完成', '1', NOW(), '1', NOW(), b'0'),
  (324, '采购收货单状态', 'pharmacy_receipt_status', 0, '采购收货单状态：0待提交/1已提交/2已入账/3已作废', '1', NOW(), '1', NOW(), b'0'),
  (325, '收货差异标记', 'pharmacy_receipt_diff_type', 0, '收货差异标记：0无/1数量差异/2价格差异', '1', NOW(), '1', NOW(), b'0'),
  (326, '收货质检结果', 'pharmacy_quality_status', 0, '收货单质检结果：0未检/1合格/2有异常', '1', NOW(), '1', NOW(), b'0'),
  (327, '收货明细质检标记', 'pharmacy_quality_flag', 0, '收货明细质检标记：0待检/1通过/2异常拒收', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 2. 字典数据：pharmacy_po_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_po_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1730, 1, '已取消',   '-1', 'pharmacy_po_status', 0, 'danger',  '', '已取消',     '1', NOW(), '1', NOW(), b'0'),
  (1731, 2, '草稿',     '0',  'pharmacy_po_status', 0, 'info',    '', '草稿',       '1', NOW(), '1', NOW(), b'0'),
  (1732, 3, '已提交',   '1',  'pharmacy_po_status', 0, 'warning', '', '待审批',     '1', NOW(), '1', NOW(), b'0'),
  (1733, 4, '已审批',   '2',  'pharmacy_po_status', 0, 'primary', '', '审批通过',   '1', NOW(), '1', NOW(), b'0'),
  (1734, 5, '已发出',   '3',  'pharmacy_po_status', 0, 'primary', '', '已发往供应商', '1', NOW(), '1', NOW(), b'0'),
  (1735, 6, '部分到货', '4',  'pharmacy_po_status', 0, 'warning', '', '部分收货完成', '1', NOW(), '1', NOW(), b'0'),
  (1736, 7, '已完成',   '5',  'pharmacy_po_status', 0, 'success', '', '全部收货完成', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 3. 字典数据：pharmacy_receipt_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_receipt_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1740, 1, '待提交', '0', 'pharmacy_receipt_status', 0, 'info',    '', '待提交',   '1', NOW(), '1', NOW(), b'0'),
  (1741, 2, '已提交', '1', 'pharmacy_receipt_status', 0, 'warning', '', '已提交待入账', '1', NOW(), '1', NOW(), b'0'),
  (1742, 3, '已入账', '2', 'pharmacy_receipt_status', 0, 'success', '', '库存已入账', '1', NOW(), '1', NOW(), b'0'),
  (1743, 4, '已作废', '3', 'pharmacy_receipt_status', 0, 'danger',  '', '已作废',   '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 4. 字典数据：pharmacy_receipt_diff_type
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_receipt_diff_type';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1750, 1, '无差异',   '0', 'pharmacy_receipt_diff_type', 0, 'info',    '', '与采购订单一致', '1', NOW(), '1', NOW(), b'0'),
  (1751, 2, '数量差异', '1', 'pharmacy_receipt_diff_type', 0, 'warning', '', '实收数量与订单不一致', '1', NOW(), '1', NOW(), b'0'),
  (1752, 3, '价格差异', '2', 'pharmacy_receipt_diff_type', 0, 'danger',  '', '实收金额与订单不一致', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 5. 字典数据：pharmacy_quality_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_quality_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1760, 1, '未检',   '0', 'pharmacy_quality_status', 0, 'info',    '', '未质检',   '1', NOW(), '1', NOW(), b'0'),
  (1761, 2, '合格',   '1', 'pharmacy_quality_status', 0, 'success', '', '质检合格', '1', NOW(), '1', NOW(), b'0'),
  (1762, 3, '有异常', '2', 'pharmacy_quality_status', 0, 'danger',  '', '质检异常', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 6. 字典数据：pharmacy_quality_flag
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_quality_flag';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1770, 1, '待检',     '0', 'pharmacy_quality_flag', 0, 'info',    '', '待质检',       '1', NOW(), '1', NOW(), b'0'),
  (1771, 2, '通过',     '1', 'pharmacy_quality_flag', 0, 'success', '', '质检通过',     '1', NOW(), '1', NOW(), b'0'),
  (1772, 3, '异常拒收', '2', 'pharmacy_quality_flag', 0, 'danger',  '', '质检异常，拒收', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 7. 菜单：采购订单（菜单项 + 9 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22330, '采购订单', '', 2, 30, 22300, 'order', 'ep:document-copy', 'pharmacy/purchase/order/index', 'PharmacyPurchaseOrder', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22331, '订单查询', 'pharmacy:purchase:order:query',   3, 10, 22330, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22332, '订单创建', 'pharmacy:purchase:order:create',  3, 20, 22330, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22333, '订单更新', 'pharmacy:purchase:order:update',  3, 30, 22330, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22334, '订单删除', 'pharmacy:purchase:order:delete',  3, 40, 22330, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22335, '订单导出', 'pharmacy:purchase:order:export',  3, 50, 22330, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22336, '订单提交', 'pharmacy:purchase:order:submit',  3, 60, 22330, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22337, '订单审批', 'pharmacy:purchase:order:approve', 3, 70, 22330, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22338, '标记发出', 'pharmacy:purchase:order:issue',   3, 80, 22330, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22339, '订单取消', 'pharmacy:purchase:order:cancel',  3, 90, 22330, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 8. 菜单：采购收货（菜单项 + 8 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22340, '采购收货', '', 2, 40, 22300, 'receipt', 'ep:box', 'pharmacy/purchase/receipt/index', 'PharmacyPurchaseReceipt', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22341, '收货查询', 'pharmacy:purchase:receipt:query',  3, 10, 22340, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22342, '收货创建', 'pharmacy:purchase:receipt:create', 3, 20, 22340, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22343, '收货更新', 'pharmacy:purchase:receipt:update', 3, 30, 22340, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22344, '收货删除', 'pharmacy:purchase:receipt:delete', 3, 40, 22340, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22345, '收货导出', 'pharmacy:purchase:receipt:export', 3, 50, 22340, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22346, '收货提交', 'pharmacy:purchase:receipt:submit', 3, 60, 22340, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22347, '收货入账', 'pharmacy:purchase:receipt:post',   3, 70, 22340, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22348, '收货作废', 'pharmacy:purchase:receipt:void',   3, 80, 22340, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 9. 角色菜单绑定：超管（tenant 1 / role 1）
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` BETWEEN 22330 AND 22348;
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, 22330, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22331, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22332, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22333, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22334, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22335, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22336, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22337, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22338, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22339, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22340, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22341, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22342, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22343, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22344, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22345, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22346, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22347, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22348, 1, '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 10. 角色菜单绑定：FirstSun 租户管理员（tenant 163 / role 167，共享账号 0407）
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 163 AND `role_id` = 167 AND `menu_id` BETWEEN 22330 AND 22348;
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (167, 22330, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22331, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22332, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22333, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22334, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22335, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22336, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22337, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22338, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22339, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22340, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22341, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22342, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22343, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22344, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22345, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22346, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22347, 163, 'system', NOW(), 'system', NOW(), b'0'),
  (167, 22348, 163, 'system', NOW(), 'system', NOW(), b'0');

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
