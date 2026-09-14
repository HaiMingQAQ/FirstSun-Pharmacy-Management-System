# B 成员交付说明 —— 供应商与采购收货

> 成员：B（供应商与采购收货）
> 后端模块：`backend/yudao-module-pharmacy`（采购域，错误码段 `1-031-xxx-xxx`）
> 前端目录：`admin-ui/src/views/pharmacy/purchase`、`admin-ui/src/api/pharmacy/purchase`
> 负责数据表：`ph_supplier`、`ph_supplier_license`、`ph_po_order`、`ph_po_order_line`、`ph_po_receipt`、`ph_po_receipt_line`

## 一、交付进度

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 一 | 供应商档案（后端 + 管理端页面） | 已交付 |
| 一 | 供应商证照（后端 + 管理端页面 + 到期提醒） | 已交付 |
| 二 | 采购订单（CRUD + 提交/审批/发出/取消状态流转 + 金额服务端重算） | 已交付 |
| 二 | 采购收货（分批收货、批号/效期/货位校验、CAS 防重复入账、批次回写、订单已收累计） | 已交付（入账依赖 C 的库存服务，见第七节） |
| 三 | 采购统计、无单收货完善、证照到期自动任务 | 计划中 |

**未修改任何表结构**：6 张表已由 `sql/firstsun_pharmacy_init.sql` 定义，本模块只新增菜单与字典数据。

## 二、数据表与字段说明（B 主维护）

| 表 | 说明 | 关键约束 |
| --- | --- | --- |
| `ph_supplier` | 供应商档案 | `uk_supplier_code`；`approve_status` 0待审/1通过/2驳回；`status` 1启用/0停用；`audit_by/audit_at/audit_opinion` 仅审核接口维护 |
| `ph_supplier_license` | 供应商证照 | `idx_supplier_expire`；`status` 1有效/0过期（按到期日计算，前端不传） |
| `ph_po_order` | 采购订单头 | `uk_order_no`；`status` -1取消/0草稿/1提交/2审批/3发出/4部分到货/5完成 |
| `ph_po_order_line` | 采购订单明细 | `uk_po_line(order_id,line_no)`；CHECK 保证 `received_qty <= order_qty` |
| `ph_po_receipt` | 采购收货单头 | `uk_receipt_no`；`status` 0待提交/1已提交/2已入账/3已作废；`posted_at` 与 status 组成入账 CAS |
| `ph_po_receipt_line` | 采购收货明细 | `uk_receipt_line(receipt_id,line_no)`；批号/效期必填，`location_id` 入账必填；`create_batch_id` 由库存服务回写 |

### 金额口径（服务端统一重算，不信任前端）

```text
totalAmount     = Σ(orderQty × unitPrice)                     含税总金额（元）
discountAmount  = Σ(orderQty × unitPrice × (1 - discountRate)) 优惠金额（元）
payableAmount   = totalAmount - discountAmount                 应付金额（元）
lineAmount      = orderQty × unitPrice × discountRate          行金额（元，四舍五入 2 位）
```

## 三、接口清单

统一前缀 `/admin-api`，返回 yudao 标准 `CommonResult`。

### 3.1 供应商 `/pharmacy/purchase/supplier`

| 方法 | 路径 | 权限标识 | 说明 |
| --- | --- | --- | --- |
| POST | `/create` | `pharmacy:purchase:supplier:create` | 新建，强制 `approveStatus=0` |
| PUT | `/update` | `pharmacy:purchase:supplier:update` | 更新，不接受审核字段 |
| DELETE | `/delete` | `pharmacy:purchase:supplier:delete` | 被采购订单引用拒绝；证照随之逻辑删除 |
| GET | `/get?id=` | `pharmacy:purchase:supplier:query` | 详情，返回完整银行账号 |
| GET | `/page` | `pharmacy:purchase:supplier:query` | 分页，银行账号仅返回 `bankAccountMasked` |
| GET | `/simple-list?keyword=` | 登录即可 | 启用且首营通过的供应商（采购单/收货单下拉） |
| GET | `/all-simple-list` | 登录即可 | 全部供应商（证照登记与筛选） |
| PUT | `/approve?id=&approveStatus=&auditOpinion=` | `pharmacy:purchase:supplier:approve` | 首营审核，仅 1通过/2驳回，只能从待审流转一次 |
| GET | `/export-excel` | `pharmacy:purchase:supplier:export` | 导出（银行账号按掩码） |

### 3.2 供应商证照 `/pharmacy/purchase/license`

| 方法 | 路径 | 权限标识 | 说明 |
| --- | --- | --- | --- |
| POST / PUT / DELETE | `/create` `/update` `/delete?id=` | `...:license:create/update/delete` | 增改删 |
| GET | `/get?id=` | `...:license:query` | 详情 |
| GET | `/page` | `...:license:query` | 分页，回填 `supplierName`、`daysToExpire` |
| GET | `/list-by-supplier?supplierId=` | `...:license:query` | 某供应商全部证照 |
| GET | `/expiring-list?days=30` | `...:license:query` | 到期提醒（ADM-002），含已过期 |
| PUT | `/refresh-status` | `...:license:update` | 按当天重算证照状态，返回变更行数 |
| GET | `/export-excel` | `...:license:export` | 导出 |

### 3.3 采购订单 `/pharmacy/purchase/order`

| 方法 | 路径 | 权限标识 | 说明 |
| --- | --- | --- | --- |
| POST | `/create` | `...:order:create` | 新建（草稿）。校验门店营业、供应商启用且首营通过、药品可采购；单号 `PO{门店}-yyyyMMdd-流水`，唯一键冲突自动重新取号 |
| PUT | `/update` | `...:order:update` | 仅草稿可改，明细整体重建 |
| DELETE | `/delete?id=` | `...:order:delete` | 仅草稿且无收货记录 |
| GET | `/get?id=` | `...:order:query` | 详情（订单头 + 明细 + 门店/供应商/药品名称） |
| GET | `/page` | `...:order:query` | 分页（回填门店名与供应商名） |
| PUT | `/submit?id=` | `...:order:submit` | 草稿 → 已提交 |
| PUT | `/approve?id=` | `...:order:approve` | 已提交 → 已审批；审批人取当前登录员工，重复审批被拒绝 |
| PUT | `/issue?id=` | `...:order:issue` | 已审批 → 已发出 |
| PUT | `/cancel?id=` | `...:order:cancel` | 草稿/已提交/已审批 → 已取消；存在未作废收货单时拒绝 |
| GET | `/export-excel` | `...:order:export` | 导出 |

### 3.4 采购收货 `/pharmacy/purchase/receipt`

| 方法 | 路径 | 权限标识 | 说明 |
| --- | --- | --- | --- |
| POST | `/create` | `...:receipt:create` | 新建（待提交）。有单收货校验订单可收状态、行归属、药品一致、剩余可收数量；无单收货要求 `isFreeReceipt=1` 且当前员工为店长（WH-002）；单号 `GR{门店}-yyyyMMdd-流水` |
| PUT | `/update` | `...:receipt:update` | 仅待提交可改，明细整体重建 |
| DELETE | `/delete?id=` | `...:receipt:delete` | 仅待提交或已作废 |
| GET | `/get?id=` | `...:receipt:query` | 详情（收货单头 + 明细 + 药品名称 + 批次号） |
| GET | `/page` | `...:receipt:query` | 分页（回填门店名、采购订单号、收货人姓名） |
| PUT | `/submit?id=` | `...:receipt:submit` | 待提交 → 已提交 |
| PUT | `/post?id=` | `...:receipt:post` | 入账：状态 CAS → 调库存服务 → 回写批次号 → 累计订单已收数量与订单状态（同一事务） |
| PUT | `/void?id=` | `...:receipt:void` | 待提交 → 已作废；已入账不可直接作废 |
| GET | `/export-excel` | `...:receipt:export` | 导出 |

### 关键业务校验（服务端强制，前端按钮不代替）

1. **超收拦截**：`本次数量 + 其他未作废收货单已占用数量 + 订单行已收数量 ≤ 订购数量`；
2. **效期拦截**：收货明细有效期不得早于今天、不得早于生产日期；批号必填；
3. **入账前证照校验**：供应商须有有效期内的经营许可证或 GSP 证（`validateOperateLicenseValid`）；
4. **入账前货位校验**：每条明细必须有 `locationId`；
5. **入账防重复**：`UPDATE ... WHERE status=1 AND posted_at IS NULL` 的 CAS，影响 0 行即抛「已入账」；
6. **差异标记**：数量差异(1) / 价格差异(2) 由服务端比对订单行计算；质检结果由明细质检标记聚合；
7. **无单收货**：仅店长岗位，且必须显式标记 `isFreeReceipt=1`。

## 四、字典清单（B 新增）

| 字典 type | 名称 | 取值 | 类型 ID / 数据 ID |
| --- | --- | --- | --- |
| `pharmacy_supplier_approve_status` | 供应商首营审核状态 | 0待审/1通过/2驳回 | 310 / 1600-1602 |
| `pharmacy_license_type` | 证照类型 | 0经营许可证/1生产许可证/2GSP证/3营业执照/4其他 | 311 / 1610-1614 |
| `pharmacy_license_status` | 证照状态 | 1有效/0过期 | 312 / 1620-1621 |
| `pharmacy_po_status` | 采购订单状态 | -1取消/0草稿/1提交/2审批/3发出/4部分到货/5完成 | 313 / 1630-1636 |
| `pharmacy_receipt_status` | 收货单状态 | 0待提交/1已提交/2已入账/3已作废 | 314 / 1640-1643 |
| `pharmacy_receipt_diff_type` | 收货差异标记 | 0无/1数量差异/2价格差异 | 315 / 1650-1652 |
| `pharmacy_quality_status` | 收货单质检结果 | 0未检/1合格/2有异常 | 316 / 1660-1662 |
| `pharmacy_quality_flag` | 收货明细质检标记 | 0待检/1通过/2异常拒收 | 317 / 1670-1672 |

后端常量：`DictTypeConstants`；前端常量：`admin-ui/src/utils/dict.ts`（**公共文件，本次仅追加常量，需 A 合并确认**）。

## 五、菜单与权限

| 迁移脚本 | 内容 |
| --- | --- |
| `sql/migrations/20260912_b_purchase_supplier_menu.sql` | 采购管理目录 22100；供应商 22110-22116；供应商证照 22120-22125；字典 310-312 |
| `sql/migrations/20260913_b_purchase_order_receipt_menu.sql` | 采购订单 22130-22139；采购收货 22140-22148；字典 313-317 |

- 菜单组件：`pharmacy/purchase/supplier/index`（`PharmacyPurchaseSupplier`）、`.../license/index`（`PharmacyPurchaseLicense`）、`.../order/index`（`PharmacyPurchaseOrder`）、`.../receipt/index`（`PharmacyPurchaseReceipt`）；`defineOptions({ name })` 与 `component_name` 一致。
- 角色绑定：超管（tenant 1 / role 1）与 FirstSun 租户管理员（tenant 163 / role 167，共享账号 `0407`），并刷新租户套餐 114 的 `menu_ids`。
- 执行后清 Redis 菜单缓存：`docker exec firstsun-pharmacy-redis redis-cli FLUSHDB`。

## 六、错误码（新增段 `1-031-xxx-xxx`）

| 段 | 内容 |
| --- | --- |
| `1_031_001_000-009` | 供应商：不存在、编码重复、信用代码重复、折扣率非法、已停用、审核未通过、被订单引用、重复审核、审核状态非法、审核人非员工 |
| `1_031_002_000-005` | 证照：不存在、供应商不存在、证照号重复、日期非法、已过期、缺少有效经营类证照 |
| `1_031_003_000-010` | 采购订单：不存在、单号重复、明细为空、状态不允许、药品不可采购、金额非法、审批人非员工、已存在收货记录、数量非法、折扣率非法、重复审批 |
| `1_031_004_000-013` | 采购收货：不存在、单号重复、明细为空、状态不允许、超收、效期非法、批号必填、重复入账、订单不可收货、无单收货非店长、订单行不匹配、药品不存在、收货人非员工、货位必填 |

## 七、跨模块契约（B 发起，实现方 C）

`InventoryFacade` 原只有销售侧 `deduct` / `returnBack`。B 的收货入账需要库存入库能力，已按方案第 3 节「收货入账：B 发起、C 维护库存服务」新增契约：

```java
// api/inventory/InventoryFacade.java
ReceiveResult receive(Long storeId, String receiptNo, List<ReceiveItem> items);

// api/inventory/dto/ReceiveItem.java：bizLineId / storeId / drugId / batchNo /
//   manufactureDate / expiryDate / qty / unitPrice / warehouseId / locationId
// api/inventory/dto/ReceiveResult.java：逐行返回 bizLineId → batchId / locationStockId
```

- 幂等键：`receiptNo + bizLineId`（收货单号 + 收货明细编号），C 实现时需按此去重；
- B 侧调用已处理降级：`UnsupportedOperationException` → 业务错误 `INV_SERVICE_UNAVAILABLE`，**整单事务回滚**，收货单不会进入「已入账」，订单已收数量不增加；
- 需要 C 确认：接口签名、字段口径（尤其是 `locationId` 与仓库校验）、幂等键约定。
- 变更文件：`api/inventory/InventoryFacade.java`、`api/inventory/dto/ReceiveItem.java`、`api/inventory/dto/ReceiveResult.java`，以及为保持编译通过的降级实现 `api/inventory/InventoryFacadeImpl.java`（新增方法抛 `UnsupportedOperationException`）。

## 八、自测与验证记录（本轮已实际执行）

### 8.1 后端编译（真实编译器）

本机装有 JDK 21 与 Docker，临时落地了一份 Maven 3.9.9（`D:\作业\药店\tools`，不随仓库提交），对 pharmacy 模块与整包分别编译：

```text
mvn -pl yudao-module-pharmacy -am -DskipTests compile  →  BUILD SUCCESS（150 个源文件）
mvn -pl yudao-server -am -DskipTests package           →  BUILD SUCCESS（产出 yudao-server.jar）
```

首次编译暴露并修复了一处真实编译错误：`SupplierLicenseMapper.selectExpiringList` 中
`LambdaQueryWrapperX` 的原生方法 `le()` 会返回父类型 `LambdaQueryWrapper`，导致后续 `eqIfPresent()` 找不到符号
（`LambdaQueryWrapperX` 只重写了 `eq/orderByDesc/last/in/exists/notExists/and/or/nested/not` 与全部 `*IfPresent`）。
修法：扩展方法必须写在原生方法之前。已加注释，并用脚本扫描全模块 134 个 wrapper 链，当前 0 处断链。

#### 8.1.1 单元测试（`mvn test`，45/45 通过）

新增 `PurchaseOrderServiceImplTest`（14 例）、`PurchaseReceiptServiceImplTest`（13 例）、`SupplierServiceImplTest`（10 例）、`SupplierLicenseServiceImplTest`（8 例），与 D 的存量测试同风格（JUnit5 + Mockito），覆盖接口测试难以构造的分支：

| 覆盖点 | 说明 |
| --- | --- |
| 金额服务端重算 | 140.00 / 10.00 / 130.00 与逐行金额 120.00 / 10.00 |
| **药品 id 去重（D5 回归）** | 同一药品出现在两行时，传给 `DrugApi.validateDrugList` 的必须是去重后的 id |
| 行号服务端生成 | 忽略前端传值，避免 `uk_*_line` 唯一键冲突 |
| **取号跳过逻辑删除占号（D6 回归）** | 前缀内最大号为 0023 时，新号必须是 0024，不能用 `selectCount`（会漏掉 `deleted=1` 的行） |
| **并发抢占后重算单号（D6 回归）** | 首次插入撞唯一键 → 重算单号重试成功（0024 被抢 → 0025），而不是直接抛"单号已存在" |
| **`applyReceiptPosted` 原子性（B2 回归）** | 走 `update(null, wrapper)` 原子累加；影响 0 行即报超收；全部收货→完成(5)、部分→部分到货(4)；跨订单行拒绝 |
| 入账幂等与顺序 | CAS 影响 0 行→重复入账；货位校验在 CAS 之前（不写状态、不调库存） |
| 库存服务不可用 | 抛 `UnsupportedOperationException` → 明确业务错误，且**绝不调用订单累计**（配合 `@Transactional` 保证单据与库存一致） |
| 状态机拦截 | 非草稿不可改、已发出不可取消、已有收货单不可取消、已提交/已入账不可作废或删除 |
| 供应商规则 | 编码/信用代码唯一、折扣率 0~1、新建一律待审、被订单引用禁删、删除级联证照、审核人须为绑定员工（静态 mock） |
| 证照规则 | 日期校验、同供应商证照号唯一、状态按到期日自动计算、入账前经营类证照有效性（缺证/过期拦截、有效放行） |

```powershell
cd D:\作业\药店\FirstSun-Pharmacy-Management-System\backend
mvn -s D:\作业\药店\tools\settings.xml -pl yudao-module-pharmacy -am -B test `
  "-Dtest=SupplierServiceImplTest,SupplierLicenseServiceImplTest,PurchaseOrderServiceImplTest,PurchaseReceiptServiceImplTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" "-DargLine=-Djdk.attach.allowAttachSelf=true"
# → Tests run: 45, Failures: 0, Errors: 0  BUILD SUCCESS
```

### 8.2 迁移脚本（真实 MySQL 执行）

两个迁移脚本通过 JDBC 直连 `firstsun_pharmacy` 执行：**16 条 + 19 条语句全部成功，0 失败**。执行后查询校验：

| 校验项 | 结果 |
| --- | --- |
| 采购域菜单条数（22100-22148） | 33 |
| 菜单名称/父级/路径/组件 | 采购管理(22000 下) → 供应商 / 供应商证照 / 采购订单 / 采购收货，`component_name` 与页面 `defineOptions.name` 一致 |
| 字典数据中文文案 | `pharmacy_po_status` 7 条、`pharmacy_receipt_status` 4 条，label/value/color 均正确 |
| 角色绑定 | tenant 1/role 1 → 33 条；tenant 163/role 167（共享账号 0407）→ 33 条 |

### 8.3 接口自测（真实 HTTP + 真实 MySQL/Redis）

`b-verify-purchase.ps1` 在本地实例（48081，与 Docker 同一套 MySQL/Redis）上执行，脚本内含 **30 项断言，全部通过**（其中 2 项是 D1 修复后新增的日期回归断言）：

| 分组 | 通过项 |
| --- | --- |
| 供应商 | 登录、创建、重复编码拒绝、分页、**列表不返回账号原文（NFR-12）**、列表返回末 4 位掩码、详情返回原文供回填、首营审核、重复审核拒绝、被订单引用时禁止删除 |
| 证照 | 创建、列表回填供应商名与到期天数、日期非法拒绝、**查询不存在证照返回业务错误码 1031002000** |
| 采购订单 | 创建、**金额服务端重算（含税 50.00 / 优惠 5.00 / 应付 45.00）**、单号规则、提交、审批、重复审批拒绝、标记发出 |
| 采购收货 | 创建、**部分收货标记数量差异(1)**、超收拦截、提交、入账返回库存服务未就绪(1029002001)、**入账失败后状态回滚为已提交且订单已收数量保持 0** |

#### 8.3.1 补充接口覆盖（`tools/check-extra-endpoints.ps1`，13 项全部通过）

把主自测未覆盖的接口全部打了一遍，确保采购域 38 条路由都有真实调用记录：

| 分组 | 覆盖内容 |
| --- | --- |
| 下拉 | 供应商 `simple-list`（仅可采购）与 `all-simple-list`（全部），并校验前者是后者的子集 |
| 证照 | `expiring-list?days=30` 到期提醒、`refresh-status` 状态刷新（返回变更行数） |
| 条件分页 | 证照按类型+状态、采购订单按**下单日期区间**（`betweenIfPresent` + `LocalDate[]` 分支）、收货单按状态+差异、供应商按审核状态+启停 |
| 导出 | 供应商 / 证照 / 采购订单 / 采购收货 **4 个 Excel 导出均返回真实文件流**（HTTP 200、非 JSON、字节数 > 1KB）；并用 `tools/check-export.js` 解 zip 逐份校验：4 份都是结构完整的 xlsx（9 个 zip 条目、含 sharedStrings），内容为真实业务数据中文表头；**供应商导出含掩码 `****1234` 且不含账号原文 `6222020200001234`**（NFR-12 在导出路径同样成立） |

#### 8.3.2 负向路径自测（`tools/check-negative-paths.ps1`，14 项全部通过）

覆盖团队规范要求的「参数错误 / 状态不允许 / 业务规则」类自测场景（在 Docker 后端上执行）：

| 场景 | 期望 | 实际错误码 | 结果 |
| --- | --- | --- | --- |
| 供应商未通过首营审核 → 创建采购订单 | 拒绝 | `1_031_001_005` | 通过 |
| 采购明细为空 | 参数校验拒绝 | 非 0 | 通过 |
| 折扣率大于 1 | 参数校验拒绝 | 非 0 | 通过 |
| 草稿状态订单 → 收货 | 拒绝 | `1_031_004_008` | 通过 |
| 批次有效期早于今天 | 拒绝（过期药品不得入库） | `1_031_004_005` | 通过 |
| 无单收货由非店长操作（0407 岗位=系统管理员） | 拒绝 | `1_031_004_009` | 通过 |
| 收货单缺货位 → 入账 | 拒绝 | `1_031_004_013` | 通过 |
| 供应商只有过期证照 → 入账 | 拒绝并保持「已提交」 | `1_031_002_004` + status=1 | 通过 |

自测矩阵至此完整：**正常流程（30 项）+ 补齐 update/cancel/void/delete（21 项）+ 参数错误/状态拦截（14 项）+ 权限不足（11 项）+ 补充接口/导出（13 项）+ 并发行为（13 项）+ 重复操作（重复编码/重复审核/重复审批/超收，含在前述各项内）**，合计 **102 项断言**，全部在真实 MySQL/Redis 上通过。

#### 8.3.2 补齐更新/取消/作废/删除覆盖（`tools/check-crud-remaining.ps1`，21 项全部通过）

第一轮自测只覆盖了「新增/查询/审核/提交/审批/发出/入账」，遗漏了 `update / cancel / void / delete` 这 7 个接口；补测后采购域 38 条路由**全部有真实调用记录**：

| 分组 | 覆盖内容 |
| --- | --- |
| 供应商 | 更新（名称/联系人/折扣率）→ 详情回读一致 |
| 供应商证照 | 更新（类型/证照号/到期日）→ 详情回读一致、状态重算为有效 |
| 采购订单 | 更新（明细整体重建：2 行、金额重算 140.00 / 10.00 / 130.00）→ 取消（草稿→已取消 -1）；**已有收货记录的已审批订单取消被拒**（`1_031_003_007`） |
| 采购收货 | 更新（仓库/数量/批号/质检标记 → 质检结论聚合为「有异常」）→ 作废（待提交→已作废 3）→ 删除已作废单据 → 详情查不到 |

> 这一轮补测**直接暴露了两个真实缺陷（D4、D5）**，见 8.7 节。

### 8.4 前端与静态一致性检查

| 检查 | 方法 | 结果 |
| --- | --- | --- |
| ESLint | `eslint src/views/pharmacy/purchase src/api/pharmacy/purchase` | **通过，0 错误** |
| 前后端接口契约 | `tools/check-contract.js`：解析前端 `request.xxx({url})` 与后端 `@RequestMapping` + `@XxxMapping` | 前端调用 **100% 命中后端路由**；菜单 `component_name` ↔ 页面 `defineOptions.name` **4/4 一致** |
| 页面静态体检 | `tools/check-vue-usage.js`：模板组件是否可解析、图标集合是否合法、API 函数是否真实导出、`DICT_TYPE` 常量是否存在、列表页是否有 `Pagination` 与 `v-hasPermi` | **8 个页面全部通过** |
| **SFC 编译** | `tools/check-sfc-compile.js`：用 Vue 官方 `@vue/compiler-sfc` 对 8 个 `.vue` 逐个执行 `parse` + `compileScript` + `compileTemplate`（带 binding metadata） | **8 个页面全部编译通过**（模板/脚本无语法或指令错误） |
| 权限三方一致 | `tools/check-permissions.js`：菜单 SQL ↔ 后端 `@PreAuthorize` ↔ 前端 `v-hasPermi/checkPermi` | 菜单 28 个权限，后端全部使用、前端引用 27 个，**无缺失、无悬空**（`supplier:query` 仅服务端使用，与 A 的页面惯例一致） |
| 字典数据 | 直接查询 `system_dict_type` / `system_dict_data` | B 的 8 个字典类型均有数据（2/3/3/3/3/4/5/7 条，共 30 条） |
| vue-tsc | 全仓类型检查 | 13786 条错误**全部**是 `TS2304 Cannot find name 'ref'/'useMessage'`——因 `pnpm install --ignore-scripts` 跳过了 vite 自动导入声明文件生成，A 的既有模块同样报 104 条同类错误；**B 模块 0 条非 TS2304 错误** |
| **Docker 产物 chunk 校验** | `tools/check-frontend-chunk.js`：从 nginx 取入口页 → 解析动态路由映射 chunk → 下载 4 个页面 chunk 并检查组件名与权限串 | **4/4 通过**：`supplier-*.js`(10.4KB)、`license-*.js`(8.9KB)、`order-*.js`(11.6KB)、`receipt-*.js`(12.1KB)，每个 chunk 都含 `defineOptions.name`（与菜单 `component_name` 一致）与采购权限串 |
| **字典运行时数据** | 调用前端实际使用的字典接口（`/system/dict-data/list-all-simple`） | **8/8 通过**：B 的 8 个字典类型全部返回，标签为正确中文（待审/通过/驳回、经营许可证…、已取消/草稿/…/已完成、待提交/已提交/已入账/已作废、无差异/数量差异/价格差异、未检/合格/有异常、待检/通过/异常拒收） |

> 说明：本地 `vite build` 无法执行——沙箱禁止 Node 以管道 stdio 启动子进程（`--configLoader bundle` 报 `spawn EPERM`），改用 `--configLoader runner` 时又因 CJS 依赖（picomatch）在 ESM runner 下缺 `require` 而失败；两者均为运行环境限制，与页面代码无关。前端产物构建由 Docker（`docker compose up -d --build admin-ui`）完成；上面 5 项检查（ESLint、SFC 官方编译、结构体检、权限一致、类型）已覆盖构建期最常见的失败原因。

### 8.5 权限强制校验（行为化验证）

`tools/check-permission-enforcement.ps1`：用租户 1 超管（`admin`）临时创建「不含任何采购菜单」的角色与用户，
验证受限账号的真实调用结果，验证后自动清理（不污染药店演示账号）。**11 项断言全部通过**：

| 断言 | 结果 |
| --- | --- |
| 超管登录成功 | 通过 |
| 有采购权限账号访问 `/pharmacy/purchase/supplier/page` | **通过（code=0）** |
| 创建无采购权限角色 / 自测用户 | 通过 |
| 受限账号登录成功 | 通过 |
| 受限账号查询供应商 | **被拒绝（403）** |
| 受限账号创建供应商 | **被拒绝（403）** |
| 受限账号执行收货入账 `/receipt/post` | **被拒绝（403）** |
| 受限账号的写入未落库（超管复查 count=0） | 通过 |
| 清理自测用户与角色 | 通过 |

> 该结果同时证明：迁移脚本对 **tenant 1 / role 1（超管）** 与 **tenant 163 / role 167（共享账号 0407）** 的菜单绑定都真实生效，权限串与后端 `@PreAuthorize` 完全对应。

补充核对前端**左侧栏与按钮权限的数据源**（`GET /system/auth/get-permission-info`，账号 0407）：

```text
下发权限 63 个，其中采购域 28 个（license/order/receipt/supplier 全量）
菜单树：采购管理（path=purchase）
          ├─ 供应商      component=pharmacy/purchase/supplier/index
          ├─ 供应商证照  component=pharmacy/purchase/license/index
          ├─ 采购订单    component=pharmacy/purchase/order/index
          └─ 采购收货    component=pharmacy/purchase/receipt/index
租户套餐 114：菜单 82 个，其中采购域 33 个
```

即：登录后左侧会渲染这四个菜单，页面内所有按钮的 `v-hasPermi` 也都能通过。

### 8.6 浏览器验收暴露的缺陷与修复（与 yudao 日期契约相关）

用户在浏览器验收「供应商 / 供应商证照」页面时发现三处问题，均已定位并修复（**均为 B 模块自身问题，非框架缺陷**）：

| 编号 | 现象 | 根因 | 修复 |
| --- | --- | --- | --- |
| D1 | 收货时间落库为 1970，单号出现 `GR407-19700101-*` | yudao 全局把 `LocalDateTime` 映射为**毫秒时间戳**（`TimestampLocalDateTimeDeserializer` 用 `getValueAsLong()`，传字符串静默得 0），我的收货表单却用 `value-format="YYYY-MM-DD HH:mm:ss"` 传字符串 | 收货时间选择器改 `value-format="x"`（提交数字）；查询区间改 `datetimerange` + `YYYY-MM-DD HH:mm:ss`（查询参数走 `@DateTimeFormat`，与 body 不同）；自测脚本改传 `[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()`；**新增 2 条回归断言**（时间戳 > 1e12、单号日期段 = 当天） |
| D2 | 日期列显示成 `2026,9,11`；编辑表单里日期为空 | yudao 对 `LocalDate` 用 Jackson 默认序列化 → 响应是数组 `[2026,9,11]`，页面直接渲染数组，且数组无法回填 `el-date-picker` | 新增 `views/pharmacy/purchase/utils/date.ts`（`formatLocalDate` / `localDateFormatter` / `toLocalDateString`）；**逐处整改**：证照列表 发证/到期日、订单列表 下单/预计到货、订单详情、收货列表 收货时间（`dateFormatter`）、收货详情明细 生产日期/有效期至、证照「到期提醒」弹窗、供应商页「查看证照」弹窗；证照/订单/收货三个表单打开时把数组转 `YYYY-MM-DD` 字符串。新增脚本 `tools/check-date-columns.js` 做机械化校验，当前 **0 处未格式化日期列** |
| D3 | 自测数据里的中文名称全变成 `?`（如 `??????????`） | 自测脚本用 Windows PowerShell 5.1 发 JSON，字符串 body 被按 Latin-1 编码，中文在**发请求时**就丢了（页面与数据库本身处理中文正常） | 两个自测脚本改为 `[System.Text.Encoding]::UTF8.GetBytes()` + `charset=utf-8`；已落库的问号数据用 `tools/fix-demo-data.sql` 改写为真实名称 |

同时把 API 类型里的日期字段改为与后端契约一致：`LocalDateTime` → `number`（毫秒时间戳），`LocalDate` → `string | number[]`（表单绑定字符串、响应为数组）。

**日期契约的补充实测**：
- 前端 `value-format="x"` 在不同 Element Plus 版本下可能给出**数字**或**数字字符串**，两种都实测通过：用数字字符串 `"1789130516000"` 提交收货单 → `code=0`、单号 `GR407-20260911-0009`、`receiveDate=1789130516000`（非 1970）；
- `LocalDate` 以 `"2026-09-01"` 字符串提交 → 落库正确，响应回读为 `[2026,9,1]` 数组（前端已统一格式化）；
- 修复后新建的自测供应商名称实测为 `【接口自测】临时供应商`（中文写入正常，证明脚本编码问题已解决）。

> **给后续开发者的约定（重要）**：
> 1. 表单里凡是后端 `LocalDateTime` 字段，`el-date-picker` 必须 `value-format="x"`，禁止传 `YYYY-MM-DD HH:mm:ss` 字符串；
> 2. 表格里渲染 `LocalDate` 必须用 `localDateFormatter`，渲染 `LocalDateTime` 用 `dateFormatter`（`@/utils/formatTime`）；
> 3. PowerShell 自测脚本发中文 JSON 必须显式 UTF-8 字节编码；
> 4. PowerShell 脚本（`.ps1`）若含中文，**必须带 UTF-8 BOM**——PowerShell 5.1 会把无 BOM 文件按 ANSI 解析，导致中文串截断报语法错误。

### 8.6.1 Docker 重建后暴露的缺陷 D6：收货单号生成器与唯一键口径不一致

用户在本机执行 `docker compose up -d --build backend admin-ui` 后跑全量自测，发现 **`POST /pharmacy/purchase/receipt/create` 一律返回 `1_031_004_001 收货单号已存在`**，即收货单完全无法创建（D1~D5 之外的独立缺陷）。

| 项 | 内容 |
| --- | --- |
| 现象 | 任意新建收货单都报「收货单号已存在」，但库里该前缀下一个可用号都没有 |
| 根因 | `uk_receipt_no` 唯一键**不包含 `deleted` 列**，逻辑删除的单据仍占号；而 `generateReceiptNo` 用 MyBatis-Plus 的 `selectCount` 取计数，它会**自动过滤 `deleted=1`**。两者口径不一致 |
| 现场数据 | 前缀 `GR407-20260911-` 实际 23 行，其中 **5 行 `deleted=1`**；`selectCount` 只数到 **18** |
| 失败链条 | 算出的号是 `0019`…`0023`，正好全被那 5 行已删单据占着 → 5 次重试全部 `DuplicateKeyException` → 抛「收货单号已存在」 |
| 取证方式 | 临时日志打印 `prefix / count / attempt / 生成的号`，实测输出：`prefix=GR407-20260911- count=18 attempt=0 -> GR407-20260911-0019` … `attempt=4 -> -0023` |
| 修复 | 改为「取前缀内 `MAX(receipt_no)` 的下一个流水」，SQL 显式**不排除逻辑删除行**；`generateOrderNo` 存在同样问题，一并修复 |
| 验证 | 修复后连续创建 3 张：`0024 / 0025 / 0026`（首个可用号，不再撞旧号）；并新增 2 条单元测试回归（跳过已删占号 + 并发抢占后重算单号） |

```java
// 修复前（错）：selectCount 会漏掉逻辑删除的行
Long count = receiptMapper.selectCount(new LambdaQueryWrapperX<PurchaseReceiptDO>()
        .likeRight(PurchaseReceiptDO::getReceiptNo, prefix));
long seq = (count == null ? 0L : count) + 1 + attempt;

// 修复后（对）：取已存在的最大流水号，含逻辑删除行
@Select("SELECT MAX(receipt_no) FROM ph_po_receipt WHERE receipt_no LIKE CONCAT(#{prefix}, '%')")
String selectMaxReceiptNo(@Param("prefix") String prefix);
```

> **为什么前几轮没测出来**：只有当「当天已有单据被作废/删除」时才会撞号。前几轮自测跑在干净数据上，
> 而 Docker 重建前已经累积了若干作废/删除单据，正好触发。这也说明**"重建容器后必须重跑全量自测"**是必要的。
>
> **给后续开发者的约定（重要）**：单号类字段的"计数"与"唯一键"口径必须一致——若唯一键不含 `deleted` 列，
> 取号时就绝不能依赖会过滤逻辑删除行的 `selectCount`，必须按 `MAX(单号)` 取下一个。

排查用的机械化校验脚本（均针对 B 模块，可随时复跑）：

```powershell
node tools\check-date-columns.js          # 日期列是否都挂了 formatter（当前 0 处遗漏）
node tools\check-sfc-compile.js           # 8 个页面能否被 Vue 官方编译器编译
node tools\check-vue-usage.js             # 组件/图标/API 函数/DICT_TYPE/分页/权限
node tools\check-contract.js              # 前端路由 ↔ 后端接口、菜单组件名 ↔ 页面名
node tools\check-permissions.js           # 菜单权限 ↔ 后端 @PreAuthorize ↔ 前端 v-hasPermi
node tools\check-frontend-freshness.js    # nginx 上正在提供的前端产物是否包含本轮修复
```

### 8.7 代码审查（外部评审）与修复记录
| 编号 | 问题 | 处理 |
| --- | --- | --- |
| A-0 | `LambdaQueryWrapperX` 断链导致编译失败 | 已修复并编译验证 |
| B1 | 分页接口把银行账号**原文**随 JSON 返回（违反 NFR-12） | **已修复**：列表/导出只保留掩码并置空原文，详情接口仍返回原文；自测新增两条有效断言 |
| B2 | `applyReceiptPosted` 读-改-写，并发入账会丢更新 | **已修复**：改为 `UPDATE ... SET received_qty = received_qty + ? WHERE id = ? AND received_qty + ? <= order_qty` 原子累加，影响 0 行即报超收 |
| B4 | `/license/get` 对不存在 id 返回 `code=500 系统异常` | **已修复**：改走存在性校验，返回 `PURCHASE_LICENSE_NOT_EXISTS(1_031_002_000)`；自测已覆盖 |
| — | 部分收货被误判为「价格差异」 | **已修复**：价格差异只看单价，数量差异看本次实收与订单总量；自测已覆盖 |
| B3 | `FacadeFallbackConfiguration` 用普通 `@Configuration` + `@ConditionalOnMissingBean`，C 注册实现后可能双 Bean 冲突导致启动失败 | **待协调**（共享配置，见第九节） |
| B5 | 无单收货不校验供应商证照（`ph_po_receipt` 无 `supplier_id`） | 已在第九节显式声明为已知限制 |
| B6 | 收货单列表按收货人逐条查员工（N+1） | 已知限制，见第九节 |
| D4 | **更新采购订单/收货单直接 500** | 明细唯一键 `uk_po_line(order_id,line_no)` / `uk_receipt_line(receipt_id,line_no)` **不含 `deleted` 列**，而「明细整体重建」用的是 MyBatis-Plus **逻辑删除**：旧行仍留在表里，重新插入相同行号即触发唯一键冲突（实测报错 `Duplicate entry '12-1' for key 'ph_po_receipt_line.uk_receipt_line'`） | **已修复**：新增 `physicalDeleteByOrderId` / `physicalDeleteByReceiptId`（`@Delete` 原生 SQL）用于重建前清空旧明细；行号改为**服务端按顺序生成**（忽略前端传值），避免前端重复行号再触发同类冲突。修复后 21 项覆盖测试全绿 |
| D5 | 采购订单明细中**同一药品出现两行**时被误判「药品不存在」 | `DrugService#validateDrugList` 用 `drugs.size() != ids.size()` 判断缺失，传入含重复药品 id 的集合时（同药不同批次/价格是合法场景）会误抛 `PHARMACY_DRUG_NOT_EXISTS` | **已在调用方修复**：`PurchaseOrderServiceImpl#validateLines` 先对药品 id **去重**再校验。⚠️ **需同步提醒 A / D**：该门面方法对重复 id 敏感，D 的 POS 销售（同药多批次多行）与任何新调用方都应注意，建议 A 在 `validateDrugList` 内部先 `distinct()` 兜底 |

### 8.8 自测遗留数据

自测会在共享开发库写入带「【接口自测】」标记的数据：供应商（名称含「接口自测」）、其证照/采购订单/收货单，以及一份自测药品与分类。
由于订单已存在收货单、收货单已处于「已提交」，按业务规则无法通过接口删除，因此保留下来供浏览器验收参观。

**库内现状快照**（Docker 环境实测，D6 修复后、浏览器验收通过时）：

| 表 | 总数 | 其中自测残留 |
| --- | --- | --- |
| `ph_supplier` | 32 | 26（名称含「接口自测」） |
| `ph_supplier_license` | 24 | 15 |
| `ph_po_order` | 29 | 21（含 5 张已取消、8 张草稿） |
| `ph_po_receipt` | 42 | — |
| `ph_po_receipt`（`deleted=1`） | 7 | 逻辑删除行，**正是 D6 的成因**（占号但不被 `selectCount` 计入） |
| `ph_drug` / `ph_category` | 1 / 1 | 自测药品 `QADRG194352` + 分类「抗感染用药」 |

**保留的真实演示数据（6 家，不要删）**：`康泰医药商业集团有限公司`(1)、`华信医药供应链有限公司`(2)、`恒瑞康医药有限公司`(3)、`广济堂医药连锁有限公司`(4)、`仁和堂医药股份有限公司`(5, 待审)、`华东医药商业有限公司`(6)。
**注**：这 6 家已被采购订单引用，删除会被业务规则拦截（`1_031_001_006`），清理脚本也不含它们。

> **清理 SQL 已改为「按标记精确删除」**（早期版本用 `receipt_no LIKE 'GR%'` / `order_no LIKE 'PO%'`，会误删真实单据，**不要用旧版**）。以下脚本**仅删除自测数据**，请在本地开发库执行：

```sql
-- 仅清理 B 模块自测数据：按供应商名称里的「接口自测」标记精确定位
CREATE TEMPORARY TABLE tmp_qa_supplier AS
  SELECT id FROM ph_supplier WHERE supplier_name LIKE '%接口自测%';
CREATE TEMPORARY TABLE tmp_qa_order AS
  SELECT id FROM ph_po_order WHERE supplier_id IN (SELECT id FROM tmp_qa_supplier);

DELETE FROM ph_po_receipt_line WHERE receipt_id IN (SELECT id FROM ph_po_receipt WHERE order_id IN (SELECT id FROM tmp_qa_order));
DELETE FROM ph_po_receipt      WHERE order_id IN (SELECT id FROM tmp_qa_order);
DELETE FROM ph_po_order_line   WHERE order_id IN (SELECT id FROM tmp_qa_order);
DELETE FROM ph_po_order        WHERE id IN (SELECT id FROM tmp_qa_order);
DELETE FROM ph_supplier_license WHERE supplier_id IN (SELECT id FROM tmp_qa_supplier);
DELETE FROM ph_supplier        WHERE id IN (SELECT id FROM tmp_qa_supplier);

DELETE FROM ph_drug_barcode WHERE drug_id IN (SELECT id FROM ph_drug WHERE drug_code LIKE 'QADRG%');
DELETE FROM ph_drug         WHERE drug_code LIKE 'QADRG%';
DELETE FROM ph_category     WHERE cat_name LIKE '%接口自测%';

DROP TEMPORARY TABLE tmp_qa_order;
DROP TEMPORARY TABLE tmp_qa_supplier;
```

**清理后自检**（应依次为 6 / 0 / 8，`ph_po_receipt` 若仍有无单收货残留需单独处理）：

```sql
SELECT (SELECT COUNT(*) FROM ph_supplier WHERE supplier_name LIKE '%接口自测%') AS 自测供应商,
       (SELECT COUNT(*) FROM ph_po_order) AS 订单总数,
       (SELECT COUNT(*) FROM ph_po_receipt WHERE order_id IS NULL) AS 无单收货残留;
```

> 脚本用的是 `DELETE`（物理删除），与仓库既有迁移脚本的处理方式一致；上表「自测供应商 / 订单总数」应分别归零与回到真实单据数。

> **注意 D6 与清理的关系**：清理会删掉那 7 行逻辑删除的收货单，但它们占用的单号在**清理前**必须被正确跳过——
> 这正是 D6 的修复点。如果先回退了 D6 修复再清理，取号会重新踩到已删单号。

### 8.9 Docker 环境复测（用户在本机执行）

用户在自机 Docker 环境执行 `b-verify-purchase.ps1`（导入两个迁移 + 重建 backend/admin-ui + 清 Redis + 接口自测）：

```text
第 1 步 迁移：采购域菜单 33 条 ✓
第 3 步 后端就绪：租户 FirstSun 可查询 ✓
第 4 步 接口自测：通过 30 项，全部通过
```

随后用同一套脚本对**Docker 构建产物**（`http://127.0.0.1:48080`）再跑一遍补充验证，确认容器内产物与本地验证结果一致：

| 脚本 | 结果 |
| --- | --- |
| `tools/check-extra-endpoints.ps1`（下拉/到期提醒/状态刷新/条件分页/4 个导出） | **13/13 通过** |
| `tools/check-permission-enforcement.ps1`（无权限角色被拒 + 清理） | **11/11 通过** |

即：后端在本机真实编译、在 Docker 镜像内运行，两处行为一致。

### 8.10 浏览器目视验收（1440 宽真实截图，已通过）

用户在自机浏览器完成目视验收，截图归档在 `docs/purchase/screenshots/`（第 2 轮为验收通过的一组）：

| 截图 | 验收要点 | 结果 |
| --- | --- | --- |
| `第2轮-01-供应商.png` | 银行账号列显示 `********1234`（NFR-12 掩码，无原文） | 通过 |
| `第2轮-02-供应商证照.png` | 发证/到期日为 `2026-09-11` / `2026-09-10`；状态「已过期 1 天」/「731 天」 | 通过 |
| `第2轮-03-采购订单.png` | 下单/预计到货日为 `YYYY-MM-DD`（D2 修复）；金额为服务端重算值 | 通过 |
| `第2轮-04-采购收货.png` | 收货时间 `2026-09-11 23:01:17`（D1 修复，非 1970）；单号 `GR407-20260911-0044` 为当天连续流水（D6 修复） | 通过 |
| `第2轮-05-菜单展开.png` | `药店业务 → 采购管理` 下四个菜单齐全 | 通过 |

**D6 修复的线上实证**：第 1 轮截图（D6 修复前）收货单最大号为 `GR407-20260911-0022` 且新建必失败；
第 2 轮截图最大号为 `0044`，且我用脚本对容器后端实测连续创建拿到 `0041`、`0042`，确认「取号跳过逻辑删除占号」在线上生效。

> 两轮截图都保留：第 1 轮是**缺陷对照证据**（不要作为验收依据），第 2 轮才是验收依据。

## 九、已知限制

1. **收货入账依赖 C**：C 的库存服务未实现前，收货流程可走到「已提交」，`post` 返回 `INV_SERVICE_UNAVAILABLE(1029002001)` 且整单回滚，不会伪造库存数据（已自测验证）；
2. **共享配置风险（需协调 D/A）**：`config/FacadeFallbackConfiguration.java` 用普通 `@Configuration` + `@ConditionalOnMissingBean` 注册降级门面。这种写法依赖 Bean 定义注册顺序，**不能可靠让位**：C 用 `@Component/@Service` 实现 `InventoryFacade` 后可能同时存在两个 Bean，注入点会抛 `NoUniqueBeanDefinitionException` 导致后端启动失败。建议把降级实现迁到独立 `@AutoConfiguration`（配 `AutoConfiguration.imports`），或由 C 的实现加 `@Primary`。该文件属共享配置，B 未擅自修改；
3. **无单收货不校验供应商证照**：`ph_po_receipt` 没有 `supplier_id`，无单收货（`isFreeReceipt=1`，限店长）链路无法关联供应商，因此证照有效性校验只在有单收货入账时执行；
4. 仓库与货位当前以**编号**录入（`warehouseId` / `locationId`），待 C 提供库存主数据下拉接口后替换为选择器；
5. 采购订单的「采购建议自动生成（ADM-005）」只保留 `is_auto` 标记，未实现建议算法；
6. 已入账收货单不支持作废（需库存回补能力，属 C 的范围），当前仅待提交可作废；
7. 收货单列表回填收货人姓名按员工编号逐条查询（页内去重），分页较大时存在 N+1；待 A 提供 `getEmployeeList(Collection<Long>)` 后优化；
8. `ph_supplier.credit_code` 数据库层无唯一键，重复校验在 Service 层完成（并发写入存在极小概率穿透），如需补唯一索引需与 A 确认后另加迁移脚本；
9. 证照影像仅保存 URL，未接入 OCR 或有效期自动识别；到期提醒为手动触发的接口，未接入定时任务。


