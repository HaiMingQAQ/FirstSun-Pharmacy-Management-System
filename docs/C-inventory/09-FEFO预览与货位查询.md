# FEFO 预览与货位查询

日期：2026-09-11。接续 08，仍为 P0 开发阶段。

后续仓库/货位编辑及启停增量见 [10-资料编辑与启停校验.md](10-资料编辑与启停校验.md)。

## 基线核对

本次重新 fetch 后 origin/main 仍为 `c6a9f63b`，无新的可接入变更。首次 Windows schannel 返回 `SEC_E_NO_CREDENTIALS`；使用单次 `git -c http.sslBackend=openssl fetch origin` 成功，没有关闭 TLS 验证或修改持久 Git 配置。仍在 feature/C-inventory，无提交、推送或工作区重置。

## 新增实现

- 仓库页的“货位”操作打开当前仓库货位抽屉，复用已有 location/page 接口；独立检查 `pharmacy:inventory-location:query` 权限，支持编码/状态筛选、分页、容量空值与 0 区分、错误提示。请求编号阻止较早响应覆盖后来选择的仓库或筛选结果。
- 创建表单和查询抽屉共用既有五种货位类型映射，没有新增公共字典或虚构待上架类型。
- 新增 InventoryPreviewReqVO、InventoryPreviewMapper/XML、InventoryPreviewService；在 InventoryReadController 增加 GET 入口。
- 增加前端 FEFO 预览 API 类型及调用函数，尚无对应 FEFO 表单。没有注册 D InventoryFacade 的实现，也没有修改 D 调用时序。

## GET /pharmacy/inventory/batch/fefo-preview

沿用管理端前缀 `/admin-api` 和 CommonResult。权限：`pharmacy:inventory-batch:query`。

参数全部必填：

| 参数 | 约束 |
| --- | --- |
| warehouseId | 正数，本人员工门店内启用仓库 |
| drugId | 正数，通过 A DrugApi.validateDrugList 校验 |
| quantity | 1..2147483647 的整数 |
| expiryDayPolicy | ALLOW_ON_EXPIRY_DATE 或 BLOCK_ON_EXPIRY_DATE |

示例查询串：`warehouseId=123&drugId=456&quantity=5&expiryDayPolicy=BLOCK_ON_EXPIRY_DATE`。

成功 data 包含 evaluatedAt（实际计算时间）、expiryDayPolicy（本次明确选择的边界）、reserved=false，以及 allocations 数组，每项为 batchId/locationId/quantity。不返回成本、不保存预占、不保证后续销售仍有同样分配。

**效期边界仅为模拟参数**：团队尚未确认效期当天销售口径，因此这里不设置隐含默认值，也不将客户端选择用作真实销售授权。真实扣库服务未来必须使用经确认的服务端策略，并完成业务单据、药师/处方/特管限制、操作幂等及加锁后的重新校验；不能提交预览结果直接扣库存。

## 后端校验与一致性

1. 先校验真实租户、登录、在职员工及单门店范围；仓库缺失/删除/越界统一 NOT_FOUND，停用仓库拒绝。
2. 使用 A DrugApi.validateDrugList 验证药品存在、启用、审核通过；未复制 A 校验实现。
3. 在 REPEATABLE_READ 只读事务内读取单仓单药品的批次及货位快照。不锁行，不占库存。
4. 查询使用 LEFT JOIN，保留没有货位的批次及同租户内错误的货位归属，让校验发现异常。未通过 JOIN 或可售筛选提前隐藏这些异常。
5. 每批校验 total=avail+frozen、数量非负，以及 total/frozen 分别等于全部未删除货位明细汇总。qtySold 不计入在库守恒。货位停用仍计入对账，不能分配。
6. 货位所属仓库/租户、药品与批次矛盾，或缺失/已删除货位，直接拒绝。查询按当前租户读取，不跨租户展示资料。
7. 仅正常质量批次、启用货位、符合本次日期边界的数据可参与分配；普通可分配量始终为 qty-qtyFrozen。
8. 捕获一次时间，按 Asia/Shanghai 日期计算；排序为效期、批次 ID、货位 ID。不足时整个请求失败，不返回部分成功分配。
9. 单仓单药品最多读取 10001 行作为保护：超过 10000 行即拒绝，不能截断后声称库存充足或账目一致。

这是预览前的批次/货位数量校验，不等于 P1 完整三账核对；尚未核对有效冻结锁、流水累计或期初导入余额。

## 可重复验证

本地非数据库测试：

```powershell
# backend
mvn -o -pl yudao-module-pharmacy -am test '-Dtest=InventoryRulesTest,InventoryReadAccessTest,InventoryReadMapperTest,InventoryCatalogTest,InventoryPreviewTest,SaleAmountCalculatorTest' '-Dsurefire.failIfNoSpecifiedTests=false'

# admin-ui
node node_modules/prettier/bin/prettier.cjs --check src/views/pharmacy/inventory src/api/pharmacy/inventory
node node_modules/eslint/bin/eslint.js src/views/pharmacy/inventory src/api/pharmacy/inventory
node --max_old_space_size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit
node --max_old_space_size=8192 node_modules/vite/bin/vite.js build
```

InventoryPreviewTest 使用 Mock 数据快照、真实规则计算、Jakarta Validation 与 MyBatis XML 解析。覆盖 FEFO 顺序、冻结量、账不平、停用货位参与汇总但不能分配、过期/质量停售、越范围、药品拒绝、归属错误、行数上限、明确日期策略以及有批次库存但无货位明细的情况。它不证明 MySQL 快照隔离、驱动映射、权限拦截或真实药品服务查询。

本次实际结果：

- Maven 成功。C 测试 33 项（Catalog 8、Preview 11、ReadAccess 11、ReadMapper 2、Rules 1），D SaleAmountCalculator 10 项，共 43，失败/错误/跳过均为 0。Rules 内仍包含 50 组检查与 500 次固定种子案例。
- 库存目录 Prettier、ESLint 通过；全量 Vite 构建通过，32.41 秒。
- 全仓 TypeScript 检查未通过，仍为其他模块 11 条错误，C 库存目录无报错；不据构建成功宣称类型检查通过。
- 本地日志：`backend/yudao-module-pharmacy/target/inventory-preview-tests.log`、`inventory-typescript-check.log`、`inventory-vite-build.log`；均位于 Git 忽略目录。
- 含未跟踪文件的空白检查通过。tracked diff 为空；C 文件仍未提交，原始文档保留。未执行数据库操作或迁移、未启动应用服务、未采集真实页面截图。

独立环境验收仍待执行：

1. 沿用 08 的独立库归属和权限集成前提。无需本阶段迁移；不重复初始化，不在小组共享库造测试数据。
2. 同一药品建效期不同的两批，最早批总量 5/冻结 3，较晚批总量 10/冻结 0。所有货位与批次账保持一致，请求 4 应分配最早批 2、较晚批 2；验证所有库存表行数、数量及流水均未变化。
3. 验证过期、质量停售、全部冻结、停用货位/仓库、停用药品不被分配。效期当天分别选择两种策略，响应必须明确回显策略；不得由此宣称团队销售政策已确定。
4. 在独立测试数据中制造批次/货位数量不平、错误归属及正库存但缺明细，确认请求失败，不返回可用的部分结果。
5. 两连接并发修改测试库存，验证一次预览来自一致快照；随后真实扣库必须再次校验，不能消费旧预览。
6. 他店仓库、未关联员工、跨租户、缺 query 权限分别拒绝。货位抽屉快速切换/查询后确认旧响应不能覆盖新结果，检查空数据、错误、分页及容量 0 显示。
7. 在真实菜单/登录环境采集 1440/1024 截图，交 A 评审；本轮未伪造截图或模拟业务成功。

## 尚存依赖

仓库/货位编辑启停与默认仓、统一收货/扣库/原销售退货事务仍未完成，P1 尚未进入验收。操作级幂等和原出库引用迁移、B/D 来源及事务契约、独立 MySQL、A 菜单权限与页面评审仍待确认。原先“main 缺 pharmacy”的阻塞已解除，不再沿用。
