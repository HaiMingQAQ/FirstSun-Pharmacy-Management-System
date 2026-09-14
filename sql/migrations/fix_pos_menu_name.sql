SET NAMES utf8mb4;

-- 修正 POS 菜单 name（之前经 PowerShell 管道执行时中文被双重编码）
-- 执行方式：docker cp 本文件进容器后 source（避免宿主管道编码转换）
UPDATE `system_menu` SET `name` = '药店POS管理' WHERE `id` = 61000;
UPDATE `system_menu` SET `name` = '收银台' WHERE `id` = 61001;
UPDATE `system_menu` SET `name` = '收银' WHERE `id` = 61002;
UPDATE `system_menu` SET `name` = '销售单' WHERE `id` = 61003;
UPDATE `system_menu` SET `name` = '销售查询' WHERE `id` = 61004;
UPDATE `system_menu` SET `name` = '销售单详情' WHERE `id` = 61005;
UPDATE `system_menu` SET `name` = '退货单' WHERE `id` = 61006;
UPDATE `system_menu` SET `name` = '退货创建' WHERE `id` = 61007;
UPDATE `system_menu` SET `name` = '退货查询' WHERE `id` = 61008;
UPDATE `system_menu` SET `name` = '收银班次' WHERE `id` = 61009;
UPDATE `system_menu` SET `name` = '开台/交班' WHERE `id` = 61010;
UPDATE `system_menu` SET `name` = '班次查询' WHERE `id` = 61011;
UPDATE `system_menu` SET `name` = '支付明细' WHERE `id` = 61012;
UPDATE `system_menu` SET `name` = '支付查询' WHERE `id` = 61013;
UPDATE `system_menu` SET `name` = '销售统计' WHERE `id` = 61014;
UPDATE `system_menu` SET `name` = '统计查询' WHERE `id` = 61015;
