SET NAMES utf8mb4;

-- =============================================================
-- 迁移脚本：A 成员药店模块-药品字典 + 药品菜单
-- 文件：20260908_d_pharmacy_drug_menu.sql
-- 说明：
--   1. 仅新增药品所需字典与菜单，不修改已有数据。
--   2. 幂等可重复执行。
--   3. 字典类型 ID：305-308；字典数据 ID：1550+。
--   4. 药品菜单 ID 段 22040+（阶段一 22000+、二 22020+、三 22030+）。
--   5. 药品菜单挂在父节点 22010（基础资料）下，与分类/门店/员工平级。
-- 作者：A 成员
-- 日期：2026-09-08
-- =============================================================

-- -------------------------------------------------------------
-- 1. 字典类型
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (305, '药品类型',        'pharmacy_drug_type',          0, '药品类型：0处方/1OTC甲/2OTC乙/3特管/4饮片/5保健/6器械/7日化/8其他', '1', NOW(), '1', NOW(), b'0'),
  (306, '医保类别',        'pharmacy_insurance_type',     0, '医保类别：0自费/1甲类/2乙类', '1', NOW(), '1', NOW(), b'0'),
  (307, '储存条件',        'pharmacy_storage_cond',        0, '储存条件：0常温/1阴凉/2冷藏/3冷冻', '1', NOW(), '1', NOW(), b'0'),
  (308, '药品审核状态',    'pharmacy_drug_approve_status', 0, '药品审核状态：0待审/1通过/2驳回', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 2. 字典数据：pharmacy_drug_type
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_drug_type';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1550, 1, '处方药',  '0', 'pharmacy_drug_type', 0, 'danger',  '', '', '1', NOW(), '1', NOW(), b'0'),
  (1551, 2, 'OTC甲',  '1', 'pharmacy_drug_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1552, 3, 'OTC乙',  '2', 'pharmacy_drug_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1553, 4, '特管',    '3', 'pharmacy_drug_type', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1554, 5, '饮片',    '4', 'pharmacy_drug_type', 0, 'info',    '', '', '1', NOW(), '1', NOW(), b'0'),
  (1555, 6, '保健',    '5', 'pharmacy_drug_type', 0, 'info',    '', '', '1', NOW(), '1', NOW(), b'0'),
  (1556, 7, '器械',    '6', 'pharmacy_drug_type', 0, 'info',    '', '', '1', NOW(), '1', NOW(), b'0'),
  (1557, 8, '日化',    '7', 'pharmacy_drug_type', 0, 'info',    '', '', '1', NOW(), '1', NOW(), b'0'),
  (1558, 9, '其他',    '8', 'pharmacy_drug_type', 0, 'info',    '', '', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 3. 字典数据：pharmacy_insurance_type
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_insurance_type';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1560, 1, '自费', '0', 'pharmacy_insurance_type', 0, 'info',    '', '', '1', NOW(), '1', NOW(), b'0'),
  (1561, 2, '甲类', '1', 'pharmacy_insurance_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1562, 3, '乙类', '2', 'pharmacy_insurance_type', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 4. 字典数据：pharmacy_storage_cond
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_storage_cond';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1570, 1, '常温', '0', 'pharmacy_storage_cond', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1571, 2, '阴凉', '1', 'pharmacy_storage_cond', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1572, 3, '冷藏', '2', 'pharmacy_storage_cond', 0, 'info',    '', '', '1', NOW(), '1', NOW(), b'0'),
  (1573, 4, '冷冻', '3', 'pharmacy_storage_cond', 0, 'danger',  '', '', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 5. 字典数据：pharmacy_drug_approve_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_drug_approve_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1580, 1, '待审', '0', 'pharmacy_drug_approve_status', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1581, 2, '通过', '1', 'pharmacy_drug_approve_status', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
  (1582, 3, '驳回', '2', 'pharmacy_drug_approve_status', 0, 'danger',  '', '', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 6. 菜单：药品档案（菜单项 + 6 个按钮权限，含审核）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22040, '药品档案', '', 2, 40, 22010, 'drug', 'ep:first-aid-kit', 'pharmacy/base/drug/index', 'PharmacyBaseDrug', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22041, '药品查询', 'pharmacy:base:drug:query',   3, 10, 22040, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22042, '药品创建', 'pharmacy:base:drug:create',  3, 20, 22040, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22043, '药品更新', 'pharmacy:base:drug:update',  3, 30, 22040, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22044, '药品删除', 'pharmacy:base:drug:delete',  3, 40, 22040, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22045, '药品导出', 'pharmacy:base:drug:export',  3, 50, 22040, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22046, '药品审核', 'pharmacy:base:drug:approve', 3, 60, 22040, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 7. 角色菜单关联：超管角色（role_id=1）绑定药品菜单
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` IN (22040, 22041, 22042, 22043, 22044, 22045, 22046);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, 22040, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22041, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22042, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22043, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22044, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22045, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22046, 1, '1', NOW(), '1', NOW(), b'0');
