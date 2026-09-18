# B 模块验收缺陷修复记录

对应验收报告：`docs/acceptance/reports/20260915-090535-main-functional-report.md`

| 轮次 | 分支 | 范围 | 结果 |
| --- | --- | --- | --- |
| 第 1 轮 | `fix/b-purchase-ts`（PR #35，已合并） | 6 条：`dict-tag` 值与表单态类型 | 采购错误 6 → 0 |
| 第 2 轮 | `fix/b-purchase-listid` | 4 条：下拉列表项主键 `id` 被声明为可选 | 采购错误 4 → 0 |

修复范围：仅 `admin-ui` 采购域（B 主责目录），未改动后端、SQL、Docker 配置与共享组件。

## 一、分派给 B 的缺陷

| ID | 严重度 | 摘要 | 复验要求 |
| --- | --- | --- | --- |
| ACC-20260915-005 | P2 | 采购页面 TypeScript 检查失败（`admin-ui/src/views/pharmacy/purchase/**`） | 采购相关 vue-tsc 错误归零，创建/提交无回归 |

报告中「B 收货入库/入账被 C 的 `InventoryFacade.receive` 阻塞」属**依赖阻塞**，主责为 C（ACC-20260915-001），不在本次 B 的修复范围。

---

# 第 2 轮修复（4 条列表项主键类型）

## 一、现象

在最新 `origin/main`（`ac111d62`）上执行 `node ./node_modules/vue-tsc/bin/vue-tsc.js --noEmit`，`pharmacy/purchase` 目录残留 4 条 TS2322，全部落在 `<el-option :value="...id">` 绑定上：

```
order/OrderForm.vue(107,18)       TS2322  :value="drug.id"
receipt/index.vue(73,16)          TS2322  :value="item.id"
receipt/ReceiptForm.vue(31,18)    TS2322  :value="item.id"
receipt/ReceiptForm.vue(115,18)   TS2322  :value="drug.id"

Type 'number | undefined' is not assignable to type
'EpPropMergeType<(NumberConstructor | ObjectConstructor | BooleanConstructor | StringConstructor)[], unknown, unknown>'
```

`el-option` 的 `value` prop 不接受 `undefined`，而三个列表项的 `id` 都被声明成了可选。

## 二、根因

| 列表 | 使用的类型 | 原声明 | 后端真实契约 |
| --- | --- | --- | --- |
| 药品下拉 ×2 | `DrugVO`（A 维护） | `id?: number` | `DrugSimpleRespVO.id` = `private Long id`（REQUIRED） |
| 订单下拉 | `PurchaseOrderVO`（B 维护） | `id?: number` | `PurchaseOrderRespVO.id` = `private Long id` |
| 门店下拉 | `StoreSimpleVO`（A 维护） | `id: number` ✅ | `StoreSimpleRespVO.id` = `private Long id` |

对照可见：**同类"精简列表"VO 在门店侧已声明为必填，药品/订单侧却写成了可选**——这是类型声明偏差，不是真实可空。

另外 `GET /pharmacy/base/drug/simple-list` 返回的是 `DrugSimpleRespVO`（仅 6 个字段），而前端 `getSimpleDrugList()` **没有声明返回类型**（推断为 `any`），并被赋给 `DrugVO[]`，属契约不符。

## 三、修复（不使用非空断言绕过）

### 3.1 B 自己的订单/收货单 VO：按后端契约改正

`PurchaseOrderVO.id`、`PurchaseReceiptVO.id` 改为必填，并在注释中写明依据的后端 VO 与字段。

### 3.2 药品下拉：在采购域内按精简列表契约收窄

新增 `admin-ui/src/api/pharmacy/purchase/module-types.ts`，定义 `DrugSimpleVO`，字段与后端 `DrugSimpleRespVO` 一一对应且 `id` 必填；`OrderForm.vue` / `ReceiptForm.vue` 的 `drugList` 改用该类型。

**为什么不直接改 `@/api/pharmacy/base/drug`**：该文件属 A 负责，本次修复限定在 B 的采购域内。`module-types.ts` 的文件注释里写明了：若 A 后续给 `getSimpleDrugList` 补上正确返回类型，本文件可移除并改用它。

**为什么不用非空断言**：`drug.id!` 会掩盖真实可空性，一旦后端某条记录缺 id（数据异常）就变成运行时静默错误。改为在类型层面如实声明 `id` 必填，让契约不符在编译期暴露。

### 3.3 `if (!row.id) return` 守卫为何保留

`order/index.vue`、`receipt/index.vue`、`supplier/index.vue` 的命令分发里有 `if (!row.id) return`。这是**运行时防御**（脏数据兜底），不是类型绕过，且 `strict` 模式下 TypeScript 不会因"条件恒真"报错，故保留。

## 四、复验证据

### 4.1 类型检查

> ⚠️ **前置条件**：`admin-ui` 的 `auto-imports.d.ts` / `auto-components.d.ts` 被 `.gitignore` 的 `auto-*.d.ts` 忽略，且 `build/vite/index.ts` 里 `dts: !isBuild && ...` 表示**只在 dev 模式生成**。
> 缺少这两份声明时，`<el-option>` 会被当成宽松的 HTML 元素，**反而把本缺陷掩盖掉**（实测缺 `auto-components.d.ts` 时采购错误显示为 0 条，是假阴性）。
> 因此复验前需先生成声明文件，否则结果不可信。

```powershell
cd admin-ui
# 生成声明（等价 vite dev 启动一次），随后执行类型检查
node --max_old_space_size=8192 ./node_modules/vue-tsc/bin/vue-tsc.js --noEmit --pretty false
```

| 指标 | 修复前 | 修复后 |
| --- | --- | --- |
| **采购相关错误** | **4 条** | **0 条** ✅ |
| 全项目错误 | 20 条 | 16 条 |

修复后全项目剩余 16 条，**全部不在 B 范围**：

| 归属 | 条数 | 明细 |
| --- | --- | --- |
| E 处方 | 5 | `prescription/create.vue(261)`、`index.vue(134,144)`、`review.vue(106,191)` |
| 框架其他模块 | 11 | `mes/wm/*`、`ai/chat`、`fms/report`、`pms/kb`、`iot/alert`、`bpm/oa` 的 `ElMessage`/`ElMessageBox`/`ElTree` 全局未声明 |

### 4.2 前端生产构建

```powershell
cd admin-ui
node --max_old_space_size=8192 ./node_modules/vite/bin/vite.js build --mode env.local
# → ✓ built in 26.36s   BUILD_EXIT=0
```

### 4.3 四个页面加载

新产物已部署到 admin-ui 容器，四个列表页与四个表单 chunk 全部 HTTP 200：

```
receipt-BejHY4Og.js      HTTP 200  13.5 KB     ReceiptForm-D5R14a1N.js   HTTP 200  12.7 KB
order-CX5mXxQp.js        HTTP 200  12.4 KB     OrderForm-BYvRrA86.js     HTTP 200   9.8 KB
supplier-BW7C88pR.js     HTTP 200  11.0 KB     SupplierForm-BbuBHbmA.js  HTTP 200   7.0 KB
license-DmPNzASb.js      HTTP 200   9.6 KB     LicenseForm-8R6idktZ.js   HTTP 200   4.9 KB
```

### 4.4 功能回归（含采购收货入账流程）

`tools/run-all-checks.ps1`：**共 14 项检查，失败 0 项**。

入账流程关键断言（主自测 30 项内）：

```
[通过] 创建供应商成功 / 登记供应商证照成功 / 证照列表回填状态=有效(1)
[通过] 创建采购订单成功 → 金额服务端重算（50.00/5.00/45.00）→ 提交 → 审批 → 标记已发出
[通过] 创建收货单成功 → 部分收货被标记为数量差异(1)
[通过] 收货时间按毫秒时间戳正确落库（非 1970）
[通过] 收货单号日期段为当天（回归校验）
[通过] 超收拦截生效（收货数量超过订购数量被拒绝）
[通过] 提交收货单（待提交→已提交）
[通过] 收货入账在库存服务未就绪时返回明确错误（code=1029002001）
[通过] 入账失败后状态回滚为已提交(1)，未写入入账时间（事务一致）
[通过] 入账失败后订单已收数量保持 0（库存与单据一致，不虚增）
```

> 入账的**成功**路径仍被 C 的 `InventoryFacade.receive` 阻塞（ACC-20260915-001）；B 侧已验证「未就绪时明确报错 + 整单回滚」。

### 4.5 改动范围与合规

```
 M admin-ui/src/api/pharmacy/purchase/order/index.ts           (+8 -1)
 M admin-ui/src/api/pharmacy/purchase/receipt/index.ts         (+6 -1)
 M admin-ui/src/views/pharmacy/purchase/order/OrderForm.vue    (+4 -1)
 M admin-ui/src/views/pharmacy/purchase/receipt/ReceiptForm.vue (+4 -1)
?? admin-ui/src/api/pharmacy/purchase/module-types.ts           (新增)
```

- 全部改动位于 `pharmacy/purchase` 采购域，**未触碰 B 以外代码**；
- 全文检索确认**无 `as any` / `@ts-ignore` / `@ts-expect-error` / 非空断言**。

---

# 第 1 轮修复（6 条类型错误，已合并）

## 一、分派给 B 的缺陷

| ID | 严重度 | 摘要 | 复验要求 |
| --- | --- | --- | --- |
| ACC-20260915-005 | P2 | 采购页面 TypeScript 检查失败（`admin-ui/src/views/pharmacy/purchase/**`） | 采购相关 vue-tsc 错误归零，创建/提交无回归 |

报告中「B 收货入库/入账被 C 的 `InventoryFacade.receive` 阻塞」属**依赖阻塞**，主责为 C（ACC-20260915-001），不在本次 B 的修复范围。

## 二、缺陷定位过程

### 2.1 先排除环境噪声

直接跑 `vue-tsc` 会刷出数百条 `Cannot find name 'ref' / 'computed' / 'ElMessage'`，**不能据此判定缺陷**。原因是两份声明文件不入库：

```
admin-ui/.gitignore:7:  auto-*.d.ts
```

而 `admin-ui/build/vite/index.ts` 里 `dts: !isBuild && 'src/types/auto-imports.d.ts'` —— 这两个文件**只在 vite dev 模式生成，build 不生成**，因此干净检出下必然缺失。

处理：临时调用 `unplugin-auto-import` / `unplugin-vue-components` 的 `buildStart` 钩子生成 `src/types/auto-imports.d.ts`（该文件被 gitignore，不提交），再重新执行类型检查，才得到真实错误集。

### 2.2 真实错误集（B 主责 6 条）

```
src/views/pharmacy/purchase/order/index.vue(239,59)      TS2322
src/views/pharmacy/purchase/order/OrderForm.vue(274,67)  TS2345
src/views/pharmacy/purchase/receipt/index.vue(256,64)    TS2322
src/views/pharmacy/purchase/receipt/index.vue(266,67)    TS2322
src/views/pharmacy/purchase/receipt/index.vue(269,64)    TS2322
src/views/pharmacy/purchase/receipt/ReceiptForm.vue(412,9) TS2322
```

全项目同期 40 条，其余为 E 的处方页（5 条）与框架自带其他模块（`mes`/`ai`/`fms`/`pms`/`iot`/`bpm` 的 `ElMessage`/`ElMessageBox`/`ElTree` 全局未声明），均不在 B 范围。

## 三、根因与修复

### 3.1 响应 VO 把必填字段声明成了可选（4 条 `dict-tag` 错误）

`dict-tag` 组件的 `value` prop 声明为必填（`type: [String, Number, Boolean, Array], required: true`），而我此前把响应 VO 的字段几乎全写成了可选：

```ts
// 修复前 —— 数据库里这些列都是 NOT NULL，后端必定返回
status?: number          // ph_po_order.status    NOT NULL DEFAULT 0
diffType?: number        // ph_po_receipt.diff_type     NOT NULL DEFAULT 0
status?: number          // ph_po_receipt.status        NOT NULL DEFAULT 0
qualityStatus?: number   // ph_po_receipt.quality_status NOT NULL DEFAULT 0
```

模板里 `:value="detail.status"` 因此被推断为 `number | undefined`，触发 TS2322。

**修复：让类型如实反映后端契约**，把这四个字段改为必填（已核对 `information_schema` 确认列定义均为 `NOT NULL`）。这比在页面上加 `?? 0` 兜底更正确——兜底会掩盖「后端漏返回字段」这类真实问题。

### 3.2 表单态类型与提交 DTO 类型混用（2 条错误）

| 位置 | 问题 |
| --- | --- |
| `OrderForm.vue` | `calcLineAmount(line: PurchaseOrderLineCreateVO)` 要求 `drugId` 必填，但两个调用点传的都是表单行 `OrderLineFormItem`（`drugId` 允许为空）→ TS2345 |
| `ReceiptForm.vue` | `ReceiptLineFormItem` 由提交 DTO 经 `Omit/&` 派生，可空性与基础接口冲突 → TS2322 |

**修复：**

1. `calcLineAmount` 形参改为表单态类型 `OrderLineFormItem`（纯类型修正，运行时行为不变）；
2. `ReceiptForm.vue` 的 `ReceiptLineFormItem` 改为**按表单态独立定义**，不再从提交 DTO 拼接。与提交态的差异收敛为**仅 `drugId` 一处**（允许为空，提交前由 `buildSaveData` 收窄），其余字段保持必填——这样 `buildSaveData` 的返回值无需强制类型转换即可满足 `PurchaseReceiptLineCreateVO` 契约；
3. 详情回填的字段映射抽成带显式返回类型的 `toReceiptLineFormItem()`，把「响应 VO → 表单态」的转换集中一处，并对可空的 `batchNo` 做 `|| ''` 归一。

**关键点**：`buildSaveData` 原本就在提交前做了运行时收窄（校验 `drugId`/`batchNo`/`expiryDate`/`qty` 必填），只是类型没同步表达。本次是**让类型追上已有的运行时行为**，不是改行为。

## 四、复验证据

### 4.1 类型检查

```powershell
cd admin-ui
node --max_old_space_size=8192 ./node_modules/vue-tsc/bin/vue-tsc.js --noEmit --pretty false
```

| 指标 | 修复前 | 修复后 |
| --- | --- | --- |
| **采购相关错误** | **6 条** | **0 条** ✅ |
| 全项目错误 | 40 条 | 16 条（E 5 条 + 框架其他模块 11 条，均非 B 范围） |

### 4.2 构建与静态检查

| 项 | 结果 |
| --- | --- |
| 生产构建 `vite build --mode env.local` | **成功**（29.39s，0 错误） |
| ESLint（采购目录 .vue/.ts） | **0 错误** |
| SFC 官方编译 8 个页面 | 通过 |
| 日期列格式化完整性 | 0 处遗漏 |
| 前后端路由/菜单契约 | 通过 |
| 页面静态体检（组件/图标/API/字典/分页/权限） | 通过 |

### 4.3 功能回归（复验要求「创建/提交无回归」）

一键回归 `tools/run-all-checks.ps1` 对 Docker 环境实跑：

```
共 14 项检查，失败 0 项
  主自测（正常流程 + 入账回滚）      通过
  补齐 update/cancel/void/delete  通过
  参数错误 / 状态拦截              通过
  补充接口与导出                  通过
  权限强制校验                    通过
  并发验证（并发入账 / 并发更新）     通过
  导出内容深度校验                 通过
  日期列格式化完整性                通过
  Vue SFC 官方编译                通过
  页面静态体检                    通过
  前后端路由与菜单契约              通过
  权限串三方一致                   通过
  迁移 SQL 列数与值数一致           通过
  前端产物是否含最新修复             通过
```

其中主自测 30 项含**供应商创建、采购订单创建/提交/审批、收货单创建/提交**全链路，确认创建与提交无回归。

### 4.4 页面加载验证

新构建产物已部署到 admin-ui 容器，采购域 12 个 chunk（`receipt-BejHY4Og.js`、`ReceiptForm-D5R14a1N.js`、`order-CX5mXxQp.js`、`OrderForm-BYvRrA86.js`、`supplier-BW7C88pR.js`、`SupplierForm-BbuBHbmA.js`、`license-DmPNzASb.js`、`LicenseForm-8R6idktZ.js` 等）全部 HTTP 200，路由动态导入映射正常解析。

## 五、遗留与说明

1. **`auto-imports.d.ts` / `auto-components.d.ts` 不入库是仓库既有设计**（`.gitignore` 的 `auto-*.d.ts`），本次未改动。副作用是：**干净检出下直接跑 `pnpm ts:check` 会因缺声明而刷出数百条假错误**。建议团队考虑把这两个文件入库，或在 `ts:check` 前加一步生成（属仓库级决策，不在 B 范围内擅自改动）。
2. E 的处方页 5 条类型错误（ACC-20260915-006）与框架其他模块 11 条未声明全局错误，均未触碰。
3. B 的收货入库/入账闭环仍被 C 的 `InventoryFacade.receive` 阻塞（ACC-20260915-001，主责 C）；C 实现后需复验采购入库动态成功路径。
