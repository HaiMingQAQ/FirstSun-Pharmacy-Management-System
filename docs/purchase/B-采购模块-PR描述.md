# B 模块 Pull Request 描述（可直接粘贴到 GitHub PR）

> 按团队《开发规范与 AI 协作规则》要求：PR 必须说明**修改范围、真实接口验证结果、已知限制**。本文件为 B（供应商与采购收货）模块的 PR 正文，附图清单见第 6 节。

## 1. 基本信息

| 项 | 内容 |
| --- | --- |
| 分支建议 | `feat/b-purchase-supplier` → `main` |
| 成员 | B（供应商与采购收货） |
| 模块 | 采购域：供应商、供应商证照、采购订单、采购收货 |
| 负责表 | `ph_supplier`、`ph_supplier_license`、`ph_po_order`、`ph_po_order_line`、`ph_po_receipt`、`ph_po_receipt_line` |
| 表结构变更 | **无**（6 张表已在 `sql/firstsun_pharmacy_init.sql` 定义，本次只新增菜单与字典数据） |

## 2. 修改范围

### 2.1 后端新增（`backend/yudao-module-pharmacy`，采购域）

```text
controller/admin/purchase/                      4 个 Controller + vo/{supplier,license,order,receipt} 全部 VO
service/purchase/                               SupplierService(Impl) / SupplierLicenseService(Impl)
                                                PurchaseOrderService(Impl) / PurchaseReceiptService(Impl)
dal/dataobject/purchase/                        SupplierDO / SupplierLicenseDO
                                                PurchaseOrderDO / PurchaseOrderLineDO
                                                PurchaseReceiptDO / PurchaseReceiptLineDO
dal/mysql/purchase/                             6 个 Mapper
enums/                                          SupplierApproveStatusEnum / SupplierLicenseTypeEnum
                                                PurchaseOrderStatusEnum / PurchaseReceiptStatusEnum
api/inventory/dto/                              ReceiveItem / ReceiveResult（收货入库契约 DTO）
```

### 2.2 后端修改（3 个文件，均为追加式）

| 文件 | 变更 |
| --- | --- |
| `enums/ErrorCodeConstants.java` | 新增 `1-031-001~004` 段（供应商/证照/采购订单/采购收货错误码），**纯追加**，未改动 `1-029`(D)/`1-030`(A) 段 |
| `enums/DictTypeConstants.java` | 新增 8 个采购域字典常量，**纯追加** |
| `api/inventory/InventoryFacade.java` + `InventoryFacadeImpl.java` | 新增 `receive(storeId, receiptNo, items)` 收货入库方法；降级实现同步抛 `UnsupportedOperationException`。**属跨模块接口，需 C 确认签名与幂等键**（见第 5 节） |

### 2.3 前端新增（`admin-ui`）

```text
src/api/pharmacy/purchase/supplier/index.ts      供应商：类型 + 9 个接口
src/api/pharmacy/purchase/license/index.ts       证照：类型 + 9 个接口
src/api/pharmacy/purchase/order/index.ts         采购订单：类型 + 10 个接口
src/api/pharmacy/purchase/receipt/index.ts       采购收货：类型 + 9 个接口
src/views/pharmacy/purchase/supplier/{index.vue, SupplierForm.vue}
src/views/pharmacy/purchase/license/{index.vue, LicenseForm.vue}
src/views/pharmacy/purchase/order/{index.vue, OrderForm.vue}
src/views/pharmacy/purchase/receipt/{index.vue, ReceiptForm.vue}
```

- 列表页统一：`pharmacy-page pharmacy-modern-page` + `PharmacyPageHeader` + 两个 `ContentWrap.pharmacy-panel` + `Pagination`；高频查询默认展示、低频条件收进「更多筛选」；状态一律 `dict-tag`；权限 `v-hasPermi` + `checkPermi` 双控。
- 表单：供应商（12 字段）与采购订单/收货（含明细表格）用 `el-drawer size="min(1000px,92vw)"` + `el-divider` 连续分区；证照（5 字段）用 `Dialog`。
- 详情：`Dialog` + `el-descriptions` + 明细分表。

### 2.4 前端修改（1 个公共文件，需 A 确认）

| 文件 | 变更 |
| --- | --- |
| `src/utils/dict.ts` | **仅追加** 8 个采购域 `DICT_TYPE` 常量，未改动既有条目 |

### 2.5 数据库迁移（`sql/migrations`，幂等可重复执行）

| 文件 | 内容 |
| --- | --- |
| `20260912_b_purchase_supplier_menu.sql` | 采购管理目录 22100；供应商 22110-22116；供应商证照 22120-22125；字典类型 310-312 + 数据 1600-1621 |
| `20260913_b_purchase_order_receipt_menu.sql` | 采购订单 22130-22139；采购收货 22140-22148；字典类型 313-317 + 数据 1630-1672 |

两个脚本共 33 条菜单 + 8 个字典类型（30 条字典数据），并为 **tenant 1 / role 1（超管）** 与 **tenant 163 / role 167（FirstSun 共享账号 0407）** 绑定角色菜单，同时刷新租户套餐 114 的 `menu_ids`。

### 2.6 文档

`docs/purchase/B-采购域交付说明.md`：接口清单、字段与金额口径、字典、菜单权限、错误码、跨模块契约、9 节验证记录、已知限制。

## 3. 真实接口验证结果

| 验证项 | 方式 | 结果 |
| --- | --- | --- |
| 后端编译 | 本地 Maven `-pl yudao-module-pharmacy -am compile` 与 `-pl yudao-server -am package` | **BUILD SUCCESS** |
| 单元测试 | `mvn test`（JUnit5 + Mockito，4 个测试类） | **45/45 通过**：金额重算、药品 id 去重（D5 回归）、物理删除重建（D4 回归）、取号跳过逻辑删除占号 + 并发抢占后重算单号（D6 回归）、入账 CAS 防重复、货位校验顺序、库存不可用时不推进订单、供应商/证照业务规则 |
| 迁移脚本 | JDBC 直连真实 MySQL 执行两个脚本 | **16 + 19 条语句 0 失败**；菜单 33 条、字典 30 条、双角色各 33 条绑定 |
| 接口自测（本地实例） | 真实 HTTP + 真实 MySQL/Redis | **30/30 通过**：创建/唯一性/审核状态机/NFR-12 掩码/详情回填/金额服务端重算（50.00-5.00=45.00）/单号规则/提交审批重复审批拒绝/超收拦截/**入账失败整单回滚（状态回已提交、订单已收数量保持 0）** |
| 补齐 update/cancel/void/delete 覆盖 | `tools/check-crud-remaining.ps1` | **21/21 通过**：供应商/证照/订单/收货单更新、订单取消（含"已有收货记录不可取消"）、收货单作废与删除；期间发现并修复 D4（编辑明细唯一键冲突 500）与 D5（同药品多行误判药品不存在） |
| 参数错误与状态拦截 | `tools/check-negative-paths.ps1` | **14/14 通过**（错误码与预期一致） |
| 接口自测（Docker 环境） | 用户在自机执行同一脚本 | **30 项全部通过** |
| 补充接口覆盖 | 4 个 Excel 导出（返回真实文件流）、两个 simple-list、到期提醒、状态刷新、4 组条件分页 | **13/13 通过**（本地与 Docker 各一次） |
| 权限强制校验 | 用无采购权限的临时角色+用户真实调用，验证后清理 | **11/11 通过**：查询/创建/收货入账均 403、写入未落库、有权限账号正常 |
| 前端 | ESLint（0 错误）、Vue 官方 `@vue/compiler-sfc` 编译 8 个页面、结构体检（组件/图标/API/字典/分页/权限） | 全部通过 |
| 一致性 | 前端 API 路径 ↔ 后端路由（38 条）100% 对应；菜单 `component_name` ↔ 页面 `defineOptions.name` 4/4 一致；权限串三方一致（菜单 28 ↔ 后端 28 ↔ 前端 27） | 通过 |
| 并发行为 | `tools/check-concurrency.js`（Node 真并发） | **13/13 通过**：4 路并发入账（证照过期单全部被拦、证照有效单全部 `INV_SERVICE_UNAVAILABLE`）、3 路并发更新同一草稿单；无 500、无半成功、无重复入账、明细不重复 |
| 一键回归 | `tools/run-all-checks.ps1` | **14/14 项全通过**（接口 5 项 + 并发 + 导出深度校验 + 静态 7 项），逐项汇总，供 D/评审复跑 |
| **Docker 环境最终复跑** | 用户在自机 `docker compose up -d --build backend admin-ui` 后，我实测容器内构建并跑全量 | **102 项断言全通过 + 14/14 项检查全绿**；并额外实测新建收货单连续拿到 `GR407-20260911-0041`、`0042`（证明 D6 已修复并在线上生效） |
| 浏览器目视验收 | 1440 宽真实浏览器截图（见第 6 节附件 1-5） | **通过**：日期列 `2026-09-11`/`2026-09-11 23:01:17` 正常、银行账号 `********1234` 掩码、收货单号连续无 1970、左侧四个菜单齐全 |

## 4. 验收路径（评审同学可照做）

> 已实测通过的路径是**在已建好的 Docker 环境上直接复跑回归**（下方 A），不需要重新导入初始化脚本。

**A. 跑回归（推荐，约 1 分钟）**

```powershell
Set-Location 'D:\作业\药店\FirstSun-Pharmacy-Management-System'
docker compose up -d                      # 确保 mysql/redis/backend/admin-ui 都在
powershell -ExecutionPolicy Bypass -File D:\作业\药店\tools\run-all-checks.ps1
# → 共 14 项检查，失败 0 项（原件见 docs/purchase/最终回归-14项全通过.log）
```

**B. 浏览器验收**

<http://localhost/> → 登录 `FirstSun` / `0407` / `123456` → 左侧 **药店业务 → 采购管理** → 供应商 / 供应商证照 / 采购订单 / 采购收货。

推荐走一遍：建供应商 → 登记证照 → 首营审核通过 → 建采购订单（看金额被服务端重算）→ 提交 → 审批 → 标记发出 → 建收货单（部分收货）→ 提交 → 收货入账（**预期**提示库存服务未就绪，且状态回滚为已提交）。

**C. 从零重建环境（仅首次或需要干净库时）**

```powershell
Set-Location 'D:\作业\药店'
powershell -ExecutionPolicy Bypass -File .\b-verify-purchase.ps1   # 导入迁移 + 重建镜像 + 30 项自测
```

> ⚠️ C 会重建镜像并清 Redis；若只是复验收，用 A + B 即可。

## 5. 需要其他成员确认的事项

1. **C（库存）**：`InventoryFacade.receive(storeId, receiptNo, items)` 的签名、字段口径（`locationId`/仓库校验）与幂等键（`receiptNo + bizLineId`）需要 C 确认后实现；未实现前 B 侧返回 `INV_SERVICE_UNAVAILABLE(1029002001)` 并整单回滚，不伪造库存。
2. **A（公共平台）**：`src/utils/dict.ts` 仅追加 8 个常量，请确认；`InventoryFacade` 属跨模块接口。
3. **D/A（共享配置风险，非本次改动引入）**：`config/FacadeFallbackConfiguration.java` 用普通 `@Configuration` + `@ConditionalOnMissingBean` 注册降级门面，依赖 Bean 注册顺序、不能可靠让位；C 用 `@Component/@Service` 实现后可能出现双 Bean 导致**后端启动失败**。建议迁到独立 `@AutoConfiguration` 或由实现方加 `@Primary`。B 未擅自修改该共享文件。

## 6. 附件清单（评审需要）

截图统一归档在 `docs/purchase/screenshots/`。第 2 轮截图是**验收通过的那一组**（D6 修复已部署后所截）。

| # | 附件 | 状态 |
| --- | --- | --- |
| 0 | `docs/purchase/B-自测记录.md`（102 项接口断言 + 45 项单元测试的完整用例矩阵、缺陷发现与修复记录） | **已提供** |
| 1 | 供应商列表 → `screenshots/第2轮-01-供应商.png` | **已提供**（1440 宽） |
| 2 | 供应商证照列表 → `screenshots/第2轮-02-供应商证照.png` | **已提供**（1440 宽） |
| 3 | 采购订单列表 → `screenshots/第2轮-03-采购订单.png` | **已提供**（1440 宽） |
| 4 | 采购收货列表 → `screenshots/第2轮-04-采购收货.png` | **已提供**（1440 宽） |
| 5 | 菜单展开（药店业务 → 采购管理 四个菜单）→ `screenshots/第2轮-05-菜单展开.png` | **已提供** |
| 6 | 供应商抽屉、采购订单抽屉（含明细）、采购收货抽屉、详情弹窗 | **待补**（抽屉/弹窗内交互未截图） |
| 7 | 自测脚本输出 → `docs/purchase/最终回归-14项全通过.log`（14/14 项汇总原件） | **已提供** |
| 8 | `docs/purchase/B-采购域交付说明.md` | **已提供** |
| 9 | 第 1 轮截图（D6 修复前，保留作缺陷对照）→ `screenshots/第1轮-*.png` | 已归档（**不作为验收依据**） |

### 6.1 截图可核对的关键点（评审可直接对照）

| 截图 | 应看到 |
| --- | --- |
| 第2轮-01 供应商 | 银行账号列为 `********1234`（NFR-12 掩码，无原文）；日期类字段无 `2026,9,11` 数组形态 |
| 第2轮-02 供应商证照 | 发证/到期日为 `2026-09-11` / `2026-09-10`；状态标签「已过期 1 天」/「731 天」 |
| 第2轮-03 采购订单 | 下单/预计到货日为 `YYYY-MM-DD`；金额列为服务端重算值 |
| 第2轮-04 采购收货 | 收货时间为 `2026-09-11 23:01:17`（**非 1970**）；单号 `GR407-20260911-0044` 为当天连续流水 |
| 第2轮-05 菜单 | `药店业务 → 采购管理` 下四个菜单：供应商 / 供应商证照 / 采购订单 / 采购收货 |

## 7. 评审记录

### 7.1 自评（B 成员）

| 项 | 结论 |
| --- | --- |
| 需求覆盖 | 供应商（含首营审核）、证照（含到期提醒/状态刷新）、采购订单（含审批/发出/取消）、采购收货（含分批/差异/质检/作废）均实现 |
| 事务一致性 | 收货入账用状态 CAS 防重复 + 库存服务失败整单回滚（已实测：状态回已提交、订单已收数量保持 0、未写入入账时间） |
| 幂等性 | 入账幂等键 `receiptNo + bizLineId`；重复入账返回 `1_031_004_007` |
| 权限 | 28 个权限串三方一致；无权限账号真实调用被 403 且未落库 |
| 敏感数据 | 列表/导出脱敏（掩码 + 置空原文），详情按需返回原文；导出文件已解压深度校验 |
| 交付物 | 代码 + 2 个迁移脚本 + 3 份文档 + 10 张截图 + 回归脚本；`git status` 仅含 B 模块文件 |

### 7.2 自测与验收结论

- **10 个缺陷**在开发过程中被发现并修复（A-0、B1、B2、B4、D1、D2、D3、D3′、D4、D5、D6 中，D4/D5/D6 均由自测发现），其中 D4（编辑必崩）、D6（收货单无法创建）属阻断级；
- **最终验证**：45 项单元测试 + 102 项接口断言 + 并发 13 项 + 静态检查 7 项，**全部通过**；
- **浏览器目视验收**：1440 宽截图通过（本文件第 6 节）；
- **未闭环项**：抽屉/弹窗交互截图待补（附件 6）；收货入账真实成功路径依赖 C 的库存服务，当前仅验证「未就绪时明确报错 + 整单回滚」。

### 7.3 待其他成员确认（阻塞/风险项，详见第 5 节）

1. **C**：`InventoryFacade.receive` 签名与幂等键确认后实现；
2. **A**：`src/utils/dict.ts` 追加 8 个常量确认；`validateDrugList` 建议改为去重后比较（D5 根因）；
3. **A/D**：`FacadeFallbackConfiguration` 的 `@ConditionalOnMissingBean` 在普通 `@Configuration` 上不可靠，C 实现后可能双 Bean 启动失败。

## 8. 已知限制（摘要，详见交付说明第九节）

1. 收货入账依赖 C 的库存服务，未就绪时仅到「已提交」；
2. 无单收货（限店长）不校验供应商证照（`ph_po_receipt` 无 `supplier_id`）；
3. 仓库/货位当前以编号录入，待 C 提供库存主数据下拉后替换；
4. 采购建议自动生成（ADM-005）仅保留 `is_auto` 标记；
5. 已入账收货单不支持作废（需库存回补能力，属 C 范围）；
6. 收货单列表回填收货人姓名为页内去重逐个查询，待 A 提供批量员工接口后优化；
7. `ph_supplier.credit_code` 无数据库唯一键，重复校验在 Service 层（并发穿透概率极小）；
8. 证照影像仅存 URL，未接入 OCR；到期提醒为手动触发接口，未接定时任务。

