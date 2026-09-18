# AI 助手技术原型运行与验证

## 范围与关键差异

本原型仅支持管理端药品档案查询、当前员工所属门店的批次库存查询，以及药品新增、修改、删除的预览与二次确认。没有 RAG、任意 SQL、库存写入、采购/销售/支付操作。

外部参考资料把 `ph_store` 当作药品和库存表，并认为 `tenant_id` 可代表门店隔离；当前仓库实际使用 `ph_drug`、`ph_inv_batch`、`ph_inv_location_stock`，且门店通过 `EmployeeService.getEmployeeByUserId` 与 `InventoryReadAccess` 在服务端解析。Spring AI 实际版本为 1.1.8，不采用参考资料的 1.0.1。

## 数据库

在已有药店迁移之后执行：

```sql
source sql/migrations/20260917_o_pharmacy_ai_prototype.sql;
```

脚本创建 `ph_ai_command` 命令快照表，增加 `pharmacy:ai:chat` 能力权限，并为系统超管与 FirstSun 共享演示账号所属角色授权，同时刷新药店套餐 114 的权限范围。脚本包含重复执行保护。

## 模型配置

原型复用 `yudao-module-ai` 的模型、API Key 管理和默认聊天模型。只启用一个 OpenAI 兼容聊天模型：

1. 在 AI 模型管理中创建 API Key，值使用环境变量占位符，例如 `${PHARMACY_AI_API_KEY}`；
2. 在运行后端前设置 `PHARMACY_AI_API_KEY`，不要把真实密钥写入仓库；
3. 创建并启用对应聊天模型，使其成为排序第一的默认聊天模型；
4. 建议模型温度 0.1、最大输出 800 Token。

模型不可用时，药店 AI SSE 会返回 `error` 与 `done` 事件。AI 适配代码只被 `/pharmacy/ai/**` 调用，不改变药品、库存和 POS 原有 Controller。

## 启动

```powershell
cd backend
mvn -pl yudao-server -am -DskipTests package
mvn -pl yudao-server spring-boot:run

cd ..\admin-ui
pnpm install
pnpm dev
```

使用共享演示账号登录：租户 `FirstSun`、用户名 `0407`、密码 `123456`。该账号对应 tenant_id=163、user_id=407、role_id=167，并已绑定 store_id=407、employee_id=407。

登录账号必须拥有 `pharmacy:ai:chat`，并按工具另行拥有药品 query/create/update/delete 或 `pharmacy:inventory-batch:query`。库存查询还要求该 system 用户绑定状态为在职的 `ph_employee`；管理员未绑定员工时默认拒绝，不回退到全租户库存。

`AI助手` 是左侧导航底部的布局动作入口，不是普通路由。执行迁移后需要退出并重新登录，以刷新权限和菜单缓存；点击入口不会改变 URL、面包屑或 TagsView。抽屉首次打开后保持挂载，点击遮罩、关闭按钮或按 Esc 只隐藏抽屉，不会清空会话、草稿、消息或中止正在进行的 SSE。草稿仅保存在当前标签页的 `sessionStorage`，退出登录时清除。

## 接口与 SSE

- `POST /admin-api/pharmacy/ai/chat/message/send-stream`
- `POST /admin-api/pharmacy/ai/command/{commandId}/confirm`
- `POST /admin-api/pharmacy/ai/command/{commandId}/cancel`
- `GET /admin-api/pharmacy/ai/command/recent`

SSE 事件为 `metadata`、`delta`、`tool_started`、`tool_result`、`tool_preview`、`error`、`done`。确认接口只接受一次性凭证；业务参数来自服务端冻结快照。

## 演示话术

- “查询名称包含阿莫西林的药品，最多返回 5 条。”
- “查看药品编号 1 的详细信息。”
- “查询阿莫西林在我当前门店的可用量、冻结量、批次和效期。”
- “把药品 1 的零售价修改为 18.50 元。”生成差异卡片后点击“确认修改药品”。
- “删除药品 1。”生成目标卡片；若被条码引用，确认后仍由 `DrugService` 返回真实拒绝原因。

刷新页面后可从“待确认操作”看到服务端持久化命令状态；一次性确认明文不会持久化或再次下发，刷新后应重新生成预览才能确认。
