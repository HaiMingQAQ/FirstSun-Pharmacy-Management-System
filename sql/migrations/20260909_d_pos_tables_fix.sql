-- ============================================================
-- FirstSun 药店管理系统 | D 成员增量脚本
-- 20260909_d_pos_tables_fix.sql
-- 说明：POS 明细表结构微调（3 处，均已幂等）
-- 1. ph_sale_order_line.expiry_date  → 可空
--    背景：销售行效期快照依赖库存域（C 成员 InventoryFacade），DeductResult
--    契约当前不含效期字段；C 未实现期间 D 无法获得效期，NOT NULL 无默认值
--    会导致销售行插入失败（接口 500）。待 C 提供效期契约后可回填并收紧。
-- 2. ph_sale_order_line.location_id  → 可空
--    背景：出库货位由库存域确定，D 收银录入时不掌握。
-- 3. ph_sale_return_line.location_id → 可空
--    背景：验收回库货位由库存域确定，D 退货时不掌握。
-- 注意：请 A 成员同步更新 sql/firstsun_pharmacy_init.sql 中对应列定义
-- ============================================================
ALTER TABLE `ph_sale_order_line`
    MODIFY COLUMN `expiry_date` date NULL COMMENT '效期快照(库存域未就绪时可为空)';

ALTER TABLE `ph_sale_order_line`
    MODIFY COLUMN `location_id` bigint NULL COMMENT '出库货位(库存域确定,可空)';

ALTER TABLE `ph_sale_return_line`
    MODIFY COLUMN `location_id` bigint NULL COMMENT '验收回库货位(可空)';
