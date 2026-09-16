# F 自测记录：会员积分门面与线上订单库存闭环

日期：2026-09-16。分支：`feat/f-member-points-inventory`。基线：`main` @ `577c422f`。

范围：会员积分门面实现、线上订单库存生命周期（支付出库 / 取消退款回补）、F 相关前端类型错误。**不包含** ACC-20260915-007 / ACC-20260915-008（已随 PR #28 合并，本次未重复修改）。

## 1. 修改文件

后端主代码：

| 文件 | 变更 |
| --- | --- |
| `backend/.../enums/ErrorCodeConstants.java` | 新增积分不足（`1_030_008_002`）、返还超限（`1_030_008_003`）、订单库存不足（`1_030_011_011`）、库存作业失败（`1_030_011_012`）、出库分配缺失（`1_030_011_013`） |
| `backend/.../api/member/MemberPointFacade.java` | 新增 `deductPoints`（积分抵扣扣减）、`returnPoints`（取消 / 退货返还） |
| `backend/.../api/member/MemberPointFacadeAdapter.java` | 实现新增的 2 个门面方法 |
| `backend/.../api/member/MemberPointFacadeImpl.java` | 降级实现补齐新方法（保持装配兜底语义） |
| `backend/.../service/member/MemberPointRecordService.java` | 新增 `deductPoints`、`returnPoints` 契约与幂等 / 边界说明 |
| `backend/.../service/member/MemberPointRecordServiceImpl.java` | 积分变动统一入口改造：幂等键改为 (会员, 业务类型, 业务单号)、扣减支持「积分不足抛业务异常」、返还上限校验 |
| `backend/.../dal/mysql/member/MemberPointRecordMapper.java` | 新增按 (会员, 业务类型, 业务单号) 的查询 |
| `backend/.../dal/dataobject/member/WxOrderLineAllocDO.java` | 新增：出库分配 DO |
| `backend/.../dal/mysql/member/WxOrderLineAllocMapper.java` | 新增：出库分配 Mapper |
| `backend/.../service/member/WxOrderService.java` | 新增 `refundWxOrder`；`cancelWxOrder` 语义补充 |
| `backend/.../service/member/WxOrderServiceImpl.java` | 支付 / 取消 / 退款加事务；扣库请求补齐 `bizNo` + `bizLineId`；回补请求补齐 `originalBizNo` + `originalBizLineId` + 原批次原货位；出库分配落库；库存异常翻译 |
| `backend/.../controller/admin/member/WxOrderController.java` | 新增 `PUT /refund` 退款接口 |

测试：

| 文件 | 变更 |
| --- | --- |
| `backend/.../src/test/.../service/member/MemberPointRecordServiceImplTest.java` | 由 7 项扩到 14 项：新增抵扣、积分不足、抵扣幂等、返还幂等、返还上限、无抵扣记录等 |
| `backend/.../src/test/.../service/member/WxOrderStockLifecycleTest.java` | 新增 10 项：支付出库、多批次分配、重复回调、库存不足、明细缺失、取消回补、重复取消、已回补跳过、未支付无动作、退款只回补一次、已完成拒绝退款 |

其他：

| 文件 | 变更 |
| --- | --- |
| `sql/migrations/20260916_f_wx_order_line_alloc.sql` | 新增 F 自有出库分配表（可重复执行） |
| `docker-compose.yml` | 挂载上述迁移（`25-pharmacy-wx-alloc.sql`） |
| `admin-ui/src/views/pharmacy/member/MemberForm.vue` | `el-avatar size="80"` → `:size="80"`，修掉 F 页面残留类型错误 |
| `docs/F-member/线上订单库存生命周期与接口需求.md` | 新增：给 C 的接口需求与门禁问题说明 |

## 2. 根因与实现

### 2.1 积分门面「未实现」

原 `MemberPointFacade` 只有加分 `addPoints` 与扣回 `backPoints`，缺少「订单使用积分抵扣」与「取消 / 退货返还」两类场景，且实现类为抛 `UnsupportedOperationException` 的降级 Bean。

实现要点：

- 幂等键与数据库唯一索引 `member_point_record.uk_point_event(tenant_id,user_id,biz_type,biz_id)` 对齐，改为按「会员 + 业务类型 + 业务单号」判重。原实现按「会员 + 业务单号」判重，会让同一订单的「消费获得」与「消费抵扣」互相顶掉。
- 业务类型对齐字典 `pharmacy_member_point_biz_type`：2 消费获得、3 消费抵扣、6 退款冲回。原 `backPoints` 用的是 3（消费抵扣），与「退款冲回」语义不符，本次修正为 6。
- 积分不足：`deductPoints` 抛 `PHARMACY_MEMBER_POINT_NOT_ENOUGH`（业务异常），调用方 `@Transactional` 整体回滚，不会出现「订单成功但积分未扣减」。
- 返还：`returnPoints` 以「该业务单号已抵扣积分」为上限，幂等键保证同一单号只返还一次，不超返。
- 每次变动都写流水，且 `biz_id` 记录业务单号，`total_point` 记录变动后积分。

### 2.2 线上订单库存生命周期

**根因 A：扣库请求不符合 C 的契约。** `WxOrderServiceImpl.deductStock` 只传 `drugId` + `qty`，而 `InventoryFacadeAdapter.validateDeduct` 强制要求每行 `bizNo`（全行一致）与 `bizLineId`（唯一且为正整数）作为操作级幂等键，缺失会直接抛「销售扣库缺少来源单据、来源行或药品数量」。因此线上订单支付一旦接到真实库存实现就会失败。

**根因 B：回补请求缺少原出库引用与批次货位。** 原 `returnBackStock` 用订单明细的 `batchId` / `locationId`，而 C 的 `validateReturn` 还强制要求 `originalBizNo`、`originalBizLineId`、`batchId`、`locationId` 全部非空；且订单明细只存单个批次，FEFO 拆分成多批次时必然漏补。

**根因 C：状态与库存不在同一事务。** `payWxOrder` / `cancelWxOrder` 原先没有事务，且支付路径是「先扣库再翻转状态」，并发或失败时可能出现「库存已扣、订单未支付」。

实现要点：

- 支付改为**先条件更新抢占状态，再扣库**，并加 `@Transactional`：并发重复回调 `rows=0` 直接返回，只有抢到状态翻转的请求才扣库；扣库失败整体回滚。
- 扣库请求携带 `bizNo = 订单号`、`bizLineId = 订单明细 ID`，满足 C 的幂等键。
- 新增 `ph_wx_order_line_alloc` 记录 C 返回的 FEFO 实际分配（批次 + 货位 + 数量），并把首个分配回填订单明细便于拣货。
- 取消 / 退款回补按分配逐条提交：`bizNo`（`WXC-` / `WXR-` + 订单号）、`bizLineId`（分配记录 ID）、`originalBizNo`（订单号）、`originalBizLineId`（订单明细 ID）、原批次、原货位。
- 幂等三重保护：订单状态条件更新、分配表 `returned_qty` / `status`、C 的流水幂等键与累计回补上限。
- 新增退款入口 `PUT /pharmacy/member/order/refund`：已支付未完成 → 已退款 + 取消 + 回补库存；重复退款直接返回；已完成订单要求走 D 的销售退货，接口显式拒绝。
- 库存异常翻译：C 的「可用库存不足」转为 `PHARMACY_WX_ORDER_STOCK_NOT_ENOUGH`，`AccessDeniedException` 转为「无库存作业权限」，其余保留原因为 `PHARMACY_WX_ORDER_STOCK_OP_FAILED`。

### 2.3 前端残留类型错误

全量 `vue-tsc` 发现 `MemberForm.vue` 的 `el-avatar size="80"`（字符串赋给 `number | 'default' | 'small' | 'large'`）报 TS2322；上次为增量检查未覆盖，本次修正为 `:size="80"`。

## 3. 自动化测试结果

```text
mvn -pl yudao-module-pharmacy test
Tests run: 全部通过（含 MemberPointRecordServiceImplTest 14 项、WxOrderStockLifecycleTest 10 项、MemberUserServiceImplTest 4 项）
BUILD SUCCESS
```

覆盖的验收点：

| 验收项 | 用例 |
| --- | --- |
| 积分扣减成功 | `testDeductPoints_success` |
| 积分不足被拒绝 | `testDeductPoints_notEnough` |
| 同一订单重复扣积分不会重复扣减 | `testDeductPoints_idempotent` |
| 取消 / 退货后积分只返还一次 | `testReturnPoints_onlyOnce`、`testReturnPoints_success` |
| 返还不超过已抵扣 | `testReturnPoints_clampToDeducted` |
| 支付成功后扣库并记录批次货位 | `testPayWxOrder_deductsStockAndRecordsAllocation` |
| 重复支付回调不重复扣库 | `testPayWxOrder_duplicateCallbackIsIdempotent` |
| 库存不足给出明确业务错误 | `testPayWxOrder_stockNotEnough` |
| 取消订单按原批次原货位回补 | `testCancelWxOrder_returnsStockToOriginalBatch` |
| 重复取消 / 重复退款不重复回补 | `testCancelWxOrder_repeatIsIdempotent`、`testRefundWxOrder_returnsStockOnlyOnce`、`testCancelWxOrder_returnedAllocationIsSkipped` |
| 未出库订单取消无库存动作 | `testCancelWxOrder_unpaidOrderHasNoStockAction` |
| 已完成订单不允许直接退款 | `testRefundWxOrder_completedOrderRejected` |

前端：`vue-tsc` 中 `src/views/pharmacy/member/**` 报错为 0（其余报错属 B 采购、E 处方等，不在 F 范围）。

## 4. Docker 实际验证结果

环境：`firstsun-pharmacy-mysql/redis/backend` 运行中，后端为本次构建产物（`yudao-server` 全量 `mvn package` 成功，220 MB）。

| 项目 | 结果 |
| --- | --- |
| 构建 | `mvn -pl yudao-server -am package -DskipTests` 成功（与镜像 Dockerfile 的构建命令一致） |
| healthcheck | `GET /actuator/health` → HTTP 200 `{"status":"UP"}`，A 的修复未回归 |
| 共享账号登录 | 租户 FirstSun（`tenant-id: 163`）+ `0407/123456` 登录成功，`userId=407` |
| 新增迁移 | `sql/migrations/20260916_f_wx_order_line_alloc.sql` 应用成功，`ph_wx_order_line_alloc` 存在，可重复执行 |

真实链路（订单夹具：门店 407、药品 163104、数量 2、批次 163504、货位 163034、初始 30/30/0/0）：

| 步骤 | 调用 | 结果 |
| --- | --- | --- |
| 1 | `PUT /order/pay` | `code=0`；订单 → 待拣货 / 已支付；批次 `qty_avail 30→28`、`qty_sold 0→2`；货位 30→28；生成 1 条出库分配（批次 163504 / 货位 163034 / 数量 2）；库存流水 `flow_type=20`、`biz_no=订单号`、`biz_line_id=明细ID`、`out_qty=2` |
| 2 | 重复 `PUT /order/pay` | `code=0`；库存与流水**无新增**（仅 1 条扣库流水） |
| 3 | `PUT /order/refund` | `code=0`；订单 → 取消 / 已退款；批次回到 `30/30/0/0`；货位回到 30；分配 `returned_qty=2`、`status=1`；新增流水 `flow_type=21`（`biz_no=WXR-订单号`、`original_flow_id` 精确指向原出库流水、`in_qty=2`） |
| 4 | 重复 `PUT /order/refund` | `code=0`；库存与流水**无新增**（共 2 条流水） |
| 5 | 夹具清理 | 订单 / 明细 / 分配 / 本次流水全部删除，批次恢复 30/30/0/0，`remaining=0` |

补充证据（事务回滚）：首次支付时因运行库未应用 C 的 `20260915_c_inventory_facade_flow_ref.sql`（缺 `original_flow_id` 列）导致 C 的流水写入失败，接口返回 500，此时订单仍为待支付、库存与分配表**均未变化**，验证了「扣库失败整体回滚」。

## 5. 尚未解决的问题

1. **待支付订单不冻结库存**：`InventoryFacade` 无 `reserve` / `release` / `consumeReservation`，无法实现「下单冻结 → 支付转出库 → 取消 / 超时释放」。数据模型（`qty_frozen`、`ph_inv_lock`、`flow_type 80/81/82`）已具备，但写入侧完全缺失，`frozen_delta` 恒写 0。已提交接口需求。
2. **支付超时自动释放未实现**：依赖第 1 项的 `release`，且当前未支付订单本无占用。
3. **下单前库存校验未实现**：`InventoryFacade` 无只读可售量接口，`InventoryReadAccess` 为 C 内部组件；因此「库存不足时不得创建有效订单」目前只能等到支付时才发现。
4. **库存作业门禁**：`InventoryReadAccess.requireScope` 要求登录用户为 ADMIN 且是目标门店在职员工，小程序端（会员）与后台非该门店员工无法触发库存作业，需 C 与组长确认方案（系统级入口 / 改为门店员工操作节点扣库）。
5. **渠道退款降级**：`refundWxOrder` 会调用 E 的 `PaymentFacade.refund`，E 未就绪时降级不阻塞（与下单创建支付单的降级策略一致），此期间订单侧已标记退款但渠道未真正退款。
6. **积分门面的线上订单接入点**：`ph_wx_order` 无积分抵扣字段，因此积分抵扣 / 返还由 D 的 POS 链路调用门面；F 侧只提供能力与测试，未在线上订单流程中调用。
7. **`docker compose build backend` 未在本轮重跑**：本轮以本地全量 `mvn package`（与镜像构建同命令）+ 容器内替换产物重启的方式验证运行态；镜像层重建建议由 CI / 主验收执行。

## 6. 是否建议创建 Pull Request

建议创建。理由：

- 改动集中在 F 的会员 / 积分 / 会员订单范围，未修改 A/B/C/D/E 的业务实现。
- 后端全量测试通过，真实 Docker 环境端到端验证了「支付出库 → 退款原批次回补 → 重复回调幂等」，并验证了失败回滚。
- 前端 F 相关类型错误归零。
- 冻结 / 释放相关能力以接口需求文档形式提交，未擅自扩展 C 的库存实现。

PR 前需组长确认第 5 节的第 4 项（库存作业门禁方案），因为它决定线上订单在「支付扣库」与「核销扣库」之间的最终语义。
