SET NAMES utf8mb4;
SET time_zone = '+08:00';
START TRANSACTION;

-- FirstSun 管理后台交付演示数据（tenant_id=163）。
-- 固定 ID、业务编号和唯一键使本脚本可重复执行；不删除、不清空既有数据。
-- 药品图片均为仓库内原创通用占位图，由 admin-ui 同源静态服务提供。

-- A 基础档案：补充中成药、处方药、保健品、器械和冷链分类及冷藏库位。
INSERT INTO ph_category
  (id, cat_code, cat_name, parent_id, cat_type, sort, status, creator, updater, deleted, tenant_id)
VALUES
  (163006, 'FS-CAT-RX', '处方药品', 0, 0, 13, 1, 'delivery-demo', 'delivery-demo', b'0', 163),
  (163007, 'FS-CAT-TCM', '中成药', 0, 0, 14, 1, 'delivery-demo', 'delivery-demo', b'0', 163),
  (163008, 'FS-CAT-HEALTH', '营养保健品', 0, 1, 21, 1, 'delivery-demo', 'delivery-demo', b'0', 163),
  (163009, 'FS-CAT-CONSUM', '医用耗材', 163005, 2, 31, 1, 'delivery-demo', 'delivery-demo', b'0', 163),
  (163010, 'FS-CAT-COLDCHAIN', '冷链及特殊储存', 0, 0, 40, 1, 'delivery-demo', 'delivery-demo', b'0', 163)
ON DUPLICATE KEY UPDATE cat_name=VALUES(cat_name), parent_id=VALUES(parent_id), cat_type=VALUES(cat_type),
  sort=VALUES(sort), status=1, deleted=b'0', tenant_id=163;

INSERT INTO ph_warehouse
  (id, store_id, wh_code, wh_name, temp_zone, is_default, status, creator, updater, deleted, tenant_id)
VALUES
  (163023, 407, 'FS-WH-COLD', '中心店冷藏库', 2, 0, 1, 'delivery-demo', 'delivery-demo', b'0', 163)
ON DUPLICATE KEY UPDATE wh_name=VALUES(wh_name), temp_zone=2, status=1, deleted=b'0', tenant_id=163;

INSERT INTO ph_location
  (id, warehouse_id, location_code, location_type, max_capacity, status, last_use_at,
   creator, updater, deleted, tenant_id)
VALUES
  (163035, 163023, 'C-02-01', 0, 120, 1, NOW(), 'delivery-demo', 'delivery-demo', b'0', 163)
ON DUPLICATE KEY UPDATE warehouse_id=VALUES(warehouse_id), max_capacity=VALUES(max_capacity),
  status=1, last_use_at=NOW(), deleted=b'0', tenant_id=163;

-- 既有 5 个药品也补齐仓库内图片。
UPDATE ph_drug SET image_url='/pharmacy-demo/medicine.svg', images=JSON_ARRAY('/pharmacy-demo/medicine.svg'), updater='delivery-demo'
 WHERE tenant_id=163 AND id IN (163101,163102,163103) AND deleted=b'0';
UPDATE ph_drug SET image_url='/pharmacy-demo/health.svg', images=JSON_ARRAY('/pharmacy-demo/health.svg'), updater='delivery-demo'
 WHERE tenant_id=163 AND id=163104 AND deleted=b'0';
UPDATE ph_drug SET image_url='/pharmacy-demo/device.svg', images=JSON_ARRAY('/pharmacy-demo/device.svg'), updater='delivery-demo'
 WHERE tenant_id=163 AND id=163105 AND deleted=b'0';

INSERT INTO ph_drug
  (id, drug_code, category_id, generic_name, trade_name, spell_code, specification,
   dosage_form, manufacturer, approval_no, drug_type, is_rx, is_special,
   is_pseudoephedrine, is_cold_chain, unit, conversion_ratio, retail_price,
   member_price, cost_price, min_sale_price, tax_rate, insurance_type, min_stock,
   max_stock, storage_cond, need_expiry, default_location_id, saleable_online,
   status, remark, image_url, images, description, approve_status, audit_by, audit_at,
   creator, updater, deleted, tenant_id)
VALUES
  (163106,'FS-DRUG-006',163001,'对乙酰氨基酚片','晨安','dyxajf','0.5g*20片/盒','片剂','华中演示制药有限公司','国药准字H-DEMO-0006',1,0,0,0,0,'盒',20,12.80,11.50,5.80,9.50,13.00,1,25,320,0,1,163031,1,1,'OTC演示商品','/pharmacy-demo/medicine.svg',JSON_ARRAY('/pharmacy-demo/medicine.svg'),'用于管理后台演示，不代表真实品牌或疗效。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163107,'FS-DRUG-007',163001,'氯雷他定片','舒敏','lltd','10mg*10片/盒','片剂','江南演示药业有限公司','国药准字H-DEMO-0007',2,0,0,0,0,'盒',10,18.60,16.80,7.50,13.50,13.00,0,15,180,0,1,163031,1,1,'OTC抗过敏演示商品','/pharmacy-demo/medicine.svg',JSON_ARRAY('/pharmacy-demo/medicine.svg'),'用于管理后台演示，不代表真实品牌或疗效。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163108,'FS-DRUG-008',163003,'蒙脱石散','安腹','mts','3g*10袋/盒','散剂','西南演示制药有限公司','国药准字H-DEMO-0008',1,0,0,0,0,'盒',10,16.90,15.20,6.20,12.00,13.00,1,18,220,0,1,163031,1,1,'OTC消化系统演示商品','/pharmacy-demo/medicine.svg',JSON_ARRAY('/pharmacy-demo/medicine.svg'),'用于管理后台演示，不代表真实品牌或疗效。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163109,'FS-DRUG-009',163006,'阿莫西林胶囊','欣宁','amxl','0.25g*24粒/盒','胶囊剂','华东演示制药有限公司','国药准字H-DEMO-0009',0,1,0,0,0,'盒',24,25.80,23.50,12.00,19.00,13.00,1,20,240,1,1,163032,0,1,'处方药演示商品','/pharmacy-demo/medicine.svg',JSON_ARRAY('/pharmacy-demo/medicine.svg'),'处方药演示数据，须经药师审核。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163110,'FS-DRUG-010',163006,'缬沙坦胶囊','稳压宁','xst','80mg*14粒/盒','胶囊剂','北方演示药业有限公司','国药准字H-DEMO-0010',0,1,0,0,0,'盒',14,38.50,35.00,21.00,29.00,13.00,2,12,160,1,1,163032,0,1,'处方药演示商品','/pharmacy-demo/medicine.svg',JSON_ARRAY('/pharmacy-demo/medicine.svg'),'处方药演示数据，须经药师审核。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163111,'FS-DRUG-011',163006,'盐酸二甲双胍片','糖衡','ysejsg','0.5g*30片/盒','片剂','华北演示制药有限公司','国药准字H-DEMO-0011',0,1,0,0,0,'盒',30,22.00,19.80,8.50,16.00,13.00,1,20,260,0,1,163031,0,1,'处方药演示商品','/pharmacy-demo/medicine.svg',JSON_ARRAY('/pharmacy-demo/medicine.svg'),'处方药演示数据，须经药师审核。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163112,'FS-DRUG-012',163007,'连花清瘟胶囊','清和','lhqw','0.35g*24粒/盒','胶囊剂','燕赵演示药业有限公司','国药准字Z-DEMO-0012',1,0,0,0,0,'盒',24,29.80,26.80,13.50,22.00,13.00,0,20,260,1,1,163032,1,1,'中成药演示商品','/pharmacy-demo/traditional.svg',JSON_ARRAY('/pharmacy-demo/traditional.svg'),'中成药分类演示数据，不代表真实品牌或疗效。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163113,'FS-DRUG-013',163007,'复方丹参片','丹心','ffds','60片/瓶','片剂','岭南演示药业有限公司','国药准字Z-DEMO-0013',2,0,0,0,0,'瓶',60,24.50,22.00,9.00,18.00,13.00,1,15,180,1,1,163032,1,1,'中成药演示商品','/pharmacy-demo/traditional.svg',JSON_ARRAY('/pharmacy-demo/traditional.svg'),'中成药分类演示数据，不代表真实品牌或疗效。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163114,'FS-DRUG-014',163007,'板蓝根颗粒','青叶','blg','10g*20袋/包','颗粒剂','齐鲁演示药业有限公司','国药准字Z-DEMO-0014',1,0,0,0,0,'包',20,21.80,19.50,6.00,15.00,13.00,0,25,300,0,1,163031,1,1,'中成药演示商品','/pharmacy-demo/traditional.svg',JSON_ARRAY('/pharmacy-demo/traditional.svg'),'中成药分类演示数据，不代表真实品牌或疗效。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163115,'FS-DRUG-015',163008,'钙维生素D咀嚼片','健骨乐','gwssd','1.5g*60片/瓶','片剂','晨光演示健康产业有限公司','食健备G-DEMO-0015',5,0,0,0,0,'瓶',60,49.00,44.00,22.00,36.00,13.00,0,10,120,0,1,163031,1,1,'保健品演示商品','/pharmacy-demo/health.svg',JSON_ARRAY('/pharmacy-demo/health.svg'),'保健品分类演示数据，不代表真实品牌或功效。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163116,'FS-DRUG-016',163008,'鱼油软胶囊','海悦','yy','1g*100粒/瓶','软胶囊','海滨演示健康科技有限公司','食健备G-DEMO-0016',5,0,0,0,0,'瓶',100,79.00,69.00,35.00,55.00,13.00,0,8,100,0,1,163031,1,1,'保健品演示商品','/pharmacy-demo/health.svg',JSON_ARRAY('/pharmacy-demo/health.svg'),'保健品分类演示数据，不代表真实品牌或功效。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163117,'FS-DRUG-017',163009,'医用外科口罩','FirstCare','yywk kz','10只/袋','医疗器械','鹏城演示医疗用品有限公司','粤械注准DEMO20260117',6,0,0,0,0,'袋',10,6.90,5.90,0.45,4.50,13.00,0,50,800,0,0,163033,1,1,'医疗器械演示商品','/pharmacy-demo/device.svg',JSON_ARRAY('/pharmacy-demo/device.svg'),'器械分类演示数据，不代表真实备案产品。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163118,'FS-DRUG-018',163005,'血糖仪','FirstCare Home','xty','套装/盒','医疗器械','鹏城演示医疗科技有限公司','粤械注准DEMO20260118',6,0,0,0,0,'盒',1,139.00,125.00,78.00,108.00,13.00,0,5,50,0,0,163033,1,1,'医疗器械演示商品','/pharmacy-demo/device.svg',JSON_ARRAY('/pharmacy-demo/device.svg'),'器械分类演示数据，不代表真实备案产品。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163119,'FS-DRUG-019',163010,'重组人胰岛素注射液','冷链一号','zrryds','3ml:300单位/支','注射剂','北辰演示生物制药有限公司','国药准字S-DEMO-0019',0,1,0,0,1,'支',1,56.00,52.00,38.00,45.00,13.00,2,8,80,2,1,163035,0,1,'2-8℃冷链处方药演示商品','/pharmacy-demo/cold-chain.svg',JSON_ARRAY('/pharmacy-demo/cold-chain.svg'),'冷链处方药演示数据，须经药师审核。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163),
  (163120,'FS-DRUG-020',163010,'益生菌冻干粉','活力菌','ysj','2g*12袋/盒','粉剂','晨露演示健康科技有限公司','食健备G-DEMO-0020',5,0,0,0,1,'盒',12,68.00,62.00,28.00,50.00,13.00,0,8,90,2,1,163035,1,1,'冷藏保健品演示商品','/pharmacy-demo/cold-chain.svg',JSON_ARRAY('/pharmacy-demo/cold-chain.svg'),'冷链保健品分类演示数据，不代表真实品牌或功效。',1,163012,NOW(),'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE category_id=VALUES(category_id), generic_name=VALUES(generic_name), trade_name=VALUES(trade_name),
  specification=VALUES(specification), dosage_form=VALUES(dosage_form), manufacturer=VALUES(manufacturer),
  approval_no=VALUES(approval_no), drug_type=VALUES(drug_type), is_rx=VALUES(is_rx),
  is_cold_chain=VALUES(is_cold_chain), retail_price=VALUES(retail_price), member_price=VALUES(member_price),
  cost_price=VALUES(cost_price), min_stock=VALUES(min_stock), max_stock=VALUES(max_stock),
  storage_cond=VALUES(storage_cond), default_location_id=VALUES(default_location_id),
  saleable_online=VALUES(saleable_online), status=1, image_url=VALUES(image_url), images=VALUES(images),
  description=VALUES(description), approve_status=1, deleted=b'0', tenant_id=163;

INSERT INTO ph_drug_barcode
  (id, drug_id, barcode, barcode_type, is_default, creator, updater, deleted, tenant_id)
VALUES
  (163206,163106,'6900000163106',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163207,163107,'6900000163107',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163208,163108,'6900000163108',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163209,163109,'6900000163109',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163210,163110,'6900000163110',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163211,163111,'6900000163111',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163212,163112,'6900000163112',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163213,163113,'6900000163113',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163214,163114,'6900000163114',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163215,163115,'6900000163115',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163216,163116,'6900000163116',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163217,163117,'6900000163117',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163218,163118,'6900000163118',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163219,163119,'6900000163119',0,1,'delivery-demo','delivery-demo',b'0',163),
  (163220,163120,'6900000163120',0,1,'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE drug_id=VALUES(drug_id), barcode_type=0, is_default=1, deleted=b'0', tenant_id=163;

-- B 采购：完整采购单、收货单及 15 条明细。
INSERT INTO ph_po_order
  (id, order_no, store_id, warehouse_id, supplier_id, order_date, expect_date,
   total_qty, total_amount, discount_amount, payable_amount, status, is_auto,
   remark, audit_by, audit_at, creator, updater, deleted, tenant_id)
VALUES
  (163404,'PO-FS-DELIVERY-001',407,163021,163301,CURRENT_DATE-INTERVAL 15 DAY,CURRENT_DATE-INTERVAL 12 DAY,
   435,3939.00,0.00,3939.00,5,0,'交付版常温品种采购演示',163011,NOW()-INTERVAL 14 DAY,
   'delivery-demo','delivery-demo',b'0',163),
  (163405,'PO-FS-DELIVERY-002',407,163023,163301,CURRENT_DATE-INTERVAL 15 DAY,CURRENT_DATE-INTERVAL 12 DAY,
   40,1320.00,0.00,1320.00,5,0,'交付版冷链品种采购演示',163011,NOW()-INTERVAL 14 DAY,
   'delivery-demo','delivery-demo',b'0',163),
  (163406,'PO-FS-DELIVERY-003',407,163022,163301,CURRENT_DATE-INTERVAL 15 DAY,CURRENT_DATE-INTERVAL 12 DAY,
   140,1920.00,0.00,1920.00,5,0,'交付版阴凉品种采购演示',163011,NOW()-INTERVAL 14 DAY,
   'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE total_qty=VALUES(total_qty),total_amount=VALUES(total_amount),
  payable_amount=VALUES(payable_amount),warehouse_id=VALUES(warehouse_id),status=5,
  deleted=b'0',tenant_id=163;

INSERT INTO ph_po_order_line
  (id,order_id,line_no,drug_id,order_qty,received_qty,unit_price,discount_rate,line_amount,remark,creator,updater,deleted,tenant_id)
VALUES
  (163415,163404,1,163106,80,80,5.80,1.00,464.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163416,163404,2,163107,60,60,7.50,1.00,450.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163417,163404,3,163108,50,50,6.20,1.00,310.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163418,163406,1,163109,40,40,12.00,1.00,480.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163419,163406,2,163110,30,30,21.00,1.00,630.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163420,163404,4,163111,40,40,8.50,1.00,340.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163421,163406,3,163112,40,40,13.50,1.00,540.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163422,163406,4,163113,30,30,9.00,1.00,270.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163423,163404,5,163114,50,50,6.00,1.00,300.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163424,163404,6,163115,25,25,22.00,1.00,550.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163425,163404,7,163116,20,20,35.00,1.00,700.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163426,163404,8,163117,100,100,0.45,1.00,45.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163427,163404,9,163118,10,10,78.00,1.00,780.00,NULL,'delivery-demo','delivery-demo',b'0',163),
  (163428,163405,1,163119,20,20,38.00,1.00,760.00,'冷链运输','delivery-demo','delivery-demo',b'0',163),
  (163429,163405,2,163120,20,20,28.00,1.00,560.00,'冷链运输','delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE order_id=VALUES(order_id),line_no=VALUES(line_no),drug_id=VALUES(drug_id),
  order_qty=VALUES(order_qty),received_qty=VALUES(received_qty),
  unit_price=VALUES(unit_price),line_amount=VALUES(line_amount),deleted=b'0',tenant_id=163;

INSERT INTO ph_po_receipt
  (id,receipt_no,order_id,store_id,warehouse_id,receive_by,receive_date,total_qty,total_amount,
   diff_type,is_free_receipt,status,quality_status,posted_at,creator,updater,deleted,tenant_id)
VALUES
  (163423,'GR-FS-DELIVERY-001',163404,407,163021,163014,NOW()-INTERVAL 12 DAY,435,3939.00,
   0,0,2,1,NOW()-INTERVAL 12 DAY,'delivery-demo','delivery-demo',b'0',163),
  (163424,'GR-FS-DELIVERY-002',163405,407,163023,163014,NOW()-INTERVAL 12 DAY,40,1320.00,
   0,0,2,1,NOW()-INTERVAL 12 DAY,'delivery-demo','delivery-demo',b'0',163),
  (163425,'GR-FS-DELIVERY-003',163406,407,163022,163014,NOW()-INTERVAL 12 DAY,140,1920.00,
   0,0,2,1,NOW()-INTERVAL 12 DAY,'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE total_qty=VALUES(total_qty),total_amount=VALUES(total_amount),
  warehouse_id=VALUES(warehouse_id),status=2,quality_status=1,
  posted_at=VALUES(posted_at),deleted=b'0',tenant_id=163;

-- C 库存：每个新增品种一个批次及货位库存，冷链品种进入冷藏库。
INSERT INTO ph_inv_batch
  (id,store_id,warehouse_id,drug_id,batch_no,manufacture_date,expiry_date,supplier_id,source_type,source_no,
   qty_total,qty_avail,qty_frozen,qty_sold,quality_status,expiry_status,last_issue_at,remark,cost_price,version,
   creator,updater,deleted,tenant_id)
VALUES
  (163506,407,163021,163106,'FSDEL-006',CURRENT_DATE-INTERVAL 90 DAY,CURRENT_DATE+INTERVAL 540 DAY,163301,0,'GR-FS-DELIVERY-001',78,78,0,2,0,0,NOW()-INTERVAL 1 DAY,'正常批次',5.8000,2,'delivery-demo','delivery-demo',b'0',163),
  (163507,407,163021,163107,'FSDEL-007',CURRENT_DATE-INTERVAL 75 DAY,CURRENT_DATE+INTERVAL 480 DAY,163301,0,'GR-FS-DELIVERY-001',60,60,0,0,0,0,NULL,'正常批次',7.5000,1,'delivery-demo','delivery-demo',b'0',163),
  (163508,407,163021,163108,'FSDEL-008',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 420 DAY,163301,0,'GR-FS-DELIVERY-001',50,50,0,0,0,0,NULL,'正常批次',6.2000,1,'delivery-demo','delivery-demo',b'0',163),
  (163509,407,163022,163109,'FSDEL-009',CURRENT_DATE-INTERVAL 80 DAY,CURRENT_DATE+INTERVAL 360 DAY,163301,0,'GR-FS-DELIVERY-003',38,38,0,2,0,0,NOW()-INTERVAL 1 DAY,'处方药批次',12.0000,2,'delivery-demo','delivery-demo',b'0',163),
  (163510,407,163022,163110,'FSDEL-010',CURRENT_DATE-INTERVAL 70 DAY,CURRENT_DATE+INTERVAL 390 DAY,163301,0,'GR-FS-DELIVERY-003',30,30,0,0,0,0,NULL,'处方药批次',21.0000,1,'delivery-demo','delivery-demo',b'0',163),
  (163511,407,163021,163111,'FSDEL-011',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 500 DAY,163301,0,'GR-FS-DELIVERY-001',40,40,0,0,0,0,NULL,'处方药批次',8.5000,1,'delivery-demo','delivery-demo',b'0',163),
  (163512,407,163022,163112,'FSDEL-012',CURRENT_DATE-INTERVAL 120 DAY,CURRENT_DATE+INTERVAL 50 DAY,163301,0,'GR-FS-DELIVERY-003',40,40,0,0,0,1,NULL,'近效期中成药',13.5000,1,'delivery-demo','delivery-demo',b'0',163),
  (163513,407,163022,163113,'FSDEL-013',CURRENT_DATE-INTERVAL 90 DAY,CURRENT_DATE+INTERVAL 450 DAY,163301,0,'GR-FS-DELIVERY-003',30,30,0,0,0,0,NULL,'正常批次',9.0000,1,'delivery-demo','delivery-demo',b'0',163),
  (163514,407,163021,163114,'FSDEL-014',CURRENT_DATE-INTERVAL 45 DAY,CURRENT_DATE+INTERVAL 400 DAY,163301,0,'GR-FS-DELIVERY-001',50,50,0,0,0,0,NULL,'正常批次',6.0000,1,'delivery-demo','delivery-demo',b'0',163),
  (163515,407,163021,163115,'FSDEL-015',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 600 DAY,163301,0,'GR-FS-DELIVERY-001',25,25,0,0,0,0,NULL,'保健品批次',22.0000,1,'delivery-demo','delivery-demo',b'0',163),
  (163516,407,163021,163116,'FSDEL-016',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 620 DAY,163301,0,'GR-FS-DELIVERY-001',20,20,0,0,0,0,NULL,'保健品批次',35.0000,1,'delivery-demo','delivery-demo',b'0',163),
  (163517,407,163021,163117,'FSDEL-017',CURRENT_DATE-INTERVAL 30 DAY,CURRENT_DATE+INTERVAL 900 DAY,163301,0,'GR-FS-DELIVERY-001',100,100,0,0,0,0,NULL,'器械耗材批次',0.4500,1,'delivery-demo','delivery-demo',b'0',163),
  (163518,407,163021,163118,'FSDEL-018',CURRENT_DATE-INTERVAL 30 DAY,CURRENT_DATE+INTERVAL 850 DAY,163301,0,'GR-FS-DELIVERY-001',10,10,0,0,0,0,NULL,'器械批次',78.0000,1,'delivery-demo','delivery-demo',b'0',163),
  (163519,407,163023,163119,'FSDEL-019',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 40 DAY,163301,0,'GR-FS-DELIVERY-002',19,19,0,1,0,1,NOW()-INTERVAL 1 DAY,'2-8℃冷链近效期批次',38.0000,2,'delivery-demo','delivery-demo',b'0',163),
  (163520,407,163023,163120,'FSDEL-020',CURRENT_DATE-INTERVAL 45 DAY,CURRENT_DATE+INTERVAL 85 DAY,163301,0,'GR-FS-DELIVERY-002',20,20,0,0,0,1,NULL,'2-8℃冷链近效期批次',28.0000,1,'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE warehouse_id=VALUES(warehouse_id),supplier_id=VALUES(supplier_id),
  source_type=VALUES(source_type),source_no=VALUES(source_no),qty_total=VALUES(qty_total),qty_avail=VALUES(qty_avail),
  qty_frozen=VALUES(qty_frozen),qty_sold=VALUES(qty_sold),expiry_date=VALUES(expiry_date),
  expiry_status=VALUES(expiry_status),cost_price=VALUES(cost_price),version=VALUES(version),deleted=b'0',tenant_id=163;

INSERT INTO ph_inv_location_stock
  (id,batch_id,location_id,drug_id,qty,qty_frozen,creator,updater,deleted,tenant_id)
VALUES
  (163516,163506,163031,163106,78,0,'delivery-demo','delivery-demo',b'0',163),
  (163517,163507,163031,163107,60,0,'delivery-demo','delivery-demo',b'0',163),
  (163518,163508,163031,163108,50,0,'delivery-demo','delivery-demo',b'0',163),
  (163519,163509,163032,163109,38,0,'delivery-demo','delivery-demo',b'0',163),
  (163520,163510,163032,163110,30,0,'delivery-demo','delivery-demo',b'0',163),
  (163521,163511,163031,163111,40,0,'delivery-demo','delivery-demo',b'0',163),
  (163522,163512,163034,163112,40,0,'delivery-demo','delivery-demo',b'0',163),
  (163523,163513,163032,163113,30,0,'delivery-demo','delivery-demo',b'0',163),
  (163524,163514,163031,163114,50,0,'delivery-demo','delivery-demo',b'0',163),
  (163525,163515,163031,163115,25,0,'delivery-demo','delivery-demo',b'0',163),
  (163526,163516,163031,163116,20,0,'delivery-demo','delivery-demo',b'0',163),
  (163527,163517,163033,163117,100,0,'delivery-demo','delivery-demo',b'0',163),
  (163528,163518,163033,163118,10,0,'delivery-demo','delivery-demo',b'0',163),
  (163529,163519,163035,163119,19,0,'delivery-demo','delivery-demo',b'0',163),
  (163530,163520,163035,163120,20,0,'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE location_id=VALUES(location_id),drug_id=VALUES(drug_id),qty=VALUES(qty),
  qty_frozen=VALUES(qty_frozen),deleted=b'0',tenant_id=163;

INSERT INTO ph_inv_flow
  (id,store_id,batch_id,drug_id,batch_no,flow_type,in_qty,out_qty,balance_qty,biz_type,biz_no,flow_time,
   operator,remark,location_id,biz_line_id,frozen_delta,unit_cost,creator,updater,deleted,tenant_id)
VALUES
  (163801,407,163506,163106,'FSDEL-006',10,80,0,80,1,'GR-FS-DELIVERY-001',NOW()-INTERVAL 12 DAY,163014,'采购入库',163031,163434,0,5.8000,'delivery-demo','delivery-demo',b'0',163),
  (163802,407,163507,163107,'FSDEL-007',10,60,0,60,1,'GR-FS-DELIVERY-001',NOW()-INTERVAL 12 DAY,163014,'采购入库',163031,163435,0,7.5000,'delivery-demo','delivery-demo',b'0',163),
  (163803,407,163508,163108,'FSDEL-008',10,50,0,50,1,'GR-FS-DELIVERY-001',NOW()-INTERVAL 12 DAY,163014,'采购入库',163031,163436,0,6.2000,'delivery-demo','delivery-demo',b'0',163),
  (163804,407,163509,163109,'FSDEL-009',10,40,0,40,1,'GR-FS-DELIVERY-003',NOW()-INTERVAL 12 DAY,163014,'采购入库',163032,163437,0,12.0000,'delivery-demo','delivery-demo',b'0',163),
  (163805,407,163510,163110,'FSDEL-010',10,30,0,30,1,'GR-FS-DELIVERY-003',NOW()-INTERVAL 12 DAY,163014,'采购入库',163032,163438,0,21.0000,'delivery-demo','delivery-demo',b'0',163),
  (163806,407,163511,163111,'FSDEL-011',10,40,0,40,1,'GR-FS-DELIVERY-001',NOW()-INTERVAL 12 DAY,163014,'采购入库',163031,163439,0,8.5000,'delivery-demo','delivery-demo',b'0',163),
  (163807,407,163512,163112,'FSDEL-012',10,40,0,40,1,'GR-FS-DELIVERY-003',NOW()-INTERVAL 12 DAY,163014,'采购入库',163034,163440,0,13.5000,'delivery-demo','delivery-demo',b'0',163),
  (163808,407,163513,163113,'FSDEL-013',10,30,0,30,1,'GR-FS-DELIVERY-003',NOW()-INTERVAL 12 DAY,163014,'采购入库',163032,163441,0,9.0000,'delivery-demo','delivery-demo',b'0',163),
  (163809,407,163514,163114,'FSDEL-014',10,50,0,50,1,'GR-FS-DELIVERY-001',NOW()-INTERVAL 12 DAY,163014,'采购入库',163031,163442,0,6.0000,'delivery-demo','delivery-demo',b'0',163),
  (163810,407,163515,163115,'FSDEL-015',10,25,0,25,1,'GR-FS-DELIVERY-001',NOW()-INTERVAL 12 DAY,163014,'采购入库',163031,163443,0,22.0000,'delivery-demo','delivery-demo',b'0',163),
  (163811,407,163516,163116,'FSDEL-016',10,20,0,20,1,'GR-FS-DELIVERY-001',NOW()-INTERVAL 12 DAY,163014,'采购入库',163031,163444,0,35.0000,'delivery-demo','delivery-demo',b'0',163),
  (163812,407,163517,163117,'FSDEL-017',10,100,0,100,1,'GR-FS-DELIVERY-001',NOW()-INTERVAL 12 DAY,163014,'采购入库',163033,163445,0,0.4500,'delivery-demo','delivery-demo',b'0',163),
  (163813,407,163518,163118,'FSDEL-018',10,10,0,10,1,'GR-FS-DELIVERY-001',NOW()-INTERVAL 12 DAY,163014,'采购入库',163033,163446,0,78.0000,'delivery-demo','delivery-demo',b'0',163),
  (163814,407,163519,163119,'FSDEL-019',10,20,0,20,1,'GR-FS-DELIVERY-002',NOW()-INTERVAL 12 DAY,163014,'冷链采购入库',163035,163447,0,38.0000,'delivery-demo','delivery-demo',b'0',163),
  (163815,407,163520,163120,'FSDEL-020',10,20,0,20,1,'GR-FS-DELIVERY-002',NOW()-INTERVAL 12 DAY,163014,'冷链采购入库',163035,163448,0,28.0000,'delivery-demo','delivery-demo',b'0',163),
  (163816,407,163506,163106,'FSDEL-006',20,0,2,78,2,'SO-FS-DELIVERY-001',NOW()-INTERVAL 1 DAY,163013,'销售出库',163031,163728,0,5.8000,'delivery-demo','delivery-demo',b'0',163),
  (163817,407,163509,163109,'FSDEL-009',20,0,2,38,2,'SO-FS-DELIVERY-002',NOW()-INTERVAL 1 DAY,163013,'处方销售出库',163032,163729,0,12.0000,'delivery-demo','delivery-demo',b'0',163),
  (163818,407,163519,163119,'FSDEL-019',20,0,1,19,2,'SO-FS-DELIVERY-002',NOW()-INTERVAL 1 DAY,163013,'冷链处方销售出库',163035,163730,0,38.0000,'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE biz_no=VALUES(biz_no),location_id=VALUES(location_id),biz_line_id=VALUES(biz_line_id),
  in_qty=VALUES(in_qty),out_qty=VALUES(out_qty),balance_qty=VALUES(balance_qty),
  flow_time=VALUES(flow_time),unit_cost=VALUES(unit_cost),deleted=b'0',tenant_id=163;

INSERT INTO ph_inv_expiry_alert
  (id,store_id,batch_id,drug_id,alert_level,expire_days,handle_type,handle_by,handle_at,alert_date,creator,updater,deleted,tenant_id)
VALUES
  (163553,407,163512,163112,2,50,0,NULL,NULL,CURRENT_DATE,'delivery-demo','delivery-demo',b'0',163),
  (163554,407,163519,163119,3,40,1,163011,NOW(),CURRENT_DATE,'delivery-demo','delivery-demo',b'0',163),
  (163555,407,163520,163120,2,85,0,NULL,NULL,CURRENT_DATE,'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE alert_level=VALUES(alert_level),expire_days=VALUES(expire_days),
  handle_type=VALUES(handle_type),handle_by=VALUES(handle_by),handle_at=VALUES(handle_at),deleted=b'0',tenant_id=163;

-- 收货明细放在批次之后写入，create_batch_id 均指向上面的真实批次。
INSERT INTO ph_po_receipt_line
  (id,receipt_id,line_no,order_line_id,drug_id,batch_no,manufacture_date,expiry_date,qty,unit_price,amount,
   quality_flag,qa_remark,cold_chain_temp,create_batch_id,location_id,creator,updater,deleted,tenant_id)
VALUES
  (163434,163423,1,163415,163106,'FSDEL-006',CURRENT_DATE-INTERVAL 90 DAY,CURRENT_DATE+INTERVAL 540 DAY,80,5.80,464.00,1,'验收合格',NULL,163506,163031,'delivery-demo','delivery-demo',b'0',163),
  (163435,163423,2,163416,163107,'FSDEL-007',CURRENT_DATE-INTERVAL 75 DAY,CURRENT_DATE+INTERVAL 480 DAY,60,7.50,450.00,1,'验收合格',NULL,163507,163031,'delivery-demo','delivery-demo',b'0',163),
  (163436,163423,3,163417,163108,'FSDEL-008',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 420 DAY,50,6.20,310.00,1,'验收合格',NULL,163508,163031,'delivery-demo','delivery-demo',b'0',163),
  (163437,163425,1,163418,163109,'FSDEL-009',CURRENT_DATE-INTERVAL 80 DAY,CURRENT_DATE+INTERVAL 360 DAY,40,12.00,480.00,1,'验收合格',NULL,163509,163032,'delivery-demo','delivery-demo',b'0',163),
  (163438,163425,2,163419,163110,'FSDEL-010',CURRENT_DATE-INTERVAL 70 DAY,CURRENT_DATE+INTERVAL 390 DAY,30,21.00,630.00,1,'验收合格',NULL,163510,163032,'delivery-demo','delivery-demo',b'0',163),
  (163439,163423,4,163420,163111,'FSDEL-011',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 500 DAY,40,8.50,340.00,1,'验收合格',NULL,163511,163031,'delivery-demo','delivery-demo',b'0',163),
  (163440,163425,3,163421,163112,'FSDEL-012',CURRENT_DATE-INTERVAL 120 DAY,CURRENT_DATE+INTERVAL 50 DAY,40,13.50,540.00,1,'验收合格，近效期',NULL,163512,163034,'delivery-demo','delivery-demo',b'0',163),
  (163441,163425,4,163422,163113,'FSDEL-013',CURRENT_DATE-INTERVAL 90 DAY,CURRENT_DATE+INTERVAL 450 DAY,30,9.00,270.00,1,'验收合格',NULL,163513,163032,'delivery-demo','delivery-demo',b'0',163),
  (163442,163423,5,163423,163114,'FSDEL-014',CURRENT_DATE-INTERVAL 45 DAY,CURRENT_DATE+INTERVAL 400 DAY,50,6.00,300.00,1,'验收合格',NULL,163514,163031,'delivery-demo','delivery-demo',b'0',163),
  (163443,163423,6,163424,163115,'FSDEL-015',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 600 DAY,25,22.00,550.00,1,'验收合格',NULL,163515,163031,'delivery-demo','delivery-demo',b'0',163),
  (163444,163423,7,163425,163116,'FSDEL-016',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 620 DAY,20,35.00,700.00,1,'验收合格',NULL,163516,163031,'delivery-demo','delivery-demo',b'0',163),
  (163445,163423,8,163426,163117,'FSDEL-017',CURRENT_DATE-INTERVAL 30 DAY,CURRENT_DATE+INTERVAL 900 DAY,100,0.45,45.00,1,'验收合格',NULL,163517,163033,'delivery-demo','delivery-demo',b'0',163),
  (163446,163423,9,163427,163118,'FSDEL-018',CURRENT_DATE-INTERVAL 30 DAY,CURRENT_DATE+INTERVAL 850 DAY,10,78.00,780.00,1,'验收合格',NULL,163518,163033,'delivery-demo','delivery-demo',b'0',163),
  (163447,163424,1,163428,163119,'FSDEL-019',CURRENT_DATE-INTERVAL 60 DAY,CURRENT_DATE+INTERVAL 40 DAY,20,38.00,760.00,1,'冷链验收合格',5.20,163519,163035,'delivery-demo','delivery-demo',b'0',163),
  (163448,163424,2,163429,163120,'FSDEL-020',CURRENT_DATE-INTERVAL 45 DAY,CURRENT_DATE+INTERVAL 85 DAY,20,28.00,560.00,1,'冷链验收合格',4.80,163520,163035,'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE receipt_id=VALUES(receipt_id),line_no=VALUES(line_no),order_line_id=VALUES(order_line_id),
  drug_id=VALUES(drug_id),qty=VALUES(qty),unit_price=VALUES(unit_price),amount=VALUES(amount),
  quality_flag=VALUES(quality_flag),cold_chain_temp=VALUES(cold_chain_temp),create_batch_id=VALUES(create_batch_id),
  location_id=VALUES(location_id),deleted=b'0',tenant_id=163;

-- E 处方：当前模型没有处方明细表，页面按 prescribed_items JSON 展示真实药品关联。
INSERT INTO ph_presc_record
  (id,presc_no,store_id,source,hospital,doctor_name,patient_name,patient_age,patient_id_no,diagnosis,usage_desc,
   presc_date,image_url,review_status,pharmacist_id,review_at,review_opinion,review_snapshot,is_special,
   dbl_check_by,limit_check,status,wx_member_id,images,prescribed_items,creator,updater,deleted,tenant_id)
VALUES
  (163761,'PX-FS-DELIVERY-001',407,0,'演示社区健康中心','演示医师甲','演示患者甲',45,NULL,'演示处方记录','遵医嘱使用',CURRENT_DATE-INTERVAL 2 DAY,NULL,0,NULL,NULL,NULL,NULL,0,NULL,0,0,NULL,JSON_ARRAY(),JSON_ARRAY(JSON_OBJECT('drugId',163110,'drugName','缬沙坦胶囊','specification','80mg*14粒/盒','quantity',1,'usage','遵医嘱')),'delivery-demo','delivery-demo',b'0',163),
  (163762,'PX-FS-DELIVERY-002',407,1,'演示综合门诊','演示医师乙','演示患者乙',51,NULL,'演示已审核处方','遵医嘱使用',CURRENT_DATE-INTERVAL 1 DAY,NULL,1,163012,NOW()-INTERVAL 1 DAY,'用药信息完整，演示审核通过','演示药师电子签名',0,NULL,0,1,NULL,JSON_ARRAY(),JSON_ARRAY(JSON_OBJECT('drugId',163109,'drugName','阿莫西林胶囊','specification','0.25g*24粒/盒','quantity',2,'usage','遵医嘱'),JSON_OBJECT('drugId',163119,'drugName','重组人胰岛素注射液','specification','3ml:300单位/支','quantity',1,'usage','遵医嘱')),'delivery-demo','delivery-demo',b'0',163),
  (163763,'PX-FS-DELIVERY-003',407,0,'演示社区健康中心','演示医师丙','演示患者丙',38,NULL,'演示驳回处方','信息待补充',CURRENT_DATE,NULL,2,163012,NOW(),'演示数据：用法信息不完整','演示药师电子签名',0,NULL,0,0,NULL,JSON_ARRAY(),JSON_ARRAY(JSON_OBJECT('drugId',163111,'drugName','盐酸二甲双胍片','specification','0.5g*30片/盒','quantity',1,'usage','待补充')),'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE review_status=VALUES(review_status),pharmacist_id=VALUES(pharmacist_id),
  review_at=VALUES(review_at),review_opinion=VALUES(review_opinion),status=VALUES(status),
  prescribed_items=VALUES(prescribed_items),deleted=b'0',tenant_id=163;

-- D/F 销售与会员关联：一笔 OTC 会员销售，一笔关联已审核处方的冷链处方销售。
INSERT INTO ph_sale_order
  (id,order_no,store_id,pos_no,shift_id,cashier_id,pharmacist_id,member_id,customer_name,source,sale_type,
   return_flag,total_qty,subtotal,discount_amount,coupon_amount,points_deduct,payable_amount,paid_amount,
   change_amount,cost_amount,points_earned,status,offline_flag,remark,sale_time,creator,updater,deleted,tenant_id)
VALUES
  (163717,'SO-FS-DELIVERY-001',407,'POS-01',163703,163013,NULL,163611,NULL,0,0,0,2,25.60,0,0,0,25.60,25.60,0,11.60,26,1,0,'交付版OTC会员销售',NOW()-INTERVAL 1 DAY,'delivery-demo','delivery-demo',b'0',163),
  (163718,'SO-FS-DELIVERY-002',407,'POS-01',163703,163013,163012,163612,NULL,0,0,0,3,107.60,0,0,0,107.60,107.60,0,62.00,108,1,0,'交付版处方及冷链销售',NOW()-INTERVAL 1 DAY,'delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE member_id=VALUES(member_id),total_qty=VALUES(total_qty),payable_amount=VALUES(payable_amount),
  paid_amount=VALUES(paid_amount),cost_amount=VALUES(cost_amount),points_earned=VALUES(points_earned),
  status=1,sale_time=VALUES(sale_time),deleted=b'0',tenant_id=163;

INSERT INTO ph_sale_order_line
  (id,order_id,line_no,drug_id,batch_id,batch_no,expiry_date,qty,price,normal_price,discount,line_amount,
   cost_price,is_rx,presc_id,returned_qty,is_gift,location_id,drug_name,specification,unit,creator,updater,deleted,tenant_id)
VALUES
  (163728,163717,1,163106,163506,'FSDEL-006',CURRENT_DATE+INTERVAL 540 DAY,2,12.80,12.80,0,25.60,5.80,0,NULL,0,0,163031,'对乙酰氨基酚片','0.5g*20片/盒','盒','delivery-demo','delivery-demo',b'0',163),
  (163729,163718,1,163109,163509,'FSDEL-009',CURRENT_DATE+INTERVAL 360 DAY,2,25.80,25.80,0,51.60,12.00,1,163762,0,0,163032,'阿莫西林胶囊','0.25g*24粒/盒','盒','delivery-demo','delivery-demo',b'0',163),
  (163730,163718,2,163119,163519,'FSDEL-019',CURRENT_DATE+INTERVAL 40 DAY,1,56.00,56.00,0,56.00,38.00,1,163762,0,0,163035,'重组人胰岛素注射液','3ml:300单位/支','支','delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE drug_id=VALUES(drug_id),batch_id=VALUES(batch_id),qty=VALUES(qty),price=VALUES(price),
  line_amount=VALUES(line_amount),presc_id=VALUES(presc_id),location_id=VALUES(location_id),
  drug_name=VALUES(drug_name),deleted=b'0',tenant_id=163;

INSERT INTO ph_sale_payment
  (id,order_id,pay_no,pay_method,pay_amount,channel,status,paid_at,payment_no,creator,updater,deleted,tenant_id)
VALUES
  (163737,163717,'CASH-DELIVERY-001',1,25.60,'cash',1,NOW()-INTERVAL 1 DAY,'PAY-FS-DELIVERY-001','delivery-demo','delivery-demo',b'0',163),
  (163738,163718,'CARD-DELIVERY-002',4,107.60,'bank',1,NOW()-INTERVAL 1 DAY,'PAY-FS-DELIVERY-002','delivery-demo','delivery-demo',b'0',163)
ON DUPLICATE KEY UPDATE order_id=VALUES(order_id),pay_amount=VALUES(pay_amount),status=1,
  paid_at=VALUES(paid_at),deleted=b'0',tenant_id=163;

-- F 会员：member_level 与 member_user 均沿用框架状态口径（0=启用、1=禁用）。
UPDATE member_level
SET status=0,updater='delivery-demo',deleted=b'0'
WHERE tenant_id=163 AND id IN (163601,163602,163603);

UPDATE member_user
SET status=0,updater='delivery-demo',deleted=b'0'
WHERE tenant_id=163 AND id IN (163611,163612,163613,163614);

COMMIT;
