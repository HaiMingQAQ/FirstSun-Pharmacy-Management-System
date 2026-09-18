SET NAMES utf8mb4;

-- ============================================================
-- E-2(P0) 演示库存数据货位错位纠正迁移（E 成员维护）
-- 问题：货位 163032(A-02-01, 阴凉) 错误挂在仓库 163021(中心店常温库) 下，
--       而批次 163503(FSBATCH-003, 奥美拉唑) 在仓库 163022(中心店阴凉库)，
--       货位库存 163513 / 流水 163526/163527 / 盘点 163535 均指向 163032，
--       导致"批次仓库 ≠ 货位所属仓库"的库存校验失败，阻断处方药销售。
-- 根因：演示数据生成源 20260914_l_pharmacy_demo_data.sql 中 163032 的
--       warehouse_id 误写为 163021，已同步修正生成源；本迁移负责纠正已落库环境。
-- 幂等：WHERE 限定"仍指向 163021 且未删除"才更新，重复执行无副作用。
-- ============================================================

UPDATE `ph_location`
SET `warehouse_id` = 163022,
    `updater` = 'demo',
    `update_time` = NOW()
WHERE `id` = 163032
  AND `warehouse_id` = 163021
  AND `deleted` = b'0';

-- 复查（只读，不修改数据；预期应全部为 163022）
-- SELECT id, warehouse_id, location_code, location_type
--   FROM ph_location WHERE id = 163032;            -- 预期 163022
-- SELECT id, warehouse_id, drug_id, batch_no, qty
--   FROM ph_stock WHERE id = 163503;               -- 预期 163022
-- SELECT id, batch_id, location_id, qty
--   FROM ph_location_stock WHERE id = 163513;      -- 预期 location_id = 163032（属 163022）
-- SELECT id, batch_id, location_id, biz_no, change_type
--   FROM ph_stock_movement WHERE batch_id = 163503; -- 预期 location_id 均为 163032
-- 校验：批次仓库(ph_stock.warehouse_id)、货位所属仓库(ph_location.warehouse_id)、
--       货位库存(ph_location_stock.location_id)、流水(ph_stock_movement.location_id) 一致。
