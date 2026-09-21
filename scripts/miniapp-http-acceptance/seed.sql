-- Synthetic data ONLY, empty isolated database. No upserts: reused databases must fail.
-- Schema comes from existing baseline/migrations; do not weaken constraints to pass acceptance.
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO system_tenant (id,name,contact_name,status,websites,package_id,expire_time,account_count)
VALUES (901,'HTTP acceptance A','Synthetic',0,'[]',0,DATE_ADD(NOW(),INTERVAL 1 YEAR),10),
       (902,'HTTP acceptance B','Synthetic',0,'[]',0,DATE_ADD(NOW(),INTERVAL 1 YEAR),10);
INSERT INTO system_oauth2_client
 (id,client_id,secret,name,logo,status,access_token_validity_seconds,refresh_token_validity_seconds,redirect_uris,authorized_grant_types)
VALUES (901,'default','__OAUTH_SECRET__','Synthetic acceptance','',0,3600,7200,'[]','["password","refresh_token"]');
INSERT INTO member_level (id,name,level,experience,discount_percent,status,tenant_id)
VALUES (9001,'Synthetic standard',1,0,100,0,901);
INSERT INTO member_user (id,mobile,register_ip,nickname,status,point,level_id,tenant_id)
VALUES (90011,'19900009011','127.0.0.1','Synthetic owner',0,500,9001,901),
       (90012,'19900009012','127.0.0.1','Synthetic other member',0,500,9001,901),
       (90021,'19900009021','127.0.0.1','Synthetic other tenant',0,500,NULL,902);
INSERT INTO ph_store (id,store_code,store_name,address,status,tenant_id)
VALUES (9101,'HTTP-A','Synthetic pharmacy','Synthetic address',1,901);
INSERT INTO ph_warehouse (id,store_id,wh_code,wh_name,status,tenant_id)
VALUES (9201,9101,'HTTP-W','Synthetic warehouse',1,901);
INSERT INTO ph_location (id,warehouse_id,location_code,status,tenant_id)
VALUES (9202,9201,'HTTP-L',1,901);
INSERT INTO ph_category (id,cat_code,cat_name,status,tenant_id)
VALUES (9300,'HTTP-C','Synthetic OTC',1,901);
INSERT INTO ph_drug
 (id,drug_code,category_id,generic_name,specification,drug_type,is_rx,unit,retail_price,member_price,cost_price,status,approve_status,saleable_online,tenant_id)
VALUES (9301,'HTTP-OTC',9300,'Synthetic OTC - not for clinical use','10 tablets',1,0,'box',12.34,12.34,5.00,1,1,1,901);
INSERT INTO ph_inv_batch
 (id,store_id,warehouse_id,drug_id,batch_no,expiry_date,source_type,source_no,qty_total,qty_avail,qty_frozen,qty_sold,quality_status,cost_price,tenant_id)
VALUES (9401,9101,9201,9301,'HTTP-BATCH',DATE_ADD(CURRENT_DATE,INTERVAL 1 YEAR),3,'HTTP-OPENING',50,50,0,0,0,5.00,901);
INSERT INTO ph_inv_location_stock (id,batch_id,location_id,drug_id,qty,qty_frozen,tenant_id)
VALUES (9402,9401,9202,9301,50,0,901);
INSERT INTO ph_inv_flow
 (store_id,batch_id,drug_id,batch_no,flow_type,in_qty,out_qty,balance_qty,biz_type,biz_no,biz_line_id,flow_time,operator,location_id,unit_cost,tenant_id)
VALUES (9101,9401,9301,'HTTP-BATCH',70,50,0,50,7,'HTTP-OPENING',9402,NOW(),0,9202,5.00,901);
-- No fake successful callback receiver. The current business callback route is missing;
-- task creation is checked, callback delivery remains a documented gap.
INSERT INTO pay_app (id,name,status,remark,order_notify_url,refund_notify_url,transfer_notify_url,tenant_id,app_key)
VALUES (9501,'Synthetic payment',0,'Mock only',
 'http://backend:48080/app-api/member/wx-order/payment-notify',
 'http://backend:48080/app-api/member/wx-order/refund-notify',
 'http://backend:48080/app-api/member/wx-order/transfer-notify',901,'firstsun-acceptance');
INSERT INTO pay_channel (id,code,status,remark,fee_rate,app_id,config,tenant_id)
VALUES (9502,'mock',0,'Synthetic in-process mock; no real money',0,9501,
 '{"@class":"cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.NonePayClientConfig"}',901);
COMMIT;
-- Last statement marks initialization complete for health/identity checks.
CREATE TABLE acceptance_marker (run_id CHAR(32) PRIMARY KEY, schema_version INT NOT NULL);
INSERT INTO acceptance_marker VALUES ('__RUN_ID__',1);
