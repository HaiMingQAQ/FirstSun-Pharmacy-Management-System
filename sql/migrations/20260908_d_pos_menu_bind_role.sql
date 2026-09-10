-- ============================================================
-- FirstSun 药店管理系统 | D 成员增量脚本
-- 20260908_d_pos_menu_bind_role.sql
-- 说明：将 POS 菜单（61000-61015）绑定到 super_admin 角色（role_id=1）
-- 前置：已执行 20260908_d_pos_menu.sql（菜单存在）
-- 幂等：NOT EXISTS 防重复；重复执行安全
-- 注意：绑定后需重启后端（或刷新权限缓存）才会在管理端菜单树生效
-- 执行方式：docker cp 进容器后 source（避免宿主管道中文编码问题）
-- ============================================================
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, m.id, 1, 'admin', NOW(), 'admin', NOW(), b'0'
FROM `system_menu` m
WHERE m.id BETWEEN 61000 AND 61015
  AND NOT EXISTS (
      SELECT 1 FROM `system_role_menu` rm
      WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = 0
  );
