-- ============================================================
-- 迁移：20260909_f_pharmacy_menu_path_fix.sql
-- 作者：A 成员（基础资料与公共平台）
-- 目的：修复 22010「基础资料」二级目录 path 带前导 /
--       导致 vue-router 路由拼接丢失父路径 /pharmacy 的问题。
-- 根因：22010.path = '/base'（前导/覆盖父级），应改为 'base'
--       使最终路由为 /pharmacy/base/category，而非 /base/category。
-- 幂等：ON DUPLICATE KEY UPDATE 保证重复执行不产生副作用。
-- ============================================================

-- 1. 修复 22010「基础资料」path：'/base' -> 'base'
UPDATE `system_menu` SET `path` = 'base' WHERE `id` = 22010 AND `path` = '/base';
