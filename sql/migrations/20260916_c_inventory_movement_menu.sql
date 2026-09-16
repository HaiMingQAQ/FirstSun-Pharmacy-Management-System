SET NAMES utf8mb4;

-- C/P1: same-warehouse location movement permission.
-- No table change: ph_inv_flow already carries source/target flows under its existing unique event key.
INSERT INTO system_menu
  (id, name, permission, type, sort, parent_id, path, icon, component, component_name,
   status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (22229, '上架移位', 'pharmacy:inventory-movement:execute', 3, 280, 22201, '', '', NULL, NULL,
   0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  name = VALUES(name), permission = VALUES(permission), type = VALUES(type), sort = VALUES(sort),
  parent_id = VALUES(parent_id), status = VALUES(status), visible = VALUES(visible),
  updater = VALUES(updater), update_time = VALUES(update_time), deleted = VALUES(deleted);

DELETE FROM system_role_menu WHERE tenant_id = 1 AND role_id = 1 AND menu_id = 22229;
INSERT INTO system_role_menu (role_id, menu_id, tenant_id, creator, create_time, updater, update_time, deleted)
VALUES (1, 22229, 1, '1', NOW(), '1', NOW(), b'0');
