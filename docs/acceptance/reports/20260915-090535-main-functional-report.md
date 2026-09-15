# 20260915-090535 main 全项目功能验收报告

## 执行摘要

- 结论：**不通过，不建议继续合并**。
- 确认缺陷：P1 2 项、P2 4 项；疑似缺陷 1 项；依赖阻塞 4 类；未测试 2 类。
- ACC-002（支付 Facade 占位）撤销：`PaymentFacadeAdapter` 是实际 Spring `@Service`，动态创建线上订单成功生成 `pay_order`（1000 分、待支付），不是 fallback Bean。
- ACC-003（积分 Facade 占位）撤销：`MemberPointFacadeAdapter` 是实际 Spring `@Service`；带积分抵扣的退货调用越过积分步骤后准确失败于库存回补，积分流水随事务回滚，不是 fallback Bean。
- ACC-001 保留 P1：没有 `InventoryFacade` 的真实 Adapter，动态退货返回 `code=1029002001`“库存服务未就绪，本次操作无法扣减/回补库存”。
- A 基础资料、B 采购提交、C 库存资料/盘点/报损、D 班次、E 处方写操作均已动态验证。F 等级写入通过，但会员新增返回 500，阻断后续新会员组合写。
- 未修改业务代码。所有本轮 AUTO 数据、临时低权限账号、支付单和固定 ID 夹具均已清理，复核残留为 0。

## 证据分类

### 确认缺陷

| ID | 严重度 | 主责 | 摘要 | 动态/编译证据 |
| --- | --- | --- | --- | --- |
| ACC-20260915-001 | P1 | C | 库存统一门面无真实 Adapter，采购入库、销售扣库、退货回补无法闭环 | 退货动态返回 `1029002001`；`InventoryFacadeImpl` 三个方法抛异常，未找到真实 Adapter |
| ACC-20260915-008 | P1 | F | 管理端新增会员返回系统异常，阻断会员基础写链 | `POST /pharmacy/member/user/create` 返回 `code=500`；日志为 `Field 'register_ip' doesn't have a default value`，定位 `MemberUserServiceImpl.java:46` |
| ACC-20260915-004 | P2 | A | 后端健康接口不可用 | `/actuator/health` 返回业务 `code=404`，compose 的 backend 无 healthcheck |
| ACC-20260915-005 | P2 | B | 采购页面 TypeScript 检查失败 | vue-tsc 指向采购订单/收货页面的类型错误 |
| ACC-20260915-006 | P2 | E | 处方页面 TypeScript 错误及运行时 required prop 警告 | vue-tsc 指向处方页面；控制台报告 `PharmacyPageHeader` 缺 3 个必填属性 |
| ACC-20260915-007 | P2 | F | 会员页面 TypeScript 检查失败 | vue-tsc 指向 `MemberForm.vue`、`order.vue`、`point-record.vue` |

### 疑似缺陷

| 项目 | 证据 | 判定 |
| --- | --- | --- |
| Maven 上游 infra 单测回归 | 聚合测试在 `CodegenEngineUniappTest.testExecute_treeSearch:153` 失败；infra 195 项中 1 失败、10 跳过 | 疑似与 pharmacy 无关的模板断言回归，不直接分派给 A～F |

### 依赖阻塞

| 项目 | 阻塞原因 |
| --- | --- |
| B 收货入库/入账 | 被 C 的 `InventoryFacade.receive` 阻塞 |
| D 销售成功闭环 | 被 C 的 `InventoryFacade.deduct` 阻塞 |
| D 退货成功闭环 | 被 C 的 `InventoryFacade.returnBack` 阻塞；支付与积分步骤已证明不是 fallback |
| F 新会员地址、购物车、订单组合写 | 被 F 的会员新增 500 阻塞；使用现有会员的订单创建及支付 Adapter 已通过 |

### 未测试

- 1024px 响应式：当前浏览器自动化连接只暴露页面资产能力，无法可靠设置或读取 1024px viewport；未将当前宽度冒充通过。
- FLOW-01～05 的库存成功闭环：依赖 ACC-001 修复后复验。

## 基线、构建与服务

- 分支 `main`；HEAD 与 `origin/main` 均为 `4fa5696c890f168bce3b51222f64ef65fe27b438`。
- 工作区验收前已有改动；本轮未处理或覆盖。
- 21 个迁移 SQL 均已挂载且含 UTF-8 初始化；compose 配置通过；mysql、redis、backend、admin-ui 运行，mysql/redis healthy。
- 管理端 Vite 生产构建通过；vue-tsc 失败，药店范围见 ACC-005/006/007。
- Maven 聚合测试未完成：上游 infra 单测 195 项中 1 项失败，导致 system/pay/pharmacy 被 reactor 跳过。直接运行 pharmacy 又因本地缺少同版本 system/pay/framework SNAPSHOT 构件而依赖解析失败。因此 pharmacy 自身结果为**未测试**，不是通过。

## A～F 动态验收

| 模块 | 已验证通过 | 确认缺陷/阻塞/未测 |
| --- | --- | --- |
| A | 门店新增/编辑；员工新增/编辑；分类新增、重复编码、错误父级；药品新增/编辑；条码新增/编辑及重复条码 | ACC-004 |
| B | 供应商新增/编辑/审批；证照新增；采购单创建/提交；重复提交被拒绝 | 入库与入账被 C 阻塞；ACC-005 |
| C | 仓库新增/编辑；货位新增；盘点新增/取消及错误参数；报损新增/取消；效期刷新 | ACC-001；真实收货、扣减、回补未通过 |
| D | 开班、重复开班拒绝、错误交班 ID 校验、正常交班、班次查询 | 销售和退货成功闭环被 C 阻塞 |
| E | 两张处方登记；分别审核通过/驳回；详情及台账查询；支付 Adapter 动态落库 | ACC-006；ACC-002 撤销 |
| F | 等级新增；现有会员线上订单创建；支付单关联；积分 Adapter 动态调用并事务回滚 | ACC-008、ACC-007；新会员后续写被阻塞；ACC-003 撤销 |

## 权限、隔离、幂等与控制台

- 未登录访问药品分页返回 `code=401`，通过。
- 临时无菜单低权限账号登录成功，新增分类返回 `code=403`“没有该操作权限”，通过；账号、角色、绑定及令牌已清理。
- 租户 163 的有效 token 携带 `tenant-id: 1` 访问药品数据，返回 `code=403`“您无权访问该租户的数据”，通过。
- 重复提交：采购单第二次提交被状态机拒绝；班次第二次开班返回“已有进行中班次”，通过。
- 控制台确认处方登记页缺 `PharmacyPageHeader` 三个 required prop，另有全局 Vue Router `next()` 弃用警告；无页面崩溃。
- 28 个药店路由均完成登录态加载烟测，无 404、空白页或动态组件加载失败。

## Facade 运行时判定修正

1. `PaymentFacadeAdapter`、`MemberPointFacadeAdapter` 均带 `@Service`；fallback 配置使用 `@ConditionalOnMissingBean`，不能因占位类存在而判定运行时使用。
2. 支付动态证据：创建 `AUTO-WX-PAY-090535` 返回订单 ID `163655`、`payNo=1`；`pay_order.id=1`、`merchant_order_id=AUTO-WX-PAY-090535`、`price=1000`、`status=0`。
3. 积分动态证据：夹具 `AUTO-POINT-090535` 的 `points_deduct=10`，现金退货跳过渠道退款后调用积分回退，再返回库存未就绪；未返回“会员服务未实现”，且积分流水因事务回滚无残留。
4. 因此 ACC-002、ACC-003 判为通过并撤销；ACC-001 仍为确认 P1。

## A～F 修复任务（仅动态或编译证据）

### A（1）

- **ACC-20260915-004 / P2**：补齐可用的后端健康检查及 compose backend healthcheck。复验：健康接口返回真实 UP，compose 显示 backend healthy。

### B（1）

- **ACC-20260915-005 / P2**：修正 `admin-ui/src/views/pharmacy/purchase/**` 已报告的 TypeScript 类型错误。复验：采购相关 vue-tsc 错误归零，创建/提交无回归。

### C（1）

- **ACC-20260915-001 / P1**：实现真实 `InventoryFacade` Adapter，覆盖 receive/deduct/returnBack、流水、事务与幂等。复验：采购入库、销售扣库、退货回补动态成功，异常整单回滚。

### D（0）

班次测试通过；销售/退货仅有 C 依赖阻塞，没有 D 自身动态或编译缺陷证据，不分派任务。

### E（1）

- **ACC-20260915-006 / P2**：修正处方页面 TypeScript 错误和 `PharmacyPageHeader` 缺失属性。复验：E 相关 vue-tsc 错误归零，控制台无 required prop 警告。

### F（2）

- **ACC-20260915-008 / P1**：会员新增时写入 `member_user.register_ip` 并核对其他必填列。复验：会员新增/编辑成功，地址、购物车、订单组合写完成，重复手机号返回业务错误而非 500。
- **ACC-20260915-007 / P2**：修正会员页面已报告的 TypeScript 错误。复验：F 相关 vue-tsc 错误归零，会员/订单/积分页无回归。

## 清理确认

- 已清理本轮采购、供应商、仓库、货位、盘点、报损、班次、处方、等级、线上订单、支付单、积分退货夹具，以及临时低权限账号/角色/令牌。
- 固定 ID 与 AUTO 标识复核：`remaining=0`。
- 未修改业务代码、SQL、Docker 配置，未提交或推送。
