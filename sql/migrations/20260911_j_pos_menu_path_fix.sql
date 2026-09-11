SET NAMES utf8mb4;

-- 修复药店业务与 POS 一级菜单使用相同 /pharmacy 路径导致的动态路由冲突。
UPDATE system_menu
SET path = '/pharmacy-pos', updater = 'system', update_time = CURRENT_TIMESTAMP
WHERE id = 61000 AND path <> '/pharmacy-pos';
