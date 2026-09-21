-- =====================================================================
-- 20260919_f_wx_miniapp_app_api.sql
-- F（小程序/会员端）维护：为药店小程序课堂演示补齐演示数据可用性
-- ---------------------------------------------------------------------
-- 背景：
--   小程序「处方药闭环」需要一款可在线下单的处方药（Rx）。
--   演示数据 163103 洛赛克（奥美拉唑肠溶胶囊）is_rx=1 但 saleable_online=0，
--   小程序端药品列表/下单会将其过滤为不可售，导致闭环四无法跑通。
-- 可选演示数据操作，不是结构迁移。禁止挂载到自动初始化或生产升级流程。
-- 仅在独立测试数据库中显式设置三个会话变量后执行：
-- @firstsun_demo_seed_enabled = 1、@firstsun_demo_tenant_id、@firstsun_demo_drug_id。
-- 未设置变量时不更新任何行；数据库由连接参数明确指定，本文件不包含 USE。
-- =====================================================================

UPDATE ph_drug
SET saleable_online = 1
WHERE @firstsun_demo_seed_enabled = 1
  AND id = @firstsun_demo_drug_id
  AND tenant_id = @firstsun_demo_tenant_id
  AND is_rx = 1
  AND status = 1 AND approve_status = 1 AND deleted = 0
  AND saleable_online = 0;

-- 首次最多影响 1 行；重复执行为 0 行。执行前必须由测试维护者确认目标药品与租户。
