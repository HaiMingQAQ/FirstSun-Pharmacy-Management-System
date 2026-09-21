# 支付业务通知定向复验

本轮只新增 `/app-api/member/wx-order/payment-notify`。使用支付模块现有 `PayOrderNotifyReqDTO`（merchantOrderId、payOrderId）及租户头；不需要会员 token，不接受自报的支付成功或金额。服务端锁订单后锁支付单，复用应用/会员/金额/租户绑定校验，成功响应 `{"code":0,"data":true}`。支付模块收到非零业务码/异常时沿用原有重试机制。

本地定向结果：13 项通过（MVC 契约 2、通知 H2 事务 4、支付授权校验 7）。H2 使用支付和库存测试替身，不能代替真实 HTTP/MySQL。未重跑既有 PASS_SCOPED。

## 测试模型执行命令

只操作当前独立环境，保留已通过 PASS_SCOPED 的数据库与 runtime 证据。**不运行 prepare、down 或原 accept.cjs，不删除 acceptance.started。** 不修改源码、配置、依赖或断言；不访问共享环境；失败立即停止，只允许 diagnose 收集脱敏日志。

先验证本地 Docker context、各资源归属及 manifest，再只重建 backend 镜像和容器（保留 MySQL/Redis/卷/client）。业务源文件不是旧 runtime manifest 的配置输入，本轮未修改 Compose 或已有工具，不需要篡改 manifest。

PowerShell 逐条执行：

```powershell
Set-Location -LiteralPath 'D:\github-2\FirstSun-miniapp'
$ErrorActionPreference = 'Stop'
if ((git branch --show-current) -ne 'feat/miniapp-client') { throw '分支不符' }
node -e "const e=require('./scripts/miniapp-http-acceptance/environment.cjs'); e.verifyFiles(); e.guard(); for(const s of Object.values(e.compose.services)) e.ensure(e.inspect('container',s.container_name)?.State.Running,'独立环境不在运行，停止');"
if ($LASTEXITCODE -ne 0) { throw '隔离校验失败' }
$composeArgs = @('compose','--project-name','firstsun-miniapp-http-acceptance','--env-file','scripts/miniapp-http-acceptance/runtime/acceptance.env','-f','scripts/miniapp-http-acceptance/compose.json')
# 与工具保持一致，不允许宿主环境变量覆盖独立 Compose 配置。
if (Get-ChildItem Env: | Where-Object { $_.Name -like 'ACCEPT_*' -or $_.Name -like 'COMPOSE_*' }) { throw '存在 Compose 覆盖变量，停止' }
docker @composeArgs build backend
if ($LASTEXITCODE -ne 0) { throw 'backend 构建失败' }
docker @composeArgs up -d --no-deps --no-build --force-recreate --wait --wait-timeout 600 backend
if ($LASTEXITCODE -ne 0) { throw 'backend 启动失败' }
node scripts/miniapp-http-acceptance/environment.cjs ready
if ($LASTEXITCODE -ne 0) { throw '真实 HTTP 未就绪' }
node --check scripts/miniapp-http-acceptance/notify-accept.cjs
if ($LASTEXITCODE -ne 0) { throw '通知脚本语法检查失败' }
node scripts/miniapp-http-acceptance/notify-accept.cjs
if ($LASTEXITCODE -ne 0) { throw '通知复验失败，停止' }
```

如果独立环境已不存在，停止报告，不另建空库冒充已有 PASS_SCOPED；需要另行授权重新建立前置数据。

通知专用脚本只新增一笔 1 盒、0 积分抵扣订单，用于触发支付模块真正的异步通知发送；轮询对应 `pay_notify_task.status=10` 和成功投递日志。随后无 token 重放一次及四个并发通知，核对订单状态、库存、库存流水、分配、支付和积分记录完全不变，再验证跨租户/无效支付拒绝以及有效通知重试。保留先前 `result.json`，新结果写 `runtime/notify-result.json`，新增独立防重跑标记 `notify-acceptance.started`。

预期退出 0，状态 `PASS_NOTIFY`。原数据若仍为库存 45、积分 300，则新夹具支付后库存 44、积分仍为 300；重复通知不再变化。这不是重复原支付全套验收；新增支付是触发真实自动投递的必要前置。

失败只运行 `node scripts/miniapp-http-acceptance/environment.cjs diagnose`，读取新通知报告及脱敏日志，保留现场，不删除标记后重跑。实际库存异常后的通知重试由本轮 H2 回滚测试覆盖，本脚本不向 MySQL 注入业务故障，也不宣称验证真实渠道或所有生产调度场景。
