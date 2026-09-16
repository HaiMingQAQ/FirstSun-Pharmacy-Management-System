# F 自测记录：线上订单库存冻结闭环（接入 C 的冻结接口）

日期：2026-09-16。分支：`feat/f-wx-order-reservation`。基线：`main` @ `fc1e4324`（含 C 的 PR #31/#33 与本人的 PR #32）。

范围：接入 C 新增的 `reserve` / `release` / `consumeReservation` / `getAvailableQty`，补齐「下单冻结 → 支付转出库 → 取消 / 超时释放 → 退款回补」。**不包含**已合并的 ACC-20260915-007 / 008 与积分门面（本 PR 未重复修改）。

## 1. 修改文件

| 文件 | 变更 |
| --- | --- |
| `backend/.../service/member/WxOrderService.java` | 新增 `reserveWxOrder`、`cancelWxOrderByMember`、`closeExpiredWxOrders`、`releaseFrozenStockOfClosedOrders` 契约 |
| `backend/.../service/member/WxOrderServiceImpl.java` | 冻结 / 转出库 / 释放 / 回补四段作业与分流结算；支付改为「冻结转出库」；新增出库来源行号落库 |
| `backend/.../dal/dataobject/member/WxOrderLineAllocDO.java` | 状态语义扩展为 0 已冻结 / 1 已出库 / 2 已释放或已回补；新增 `outBizLineId` |
| `backend/.../dal/mysql/member/WxOrderMapper.java` | 新增「门店 + 超时未支付」「门店 + 状态」查询，供门店清理节点使用 |
| `backend/.../controller/admin/member/WxOrderController.java` | 新增 `PUT /reserve`、`PUT /close-expired`、`PUT /release-frozen` |
| `backend/.../controller/app/member/AppWxOrderController.java` | 会员取消改走 `cancelWxOrderByMember`（不做库存作业） |
| `backend/.../src/test/.../WxOrderStockLifecycleTest.java` | 10 项扩到 18 项：冻结、可售量不足拒绝、冻结幂等、转出库、释放、会员取消无库存动作、超时关闭、批量只释放冻结 |
| `sql/migrations/20260916_f_wx_order_alloc_frozen_stage.sql` | 新增：补入「已冻结」阶段语义（v2，历史状态 +1） |
| `sql/migrations/20260916_f_wx_order_alloc_out_ref.sql` | 新增：补入 `out_biz_line_id` 并按库存流水回填（v3） |
| `docker-compose.yml` | 挂载上述两份迁移 |
| `docs/F-member/线上订单库存生命周期与接口需求.md` | 更新门禁结论与落地情况 |

## 2. 根因与实现

### 2.1 门禁决定了架构：冻结只能落在门店节点

C 的 `InventoryReadAccess.requireScope` 要求「有效租户 + ADMIN 登录用户 + 目标门店在职员工」，且新增的 4 个方法（含只读 `getAvailableQty`）全部复用该门禁；C 的 `docs/C-inventory/15-线上订单冻结接口.md` 明确「F 的管理端订单节点可以直接调用；小程序回调、无人定时任务需要受控的服务身份，不能绕过库存门店隔离」。

因此 F 侧把整条库存生命周期放在**门店管理端节点**，小程序端不触碰库存：

| 节点 | 接口 | 库存动作 |
| --- | --- | --- |
| 门店接单冻结 | `PUT /pharmacy/member/order/reserve` | `getAvailableQty` 粗校验 + `reserve`（FEFO） |
| 门店确认收款 | `PUT /pharmacy/member/order/pay` | `consumeReservation`（未冻结订单退化为 `deduct`） |
| 门店取消 / 退款 | `PUT .../cancel`、`PUT .../refund` | 仍冻结 → `release`；已出库 → `returnBack` |
| 门店清理超时 | `PUT .../close-expired`、`PUT .../release-frozen` | 关闭超时未支付订单 + `release` |
| 会员自行取消 | `PUT /app-api/.../cancel` | 只关闭订单，不动库存 |

### 2.2 分配表生命周期与幂等

`ph_wx_order_line_alloc` 的分配从冻结开始产生，状态推进为 0 已冻结 → 1 已出库 → 2 已释放或已回补。幂等由三层保证：订单状态条件更新、分配表状态、C 的 `uk_flow_event`（业务号 + 行号 + 批次 + 货位 + 流水类型，80/81/82 可共用同一业务号与行号）。

### 2.3 实测发现的契约耦合：回补的 `originalBizLineId` 取错

首次实测退款时报 `库存作业失败：未找到原销售出库流水`。根因：C 的 `returnBack` 用 `originalBizNo + originalBizLineId` 反查原出库流水，而「冻结转出库」（流水 82）为保证同一明细拆多批次时行号唯一，用的行号是**分配记录编号**；「直接扣库」（流水 20）用的是**订单明细编号**。F 原先统一按订单明细编号提交，遇到冻结链路必然查不到流水。

修复：分配表新增 `out_biz_line_id` 显式记录「正式出库流水的来源行号」（转出库=分配记录编号，直接扣库=订单明细编号），回补时按它提交 `originalBizLineId`；迁移用 `ph_inv_flow`（flow_type 20/82）回填历史数据，取不到流水时退回订单明细编号。

## 3. 自动化测试结果

```text
mvn -pl yudao-module-pharmacy test -Dtest=WxOrderStockLifecycleTest,MemberPointRecordServiceImplTest,MemberUserServiceImplTest
Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

其中 `WxOrderStockLifecycleTest` 18 项，覆盖：冻结落库与幂等、可售量不足拒绝、支付走冻结转出库（含 `originalBizNo/originalBizLineId` 与出库来源行号断言）、未冻结订单退化为直接扣库、库存不足明确报错、取消时「冻结释放 / 已出库回补」分流、重复取消 / 重复退款幂等、已了结分配跳过、会员取消无库存动作、超时关闭、批量只释放冻结。

## 4. Docker 实际验证结果

环境：真实 MySQL/Redis + 共享账号（租户 FirstSun，`0407/123456`，门店 407）；批次 163504 与货位 163034 初始 30/30/0/0；药品 163104，数量 2。后端为本次构建产物。

| 场景 | 步骤 | 结果 |
| --- | --- | --- |
| healthcheck | `GET /actuator/health` | HTTP 200 |
| 冻结 | `PUT /reserve`（两次） | `code=0`；批次 `avail 30→28`、`frozen 0→2`、`total` 不变；货位 `qty` 不变、`qty_frozen=2`；分配 `status=0`；流水 `80`、`biz_line_id=订单明细ID`、`frozen_delta=+2`；**两次调用只有 1 条流水**（幂等） |
| 支付转出库 | `PUT /pay`（两次） | `code=0`；批次 `total 30→28`、`frozen 2→0`、`sold 0→2`、`avail` 不变；分配 `status=1`、`out_biz_line_id=分配记录ID`；新增流水 `82`、`original_flow_id` 指向 80；**两次调用只有 1 条 82 流水** |
| 退款回补 | `PUT /refund`（两次） | `code=0`；订单 → 取消 / 已退款；批次按 `total+2 / avail+2 / sold-2` 回补；分配 `status=2`、`returned_qty=2`；新增流水 `21`、`original_flow_id` 指向 82；**两次调用只有 1 条 21 流水** |
| 超时释放 | `PUT /close-expired`、`PUT /release-frozen`（两次） | 关闭超时未支付订单（返回 2）；释放冻结返回 1、重复调用返回 0；批次 `avail 26→28`、`frozen 2→0`；分配 `status=2`；新增流水 `81`、`biz_no=WXC-订单号`、`original_flow_id` 指向 80；**无重复流水** |
| 账务不变式 | 每步校验 | 全程满足 `qty_total = qty_avail + qty_frozen`，无负数、无冻结量超库存 |
| 事务与清理 | — | 首次退款失败时订单状态与库存均未变化（失败整体回滚）；夹具与本次流水已清理，`remaining=0`，批次恢复 30/30/0/0 |

## 5. 尚未解决的问题

1. **小程序下单即冻结**：受门禁限制，冻结只能在门店节点完成。若要「下单即冻结」，需要 C 接受受控服务身份（C 已明确不做绕过）。
2. **无人值守的超时释放**：定时任务没有库存作业身份，当前由门店清理节点承担（`close-expired` + `release-frozen`）；若要自动化，同样需要服务身份。
3. **`getAvailableQty` 口径较粗**：C 的可售量是门店级 `qty_avail` 汇总，不校验效期与质量状态、也不看货位，只能做快速拒绝，权威判断由 `reserve` 完成；因此「下单前校验」只能在门店冻结节点做。
4. **完成态订单退货**：出库已转销售，需走 D 的销售退货流程。
5. **渠道退款**：`refundWxOrder` 会调用 E 的 `PaymentFacade.refund`；线上订单的渠道支付单号目前写在 `pay_no`（`pay_order_id` 为空），因此挂单退款不会真正打到渠道，属 F 侧待办（本次未改，避免与 E 的字段语义冲突）。
6. **`ph_inv_lock` 未被使用**：C 的冻结实现不写该表，订单锁状态只能以 F 订单表 + 库存流水 80/81/82 为准，三账核对看不到线上订单冻结锁。
7. **`docker compose build backend` 仍未重跑**：本轮以同命令的本地全量 `mvn package` + 容器内替换产物验证运行态。

## 6. 是否建议创建 Pull Request

建议创建。改动集中在 F 的会员订单域，未修改 A/B/C/D/E 的实现（迁移只新增 F 自有表结构与列）；后端相关测试 36 项全绿；冻结、转出库、释放、回补四条链路均在真实 Docker 环境端到端验证且幂等。唯一需要评审关注的是 `sql/migrations/` 下两份 F 迁移（v2 状态语义 + v3 出库来源行号）的执行顺序与回填逻辑。
