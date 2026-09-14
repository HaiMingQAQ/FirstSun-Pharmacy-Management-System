SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

-- FirstSun tenant (163) linked demo data for every pharmacy menu.
-- Fixed IDs and business numbers make this script safe to run repeatedly.

-- Base data: categories, employees, warehouses and locations.
INSERT INTO ph_category
  (id, cat_code, cat_name, parent_id, cat_type, sort, status, creator, updater, deleted, tenant_id)
VALUES
  (163001, 'FS-CAT-OTC', '常用药品', 0, 0, 10, 1, 'demo', 'demo', b'0', 163),
  (163002, 'FS-CAT-COLD', '感冒用药', 163001, 0, 11, 1, 'demo', 'demo', b'0', 163),
  (163003, 'FS-CAT-DIGEST', '消化系统用药', 163001, 0, 12, 1, 'demo', 'demo', b'0', 163),
  (163004, 'FS-CAT-VITAMIN', '维生素与保健', 0, 1, 20, 1, 'demo', 'demo', b'0', 163),
  (163005, 'FS-CAT-DEVICE', '家用医疗器械', 0, 2, 30, 1, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE cat_name = VALUES(cat_name), parent_id = VALUES(parent_id),
  cat_type = VALUES(cat_type), sort = VALUES(sort), status = VALUES(status), deleted = b'0', tenant_id = 163;

INSERT INTO ph_employee
  (id, emp_no, emp_name, phone, store_id, position, pharmacist_no, license_expire,
   health_cert_expire, hire_date, status, creator, updater, deleted, tenant_id, user_id)
VALUES
  (163011, 'FS-MGR-01', '林晓晴', '', 407, 1, NULL, NULL, '2027-08-31', '2024-03-18', 1, 'demo', 'demo', b'0', 163, NULL),
  (163012, 'FS-PHA-01', '陈文博', '', 407, 2, 'ZYYS-2026-001', '2028-12-31', '2027-06-30', '2023-08-01', 1, 'demo', 'demo', b'0', 163, NULL),
  (163013, 'FS-CAS-01', '周欣怡', '', 407, 3, NULL, NULL, '2027-04-30', '2025-02-10', 1, 'demo', 'demo', b'0', 163, NULL),
  (163014, 'FS-WH-01', '赵志远', '', 407, 4, NULL, NULL, '2027-05-31', '2024-11-06', 1, 'demo', 'demo', b'0', 163, NULL),
  (163015, 'FS-BUY-01', '苏雨桐', '', 407, 5, NULL, NULL, '2027-07-31', '2025-06-16', 1, 'demo', 'demo', b'0', 163, NULL)
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name), store_id = 407, position = VALUES(position),
  status = 1, deleted = b'0', tenant_id = 163;

INSERT INTO ph_warehouse
  (id, store_id, wh_code, wh_name, temp_zone, is_default, status, creator, updater, deleted, tenant_id)
VALUES
  (163021, 407, 'FS-WH-NORMAL', '中心店常温库', 0, 1, 1, 'demo', 'demo', b'0', 163),
  (163022, 407, 'FS-WH-COOL', '中心店阴凉库', 1, 0, 1, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE wh_name = VALUES(wh_name), temp_zone = VALUES(temp_zone),
  is_default = VALUES(is_default), status = 1, deleted = b'0', tenant_id = 163;

INSERT INTO ph_location
  (id, warehouse_id, location_code, location_type, max_capacity, status, last_use_at,
   creator, updater, deleted, tenant_id)
VALUES
  (163031, 163021, 'A-01-01', 0, 500, 1, NOW(), 'demo', 'demo', b'0', 163),
  (163032, 163021, 'A-02-01', 1, 300, 1, NOW(), 'demo', 'demo', b'0', 163),
  (163033, 163021, 'A-03-01', 3, 200, 1, NOW(), 'demo', 'demo', b'0', 163),
  (163034, 163022, 'B-01-01', 4, 200, 1, NOW(), 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE location_type = VALUES(location_type), max_capacity = VALUES(max_capacity),
  status = 1, last_use_at = NOW(), deleted = b'0', tenant_id = 163;

-- Base data: drugs and barcodes.
INSERT INTO ph_drug
  (id, drug_code, category_id, generic_name, trade_name, spell_code, specification,
   dosage_form, manufacturer, approval_no, drug_type, is_rx, is_special,
   is_pseudoephedrine, is_cold_chain, unit, conversion_ratio, retail_price,
   member_price, cost_price, min_sale_price, tax_rate, insurance_type, min_stock,
   max_stock, storage_cond, need_expiry, default_location_id, saleable_online,
   status, remark, approve_status, audit_by, audit_at, creator, updater, deleted, tenant_id)
VALUES
  (163101, 'FS-DRUG-001', 163002, '复方氨酚烷胺胶囊', '感康', 'ffafwajn', '12粒/盒', '胶囊剂', '吉林某制药股份有限公司', '国药准字H20000001', 1, 0, 0, 0, 0, '盒', 12, 19.80, 17.80, 10.50, 15.00, 13.00, 0, 20, 300, 0, 1, 163031, 1, 1, '演示用感冒药', 1, 163012, NOW(), 'demo', 'demo', b'0', 163),
  (163102, 'FS-DRUG-002', 163002, '布洛芬缓释胶囊', '芬必得', 'blfhshjn', '0.3g*20粒/盒', '胶囊剂', '中美某制药有限公司', '国药准字H20000002', 2, 0, 0, 0, 0, '盒', 20, 32.50, 29.90, 18.20, 25.00, 13.00, 1, 15, 200, 0, 1, 163031, 1, 1, '演示用解热镇痛药', 1, 163012, NOW(), 'demo', 'demo', b'0', 163),
  (163103, 'FS-DRUG-003', 163003, '奥美拉唑肠溶胶囊', '洛赛克', 'amlzcrjn', '20mg*14粒/盒', '胶囊剂', '江苏某药业有限公司', '国药准字H20000003', 0, 1, 0, 0, 0, '盒', 14, 45.00, 41.00, 24.00, 35.00, 13.00, 2, 10, 160, 1, 1, 163032, 0, 1, '处方药演示数据', 1, 163012, NOW(), 'demo', 'demo', b'0', 163),
  (163104, 'FS-DRUG-004', 163004, '维生素C咀嚼片', '果维康', 'wsscjjp', '0.1g*60片/瓶', '片剂', '华北某制药有限公司', '国食健字G20000004', 5, 0, 0, 0, 0, '瓶', 60, 26.80, 23.80, 14.00, 20.00, 13.00, 0, 12, 180, 0, 1, 163031, 1, 1, '会员热销品', 1, 163012, NOW(), 'demo', 'demo', b'0', 163),
  (163105, 'FS-DRUG-005', 163005, '电子体温计', 'FirstCare', 'dztyj', '软头型/支', '器械', '深圳某医疗科技有限公司', '粤械注准202600005', 6, 0, 0, 0, 0, '支', 1, 39.90, 35.90, 21.50, 30.00, 13.00, 0, 8, 100, 0, 0, 163033, 1, 1, '家用器械演示数据', 1, 163012, NOW(), 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE generic_name = VALUES(generic_name), trade_name = VALUES(trade_name),
  category_id = VALUES(category_id), retail_price = VALUES(retail_price), member_price = VALUES(member_price),
  cost_price = VALUES(cost_price), default_location_id = VALUES(default_location_id),
  saleable_online = VALUES(saleable_online), status = 1, approve_status = 1,
  deleted = b'0', tenant_id = 163;

INSERT INTO ph_drug_barcode
  (id, drug_id, barcode, barcode_type, is_default, creator, updater, deleted, tenant_id)
VALUES
  (163201, 163101, '6900000163101', 0, 1, 'demo', 'demo', b'0', 163),
  (163202, 163102, '6900000163102', 0, 1, 'demo', 'demo', b'0', 163),
  (163203, 163103, '6900000163103', 0, 1, 'demo', 'demo', b'0', 163),
  (163204, 163104, 'FS163104', 1, 1, 'demo', 'demo', b'0', 163),
  (163205, 163105, '6900000163105', 0, 1, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE drug_id = VALUES(drug_id), barcode_type = VALUES(barcode_type),
  is_default = 1, deleted = b'0', tenant_id = 163;

-- Purchase data: suppliers, licenses, orders and receipts.
INSERT INTO ph_supplier
  (id, supplier_code, supplier_name, credit_code, scope_code, contact, phone, address,
   payment_terms, default_discount, approve_status, status, audit_by, audit_at,
   creator, updater, deleted, tenant_id)
VALUES
  (163301, 'FS-SUP-001', '华东医药配送有限公司', '91310000DEMO00001X', '沪AA000001', '王经理', '13800001001', '上海市浦东新区医药路18号', '月结30天', 0.98, 1, 1, 163011, NOW(), 'demo', 'demo', b'0', 163),
  (163302, 'FS-SUP-002', '康健医疗器械有限公司', '91440000DEMO00002X', '粤械经营备00002号', '李主管', '13800001002', '深圳市南山区科技园88号', '现款现货', 1.00, 1, 1, 163011, NOW(), 'demo', 'demo', b'0', 163),
  (163303, 'FS-SUP-003', '北方健康产业有限公司', '91110000DEMO00003X', '京AA000003', '赵女士', '13800001003', '北京市朝阳区健康路6号', '月结45天', 0.95, 0, 1, NULL, NULL, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE supplier_name = VALUES(supplier_name), contact = VALUES(contact),
  phone = VALUES(phone), approve_status = VALUES(approve_status), status = 1,
  deleted = b'0', tenant_id = 163;

INSERT INTO ph_supplier_license
  (id, supplier_id, license_type, license_no, issue_date, expire_date, status,
   creator, updater, deleted, tenant_id)
VALUES
  (163311, 163301, 0, '沪药经许-DEMO-001', '2024-01-01', '2028-12-31', 1, 'demo', 'demo', b'0', 163),
  (163312, 163301, 3, '91310000DEMO00001X', '2023-06-01', '2033-05-31', 1, 'demo', 'demo', b'0', 163),
  (163313, 163302, 0, '粤械经营备-DEMO-002', '2025-01-01', '2027-03-31', 1, 'demo', 'demo', b'0', 163),
  (163314, 163303, 2, 'GSP-DEMO-003', '2022-10-01', '2026-11-30', 1, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE supplier_id = VALUES(supplier_id), license_type = VALUES(license_type),
  expire_date = VALUES(expire_date), status = VALUES(status), deleted = b'0', tenant_id = 163;

INSERT INTO ph_po_order
  (id, order_no, store_id, warehouse_id, supplier_id, order_date, expect_date,
   total_qty, total_amount, discount_amount, payable_amount, status, is_auto,
   remark, audit_by, audit_at, creator, updater, deleted, tenant_id)
VALUES
  (163401, 'PO-FS-DEMO-001', 407, 163021, 163301, CURRENT_DATE - INTERVAL 8 DAY, CURRENT_DATE - INTERVAL 5 DAY, 150, 2385.00, 45.00, 2340.00, 5, 0, '已完成采购单', 163011, NOW() - INTERVAL 7 DAY, 'demo', 'demo', b'0', 163),
  (163402, 'PO-FS-DEMO-002', 407, 163022, 163301, CURRENT_DATE - INTERVAL 2 DAY, CURRENT_DATE + INTERVAL 1 DAY, 80, 1920.00, 0.00, 1920.00, 3, 1, '补充阴凉库库存', 163011, NOW() - INTERVAL 1 DAY, 'demo', 'demo', b'0', 163),
  (163403, 'PO-FS-DEMO-003', 407, 163021, 163302, CURRENT_DATE, CURRENT_DATE + INTERVAL 3 DAY, 30, 645.00, 0.00, 645.00, 1, 0, '器械采购待审批', NULL, NULL, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE supplier_id = VALUES(supplier_id), warehouse_id = VALUES(warehouse_id),
  order_date = VALUES(order_date), expect_date = VALUES(expect_date), total_qty = VALUES(total_qty),
  payable_amount = VALUES(payable_amount), status = VALUES(status), deleted = b'0', tenant_id = 163;

INSERT INTO ph_po_order_line
  (id, order_id, line_no, drug_id, order_qty, received_qty, unit_price,
   discount_rate, line_amount, remark, creator, updater, deleted, tenant_id)
VALUES
  (163411, 163401, 1, 163101, 100, 100, 10.50, 1.00, 1050.00, NULL, 'demo', 'demo', b'0', 163),
  (163412, 163401, 2, 163102, 50, 50, 18.20, 1.00, 910.00, NULL, 'demo', 'demo', b'0', 163),
  (163413, 163402, 1, 163103, 80, 0, 24.00, 1.00, 1920.00, '等待到货', 'demo', 'demo', b'0', 163),
  (163414, 163403, 1, 163105, 30, 0, 21.50, 1.00, 645.00, '待审批', 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE drug_id = VALUES(drug_id), order_qty = VALUES(order_qty),
  received_qty = VALUES(received_qty), unit_price = VALUES(unit_price),
  line_amount = VALUES(line_amount), deleted = b'0', tenant_id = 163;

INSERT INTO ph_po_receipt
  (id, receipt_no, order_id, store_id, warehouse_id, receive_by, receive_date,
   total_qty, total_amount, diff_type, is_free_receipt, status, quality_status,
   posted_at, creator, updater, deleted, tenant_id)
VALUES
  (163421, 'GR-FS-DEMO-001', 163401, 407, 163021, 163014, NOW() - INTERVAL 5 DAY, 150, 1960.00, 0, 0, 2, 1, NOW() - INTERVAL 5 DAY, 'demo', 'demo', b'0', 163),
  (163422, 'GR-FS-DEMO-002', NULL, 407, 163021, 163014, NOW() - INTERVAL 1 DAY, 30, 420.00, 0, 1, 1, 1, NULL, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE order_id = VALUES(order_id), total_qty = VALUES(total_qty),
  total_amount = VALUES(total_amount), status = VALUES(status), quality_status = VALUES(quality_status),
  deleted = b'0', tenant_id = 163;

INSERT INTO ph_po_receipt_line
  (id, receipt_id, line_no, order_line_id, drug_id, batch_no, manufacture_date,
   expiry_date, qty, unit_price, amount, quality_flag, qa_remark, create_batch_id,
   location_id, creator, updater, deleted, tenant_id)
VALUES
  (163431, 163421, 1, 163411, 163101, 'FSBATCH-001', CURRENT_DATE - INTERVAL 90 DAY, CURRENT_DATE + INTERVAL 540 DAY, 100, 10.50, 1050.00, 1, '验收合格', 163501, 163031, 'demo', 'demo', b'0', 163),
  (163432, 163421, 2, 163412, 163102, 'FSBATCH-002', CURRENT_DATE - INTERVAL 60 DAY, CURRENT_DATE + INTERVAL 360 DAY, 50, 18.20, 910.00, 1, '验收合格', 163502, 163031, 'demo', 'demo', b'0', 163),
  (163433, 163422, 1, NULL, 163104, 'FSBATCH-004', CURRENT_DATE - INTERVAL 120 DAY, CURRENT_DATE + INTERVAL 75 DAY, 30, 14.00, 420.00, 1, '近效期批次演示', 163504, 163034, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE qty = VALUES(qty), unit_price = VALUES(unit_price),
  amount = VALUES(amount), quality_flag = VALUES(quality_flag), create_batch_id = VALUES(create_batch_id),
  location_id = VALUES(location_id), deleted = b'0', tenant_id = 163;

-- Inventory data: batches, location stock, flow, expiry, stocktake and damage.
INSERT INTO ph_inv_batch
  (id, store_id, warehouse_id, drug_id, batch_no, manufacture_date, expiry_date,
   supplier_id, source_type, source_no, qty_total, qty_avail, qty_frozen, qty_sold,
   quality_status, expiry_status, last_issue_at, remark, cost_price, version,
   creator, updater, deleted, tenant_id)
VALUES
  (163501, 407, 163021, 163101, 'FSBATCH-001', CURRENT_DATE - INTERVAL 90 DAY, CURRENT_DATE + INTERVAL 540 DAY, 163301, 0, 'GR-FS-DEMO-001', 99, 99, 0, 1, 0, 0, NOW() - INTERVAL 1 DAY, '正常批次', 10.5000, 3, 'demo', 'demo', b'0', 163),
  (163502, 407, 163021, 163102, 'FSBATCH-002', CURRENT_DATE - INTERVAL 60 DAY, CURRENT_DATE + INTERVAL 360 DAY, 163301, 0, 'GR-FS-DEMO-001', 49, 49, 0, 1, 0, 0, NOW() - INTERVAL 2 DAY, '正常批次', 18.2000, 2, 'demo', 'demo', b'0', 163),
  (163503, 407, 163022, 163103, 'FSBATCH-003', CURRENT_DATE - INTERVAL 150 DAY, CURRENT_DATE + INTERVAL 45 DAY, 163301, 3, 'OPENING-DEMO-003', 24, 24, 0, 6, 0, 1, NOW() - INTERVAL 3 DAY, '45天后到期', 24.0000, 4, 'demo', 'demo', b'0', 163),
  (163504, 407, 163022, 163104, 'FSBATCH-004', CURRENT_DATE - INTERVAL 120 DAY, CURRENT_DATE + INTERVAL 75 DAY, 163303, 0, 'GR-FS-DEMO-002', 30, 30, 0, 0, 0, 1, NULL, '75天后到期', 14.0000, 1, 'demo', 'demo', b'0', 163),
  (163505, 407, 163021, 163105, 'FSBATCH-005', CURRENT_DATE - INTERVAL 30 DAY, CURRENT_DATE + INTERVAL 700 DAY, 163302, 3, 'OPENING-DEMO-005', 20, 20, 0, 0, 0, 0, NULL, '器械期初库存', 21.5000, 1, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE qty_total = VALUES(qty_total), qty_avail = VALUES(qty_avail),
  qty_frozen = VALUES(qty_frozen), qty_sold = VALUES(qty_sold),
  expiry_status = VALUES(expiry_status), cost_price = VALUES(cost_price),
  version = VALUES(version), deleted = b'0', tenant_id = 163;

INSERT INTO ph_inv_location_stock
  (id, batch_id, location_id, drug_id, qty, qty_frozen, creator, updater, deleted, tenant_id)
VALUES
  (163511, 163501, 163031, 163101, 99, 0, 'demo', 'demo', b'0', 163),
  (163512, 163502, 163031, 163102, 49, 0, 'demo', 'demo', b'0', 163),
  (163513, 163503, 163032, 163103, 24, 0, 'demo', 'demo', b'0', 163),
  (163514, 163504, 163034, 163104, 30, 0, 'demo', 'demo', b'0', 163),
  (163515, 163505, 163033, 163105, 20, 0, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE qty = VALUES(qty), qty_frozen = VALUES(qty_frozen),
  drug_id = VALUES(drug_id), deleted = b'0', tenant_id = 163;

INSERT INTO ph_inv_flow
  (id, store_id, batch_id, drug_id, batch_no, flow_type, in_qty, out_qty,
   balance_qty, biz_type, biz_no, flow_time, operator, remark, location_id,
   biz_line_id, frozen_delta, unit_cost, creator, updater, deleted, tenant_id)
VALUES
  (163521, 407, 163501, 163101, 'FSBATCH-001', 70, 100, 0, 100, 7, 'OPENING-DEMO-001', NOW() - INTERVAL 6 DAY, 163014, '演示期初入库', 163031, 163521, 0, 10.5000, 'demo', 'demo', b'0', 163),
  (163522, 407, 163501, 163101, 'FSBATCH-001', 20, 0, 2, 98, 2, 'SO-FS-DEMO-001', NOW() - INTERVAL 3 DAY, 163013, '销售出库', 163031, 163721, 0, 10.5000, 'demo', 'demo', b'0', 163),
  (163523, 407, 163501, 163101, 'FSBATCH-001', 21, 1, 0, 99, 3, 'SR-FS-DEMO-001', NOW() - INTERVAL 1 DAY, 163013, '销售退货回补', 163031, 163751, 0, 10.5000, 'demo', 'demo', b'0', 163),
  (163524, 407, 163502, 163102, 'FSBATCH-002', 70, 50, 0, 50, 7, 'OPENING-DEMO-002', NOW() - INTERVAL 6 DAY, 163014, '演示期初入库', 163031, 163524, 0, 18.2000, 'demo', 'demo', b'0', 163),
  (163525, 407, 163502, 163102, 'FSBATCH-002', 20, 0, 1, 49, 2, 'SO-FS-DEMO-002', NOW() - INTERVAL 2 DAY, 163013, '销售出库', 163031, 163722, 0, 18.2000, 'demo', 'demo', b'0', 163),
  (163526, 407, 163503, 163103, 'FSBATCH-003', 70, 30, 0, 30, 7, 'OPENING-DEMO-003', NOW() - INTERVAL 8 DAY, 163014, '演示期初入库', 163032, 163526, 0, 24.0000, 'demo', 'demo', b'0', 163),
  (163527, 407, 163503, 163103, 'FSBATCH-003', 20, 0, 6, 24, 2, 'SO-FS-DEMO-003', NOW() - INTERVAL 3 DAY, 163013, '销售出库', 163032, 163723, 0, 24.0000, 'demo', 'demo', b'0', 163),
  (163528, 407, 163504, 163104, 'FSBATCH-004', 70, 30, 0, 30, 7, 'OPENING-DEMO-004', NOW() - INTERVAL 1 DAY, 163014, '演示期初入库', 163034, 163528, 0, 14.0000, 'demo', 'demo', b'0', 163),
  (163529, 407, 163505, 163105, 'FSBATCH-005', 70, 20, 0, 20, 7, 'OPENING-DEMO-005', NOW() - INTERVAL 10 DAY, 163014, '演示期初入库', 163033, 163529, 0, 21.5000, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE in_qty = VALUES(in_qty), out_qty = VALUES(out_qty),
  balance_qty = VALUES(balance_qty), flow_time = VALUES(flow_time),
  unit_cost = VALUES(unit_cost), deleted = b'0', tenant_id = 163;

INSERT INTO ph_inv_expiry_alert
  (id, store_id, batch_id, drug_id, alert_level, expire_days, handle_type,
   handle_by, handle_at, alert_date, creator, updater, deleted, tenant_id)
VALUES
  (163551, 407, 163503, 163103, 2, 45, 0, NULL, NULL, CURRENT_DATE, 'demo', 'demo', b'0', 163),
  (163552, 407, 163504, 163104, 3, 75, 1, 163011, NOW(), CURRENT_DATE, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE alert_level = VALUES(alert_level), expire_days = VALUES(expire_days),
  handle_type = VALUES(handle_type), handle_by = VALUES(handle_by),
  handle_at = VALUES(handle_at), deleted = b'0', tenant_id = 163;

INSERT INTO ph_inv_stocktake
  (id, stocktake_no, store_id, warehouse_id, stocktake_type, blind_flag, scope,
   freeze_flag, total_item, done_item, status, initiator_id, audit_by, audit_at,
   adjusted_at, creator, updater, deleted, tenant_id)
VALUES
  (163531, 'PD-FS-DEMO-001', 407, 163021, 0, 0, '中心店常温库全盘', 1, 3, 3, 3, 163014, 163011, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, 'demo', 'demo', b'0', 163),
  (163532, 'PD-FS-DEMO-002', 407, 163022, 1, 1, '近效期货位抽盘', 0, 2, 1, 1, 163014, NULL, NULL, NULL, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE total_item = VALUES(total_item), done_item = VALUES(done_item),
  status = VALUES(status), deleted = b'0', tenant_id = 163;

INSERT INTO ph_inv_stocktake_line
  (id, stocktake_id, batch_id, drug_id, book_qty, real_qty, diff_qty,
   diff_flag, scanned_flag, remark, location_id, creator, updater, deleted, tenant_id)
VALUES
  (163533, 163531, 163501, 163101, 99, 99, 0, 0, 1, '账实相符', 163031, 'demo', 'demo', b'0', 163),
  (163534, 163531, 163502, 163102, 49, 48, -1, 2, 1, '样品领用待调整', 163031, 'demo', 'demo', b'0', 163),
  (163535, 163532, 163503, 163103, 24, 24, 0, 0, 1, '已盘', 163032, 'demo', 'demo', b'0', 163),
  (163536, 163532, 163504, 163104, 30, NULL, 0, 0, 0, '待盘', 163034, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE book_qty = VALUES(book_qty), real_qty = VALUES(real_qty),
  diff_qty = VALUES(diff_qty), diff_flag = VALUES(diff_flag), scanned_flag = VALUES(scanned_flag),
  deleted = b'0', tenant_id = 163;

INSERT INTO ph_inv_damage
  (id, damage_no, store_id, damage_type, reason, total_qty, total_amount, status,
   audit_by, audit_at, execute_by, review_by, execute_at, destroy_method,
   destroy_company, supervisor_id, destroy_at, creator, updater, deleted, tenant_id)
VALUES
  (163541, 'LS-FS-DEMO-001', 407, 0, 1, 1, 18.20, 3, 163011, NOW() - INTERVAL 2 DAY, 163014, 163012, NOW() - INTERVAL 2 DAY, '集中无害化销毁', '城市医废处置中心', 163012, NOW() - INTERVAL 1 DAY, 'demo', 'demo', b'0', 163),
  (163542, 'LS-FS-DEMO-002', 407, 0, 0, 2, 48.00, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE total_qty = VALUES(total_qty), total_amount = VALUES(total_amount),
  status = VALUES(status), deleted = b'0', tenant_id = 163;

INSERT INTO ph_inv_damage_line
  (id, damage_id, batch_id, drug_id, batch_no, qty, cost_price, amount,
   dispose_type, location_id, creator, updater, deleted, tenant_id)
VALUES
  (163543, 163541, 163502, 163102, 'FSBATCH-002', 1, 18.20, 18.20, 0, 163031, 'demo', 'demo', b'0', 163),
  (163544, 163542, 163503, 163103, 'FSBATCH-003', 2, 24.00, 48.00, 1, 163032, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE qty = VALUES(qty), cost_price = VALUES(cost_price),
  amount = VALUES(amount), dispose_type = VALUES(dispose_type), deleted = b'0', tenant_id = 163;

-- Member data: levels, users, addresses, points, carts and online orders.
INSERT INTO member_level
  (id, name, level, experience, discount_percent, icon, background_url, status,
   creator, updater, deleted, tenant_id)
VALUES
  (163601, '晨光会员', 1, 0, 100, '', '', 0, 'demo', 'demo', b'0', 163),
  (163602, '青叶会员', 2, 500, 95, '', '', 0, 'demo', 'demo', b'0', 163),
  (163603, '金穗会员', 3, 2000, 90, '', '', 0, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE name = VALUES(name), experience = VALUES(experience),
  discount_percent = VALUES(discount_percent), status = 0, deleted = b'0', tenant_id = 163;

INSERT INTO member_user
  (id, mobile, password, status, register_ip, register_terminal, login_ip, login_date,
   nickname, avatar, name, sex, area_id, birthday, mark, point, level_id, experience,
   creator, updater, deleted, tenant_id, email)
VALUES
  (163611, '13900001611', '', 0, '127.0.0.1', 3, '127.0.0.1', NOW() - INTERVAL 1 DAY, '小太阳', '', '顾晨', 1, 440305, '1995-05-12', '慢病关怀会员', 680, 163602, 860, 'demo', 'demo', b'0', 163, 'guchen@example.test'),
  (163612, '13900001612', '', 0, '127.0.0.1', 3, '127.0.0.1', NOW() - INTERVAL 2 DAY, '清风', '', '林悦', 2, 440305, '1992-11-03', '母婴健康关注', 128, 163601, 260, 'demo', 'demo', b'0', 163, 'linyue@example.test'),
  (163613, '13900001613', '', 0, '127.0.0.1', 4, '127.0.0.1', NOW() - INTERVAL 4 DAY, '远山', '', '张远', 1, 440305, '1987-08-20', '企业客户联系人', 2380, 163603, 3100, 'demo', 'demo', b'0', 163, 'zhangyuan@example.test'),
  (163614, '13900001614', '', 0, '127.0.0.1', 3, '', NULL, '初见', '', '沈宁', 2, 440305, '2000-02-14', '新注册会员', 20, 163601, 40, 'demo', 'demo', b'0', 163, NULL)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), name = VALUES(name), point = VALUES(point),
  level_id = VALUES(level_id), experience = VALUES(experience), status = 0,
  deleted = b'0', tenant_id = 163;

INSERT INTO member_address
  (id, user_id, name, mobile, area_id, detail_address, default_status,
   creator, updater, deleted, tenant_id)
VALUES
  (163621, 163611, '顾晨', '13900001611', 440305, '科技园南区晨光公寓3栋1202', b'1', 'demo', 'demo', b'0', 163),
  (163622, 163612, '林悦', '13900001612', 440305, '海滨街道健康花园8栋601', b'1', 'demo', 'demo', b'0', 163),
  (163623, 163613, '张远', '13900001613', 440304, '中心商务区金穗大厦18层', b'1', 'demo', 'demo', b'0', 163),
  (163624, 163611, '顾先生', '13900001611', 440306, '大学城教师公寓2栋305', b'0', 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), name = VALUES(name),
  mobile = VALUES(mobile), detail_address = VALUES(detail_address),
  default_status = VALUES(default_status), deleted = b'0', tenant_id = 163;

INSERT INTO member_point_record
  (id, user_id, biz_id, biz_type, title, description, point, total_point,
   creator, updater, deleted, tenant_id)
VALUES
  (163631, 163611, 'SO-FS-DEMO-001', 1, '门店消费赠送积分', '完成门店消费', 40, 640, 'demo', 'demo', b'0', 163),
  (163632, 163611, 'SIGN-20260914', 2, '每日签到', '连续签到奖励', 10, 650, 'demo', 'demo', b'0', 163),
  (163633, 163611, 'ACT-AUTUMN', 3, '健康季活动奖励', '秋季健康活动积分', 30, 680, 'demo', 'demo', b'0', 163),
  (163634, 163612, 'WX-FS-DEMO-002', 1, '线上订单赠送积分', '小程序订单完成', 28, 128, 'demo', 'demo', b'0', 163),
  (163635, 163613, 'SO-FS-DEMO-003', 1, '门店消费赠送积分', '处方药消费积分', 80, 2380, 'demo', 'demo', b'0', 163),
  (163636, 163614, 'REGISTER-163614', 4, '新会员注册礼', '首次注册赠送', 20, 20, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description),
  point = VALUES(point), total_point = VALUES(total_point), deleted = b'0', tenant_id = 163;

INSERT INTO ph_wx_cart
  (id, member_id, drug_id, qty, selected_flag, add_time, store_id,
   creator, updater, deleted, tenant_id)
VALUES
  (163641, 163611, 163104, 2, 1, NOW() - INTERVAL 2 HOUR, 407, 'demo', 'demo', b'0', 163),
  (163642, 163611, 163105, 1, 0, NOW() - INTERVAL 1 DAY, 407, 'demo', 'demo', b'0', 163),
  (163643, 163612, 163101, 1, 1, NOW() - INTERVAL 3 HOUR, 407, 'demo', 'demo', b'0', 163),
  (163644, 163614, 163102, 2, 1, NOW() - INTERVAL 30 MINUTE, 407, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE qty = VALUES(qty), selected_flag = VALUES(selected_flag),
  add_time = VALUES(add_time), deleted = b'0', tenant_id = 163;

INSERT INTO ph_wx_order
  (id, order_no, member_id, store_id, order_type, goods_amount, coupon_amount,
   freight_amount, discount_amount, payable_amount, pay_no, pay_status, paid_at,
   status, address_snapshot, remark, finish_at, expire_at, pickup_code,
   verify_by, verify_at, creator, updater, deleted, tenant_id)
VALUES
  (163651, 'WX-FS-DEMO-001', 163611, 407, 0, 53.60, 5.00, 0.00, 0.00, 48.60, 'WXPAY-DEMO-001', 1, NOW() - INTERVAL 2 DAY, 4, NULL, '到店自提已完成', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 30 MINUTE, 'FS6101', 163013, NOW() - INTERVAL 2 DAY, 'demo', 'demo', b'0', 163),
  (163652, 'WX-FS-DEMO-002', 163612, 407, 1, 19.80, 0.00, 6.00, 0.00, 25.80, 'WXPAY-DEMO-002', 1, NOW() - INTERVAL 1 DAY, 1, '海滨街道健康花园8栋601', '请尽快配送', NULL, NOW() + INTERVAL 2 HOUR, NULL, NULL, NULL, 'demo', 'demo', b'0', 163),
  (163653, 'WX-FS-DEMO-003', 163613, 407, 0, 39.90, 0.00, 0.00, 0.00, 39.90, NULL, 0, NULL, 0, NULL, '待支付订单', NULL, NOW() + INTERVAL 30 MINUTE, 'FS6103', NULL, NULL, 'demo', 'demo', b'0', 163),
  (163654, 'WX-FS-DEMO-004', 163614, 407, 0, 32.50, 0.00, 0.00, 2.60, 29.90, 'WXPAY-DEMO-004', 1, NOW() - INTERVAL 4 HOUR, 3, NULL, '等待顾客自提', NULL, NOW() - INTERVAL 3 HOUR, 'FS6104', NULL, NULL, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE pay_status = VALUES(pay_status), status = VALUES(status),
  payable_amount = VALUES(payable_amount), paid_at = VALUES(paid_at),
  expire_at = VALUES(expire_at), pickup_code = VALUES(pickup_code),
  deleted = b'0', tenant_id = 163;

INSERT INTO ph_wx_order_line
  (id, wx_order_id, drug_id, qty, price, line_amount, batch_id, batch_no,
   picked_qty, location_id, drug_name, specification, unit,
   creator, updater, deleted, tenant_id)
VALUES
  (163661, 163651, 163104, 2, 26.80, 53.60, 163504, 'FSBATCH-004', 2, 163034, '维生素C咀嚼片', '0.1g*60片/瓶', '瓶', 'demo', 'demo', b'0', 163),
  (163662, 163652, 163101, 1, 19.80, 19.80, NULL, NULL, 0, NULL, '复方氨酚烷胺胶囊', '12粒/盒', '盒', 'demo', 'demo', b'0', 163),
  (163663, 163653, 163105, 1, 39.90, 39.90, NULL, NULL, 0, NULL, '电子体温计', '软头型/支', '支', 'demo', 'demo', b'0', 163),
  (163664, 163654, 163102, 1, 32.50, 32.50, 163502, 'FSBATCH-002', 1, 163031, '布洛芬缓释胶囊', '0.3g*20粒/盒', '盒', 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE qty = VALUES(qty), price = VALUES(price),
  line_amount = VALUES(line_amount), picked_qty = VALUES(picked_qty),
  batch_id = VALUES(batch_id), location_id = VALUES(location_id),
  deleted = b'0', tenant_id = 163;

-- POS data: shifts, sales, payments and returns. Sale dates cover the latest five days.
INSERT INTO ph_pos_shift
  (id, shift_no, store_id, pos_no, cashier_id, open_at, close_at, cash_expected,
   cash_actual, diff_amount, diff_reason, sale_count, sale_amount, status,
   creator, updater, deleted, tenant_id)
VALUES
  (163701, 'SC-FS-DEMO-001', 407, 'POS-01', 163013, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 8 HOUR, 220.00, 220.00, 0.00, NULL, 2, 264.60, 1, 'demo', 'demo', b'0', 163),
  (163702, 'SC-FS-DEMO-002', 407, 'POS-01', 163013, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 8 HOUR, 180.00, 179.50, -0.50, '零钱找补差异', 2, 118.30, 1, 'demo', 'demo', b'0', 163),
  (163703, 'SC-FS-DEMO-003', 407, 'POS-02', 407, NOW() - INTERVAL 4 HOUR, NULL, 96.30, NULL, NULL, NULL, 2, 96.30, 0, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE open_at = VALUES(open_at), close_at = VALUES(close_at),
  sale_count = VALUES(sale_count), sale_amount = VALUES(sale_amount),
  status = VALUES(status), deleted = b'0', tenant_id = 163;

INSERT INTO ph_sale_order
  (id, order_no, store_id, pos_no, shift_id, cashier_id, pharmacist_id, member_id,
   customer_name, source, sale_type, return_flag, total_qty, subtotal,
   discount_amount, coupon_amount, points_deduct, payable_amount, paid_amount,
   change_amount, cost_amount, points_earned, status, offline_flag, remark,
   sale_time, creator, updater, deleted, tenant_id)
VALUES
  (163711, 'SO-FS-DEMO-001', 407, 'POS-01', 163701, 163013, NULL, 163611, NULL, 0, 0, 1, 2, 39.60, 0.00, 0.00, 0.00, 39.60, 39.60, 0.00, 21.00, 40, 3, 0, '会员销售，部分退货', NOW() - INTERVAL 3 DAY, 'demo', 'demo', b'0', 163),
  (163712, 'SO-FS-DEMO-002', 407, 'POS-01', 163702, 163013, NULL, NULL, '散客', 0, 0, 0, 1, 32.50, 0.00, 0.00, 0.00, 32.50, 32.50, 0.00, 18.20, 0, 1, 0, '现金销售', NOW() - INTERVAL 2 DAY, 'demo', 'demo', b'0', 163),
  (163713, 'SO-FS-DEMO-003', 407, 'POS-01', 163701, 163013, 163012, 163613, NULL, 0, 0, 0, 6, 270.00, 0.00, 5.40, 0.00, 264.60, 264.60, 0.00, 144.00, 80, 1, 0, '处方药销售已审方', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, 'demo', 'demo', b'0', 163),
  (163714, 'SO-FS-DEMO-004', 407, 'POS-01', 163702, 163013, NULL, 163612, NULL, 0, 0, 0, 3, 85.80, 0.00, 0.00, 0.00, 85.80, 85.80, 0.00, 42.00, 29, 1, 0, '会员组合销售', NOW() - INTERVAL 1 DAY, 'demo', 'demo', b'0', 163),
  (163715, 'SO-FS-DEMO-005', 407, 'POS-02', 163703, 407, NULL, NULL, '王女士', 0, 0, 0, 2, 79.80, 0.00, 0.00, 0.00, 79.80, 100.00, 20.20, 43.00, 0, 1, 0, '今日器械销售', NOW() - INTERVAL 2 HOUR, 'demo', 'demo', b'0', 163),
  (163716, 'SO-FS-DEMO-006', 407, 'POS-02', 163703, 407, NULL, 163614, NULL, 0, 0, 0, 1, 16.50, 0.00, 0.00, 0.00, 16.50, 16.50, 0.00, 10.50, 17, 1, 0, '今日会员销售', NOW() - INTERVAL 1 HOUR, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE shift_id = VALUES(shift_id), member_id = VALUES(member_id),
  total_qty = VALUES(total_qty), payable_amount = VALUES(payable_amount),
  paid_amount = VALUES(paid_amount), cost_amount = VALUES(cost_amount),
  status = VALUES(status), sale_time = VALUES(sale_time), deleted = b'0', tenant_id = 163;

INSERT INTO ph_sale_order_line
  (id, order_id, line_no, drug_id, batch_id, batch_no, expiry_date, qty, price,
   normal_price, discount, line_amount, cost_price, is_rx, returned_qty, is_gift,
   location_id, drug_name, specification, unit, creator, updater, deleted, tenant_id)
VALUES
  (163721, 163711, 1, 163101, 163501, 'FSBATCH-001', CURRENT_DATE + INTERVAL 540 DAY, 2, 19.80, 19.80, 0.00, 39.60, 10.50, 0, 1, 0, 163031, '复方氨酚烷胺胶囊', '12粒/盒', '盒', 'demo', 'demo', b'0', 163),
  (163722, 163712, 1, 163102, 163502, 'FSBATCH-002', CURRENT_DATE + INTERVAL 360 DAY, 1, 32.50, 32.50, 0.00, 32.50, 18.20, 0, 0, 0, 163031, '布洛芬缓释胶囊', '0.3g*20粒/盒', '盒', 'demo', 'demo', b'0', 163),
  (163723, 163713, 1, 163103, 163503, 'FSBATCH-003', CURRENT_DATE + INTERVAL 45 DAY, 6, 45.00, 45.00, 0.00, 270.00, 24.00, 1, 0, 0, 163032, '奥美拉唑肠溶胶囊', '20mg*14粒/盒', '盒', 'demo', 'demo', b'0', 163),
  (163724, 163714, 1, 163104, 163504, 'FSBATCH-004', CURRENT_DATE + INTERVAL 75 DAY, 2, 26.80, 26.80, 0.00, 53.60, 14.00, 0, 0, 0, 163034, '维生素C咀嚼片', '0.1g*60片/瓶', '瓶', 'demo', 'demo', b'0', 163),
  (163725, 163714, 2, 163102, 163502, 'FSBATCH-002', CURRENT_DATE + INTERVAL 360 DAY, 1, 32.20, 32.50, 0.30, 32.20, 18.20, 0, 0, 0, 163031, '布洛芬缓释胶囊', '0.3g*20粒/盒', '盒', 'demo', 'demo', b'0', 163),
  (163726, 163715, 1, 163105, 163505, 'FSBATCH-005', CURRENT_DATE + INTERVAL 700 DAY, 2, 39.90, 39.90, 0.00, 79.80, 21.50, 0, 0, 0, 163033, '电子体温计', '软头型/支', '支', 'demo', 'demo', b'0', 163),
  (163727, 163716, 1, 163101, 163501, 'FSBATCH-001', CURRENT_DATE + INTERVAL 540 DAY, 1, 16.50, 19.80, 3.30, 16.50, 10.50, 0, 0, 0, 163031, '复方氨酚烷胺胶囊', '12粒/盒', '盒', 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE qty = VALUES(qty), price = VALUES(price),
  line_amount = VALUES(line_amount), returned_qty = VALUES(returned_qty),
  drug_name = VALUES(drug_name), deleted = b'0', tenant_id = 163;

INSERT INTO ph_sale_payment
  (id, order_id, pay_no, pay_method, pay_amount, channel, status, paid_at,
   refund_no, refund_at, payment_no, creator, updater, deleted, tenant_id)
VALUES
  (163731, 163711, 'CASH-DEMO-001', 1, 39.60, 'cash', 3, NOW() - INTERVAL 3 DAY, 'REF-DEMO-001', NOW() - INTERVAL 1 DAY, 'PAY-FS-DEMO-001', 'demo', 'demo', b'0', 163),
  (163732, 163712, 'CASH-DEMO-002', 1, 32.50, 'cash', 1, NOW() - INTERVAL 2 DAY, NULL, NULL, 'PAY-FS-DEMO-002', 'demo', 'demo', b'0', 163),
  (163733, 163713, 'WX-DEMO-003', 2, 264.60, 'wxpay', 1, NOW() - INTERVAL 3 DAY, NULL, NULL, 'PAY-FS-DEMO-003', 'demo', 'demo', b'0', 163),
  (163734, 163714, 'ALI-DEMO-004', 3, 85.80, 'alipay', 1, NOW() - INTERVAL 1 DAY, NULL, NULL, 'PAY-FS-DEMO-004', 'demo', 'demo', b'0', 163),
  (163735, 163715, 'CASH-DEMO-005', 1, 79.80, 'cash', 1, NOW() - INTERVAL 2 HOUR, NULL, NULL, 'PAY-FS-DEMO-005', 'demo', 'demo', b'0', 163),
  (163736, 163716, 'CARD-DEMO-006', 4, 16.50, 'bank', 1, NOW() - INTERVAL 1 HOUR, NULL, NULL, 'PAY-FS-DEMO-006', 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE order_id = VALUES(order_id), pay_amount = VALUES(pay_amount),
  status = VALUES(status), paid_at = VALUES(paid_at), refund_no = VALUES(refund_no),
  refund_at = VALUES(refund_at), deleted = b'0', tenant_id = 163;

INSERT INTO ph_sale_return
  (id, return_no, sale_order_id, store_id, return_type, reason, total_amount,
   refund_method, status, cashier_id, pharmacist_confirm, audit_by, return_at,
   refund_status, creator, updater, deleted, tenant_id)
VALUES
  (163741, 'SR-FS-DEMO-001', 163711, 407, 0, 1, 19.80, 0, 3, 163013, 0, 163011, NOW() - INTERVAL 1 DAY, 2, 'demo', 'demo', b'0', 163),
  (163742, 'SR-FS-DEMO-002', 163713, 407, 0, 0, 45.00, 0, 1, 163013, 1, NULL, NULL, 0, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount), status = VALUES(status),
  refund_status = VALUES(refund_status), return_at = VALUES(return_at),
  deleted = b'0', tenant_id = 163;

INSERT INTO ph_sale_return_line
  (id, return_id, sale_line_id, batch_id, drug_id, qty, price, amount,
   points_deduct, location_id, creator, updater, deleted, tenant_id)
VALUES
  (163751, 163741, 163721, 163501, 163101, 1, 19.80, 19.80, 20, 163031, 'demo', 'demo', b'0', 163),
  (163752, 163742, 163723, 163503, 163103, 1, 45.00, 45.00, 13, 163032, 'demo', 'demo', b'0', 163)
ON DUPLICATE KEY UPDATE qty = VALUES(qty), price = VALUES(price), amount = VALUES(amount),
  points_deduct = VALUES(points_deduct), deleted = b'0', tenant_id = 163;

COMMIT;
