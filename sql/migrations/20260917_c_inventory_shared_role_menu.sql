SET NAMES utf8mb4;

-- C-1: register the movement button and authorize C inventory menus for the
-- FirstSun shared tenant administrator (tenant 163 / role 167).
INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22229, '上架移位', 'pharmacy:inventory-movement:execute', 3, 280, 22201, '', '', NULL, NULL,
   0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
  `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `status` = VALUES(`status`),
  `visible` = VALUES(`visible`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`),
  `deleted` = VALUES(`deleted`);

-- The C menu is visible only after both role and tenant-package authorization.
DELETE FROM `system_role_menu`
WHERE `tenant_id` = 163 AND `role_id` = 167 AND `menu_id` BETWEEN 22200 AND 22229;

INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 167, `id`, 163, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE `id` BETWEEN 22200 AND 22229 AND `deleted` = b'0';

UPDATE `system_tenant_package` p
JOIN (
  SELECT CAST(JSON_ARRAYAGG(`id`) AS CHAR) AS menu_ids
  FROM `system_menu`
  WHERE `deleted` = b'0' AND (`id` BETWEEN 22000 AND 22999 OR `id` BETWEEN 61000 AND 61999)
) m
SET p.`menu_ids` = m.menu_ids, p.`updater` = '1', p.`update_time` = NOW()
WHERE p.`id` = 114 AND p.`deleted` = b'0';
