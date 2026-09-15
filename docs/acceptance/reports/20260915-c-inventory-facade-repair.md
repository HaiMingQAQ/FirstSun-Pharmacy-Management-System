# ACC-20260915-001 库存门面修复记录

修复日期：2026-09-15。对应主验收报告中的 C/P1 缺陷。本文只记录本次修复证据，不替代主验收报告。

## 修复内容

- 新增 `InventoryFacadeAdapter` 作为 Spring `@Service`。`FacadeFallbackConfiguration` 会因此不再注册会抛“库存服务未就绪”的 fallback。
- `receive`：锁定仓库、货位、批次和货位库存；写入批次库存、货位库存和收货流水。
- `deduct`：未指定批次/货位时按效期、批次、货位排序 FEFO；扣减可用量，禁止扣减冻结量、过期批次、停售批次和停用仓库/货位。
- `returnBack`：锁定原销售出库流水；累计回补不得超过该流水的出库量；回补原批次和原货位。
- 三个入口均先按来源单号、来源行查询流水。已有完整流水直接返回，部分流水拒绝重选批。
- `DeductItem`、`ReturnBackItem` 增加来源单据和来源行字段；`DeductResult` 返回实际 FEFO 分配。D 的销售、退货调用点改为传递已落库的销售/退货行 ID 和原销售引用。
- 新增 `original_flow_id`，使退货流水精确关联原销售流水。

## 迁移

先执行既有库存迁移，再执行：

```text
sql/migrations/20260915_c_inventory_facade_flow_ref.sql
```

脚本用临时存储过程检查列和索引，可重复运行；本机 MySQL 8.0.46 已连续执行两次，`original_flow_id` 和 `idx_inv_flow_original` 均各保留一份。

## 已执行验证

- `mvn -pl yudao-module-pharmacy -am -Dtest=InventoryFacadeAdapterMysqlIT -Dsurefire.failIfNoSpecifiedTests=false test`
  - 真实本机 Docker MySQL：1/1 通过。
  - 单一事务内执行收货、FEFO 销售、原销售退货、三次重复调用、超额退货拒绝。
  - 验证 `qty_total = qty_avail + qty_frozen`、批次库存等于货位库存汇总、`qty_sold` 销售增加且退货减少、流水唯一性。
  - 事务显式回滚后仓库、批次、货位库存、流水均为 0 行。
- `SaleOrderServiceImplTest`、`SaleReturnServiceImplTest`：14/14 通过，验证 D 调用 C 时传递销售单号、销售行 ID、退货单号、退货行 ID 和原销售引用。
- Docker Java 17：`docker compose build backend` 成功；`docker compose up -d backend` 成功，后端完成 Spring 启动。

## 待主验收复验

- 使用完整 B 采购收货单和 D POS 销售/退货页面做 HTTP 成功链路复验。当前本机库无采购、销售业务夹具，未把 Adapter 级真实事务测试写成页面联调通过。
- F 的线上订单冻结、释放、冻结转出库仍属 P2 生命周期契约，未在本次 C/P1 修复中扩展。
- `/actuator/health` 返回 404 是 ACC-20260915-004（A）的健康检查问题，不属于本次库存修复。
