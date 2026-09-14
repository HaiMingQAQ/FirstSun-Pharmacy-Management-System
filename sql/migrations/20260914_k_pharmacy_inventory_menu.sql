SET NAMES utf8mb4;

-- C 库存模块菜单与权限。库存台账、效期、盘点和报损均从仓库管理页进入。

INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22200, '库存管理', '', 1, 20, 22000, 'inventory', 'ep:box', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22201, '仓库管理', '', 2, 10, 22200, 'warehouse', 'ep:house', 'pharmacy/inventory/warehouse/index', 'PharmacyInventoryWarehouse', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
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
  (22202, '仓库查询', 'pharmacy:inventory-warehouse:query', 3, 10, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22203, '仓库新增', 'pharmacy:inventory-warehouse:create', 3, 20, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22204, '仓库更新', 'pharmacy:inventory-warehouse:update', 3, 30, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22205, '仓库删除', 'pharmacy:inventory-warehouse:delete', 3, 40, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22206, '货位查询', 'pharmacy:inventory-location:query', 3, 50, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22207, '货位新增', 'pharmacy:inventory-location:create', 3, 60, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22208, '货位更新', 'pharmacy:inventory-location:update', 3, 70, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22209, '货位删除', 'pharmacy:inventory-location:delete', 3, 80, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22210, '批次库存查询', 'pharmacy:inventory-batch:query', 3, 90, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22211, '库存流水查询', 'pharmacy:inventory-flow:query', 3, 100, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22212, '效期查询', 'pharmacy:inventory-expiry:query', 3, 110, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22213, '效期刷新', 'pharmacy:inventory-expiry:refresh', 3, 120, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22214, '效期处理', 'pharmacy:inventory-expiry:handle', 3, 130, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22215, '库存核对查询', 'pharmacy:inventory-reconciliation:query', 3, 140, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22216, '库存核对导出', 'pharmacy:inventory-reconciliation:export', 3, 150, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22217, '盘点查询', 'pharmacy:inventory-stocktake:query', 3, 160, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22218, '盘点新增', 'pharmacy:inventory-stocktake:create', 3, 170, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22219, '盘点更新', 'pharmacy:inventory-stocktake:update', 3, 180, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22220, '盘点审批', 'pharmacy:inventory-stocktake:approve', 3, 190, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22221, '盘点取消', 'pharmacy:inventory-stocktake:cancel', 3, 200, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22222, '报损查询', 'pharmacy:inventory-damage:query', 3, 210, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22223, '报损新增', 'pharmacy:inventory-damage:create', 3, 220, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22224, '报损更新', 'pharmacy:inventory-damage:update', 3, 230, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22225, '报损审批', 'pharmacy:inventory-damage:approve', 3, 240, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22226, '报损复核', 'pharmacy:inventory-damage:review', 3, 250, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22227, '报损执行', 'pharmacy:inventory-damage:execute', 3, 260, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22228, '报损取消', 'pharmacy:inventory-damage:cancel', 3, 270, 22201, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`),
  `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`),
  `icon` = VALUES(`icon`), `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`), `status` = VALUES(`status`),
  `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`), `deleted` = VALUES(`deleted`);

DELETE FROM `system_role_menu`
WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` BETWEEN 22200 AND 22228;

INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, `id`, 1, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE `id` BETWEEN 22200 AND 22228 AND `deleted` = b'0';
