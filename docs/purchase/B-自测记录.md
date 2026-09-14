# B 成员自测记录 —— 供应商与采购收货

> 对应《六人全栈开发方案》第 6 节「每人的最低提交证据」第 4 项：**正常、参数错误、权限不足和重复操作等适用场景的自测记录**。
> 所有用例均在**真实 HTTP + 真实 MySQL/Redis** 上执行，非 Mock、非静态检查。

## 一、测试环境与方法

| 项 | 内容 |
| --- | --- |
| 被测对象 | `yudao-module-pharmacy` 采购域（供应商 / 供应商证照 / 采购订单 / 采购收货） |
| 环境 A（本地构建产物） | 本地 `mvn package` 出的 jar，端口 48081，连 Docker 里的同一个 MySQL(3307) / Redis(6379) |
| 环境 B（Docker 构建产物） | `docker compose` 构建的 backend 容器，端口 48080 |
| 账号 | 租户 `FirstSun`（tenant 163）/ 用户 `0407`（角色 167，含采购域全部 28 个权限） |
| 调用方式 | Windows PowerShell 脚本，全部走 HTTP 接口；JSON body 显式 UTF-8 编码 |
| 覆盖接口 | 采购域 38 条路由 **全部有真实调用记录** |

## 二、自测脚本清单

| 脚本（仓库外层 `D:\作业\药店\tools` 与本目录） | 覆盖 | 断言数 |
| --- | --- | --- |
| `b-verify-purchase.ps1` | 迁移导入 + 就绪检查 + 正常流程主链路（供应商→证照→订单→收货→入账回滚） | 30 |
| `tools/check-crud-remaining.ps1` | 补齐 `update / cancel / void / delete` | 21 |
| `tools/check-negative-paths.ps1` | 参数错误、状态拦截、业务规则拦截 | 14 |
| `tools/check-extra-endpoints.ps1` | 下拉、到期提醒、状态刷新、条件分页、4 个 Excel 导出 | 13 |
| `tools/check-permission-enforcement.ps1` | 权限不足（无采购权限角色）与写入未落库 | 11 |
| **合计** | | **89** |

复跑命令（任选环境，`-BaseUrl` 指向 48080 或 48081）：

```powershell
cd D:\作业\药店
powershell -ExecutionPolicy Bypass -File .\b-verify-purchase.ps1 -OnlySmokeTest -BaseUrl http://127.0.0.1:48080/admin-api
powershell -ExecutionPolicy Bypass -File .\tools\check-crud-remaining.ps1 -BaseUrl http://127.0.0.1:48080/admin-api
powershell -ExecutionPolicy Bypass -File .\tools\check-negative-paths.ps1 -BaseUrl http://127.0.0.1:48080/admin-api
powershell -ExecutionPolicy Bypass -File .\tools\check-extra-endpoints.ps1 -BaseUrl http://127.0.0.1:48080/admin-api
powershell -ExecutionPolicy Bypass -File .\tools\check-permission-enforcement.ps1 -BaseUrl http://127.0.0.1:48080/admin-api
node D:\作业\药店\tools\check-concurrency.js http://127.0.0.1:48080/admin-api
```

> 也可以用一键回归脚本（接口 + 并发 + 静态检查逐项汇总）：
> `powershell -ExecutionPolicy Bypass -File .\tools\run-all-checks.ps1 -BaseUrl http://127.0.0.1:48080/admin-api`

## 三、单元测试层（`mvn test`，45 个用例全部通过）

接口自测依赖真实数据，难以覆盖「异常分支 + 并发/CAS + 依赖不可用」；因此补充 JUnit5 + Mockito 单元测试（与 D 的存量测试同风格，放在 `backend/yudao-module-pharmacy/src/test/java/.../service/purchase/`）：

| 测试类 | 用例数 | 覆盖点 |
| --- | --- | --- |
| `PurchaseOrderServiceImplTest` | 14 | 金额服务端重算（140.00/10.00/130.00 与行金额 120.00/10.00）、**药品 id 去重（D5 回归：同药两行不得误判"药品不存在"）**、行号服务端生成与已收数量初始化、明细为空/数量非法/折扣率非法/非草稿不可改/状态非法不可取消/已有收货单不可取消、**`applyReceiptPosted` 走原子更新（`update(null, wrapper)`）并在影响 0 行时报超收**、全部收货→完成(5)、部分收货→部分到货(4)、跨订单行拒绝 |
| `PurchaseReceiptServiceImplTest` | 13 | 提交/入账/作废/删除的状态机拦截、**入账 CAS 影响 0 行 → 重复入账错误**、**入账前货位校验（在 CAS 之前，不写状态不调库存）**、**库存服务抛 `UnsupportedOperationException` → 明确业务错误且绝不推进订单已收数量**、库存成功后回写批次号并调用订单累计、**D6 回归：取号必须跳过逻辑删除单据占用的流水号（最大号 0023 → 新号 0024）**、**D6 回归：并发抢占导致唯一键冲突时必须重算单号重试（0024 被抢 → 0025 成功）** |
| `SupplierServiceImplTest` | 10 | 编码/统一社会信用代码唯一性、折扣率 0~1、**新建一律待审且审核字段不可由保存接口写入**、被订单引用时禁止删除、删除时级联清理证照、停用/未审核不可采购、审核状态非法与重复审核拦截、**审核人必须是绑定员工（静态 mock 登录用户）**、审核成功记录审核人/时间/意见 |
| `SupplierLicenseServiceImplTest` | 8 | 到期日早于发证日拦截、同一供应商证照号唯一、**状态按到期日自动计算（前端不回传）**、**入账前经营类证照校验：缺经营许可证/GSP 证 → 拦截、已过期 → 拦截、有效 GSP 证 → 放行**、到期提醒默认 30 天窗口 |

运行方式（模块依赖较多，只跑 B 的四个测试类即可；沙箱/CI 下 Mockito 需要允许自附加 agent）：

```powershell
cd D:\作业\药店\FirstSun-Pharmacy-Management-System\backend
mvn -s D:\作业\药店\tools\settings.xml -pl yudao-module-pharmacy -am -B test `
  "-Dtest=SupplierServiceImplTest,SupplierLicenseServiceImplTest,PurchaseOrderServiceImplTest,PurchaseReceiptServiceImplTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" "-DargLine=-Djdk.attach.allowAttachSelf=true"
```

实测输出（`-Dtest=...*Test` + `clean` 强制重编译，避免增量编译留下旧报告）：

```text
PurchaseOrderServiceImplTest       Tests run: 14, Failures: 0, Errors: 0
PurchaseReceiptServiceImplTest     Tests run: 13, Failures: 0, Errors: 0
SupplierServiceImplTest            Tests run: 10, Failures: 0, Errors: 0
SupplierLicenseServiceImplTest     Tests run:  8, Failures: 0, Errors: 0
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> 踩坑提醒：加了新用例后如果用增量 `mvn test`，可能出现"BUILD SUCCESS 但 surefire 报告还是旧的"（读出来仍是失败）。
> 排查 D6 回归用例时就被这个坑绕了几轮；判断用例是否真的跑了，要看报告文件时间戳或直接看控制台 `Tests run` 行，
> 必要时加 `clean`。

## 四、正常流程用例（30 项，全部通过）

| 用例 | 预期 | 实测 |
| --- | --- | --- |
| 共享账号登录 | 成功 | code=0，拿到 token |
| 创建供应商（含银行账号） | 成功并返回 id | 通过 |
| 列表接口的银行账号 | 只返回掩码、不返回原文（NFR-12） | `bankAccount=null`、`bankAccountMasked=************1234` |
| 详情接口的银行账号 | 返回原文供编辑回填 | 原文一致 |
| 首营审核通过 | 成功并记录审核员工 | 通过（审核人 = 0407 绑定的员工 407） |
| 登记供应商证照 | 成功、状态按到期日计算 | 状态=有效(1)、`daysToExpire=731` |
| 证照列表回填 | 供应商名称 + 距到期天数 | 通过 |
| 创建采购订单（10×5.00，折扣 0.90） | 金额由服务端重算 | 含税 50.00 / 优惠 5.00 / 应付 45.00 |
| 订单号规则 | `PO{门店}-yyyyMMdd-流水` | `PO407-20260911-000x` |
| 提交 → 审批 → 标记发出 | 状态依次 1 → 2 → 3 | 通过（审批记录员工 407） |
| 创建收货单（部分收货 4/10） | 成功，差异=数量差异(1) | `diffType=1` |
| 收货时间与单号 | 毫秒时间戳落库、单号日期为当天 | `receiveDate=1789130516000`、`GR407-20260911-0009` |
| 提交收货单 | 待提交 → 已提交 | 通过 |
| 收货入账（C 库存服务未实现） | 明确报错 + 整单回滚 | `INV_SERVICE_UNAVAILABLE(1029002001)`，收货单仍为已提交、`postedAt` 为空、订单已收数量保持 0 |
| 删除无单引用的供应商 | 成功，证照级联删除 | 通过（剩余证照 0 条） |

## 五、并发行为验证（`tools/check-concurrency.js`，13 项全部通过）

重复提交与并发是"收货入账不重复、不出半成功"的关键，单靠串行接口测试覆盖不到，因此用 Node 原生 http 做真并发（不依赖子进程）：

| 场景 | 断言 | 实测 |
| --- | --- | --- |
| 4 路并发入账（**证照过期**供应商的已提交收货单） | 无 500/网络异常、**没有任何一次"成功"**、状态保持一致、未写入入账时间、明细行数不变 | 4 次全部返回 `1_031_002_004`（证照已过期），状态仍为已提交、`postedAt` 为空、明细 1 行 |
| 4 路并发入账（**证照有效**供应商的新收货单，走 CAS + 库存调用） | 无 500、**每次都是明确业务错误**、并发后仍为已提交且未写入账时间、明细无重复 | 4 次全部返回 `1_029_002_001`（库存服务未就绪），状态=1、`postedAt` 空、明细 1 行 |
| 3 路并发更新同一张待提交收货单 | 无 500、明细不重复 | 3 次全部 `code=0`，明细仍为 1 行（`uk_receipt_line` 未被破坏） |

> 结论：并发下**没有重复入账、没有半成功、没有唯一键炸裂**，与 D4 修复（物理删除重建）和入账 CAS 的设计一致。

## 六、参数错误 / 状态拦截 / 业务规则（14 项，全部通过）
| 用例 | 期望错误码 | 实测 |
| --- | --- | --- |
| 供应商未通过首营审核 → 建采购订单 | `1_031_001_005` | 一致 |
| 采购明细为空 | 参数校验拒绝 | 非 0 |
| 折扣率 > 1 | 参数校验拒绝 | 非 0 |
| 草稿订单 → 收货 | `1_031_004_008` | 一致 |
| 批次有效期早于今天 | `1_031_004_005` | 一致 |
| 无单收货由非店长操作（岗位=系统管理员） | `1_031_004_009` | 一致 |
| 缺货位 → 入账 | `1_031_004_013` | 一致（状态仍为已提交） |
| 供应商只有过期证照 → 入账 | `1_031_002_004` | 一致（状态仍为已提交） |
| 重复供应商编码 | `1_031_001_001` | 一致 |

> N7「缺货位 → 入账」用例踩坑记录：该用例原先是随手取「第一个可收货订单」，而订单可能属于
> N8 故意造的「过期证照供应商」，于是入账先被**证照过期**拦下（`1_031_002_004`），
> 拿到的是正确但非本用例预期的错误码。已改为**显式挑选「证照有效且未收满」的订单**再构造用例。
| 重复首营审核 | `1_031_001_007` | 一致 |
| 重复审批采购订单 | `1_031_003_010` | 一致 |
| 超收（收 100 > 订购 10） | `1_031_004_004` | 一致 |
| 证照到期日早于发证日 | `1_031_002_003` | 一致 |
| 查询不存在的证照 | `1_031_002_000` | 一致（修复前返回 500，见缺陷 D3′） |

## 七、权限不足（11 项，全部通过）

用租户 1 超管临时创建「不含任何采购菜单」的角色 + 用户，验证后自动清理：

| 用例 | 期望 | 实测 |
| --- | --- | --- |
| 有权限账号查询供应商 | 成功 | code=0 |
| 受限账号查询供应商 | 403 | 403 |
| 受限账号创建供应商 | 403 | 403 |
| 受限账号执行收货入账 | 403 | 403 |
| 受限账号写入是否落库 | 未落库 | 超管复查 count=0 |
| 前端权限数据源（`/system/auth/get-permission-info`） | 下发 28 个采购权限 + 4 个采购菜单 | 一致（套餐 114 含 33 个采购菜单） |

## 八、补充接口与导出（13 项，全部通过）

下拉两个（`simple-list` / `all-simple-list`）、证照到期提醒、证照状态刷新、4 组条件分页（含下单日期区间）、4 个 Excel 导出（解 zip 校验：结构完整的 xlsx、内容为真实数据、**供应商导出含掩码不含原文**）。

## 九、自测发现的缺陷与修复（含发现方式）

| 编号 | 缺陷 | 发现方式 | 状态 |
| --- | --- | --- | --- |
| A-0 | `LambdaQueryWrapperX` 断链导致编译失败（`le()` 后调 `eqIfPresent()`） | 真实 Maven 编译 | 已修复、编译通过 |
| D1 | 收货时间落库 1970（`LocalDateTime` 传字符串被静默解析为 0） | 用户浏览器截图（单号 `GR407-19700101-*`） | 已修复（前端 `value-format="x"` + 脚本传时间戳 + 2 条回归断言） |
| D2 | 日期列显示 `2026,9,11`、编辑表单日期为空（`LocalDate` 响应是数组） | 用户浏览器截图 | 已修复（新增日期工具 + 7 处逐项整改 + `check-date-columns.js` 防回归） |
| D3 | 自测数据中文变 `?`（脚本按 Latin-1 发 JSON） | 用户浏览器截图 | 已修复（脚本改 UTF-8 字节 + 存量数据改写为真实名称） |
| D3′ | `/license/get` 传不存在 id 返回 500 | 补断言时发现 | 已修复（改走存在性校验，返回 `1_031_002_000`） |
| B1 | 分页接口泄露银行账号原文 | 外部代码评审 | 已修复 + 自测新增 2 条有效断言 |
| B2 | 收货入账累计已收数量是读-改-写，并发会丢更新 | 外部代码评审 | 已修复（原子 SQL 累加 + 上限校验） |
| B4 | 收货差异把部分收货误判为「价格差异」 | 主自测断言 | 已修复（价格差异只看单价） |
| **D4** | **编辑采购订单/收货单直接 500**（明细唯一键不含 `deleted`，逻辑删除后重插同行号冲突） | **补齐 update 覆盖时发现** | 已修复（重建前物理删除 + 行号服务端生成） |
| **D5** | **同一药品两行时误报「药品不存在」** | **补齐 update 覆盖时发现** | 已在调用方去重修复；已提醒 A 在门面内兜底（D 的 POS 同样风险） |
| **D6** | **收货单号生成器算出「已被逻辑删除单据占用」的号 → 5 次重试全撞 → 收货单完全无法创建**（`selectCount` 自动过滤 `deleted=1`，而 `uk_receipt_no` 不含 `deleted` 列，两者口径不一致） | **Docker 重建后跑全量自测发现**：`receipt/create` 一律返回 `1_031_004_001 收货单号已存在` | 已修复（改用「前缀内 MAX(单号) + 1」，含逻辑删除行；订单号 `generateOrderNo` 同样问题一并修复） |

> D6 详细复盘：库里 `GR407-20260911-` 前缀实际 23 行（其中 5 行 `deleted=1`），但生成器 `selectCount` 只数到 18，
> 于是派号 `0019`→`0023`，正好全被 5 行已删单据占着；5 次重试全部 `DuplicateKeyException`，最终抛「收货单号已存在」。
> 已用临时日志取证（`prefix=GR407-20260911- count=18 attempt=0 -> GR407-20260911-0019`），
> 并验证修复后连续创建得到 `0024/0025/0026`。该缺陷只在「当天有作废/删除单据」时触发，
> 所以前几轮自测没暴露——**这也是"重建容器后必须跑全量自测"的价值证明**。

> 教训记录：第一轮自测只覆盖「新增/查询/审批」链路，遗漏 `update/cancel/void/delete`，导致 D4 这种"编辑必崩"的缺陷晚了一轮才暴露。现已把这些用例固化进 `check-crud-remaining.ps1` 并纳入例行回归。
> 第二条教训（D6）：单号类字段的"计数"与"唯一键"口径必须一致——唯一键不含逻辑删除列时，计数也必须包含逻辑删除行，否则单据会在"当天出现过作废/删除"后集体创建失败。

## 十、未覆盖与限制

1. **收货入账的真实成功路径**：依赖 C 的库存服务（`InventoryFacade.receive`），当前只验证了「未就绪时明确报错 + 事务回滚」，入账成功后批次/流水/订单状态推进需 C 实现后补测；
2. **浏览器目视验收**：页面渲染与交互需在 Docker 前端容器构建后由人工验收（截图作为 PR 附件）；
3. 无单收货不校验供应商证照（`ph_po_receipt` 无 `supplier_id`），已在交付说明第九节声明；
4. 并发场景只做了代码级修复（原子累加/CAS），未做压力测试。


