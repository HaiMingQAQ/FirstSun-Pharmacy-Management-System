-- =============================================================
-- 迁移脚本：F 成员药店模块-会员管理菜单与字典
-- 文件：20260911_f_pharmacy_member_menu.sql
-- 说明：
--   1. 仅新增会员管理所需字典与菜单，不修改已有数据。
--   2. 幂等可重复执行。
--   3. 字典类型 ID：310-315；字典数据 ID：1600+。
--   4. 会员菜单 ID 段 22060+（基础资料 22010+、药品 22040+、条码 22050+）。
--   5. 会员管理挂在父节点 22000（药店业务）下，与基础资料平级。
-- 作者：F 成员
-- 日期：2026-09-11
-- =============================================================

-- -------------------------------------------------------------
-- 1. 字典类型
-- -------------------------------------------------------------
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (310, '会员状态',           'pharmacy_member_status',           0, '会员状态：1启用 / 0禁用', '1', NOW(), '1', NOW(), b'0'),
  (311, '会员等级状态',       'pharmacy_member_level_status',     0, '会员等级状态：1启用 / 0禁用', '1', NOW(), '1', NOW(), b'0'),
  (312, '积分业务类型',       'pharmacy_member_point_biz_type',   0, '积分业务类型：1注册赠送/2消费获得/3消费抵扣/4管理员调整/5签到/6退款冲回', '1', NOW(), '1', NOW(), b'0'),
  (313, '小程序订单状态',     'pharmacy_wx_order_status',         0, '订单状态：0待支付/1待拣货/2拣货中/3待自提/4完成/-1取消', '1', NOW(), '1', NOW(), b'0'),
  (314, '小程序订单类型',     'pharmacy_wx_order_type',           0, '订单类型：0到店自提/1同城配送', '1', NOW(), '1', NOW(), b'0'),
  (315, '小程序支付状态',     'pharmacy_wx_pay_status',           0, '支付状态：0待支付/1已支付/2已退款', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `status` = VALUES(`status`), `remark` = VALUES(`remark`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 2. 字典数据：pharmacy_member_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_member_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1600, 1, '启用', '1', 'pharmacy_member_status', 0, 'success', '', '会员启用', '1', NOW(), '1', NOW(), b'0'),
  (1601, 2, '禁用', '0', 'pharmacy_member_status', 0, 'info',    '', '会员禁用', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 3. 字典数据：pharmacy_member_level_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_member_level_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1610, 1, '启用', '1', 'pharmacy_member_level_status', 0, 'success', '', '等级启用', '1', NOW(), '1', NOW(), b'0'),
  (1611, 2, '禁用', '0', 'pharmacy_member_level_status', 0, 'info',    '', '等级禁用', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 4. 字典数据：pharmacy_member_point_biz_type
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_member_point_biz_type';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1620, 1, '注册赠送', '1', 'pharmacy_member_point_biz_type', 0, 'success', '', '新会员注册赠送', '1', NOW(), '1', NOW(), b'0'),
  (1621, 2, '消费获得', '2', 'pharmacy_member_point_biz_type', 0, 'primary', '', '消费订单获得积分', '1', NOW(), '1', NOW(), b'0'),
  (1622, 3, '消费抵扣', '3', 'pharmacy_member_point_biz_type', 0, 'warning', '', '积分抵扣现金', '1', NOW(), '1', NOW(), b'0'),
  (1623, 4, '管理员调整', '4', 'pharmacy_member_point_biz_type', 0, 'info', '', '后台管理员调整', '1', NOW(), '1', NOW(), b'0'),
  (1624, 5, '每日签到', '5', 'pharmacy_member_point_biz_type', 0, 'success', '', '每日签到获得', '1', NOW(), '1', NOW(), b'0'),
  (1625, 6, '退款冲回', '6', 'pharmacy_member_point_biz_type', 0, 'danger',  '', '销售退款冲回积分', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 5. 字典数据：pharmacy_wx_order_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_wx_order_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1630, 1, '待支付', '0', 'pharmacy_wx_order_status', 0, 'warning', '', '订单待支付', '1', NOW(), '1', NOW(), b'0'),
  (1631, 2, '待拣货', '1', 'pharmacy_wx_order_status', 0, 'primary', '', '已支付待拣货', '1', NOW(), '1', NOW(), b'0'),
  (1632, 3, '拣货中', '2', 'pharmacy_wx_order_status', 0, 'primary', '', '拣货进行中',   '1', NOW(), '1', NOW(), b'0'),
  (1633, 4, '待自提', '3', 'pharmacy_wx_order_status', 0, 'success', '', '拣货完成待取货', '1', NOW(), '1', NOW(), b'0'),
  (1634, 5, '已完成', '4', 'pharmacy_wx_order_status', 0, 'success', '', '订单已完成',   '1', NOW(), '1', NOW(), b'0'),
  (1635, 6, '已取消', '-1', 'pharmacy_wx_order_status', 0, 'info',    '', '订单已取消',   '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 6. 字典数据：pharmacy_wx_order_type
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_wx_order_type';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1640, 1, '到店自提', '0', 'pharmacy_wx_order_type', 0, 'success', '', '到店自提', '1', NOW(), '1', NOW(), b'0'),
  (1641, 2, '同城配送', '1', 'pharmacy_wx_order_type', 0, 'primary', '', '同城配送', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 7. 字典数据：pharmacy_wx_pay_status
-- -------------------------------------------------------------
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_wx_pay_status';
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1650, 1, '待支付', '0', 'pharmacy_wx_pay_status', 0, 'warning', '', '待支付', '1', NOW(), '1', NOW(), b'0'),
  (1651, 2, '已支付', '1', 'pharmacy_wx_pay_status', 0, 'success', '', '已支付', '1', NOW(), '1', NOW(), b'0'),
  (1652, 3, '已退款', '2', 'pharmacy_wx_pay_status', 0, 'danger',  '', '已退款', '1', NOW(), '1', NOW(), b'0');

-- -------------------------------------------------------------
-- 8. 菜单：会员管理（二级目录）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22060, '会员管理', '', 1, 30, 22000, '/member', 'ep:user', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 9. 菜单：会员档案（菜单项 + 5 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22061, '会员档案', '', 2, 10, 22060, 'user', 'ep:user', 'pharmacy/member/index', 'PharmacyMemberUser', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22062, '会员查询', 'pharmacy:member:user:query',   3, 10, 22061, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22063, '会员创建', 'pharmacy:member:user:create',  3, 20, 22061, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22064, '会员更新', 'pharmacy:member:user:update',  3, 30, 22061, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22065, '会员删除', 'pharmacy:member:user:delete',  3, 40, 22061, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22066, '会员导出', 'pharmacy:member:user:export',  3, 50, 22061, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 10. 菜单：会员等级（菜单项 + 6 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22071, '会员等级', '', 2, 20, 22060, 'level', 'ep:medal', 'pharmacy/member/level', 'PharmacyMemberLevel', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22072, '等级查询', 'pharmacy:member:level:query',   3, 10, 22071, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22073, '等级创建', 'pharmacy:member:level:create',  3, 20, 22071, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22074, '等级更新', 'pharmacy:member:level:update',  3, 30, 22071, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22075, '等级删除', 'pharmacy:member:level:delete',  3, 40, 22071, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22076, '等级导出', 'pharmacy:member:level:export',  3, 50, 22071, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 11. 菜单：积分明细（菜单项 + 2 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22081, '积分明细', '', 2, 30, 22060, 'point-record', 'ep:coin', 'pharmacy/member/point-record', 'PharmacyMemberPointRecord', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22082, '积分查询', 'pharmacy:member:point-record:query',  3, 10, 22081, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22083, '积分导出', 'pharmacy:member:point-record:export', 3, 20, 22081, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 12. 菜单：收货地址（菜单项 + 3 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22091, '收货地址', '', 2, 40, 22060, 'address', 'ep:location', 'pharmacy/member/address', 'PharmacyMemberAddress', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22092, '地址查询', 'pharmacy:member:address:query',  3, 10, 22091, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22093, '地址新增', 'pharmacy:member:address:create', 3, 20, 22091, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22094, '地址删除', 'pharmacy:member:address:delete', 3, 30, 22091, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 13. 菜单：购物车管理（菜单项 + 3 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22101, '购物车管理', '', 2, 50, 22060, 'cart', 'ep:shopping-cart', 'pharmacy/member/cart', 'PharmacyMemberCart', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22102, '购物车查询', 'pharmacy:member:cart:query',  3, 10, 22101, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22103, '购物车新增', 'pharmacy:member:cart:create', 3, 20, 22101, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22104, '购物车删除', 'pharmacy:member:cart:delete', 3, 30, 22101, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 14. 菜单：小程序订单（菜单项 + 5 个按钮权限）
-- -------------------------------------------------------------
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22111, '小程序订单', '', 2, 60, 22060, 'order', 'ep:document', 'pharmacy/member/order', 'PharmacyMemberOrder', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22112, '订单查询',   'pharmacy:member:order:query',   3, 10, 22111, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22113, '订单创建',   'pharmacy:member:order:create',  3, 20, 22111, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22114, '订单更新',   'pharmacy:member:order:update',  3, 30, 22111, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22115, '订单取消',   'pharmacy:member:order:cancel',  3, 40, 22111, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (22116, '订单核销',   'pharmacy:member:order:verify',  3, 50, 22111, '', '', NULL, NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `permission` = VALUES(`permission`), `type` = VALUES(`type`), `sort` = VALUES(`sort`), `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `icon` = VALUES(`icon`), `component` = VALUES(`component`), `component_name` = VALUES(`component_name`), `status` = VALUES(`status`), `visible` = VALUES(`visible`), `keep_alive` = VALUES(`keep_alive`), `always_show` = VALUES(`always_show`), `updater` = VALUES(`updater`), `update_time` = VALUES(`update_time`);

-- -------------------------------------------------------------
-- 15. 角色菜单关联：超管角色（role_id=1）绑定会员管理菜单
-- -------------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `tenant_id` = 1 AND `role_id` = 1 AND `menu_id` IN (
  22060,
  22061, 22062, 22063, 22064, 22065, 22066,
  22071, 22072, 22073, 22074, 22075, 22076,
  22081, 22082, 22083,
  22091, 22092, 22093, 22094,
  22101, 22102, 22103, 22104,
  22111, 22112, 22113, 22114, 22115, 22116
);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (1, 22060, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22061, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22062, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22063, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22064, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22065, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22066, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22071, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22072, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22073, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22074, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22075, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22076, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22081, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22082, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22083, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22091, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22092, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22093, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22094, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22101, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22102, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22103, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22104, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22111, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22112, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22113, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22114, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22115, 1, '1', NOW(), '1', NOW(), b'0'),
  (1, 22116, 1, '1', NOW(), '1', NOW(), b'0');
