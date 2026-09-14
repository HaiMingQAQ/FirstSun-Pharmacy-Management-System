SET NAMES utf8mb4;

-- 修复药品条码菜单图标：Iconify 的 Element Plus 集合不存在 ep:barcode。
UPDATE `system_menu`
SET `icon` = 'fa:barcode',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 22050
  AND `deleted` = b'0';
