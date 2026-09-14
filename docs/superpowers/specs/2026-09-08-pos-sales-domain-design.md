# D 成员 POS 销售域设计文档(FirstSun 药店管理系统)

> 日期: 2026-09-08 · 分支: feat/d-pos · 作者: 成员 D(POS 销售域)
> 依据:《开发规范与 AI 协作规则》《六人全栈开发方案》,数据库基线 `sql/firstsun_pharmacy_init.sql`(46 表已定稿)

## 1. 背景与范围

本工程为基于 ruoyi-vue-pro / yudao-vue-pro 的医药门店管理系统。D 负责**销售收银与退货交班**(POS 销售域),数据库使用 `ph_pos_shift`、`ph_sale_order`、`ph_sale_order_line`、`ph_sale_payment`、`ph_sale_return`、`ph_sale_return_line` 六张已定稿表。

**已确认决策**(decision_id: dec-78eb6f0dbc981118, dec-3a02a47b8b14ebaa):
1. 由 D 创建 `yudao-module-pharmacy` 模块骨架并接入 `yudao-server`,在其中实现 POS 域;
2. 库存扣减/回补、渠道支付、积分变动**完整等待 C/E/F 对应服务**,D 只建立 Facade 契约并调用,不实现数据写入;
3. 本期交付全量 6 表后端 + 7 个管理端页面。

## 2. 架构

```
yudao-module-pharmacy(新)				cn.iocoder.yudao.module.pharmacy
├── api/inventory  InventoryFacade        ← 契约(实现待 C)
├── api/payment    PaymentFacade          ← 契约(实现待 E)
├── api/member     MemberPointFacade      ← 契约(实现待 F)
├── controller/admin/pos  五个 Controller(REST /admin-api/pharmacy/pos/**)
├── service/sales           六个 Service + Impl
├── convert/sales           MapStruct Convert
├── dal/dataobject/sales    六个 DO
└── dal/mysql/sales        六个 Mapper(XML 内置)
```

- 接入:`backend/pom.xml` 增加 `<module>yudao-module-pharmacy</module>`;`yudao-server/pom.xml` 增加依赖 `yudao-module-pharmacy`。yudao-server 启动扫描自动生效。
- 复用:`yudao-module-system` 的权限/用户/门店数据、`yudao-framework` 的通用 CRUD/分页/操作日志/异常框架;不详述重构 system/infra/member/pay 框架代码。

## 3. 数据模型(直接映射已定稿 SQL, 不做表变更)

| 表 | DO | 关键约束/字段 |
| --- | --- | --- |
| ph_pos_shift | PhPosShiftDO | shift_no 唯一;open/close;cash_expected/cash_actual/diff_amount;diff_reason(POS-001 必填);status 0营业中/1已交班;sale_count/sale_amount 汇总 |
| ph_sale_order | PhSaleOrderDO | order_no `SO-门店-yyyyMMddHHmmss-流水` 唯一;source 0柜台/1小程序;status -1取消/0待支付/1完成/2全退/3部分退;金额字段全部非负 CHECK;cashier/store/shift/member/wx_order 关联;offline 离线支持 |
| ph_sale_order_line | PhSaleOrderLineDO | 必绑 batch_id(先进先出 POS-002);returned_qty ≤ qty;is_rx + presc_id;名称/规格/单位/成本快照;location_id 出库货位 |
| ph_sale_payment | PhSalePaymentDO | payment_no 本系统幂等号唯一;channel+pay_no 唯一;pay_order_id 关联(现金为空);状态/已退标志 |
| ph_sale_return | PhSaleReturnDO | return_no 唯一;原销售单;全退/部分退;reason;退款方式;药师复核(处方药);refund 状态机 |
| ph_sale_return_line | PhSaleReturnLineDO | 原销售行;回原批次(POS-009);qty ≤ 可退数量;points_deduct 扣回 |

## 4. 后端服务设计

### 4.1 销售收银(SaleOrderService)
- 创建流程:校验药品可售状态(经 A 药品服务)→ 计算金额(服务端;折扣/券/积分扣减非负) → 处方药行走审方校验 → 生成 order_no → 提交 `InventoryFacade.deduct(...)`(同事务) → 写支付明细(现金等,幂等号) → 完成。
- 幂等:以 `payment_no`/`order_no` 已存在判定,杜绝同一请求二次扣库。
- 库存依赖:Facade 未就绪→抛 `INV_SERVICE_UNAVAILABLE`,事务整体回滚(数据不落库),前端提示"库存服务未就绪"。

### 4.2 退货(SaleReturnService)
- 条件:原单存在、可退数量 ≤ qty - returned、现金原路/余额切换、处方药行需 pharmacist_confirm;
- 退款:电子渠道调 PaymentFacade.refund(未就绪则业务报错回滚,现金不依赖);
- 回补:调 InventoryFacade.returnBack(原批次),同事务。

### 4.3 交班(PosShiftService)
- 开台→收银会话内销售累计→交班时核对现金应收 vs 实盘;occur 差异必填原因并固化进 ph_pos_shift.diff_reason;销售笔数/金额汇总;班次内订单不可再改。

### 4.4 统计(SalesStatisticsService)
- 按日/门店/商品维度汇总 sale_order(+line),支持时间区间;查已有完成储蓄。

### 4.5 错误码
`ErrorCodeConstants`: 如 POS_SALE_ORDER_STATUS_ERROR、POS_RETURN_QTY_EXCEED、POS_PAYMENT_NO_EXISTS、POS_SHIFT_CLOSED、FA_SET_INV_UNAVAILABLE 等,按 yudao `ErrorCode` 工厂注册。

## 6. 管理端页面(admin-ui)

路径 `src/views/pharmacy/pos`,7 页面 + `src/api/pharmacy/pos` 5 个 API 模块:

| 页面 | 说明 |
| --- | --- |
| index(收银台) | 商品搜索 / 条码扫码、购物车、折扣、现金找零、小票预览(打印) |
| orderList / orderDetail | 销售查询、详情、重打小票、退货入口 |
| returnList | 退货单、部分退流程、药师复核 |
| shiftList | 班次交班、差异校验 |
| paymentList | 支付明细对账 |
| statistics | 销售统计图表(echarts 已有依赖) |

- 菜单与按钮权限:提交增量 `sql/migrations/20260908_d_pos_menu.sql`(yudao system 菜单体系),由 A 集成共享库;admin-ui 动态路由自动加载。
- 组件风格沿用 element-plus / yudao 惯例;构建使用现有依赖(`vue3-print-nb` 打印小票)。

## 7. 跨域接口契约(本轮只定义,不实现)

| Facade | 方法(关键签名) | 归属 |
| --- | --- | --- |
| InventoryFacade | queryStock/selectBatch/deduct/returnBack | C |
| PaymentFacade | pay/createRefund/queryStatus(幂等键) | E |
| PointFacade | add/back | F |

规范"销售扣库与流水同一事务、幂等、失败整体回滚"由各 Facade 的调用方(D)以同一业务事务承载;Implementation missing 阶段返回业务错误并回滚, 不静默放行。

## 8. 错误处理与测试

- 异常:全部业务异常走 `BizException` + 错误码;事务边界 `@Transactional`;Facade 缺失 → 业务错误并整体回滚(不隐藏、不硬编码成功)。
- 单元测试(不依赖外部库):金额计算(折扣/积分/找零)、状态机(取消/全退/部分退)、退回时限(部分退不超)、幂等(重复单、重复支付号)、交接差异必须原因。
- 联调(受限):Docker 未运行不可起动 MySQL/Redis/后端 → 真实联调/`@SpringBootTest` 按"未运行检查"如实声明;本地用 `mvn -pl yudao-module-pharmacy -am compile` `test` 验证编译与单测;前端 `pnpm install`, `vue-tsc`, `build:dev` 验证类型与构建。

## 9. 交付清单与边界

**交付**: 后端 DO×6、Mapper×6、Service×6、Controller×5、Facade×3、ErrorCode、单测;前端 7 页面 + 5 API;菜单 SQL 增量;运行文档(README 章节)。

**不做**:库存/支付/积分数据写入(交给 C/E/F);不动任何 maven 下已启用 module、框架表结构、他人代码;不直接推共享库;Docker/共享环境按规范不在未授权状态下操作。