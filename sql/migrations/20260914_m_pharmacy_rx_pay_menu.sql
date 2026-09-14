SET NAMES utf8mb4;

-- E 处方与统一支付模块菜单与权限。处方登记、药师审核、处方台账、支付单与退款单查询。

INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (61200, '处方支付管理', '', 1, 30, 0, 'pharmacy-rx', 'ep:first-aid-kit', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61210, '处方登记', '', 2, 10, 61200, 'prescription/create', '', 'pharmacy/prescription/create', 'PharmacyPrescriptionCreate', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61220, '处方审核', '', 2, 20, 61200, 'prescription/review', '', 'pharmacy/prescription/review', 'PharmacyPrescriptionReview', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61230, '处方台账', '', 2, 30, 61200, 'prescription/index', '', 'pharmacy/prescription/index', 'PharmacyPrescriptionIndex', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61240, '支付单查询', '', 2, 40, 61200, 'pay/index', '', 'pharmacy/pay/index', 'PharmacyPayIndex', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61250, '退款单查询', '', 2, 50, 61200, 'pay/refund', '', 'pharmacy/pay/refund', 'PharmacyPayRefund', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
  `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`),
  `icon` = VALUES(`icon`), `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`), `status` = VALUES(`status`),
  `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);

INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (61211, '登记创建', 'pharmacy:prescription:create', 3, 10, 61210, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61221, '审核操作', 'pharmacy:prescription:review', 3, 10, 61220, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61231, '台账查询', 'pharmacy:prescription:query', 3, 10, 61230, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61232, '台账作废', 'pharmacy:prescription:update', 3, 20, 61230, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61241, '支付单查询', 'pay:order:query', 3, 10, 61240, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (61251, '退款单查询', 'pay:refund:query', 3, 10, 61250, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
  `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`),
  `icon` = VALUES(`icon`), `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`), `status` = VALUES(`status`),
  `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);

DELETE FROM `system_role_menu`
WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` BETWEEN 61200 AND 61259;

INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, `id`, 1, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE `id` BETWEEN 61200 AND 61259 AND `deleted` = b'0';

-- 同步 FirstSun 租户管理员（role_id=167）权限：与 20260910_i_firstsun_shared_account.sql 的段规则一致
DELETE FROM `system_role_menu`
WHERE `tenant_id` = 163 AND `role_id` = 167 AND `menu_id` BETWEEN 61200 AND 61259;

INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 167, `id`, 163, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE `id` BETWEEN 61200 AND 61259 AND `deleted` = b'0';
