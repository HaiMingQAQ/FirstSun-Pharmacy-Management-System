-- F 会员模块：补齐「地址编辑」菜单权限
-- 背景：后端 MemberAddressController 的 /update 接口使用 @PreAuthorize("pharmacy:member:address:update")，
--       但 20260911 的会员菜单迁移里只注册了 address 的 query / create / delete，
--       导致「编辑地址」即使前端有入口也必然 403。本迁移补齐该权限点并授予租户 1 的管理员角色。
-- 幂等：先删后插，脚本可重复执行。
SET NAMES utf8mb4;

DELETE FROM `system_menu` WHERE `id` = 22095;
INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22095, '地址更新', 'pharmacy:member:address:update', 3, 25, 22091, '', '', NULL, NULL,
   0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` = 22095;
INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
  (1, 22095, '1', NOW(), '1', NOW(), b'0', 1);

-- 说明：只涉及 F 会员模块的菜单权限数据，不改动其它模块。
