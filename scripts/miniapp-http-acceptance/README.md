## 2026-09-21 HTTP 可达性定向修复

旧验收在第一次发送验证码前发生传输失败；旧记录归档于 `runtime-history/before-http-network-fix/`。日志证明 backend 已启动，但旧 TCP healthy 与 HostConfig.PortBindings 不能证明宿主 HTTP 可用。用户报告 NetworkSettings.Ports 为空；本轮检查时旧容器、镜像、卷、网络均已清理，未直接复现当时的 NAT 状态。不能据此断言 internal 网络是底层原因。

本轮保持 internal 网络和原有 loopback 端口配置，增加同一隔离网络的专用 HTTP client，使验收不依赖宿主 26180 转发。保留所有登录、支付、MySQL、越权及并发断言；本轮仅执行真实 HTTP 就绪检查，不执行完整验收。旧 runtime 全量归档，新的 prepare 使用新随机标记及空隔离数据库；没有删除旧证据或改支付 body/通知路由。

连通性结果：四个独立容器均 healthy，`client -> http://backend:48080/actuator/health` 实际返回 HTTP 200、status=UP，已保存 `runtime/http-readiness.json`。确认后停止，未调用登录、下单或支付，未重跑 73 项测试；独立栈保留运行供下一位测试模型使用。

当前已准备环境的后续执行顺序（不要再次 prepare/up，不要修改脚本或断言）：

```powershell
Set-Location -LiteralPath 'D:\github-2\FirstSun-miniapp'
git branch --show-current
node scripts/miniapp-http-acceptance/environment.cjs ready
node scripts/miniapp-http-acceptance/accept.cjs
```

每步非零立即停止，按下文 diagnose 获取脱敏证据；不修复业务或绕过已知支付缺口。完整验收结果与 HTTP 就绪结果分开报告。仅在测试完成并保存证据后执行下文 down。

# 独立 HTTP / MySQL 模拟支付验收

只用于 `D:\github-2\FirstSun-miniapp`、`feat/miniapp-client`。本目录不改变业务实现。现有 73 项定向单测不在本流程重跑。

初次交付只做静态检查；本次 HTTP 定向修复已重新启动独立栈，并验证专用 client 的 HTTP 200/UP。完整业务验收未执行，不能用就绪结果代替登录、支付及 MySQL 业务断言。

## 文件及复用范围

- `compose.json`：JSON 是 YAML 的子集，可直接由 Compose `-f` 读取；仅 backend、MySQL、Redis 和专用 HTTP client。
- `mysql.Dockerfile` / `redis.Dockerfile`：复用既有 `mysql:8.0` / `redis:7-alpine` 基础镜像，输出独立镜像标签。backend 直接复用仓库 `backend/Dockerfile`，在容器构建阶段编译当前源代码、跳过测试；不使用宿主机旧 JAR 或 Maven 缓存目录。
- `env.example`：独立变量样例；`environment.cjs prepare` 在本目录 `runtime/` 生成随机数据库密码、Redis 密码、开发验证码、环境标记和 OAuth 客户端测试 secret。不会读、复制或修改根 `.env`；不会输出随机值。
- `seed.sql`：空库一次性合成数据。两个租户、三个会员（用于归属和租户拒绝测试）、一个会员等级、一家门店、仓库/货位、一个 OTC 药品、50 盒期初库存及期初流水、一个 Mock 支付应用/渠道、一个 OAuth 客户端。没有员工身份，没有真实密码或商户密钥。
- `environment.cjs`：准备、隔离校验、启动、脱敏诊断、精确清理。无第三方 npm 依赖。
- `http-client.Dockerfile` / `http-client.cjs`：独立 Node 22 Alpine 客户端，无宿主端口、无 Docker socket、无数据库密码；请求/令牌通过 stdin/stdout 管道传递，不进入命令行。只允许访问固定 backend 的会员 API；就绪探测只访问 Actuator。
- `accept.cjs`：宿主编排器通过异步 docker exec 在 client 内发出真实 HTTP 请求，并保留宿主侧只读 MySQL 核对；不直接写订单/支付状态或库存、不伪造 token、不替代业务处理。

准备阶段仅从 `backend/sql/mysql/ruoyi-vue-pro.sql` 提取 system/infra 的 **CREATE TABLE**，不导入原来的账号、令牌、系统参数、短信或支付数据。药店表原样复用 `sql/firstsun_pharmacy_init.sql`，再按顺序应用：

1. `20260915_c_inventory_facade_flow_ref.sql`
2. `20260916_f_wx_order_line_alloc.sql`
3. `20260916_f_wx_order_alloc_frozen_stage.sql`
4. `20260916_f_wx_order_alloc_out_ref.sql`
5. `20260918_f_member_sms_code.sql`
6. `20260918_f_wx_order_points.sql`

不加载菜单、共享账号、演示数据、处方扩展或 `20260919_f_wx_miniapp_app_api.sql`；合成 OTC 的线上标记由本目录种子明确设置。原有表约束保持不变。初始数据不是业务迁移，不可导入已有库。

## 资源与配置

| 资源 | 独立名称 / 本机入口 |
| --- | --- |
| Compose project | `firstsun-miniapp-http-acceptance` |
| backend | `firstsun-miniapp-http-backend` / `127.0.0.1:26180` |
| MySQL | `firstsun-miniapp-http-mysql` / `127.0.0.1:23316` |
| Redis | `firstsun-miniapp-http-redis` / `127.0.0.1:26386` |
| 镜像 | 上述四个容器名各加 `:acceptance` |
| 网络 | `firstsun-miniapp-http-network`，internal，运行时不出网 |
| 卷 | `firstsun-miniapp-http-mysql-data`、`firstsun-miniapp-http-redis-data`、`firstsun-miniapp-http-backend-logs` |
| 数据库 | `firstsun_miniapp_http_acceptance`，来自独立配置 |

backend 显式使用 local profile、禁用框架 mock 身份认证、启用真实 OAuth token 和租户隔离。`firstsun.miniapp.mock-payment-enabled=true` 仅在此独立配置开启，`firstsun.miniapp.pay-app-key=firstsun-acceptance`；短信为开发验证码入口，不调用真实短信。MySQL 主/从连接、Redis 和支付回调地址全部指向本网络。支付渠道只有 `mock`。固定积分规则：100 分抵 1 元，单笔最多 50%。

每个容器、镜像、网络、卷均有 `io.firstsun.acceptance` 随机归属标记。启动要求同名容器/网络/卷不存在；已有同名镜像必须属于本轮。端口冲突、资源归属不符、非本地 Docker context 或准备文件校验值变化均停止。禁止修改配置换端口或强行删除冲突资源。

## 准确执行命令

前置：已有 Node.js 20+、Git、运行中的本机 Docker Desktop Linux engine 与 Compose v2（支持 `up --wait`）；镜像构建需要访问基础镜像仓库和 Maven 仓库。不得由执行模型安装工具、启动 Docker Desktop 或改 Docker 配置。前置条件不满足即停止。

在 PowerShell 中逐条执行；每条检查 `$LASTEXITCODE`，非 0 停止：

```powershell
Set-Location -LiteralPath 'D:\github-2\FirstSun-miniapp'
git branch --show-current
node --check scripts/miniapp-http-acceptance/environment.cjs
node --check scripts/miniapp-http-acceptance/accept.cjs
node scripts/miniapp-http-acceptance/environment.cjs check
node scripts/miniapp-http-acceptance/environment.cjs prepare
node scripts/miniapp-http-acceptance/environment.cjs up
node scripts/miniapp-http-acceptance/environment.cjs ready
node scripts/miniapp-http-acceptance/accept.cjs
```

分支必须输出 `feat/miniapp-client`。`prepare` 只生成隔离配置和 SQL；若 `runtime/` 已存在则拒绝覆盖，不自动重置已使用数据库。`up` 内部执行指定 `--project-name`、`--env-file runtime/acceptance.env` 和 `-f compose.json` 的 config/build/up，不会加载根 Compose 或根 `.env`。backend 原有 TCP 检查保留；新增 client 的健康检查与 `environment.cjs ready` 实际请求 `http://backend:48080/actuator/health`，必须得到 HTTP 200 且 status=UP。up 和 accept 都以这项真实 HTTP 检查作为后续操作门槛，不能把端口配置视为可达证据。

成功时脚本退出 0，`runtime/result.json` 为 `PASS_SCOPED`，只代表所列 HTTP/MySQL 检查通过。预期数据：

- 第一单：2 盒 × 12.34 元，抵扣 100 分，应付 2368 分；创建后积分 400、库存仍 50，支付后库存 48，重复请求不变。
- 第二单：3 盒，应付 3602 分；创建后积分 300、库存仍 48；4 个并发支付请求结束后库存 45、积分仍 300。
- 总计 2 张成功支付单、2 张成功支付扩展、2 条销售出库流水、2 条积分抵扣流水；各订单仅一条出库分配和一条通知任务。支付时不赠分；完成/收货不在本轮范围。
- 同租户他人、跨租户会员、篡改 tenant-id、匿名请求必须得到预期权限/归属错误。HTTP 500 或不相关业务错误不能充当拒绝通过。

任一失败非零退出，保存已完成检查、失败阶段和不含令牌的业务状态快照。不继续运行后续用例、不改断言、不重置数据来掩盖失败。诊断命令只读取本环境日志并脱敏：

```powershell
node scripts/miniapp-http-acceptance/environment.cjs diagnose
```

仅查看 `runtime/result.json`、`runtime/command.redacted.log`、`runtime/diagnostic.redacted.log`。不输出 `.env`、原始 HTTP 响应、令牌或原始 Docker inspect/logs。构建错误保存为脱敏 command 日志；失败环境可保留供人工定位。

## 静态发现的业务缺口（不在此修改）

1. **支付创建可能直接失败**：`WxOrderServiceImpl.simulatePayWxOrderByMember` 创建 DTO 时没有 `setBody`；现有 `pay_order.body` 为无默认值的 NOT NULL。DTO 允许 null，MapStruct 转换无默认 body。这是静态证据，真实 MySQL 结果尚未执行确认。脚本保留原约束，支付非零立即停止，不补 SQL 默认值，也不预造支付单绕过创建链。
2. **业务通知投递尚缺路由**：小程序订单控制器没有对应的支付业务通知接收入口。种子把业务通知限定在隔离 backend 的 `/app-api/member/wx-order/payment-notify`，不放置返回假成功的接收器。脚本核对通知任务落库，但明确不宣称投递成功；即使核心脚本 PASS_SCOPED，也不能宣布支付通知闭环验收通过。
3. 库存预留员工路径、确认收货/销售单衔接、处方、真实支付均不属于该脚本覆盖范围。环境启动、MySQL 迁移兼容性、缺口是否导致实际失败都留给后续执行确认。

## 精确清理

仅在证据收集完成后执行：

```powershell
node scripts/miniapp-http-acceptance/environment.cjs down
```

脚本先核对每个同名资源归属，再对唯一项目执行 `down --volumes --rmi all --timeout 30`。只移除上表三个容器、四个专用镜像标签、三个卷和一个网络；不删除基础镜像、构建缓存或本地 `runtime/` 证据。不使用 prune、共享 Compose、全局容器删除或 `--remove-orphans`。发现归属不符则停止，不强行清理。

## 可直接交给执行模型的提示词

你只负责执行已准备的独立 HTTP/MySQL 验收，不负责修复。工作目录必须是 `D:\github-2\FirstSun-miniapp`，分支必须是 `feat/miniapp-client`。先读 `scripts/miniapp-http-acceptance/README.md`，理解两项已记录的支付业务缺口。不得重复运行已通过的 73 项单测。

只授权通过以下脚本操作 `firstsun-miniapp-http-acceptance` 独立环境：构建其四个专用镜像、启动其容器、初始化其空测试库中的结构及合成数据、运行真实 HTTP/MySQL 验收、获取脱敏诊断、清理其资源。源码、测试断言、依赖、已交付配置及脚本均禁止修改；`prepare` 生成隔离 runtime 文件属于允许操作。禁止操作主项目、共享环境、共享 `.env`、其他容器/镜像/卷、真实短信/支付或处方。不得安装工具、启动 Docker Desktop、提交或推送。

PowerShell 按以下顺序逐条执行，并在每步检查 `$LASTEXITCODE`：

```powershell
Set-Location -LiteralPath 'D:\github-2\FirstSun-miniapp'
git branch --show-current
node --check scripts/miniapp-http-acceptance/environment.cjs
node --check scripts/miniapp-http-acceptance/accept.cjs
node scripts/miniapp-http-acceptance/environment.cjs check
node scripts/miniapp-http-acceptance/environment.cjs prepare
node scripts/miniapp-http-acceptance/environment.cjs up
node scripts/miniapp-http-acceptance/accept.cjs
```

预期：静态检查通过，prepare 不覆盖既有文件，up 只启动专用服务，accept 退出 0 并产生 PASS_SCOPED。预期最终库存 45、会员积分 300、支付单/支付扩展/销售出库/积分抵扣各 2 条。PASS_SCOPED 不代表通知投递、收货、处方或真实资金闭环通过。

分支不符、前置工具缺失、Docker 未运行、端口/归属冲突、runtime 已存在、源文件校验值变化、构建/启动失败、任一 HTTP 或 MySQL 断言失败时立即停止后续操作，不修复、不换数据、不改断言、不改配置、不自动重跑。如果已经创建专用资源，只允许运行 `node scripts/miniapp-http-acceptance/environment.cjs diagnose` 获取脱敏证据；报告失败阶段、非零退出码、实际业务状态和已知缺口是否复现。成功或失败收集完证据后，运行 `node scripts/miniapp-http-acceptance/environment.cjs down` 精确清理；若清理保护拒绝则停止并报告。保留 runtime 下的本地脱敏报告，不输出密码、验证码、令牌及原始日志。
