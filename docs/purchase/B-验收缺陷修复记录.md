# B 模块验收缺陷修复记录

对应验收报告：`docs/acceptance/reports/20260915-090535-main-functional-report.md`
修复分支：`fix/b-purchase-ts`
修复范围：仅 `admin-ui` 采购域（B 主责目录），未改动后端、SQL、Docker 配置与共享组件。

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
