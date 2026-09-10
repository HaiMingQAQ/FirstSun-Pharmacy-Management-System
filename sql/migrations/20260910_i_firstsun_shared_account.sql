-- FirstSun 团队共享开发账号。
-- 登录信息：租户 FirstSun，用户名 407，密码 123456。

UPDATE system_tenant
SET name = 'FirstSun',
    contact_name = 'FirstSun 团队'
WHERE id = 1;

INSERT INTO system_users (
  id, username, password, nickname, remark, dept_id, post_ids,
  email, mobile, sex, avatar, status, login_ip, login_date,
  creator, create_time, updater, update_time, deleted, tenant_id
) VALUES (
  407, '407', '$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG',
  'FirstSun', '团队共享开发账号', 103, NULL,
  '', '', 0, '', 0, '', NULL,
  'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 1
) ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password = VALUES(password),
  nickname = VALUES(nickname),
  remark = VALUES(remark),
  status = VALUES(status),
  deleted = VALUES(deleted),
  tenant_id = VALUES(tenant_id);

DELETE FROM system_user_role
WHERE user_id = 407 AND role_id = 1 AND tenant_id = 1;

INSERT INTO system_user_role (
  user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
) VALUES (
  407, 1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 1
);

INSERT INTO ph_store (
  id, store_code, store_name, address, phone, manage_scope,
  license_no, license_expire, is_medical, business_hours, status,
  creator, create_time, updater, update_time, deleted, tenant_id, dept_id
) VALUES (
  407, 'FS-DEMO', 'FirstSun 演示门店', '团队本地开发环境', NULL, NULL,
  NULL, NULL, 0, '08:00-22:00', 1,
  'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 1, 103
) ON DUPLICATE KEY UPDATE
  store_name = VALUES(store_name),
  status = VALUES(status),
  deleted = VALUES(deleted),
  tenant_id = VALUES(tenant_id);

INSERT INTO ph_employee (
  id, emp_no, emp_name, phone, store_id, position,
  pharmacist_no, license_expire, health_cert_expire, hire_date, status,
  creator, create_time, updater, update_time, deleted, tenant_id, user_id
) VALUES (
  407, 'FS407', 'FirstSun', '', 407, 9,
  NULL, NULL, NULL, CURRENT_DATE, 1,
  'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, b'0', 1, 407
) ON DUPLICATE KEY UPDATE
  emp_name = VALUES(emp_name),
  store_id = VALUES(store_id),
  position = VALUES(position),
  status = VALUES(status),
  deleted = VALUES(deleted),
  tenant_id = VALUES(tenant_id),
  user_id = VALUES(user_id);
