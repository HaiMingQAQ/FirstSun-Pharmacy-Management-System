-- FirstSun 药店测试租户与团队共享开发账号。
-- 登录信息：租户 FirstSun，用户名 0407，密码 123456。

-- 系统租户不能占用药店测试租户名称。
UPDATE system_tenant SET name = '芋道源码', contact_name = '芋艿'
WHERE id = 1 AND name = 'FirstSun';

-- 套餐仅包含药店业务与 POS 菜单；菜单由前序迁移脚本创建。
INSERT INTO system_tenant_package
  (id, name, status, remark, menu_ids, creator, create_time, updater, update_time, deleted)
SELECT 114, '药店', 0, 'FirstSun 药店业务开发套餐', CAST(JSON_ARRAYAGG(id) AS CHAR),
       'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0'
FROM system_menu
WHERE deleted = b'0' AND (id BETWEEN 22000 AND 22999 OR id BETWEEN 61000 AND 61999)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status),
  remark = VALUES(remark), menu_ids = VALUES(menu_ids), deleted = VALUES(deleted);

INSERT INTO system_tenant
  (id, name, contact_user_id, contact_name, contact_mobile, status, websites,
   package_id, expire_time, account_count, creator, create_time, updater, update_time, deleted)
VALUES
  (163, 'FirstSun', 407, 'FirstSun 团队', '', 0, '', 114, '2099-12-31 23:59:59', 10,
   'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0')
ON DUPLICATE KEY UPDATE name = VALUES(name), contact_user_id = VALUES(contact_user_id),
  contact_name = VALUES(contact_name), status = VALUES(status), package_id = VALUES(package_id),
  expire_time = VALUES(expire_time), account_count = GREATEST(account_count, VALUES(account_count)),
  deleted = VALUES(deleted);

INSERT INTO system_dept
  (id, name, parent_id, sort, leader_user_id, phone, email, status,
   creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (407, 'FirstSun 演示门店', 0, 1, 407, '', '', 0,
   'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 163)
ON DUPLICATE KEY UPDATE name = VALUES(name), leader_user_id = VALUES(leader_user_id),
  status = VALUES(status), deleted = VALUES(deleted), tenant_id = VALUES(tenant_id);

INSERT INTO system_role
  (id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
   creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (167, '租户管理员', 'tenant_admin', 0, 1, '', 0, 1, 'FirstSun 药店套餐管理员',
   'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 163)
ON DUPLICATE KEY UPDATE name = VALUES(name), code = VALUES(code), status = VALUES(status),
  remark = VALUES(remark), deleted = VALUES(deleted), tenant_id = VALUES(tenant_id);

INSERT INTO system_users
  (id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex,
   avatar, status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (407, '0407', '$2a$10$isML7f7uwSNe.bBfaN7XFet7uzGqIRCNfTON98YRtQGn0NhpSjo9S',
   'FirstSun', '团队共享开发账号', 407, NULL, '', '', 0, '', 0, '', NULL,
   'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 163)
ON DUPLICATE KEY UPDATE username = VALUES(username), password = VALUES(password),
  nickname = VALUES(nickname), remark = VALUES(remark), dept_id = VALUES(dept_id),
  status = VALUES(status), deleted = VALUES(deleted), tenant_id = VALUES(tenant_id);

-- 清除错误的系统超级管理员绑定，仅保留药店租户角色。
DELETE FROM system_user_role WHERE user_id = 407;
INSERT INTO system_user_role
  (user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (407, 167, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 163);

-- 套餐变化后同步租户管理员角色权限。
DELETE FROM system_role_menu WHERE role_id = 167;
INSERT INTO system_role_menu
  (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 167, id, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 163
FROM system_menu
WHERE deleted = b'0' AND (id BETWEEN 22000 AND 22999 OR id BETWEEN 61000 AND 61999);

INSERT INTO ph_store
  (id, store_code, store_name, address, phone, manage_scope, license_no, license_expire,
   is_medical, business_hours, status, creator, create_time, updater, update_time,
   deleted, tenant_id, dept_id)
VALUES
  (407, 'FS-DEMO', 'FirstSun 演示门店', '团队本地开发环境', NULL, NULL, NULL, NULL,
   0, '08:00-22:00', 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP,
   b'0', 163, 407)
ON DUPLICATE KEY UPDATE store_name = VALUES(store_name), status = VALUES(status),
  deleted = VALUES(deleted), tenant_id = VALUES(tenant_id), dept_id = VALUES(dept_id);

INSERT INTO ph_employee
  (id, emp_no, emp_name, phone, store_id, position, pharmacist_no, license_expire,
   health_cert_expire, hire_date, status, creator, create_time, updater, update_time,
   deleted, tenant_id, user_id)
VALUES
  (407, 'FS407', 'FirstSun', '', 407, 9, NULL, NULL, NULL, CURRENT_DATE, 1,
   'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 163, 407)
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name), store_id = VALUES(store_id),
  position = VALUES(position), status = VALUES(status), deleted = VALUES(deleted),
  tenant_id = VALUES(tenant_id), user_id = VALUES(user_id);
