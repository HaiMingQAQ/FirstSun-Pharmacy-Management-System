# FirstSun 小程序候选审计与构建说明

审计基线：`9230486827719cc3e55410dbe0fb8b506cbc6353`，分支 `feat/miniapp-client`。
本次只收拢候选改动、修复风险和适配缺陷，未重新设计页面，未提交、推送或创建 PR。

## 可复现前端构建

在 `mall-uniapp` 目录执行，已验证 Node 22.22.3：

```powershell
npm ci --ignore-scripts
npm run test:pharmacy
npm run build:h5
npm run build:mp-weixin
npm run dev:h5
```

- `package-lock.json` 纳入版本管理；清除旧 HBuilderX 绝对路径记录。所有 DCloud 编译依赖固定同一版本，Vue 对齐编译器的 3.4.21，Pinia 固定为兼容的 2.0.33。
- 候选 Vue 3.5.13 与 uni-app 的 3.4.21 运行时混用曾导致部分响应式更新丢失；以实际分类按钮刷新与购物车操作验证兼容性。
- CLI 入口 `scripts/uni.cjs` 使用本工程作为输入目录及本地 npm 编译器。
- 不依赖 HBuilderX、外部 junction、个人绝对路径或 `.tmp/build-root`。
- H5 输出 `dist/build/h5`；微信输出 `dist/build/mp-weixin`。
- 微信构建只修改生成的 `app.json`，不会回写源 `manifest.json`。
- 仍有既有 Sass 弃用、商城海报循环分块、微信 img 标签选择器警告，不视为零警告构建。

## 风险修复与尚未完成的能力

1. 模拟支付配置 `firstsun.miniapp.mock-payment-enabled` 默认 false。即使显式启用，也仅允许所有 active profiles 均为 local/dev/test；prod、混合 prod、未知 profile、无 active profile 均拒绝。
2. **支付成功链尚未接通。** 现有 PaymentFacade 只有创建、退款、查询契约；库存门面要求同租户门店在职员工。已删除直接将订单标为已支付的实现。开发开关开启时仍返回明确错误，不写订单、不伪造支付记录、不冒充员工。后续必须通过已有支付成功处理链完成支付记录、库存流水、订单和审计，才可开放。
3. 确认收货保留：当前租户的本人会员、配送类型、已支付、状态 3 才能完成；自提仍走门店取货码核销。条件更新含归属/配送/支付/状态条件，重复完成不重复赠送积分，竞争失败重新核对状态。沿用既有 transferToSale 钩子和积分结算；注意 transferToSale 本身仍为既有 TODO，本轮没有宣称销售单闭环完成。
4. 处方引用检查会员、门店、有效状态和审核通过。会员入口要求令牌租户一致且不处于 tenant-ignore 模式。现有租户拦截器对处方表生效，但真实 HTTP/数据库租户隔离尚未完整验收。
5. **处方私有文件服务尚未接通。** 现有通用文件接口不能证明处方图片私有。本轮阻断真实模式上传和处方登记，不接受任意公开 URL；查询不返回历史公开影像 URL。已有公开文件并未迁移或撤销访问，仍需独立处置。元数据查询保留。Demo 本地演示独立保留。
6. 匿名库存采用专门的只读投影，显式限制租户、营业门店、启用且审核通过的线上药品、有效仓库/货位、质量合格且未过期批次；扣除冻结数量；输入最多 100 个药品编号。
7. 前端保留原设计，修正异步门店与分类不刷新、订单明细未返回、缺失地址显示 undefined、配送收货按钮条件及真实订单支付标识。

## 数据库与演示脚本

`sql/migrations/20260919_f_wx_miniapp_app_api.sql` 只是演示药品线上销售标记，不是结构迁移，不自动挂载。
现有数据库无需为了本次 app API 执行结构升级。
只有独立测试数据库中，维护者确认药品后显式设置 `@firstsun_demo_seed_enabled=1`、`@firstsun_demo_tenant_id`、`@firstsun_demo_drug_id`，才会更新；重复执行不再改变已开启记录。连接必须明确指定数据库，本文件不使用 `USE`，不硬编码个人测试库。
既有 compose 的 `MYSQL_DATABASE` 保留；不要为本次验证运行共享 compose。

## 验证命令与边界

在 backend 目录：

```powershell
mvn -pl yudao-module-pharmacy -am test '-Dtest=WxOrder*Test,AppMiniapp*Test,MemberControllerContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' -DfailIfNoTests=false
```

在仓库根目录，Docker 本地需已有 mysql:8.0：

```powershell
node scripts/miniapp-audit-inventory.cjs
```

该 SQL 验证创建独立命名临时容器和数据库，没有网络和发布端口，使用随机密码，退出时停止本次创建的容器；不操作共享开发容器。数据库可通过 FIRSTSUN_AUDIT_DB 显式配置，默认 firstsun_miniapp_audit。验证实际投影 SQL，但使用最小 fixture schema，不是完整迁移或 HTTP 验证。

证据分层：
- Java Mockito 服务/控制器测试：拒绝路径、条件状态更新、积分调用次数；不证明真实事务或并发数据库行为。
- Java 反射契约测试：仅路由/参数/注解；不证明 HTTP 可访问或授权。
- 前端测试：stub 网络适配器 + 本地 demo 冒烟；支付与上传以拒绝行为验收，不冒充已完成的成功闭环。
- 独立 MySQL：17 个库存 SQL fixture 检查。
- 浏览器：H5 构建配合拦截的 stub API，禁止向共享后端、短信和外部存储发送请求；七页面 × 375/390/430，加长药名/空/错误截图。不是实际后台联调。
- **真实 HTTP 登录→下单→支付→收货→处方闭环未验证。** 本轮未建立完整独立应用/Redis/业务数据库栈，未用共享开发环境代替。支付与私有处方成功链缺口仍在，因此当前不具备整体提交验收条件。

## 备份与主项目恢复

备份：`D:\github-2\FirstSun-audit-backups\2026-09-19T17-49-42-414Z`。
包含两工作区修改/未跟踪文件、被忽略根 .env 和已存在的 package-lock、HEAD 版本、工作树/索引补丁、SHA-256 manifest；依赖和生成缓存不纳入源文件备份。
主项目仅恢复经逐文件比对且保留于小程序的 6 个跟踪 Java 文件，移除经完整备份的 5 个复制新增 Java 文件；记录在 main-restoration.json。主项目 Git 工作区已干净。根 .env 缺少可信原始版本，保持现状，未猜测恢复、未输出秘密值。

下一步：先补齐受信任的支付成功链和处方私有存储/药师访问契约，再建立完整独立环境验证真实 HTTP、事务回滚、重复请求、越权、库存/积分/支付记录一致性；通过后再决定提交。

## 本轮结果（2026-09-20）

- 干净安装成功；最终 H5、微信目标构建成功，11 个药店源路由均存在。
- Java 共 56 项通过：41 项 Mockito 逻辑回归，15 项反射契约；0 失败、0 错误。
- 前端 stub/demo 冒烟通过；独立 MySQL 库存投影 17 项通过。
- 24 张截图覆盖七页面三种宽度和长名/空/错；另有分类选择、购物车加量 2 项浏览器交互断言通过。横向溢出、pageerror、console.error 均为 0。截图采用拦截测试数据和占位图，并非真实服务验收或完整像素基线对比。
- `npm run dev:h5 -- --host 127.0.0.1 --port 5186` 成功启动，药店路由 HTTP 200；验证后已停止该临时 dev 进程。
- 构建前后 manifest.json SHA-256 相同；两工作区根 .env 校验值与备份一致。
- 详细日志、截图及修改清单位于备份目录的 verification/ 与 candidate-after-audit.json。
