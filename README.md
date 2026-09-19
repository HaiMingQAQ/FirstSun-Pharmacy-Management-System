# FirstSun Pharmacy Management System

医药门店管理系统，基于 Yudao / RuoYi-Vue-Pro 二次开发。本仓库采用单仓库管理，包含后端、管理后台、会员小程序和数据库初始化脚本。

> 数据库文件本身不上传 GitHub，通过 Docker 初始化脚本让每位成员获得一致的开发环境。

## 统一技术版本

| 组件 | 版本 |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.5.15 |
| Maven | 3.9.x |
| Node.js | 22 |
| pnpm | 11.22.0 |
| MySQL | 8.0 |
| Redis | 7 |

后端基于 RuoYi-Vue-Pro 官方 `master-jdk17` 分支。成员本机的 JDK 安装路径可以不同，但 `java -version` 和 `mvn -version` 必须显示 Java 17 或更高版本；也可以完全使用 Docker 构建运行。

## 团队开发环境同步

拉取最新代码后执行：

```powershell
git pull
docker compose up -d --build
```
如果本次更新包含数据库初始化脚本，并且本地测试数据不需要保留：
```powershell
docker compose down -v
docker compose up -d --build
```

## 本地开发测试账号

完成数据库初始化后，可使用以下账号登录：

| 项目 | 内容 |
| --- | --- |
| 租户 | `FirstSun` |
| 用户名 | `0407` |
| 密码 | `123456` |

> 该账号仅用于本地 Docker 开发和团队联调，请勿用于生产环境。

网址：

   ```text
   http://localhost/
   ```

后端接口地址：

```text
http://localhost:48080
```

数据库连接串：

```text
jdbc:mysql://localhost:3307/firstsun_pharmacy?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```



## 项目结构

```text
backend/            基于 RuoYi-Vue-Pro 的 Java 后端
admin-ui/           基于 yudao-ui-admin-vue3 的管理后台
mall-uniapp/        基于 yudao-mall-uniapp 的会员小程序
sql/                FirstSun 药店业务初始化 SQL
docs/               团队分工、开发规范、UI 标准和交互演示
docker-compose.yml  团队统一 Docker 开发环境
```

## 当前开发进度

项目正在按照 A～F 六人模块分工并行开发。

当前已经完成并合入 `main`：

- A 模块：药品分类、药品档案、药品条码、门店管理和员工管理。
- 药店管理端 V1 公共 UI 基线。
- FirstSun 品牌主题、统一业务页面头部、数据概览条和动态菜单样式。
- 前端 UI 规范、页面迁移清单、AI 执行提示词和交互演示。
- Java 17 与 Spring Boot 3 开发环境迁移。

当前 UI 版本为团队开发基线，并非最终视觉版本。A 将继续完善公共组件、详情面板、表单视觉和响应式验收；B～F 基于公共 UI 开发各自业务模块。

## 开发文档

所有成员开始开发前，应按以下顺序阅读：

1. [团队开发须知](./docs/团队开发须知.md)：环境启动、Git 分支、提交、数据库迁移和冲突处理。
2. [六人全栈开发方案](./docs/六人全栈开发方案.md)：A～F 的模块、页面、数据表及跨模块接口分工。
3. [开发规范与 AI 协作规则](./docs/开发规范与AI协作规则.md)：成员和 AI 工具必须遵守的修改范围及交付要求。
4. [前端 UI 开发资料](./docs/frontend-ui/README.md)：新版 UI 规范、演示、迁移流程和验收要求。
5. [V2 现代化 UI 风格规范](./docs/frontend-ui/V2现代化UI风格规范-初版.md)：颜色、布局、图标、组件和动效规范。
6. [页面迁移与验收清单](./docs/frontend-ui/页面迁移与验收清单.md)：页面开发完成后的统一检查项。
7. [前端 UI 交互演示](./docs/frontend-ui/demos/pharmacy-modern-ui-v2-motion-demo.html)：目标布局和交互方向参考。

完整文档索引见 [docs/README.md](./docs/README.md)。

每位成员必须从最新 `main` 同步公共 UI 基线，只修改本人负责的业务模块。公共样式、公共 Pharmacy 组件和系统外壳由 A 统一维护；其他成员需要扩展公共组件时，应先与 A 沟通。

## 前端 UI 协作方式

其他成员开始页面适配前，先保存自己的修改，然后同步最新主分支：

```powershell
git status
git add .
git commit -m "wip: 保存当前开发进度"

git fetch origin
git merge origin/main
```

如果工作区没有未提交修改，可以省略 `git add` 和 `git commit`。

页面开发统一遵守以下要求：

- 正式页面继续使用 Vue 3、Element Plus、真实 API、动态菜单、权限和字典。
- 不得复制演示页面中的固定数据或写死菜单。
- 页面根容器使用统一的药店页面样式。
- 标题和数据概览优先使用公共 `Pharmacy` 组件。
- 菜单和按钮使用项目统一的 SVG/Iconify 图标，不使用单字或 Emoji 充当图标。
- 高频查询条件默认展示，低频条件放入“更多筛选”。
- 简单 CRUD 页面参考药品分类。
- 复杂业务列表和表单参考药品档案。
- 简单表单使用 Dialog，复杂表单使用 Drawer 和连续分区。
- 每位成员先完成一个代表页面并提交截图评审，通过后再迁移同模块其他页面。
- 公共样式或公共组件存在问题时统一反馈给 A，不在个人分支中自行覆盖。

## 一键启动开发环境

### 前提

安装并启动 Docker Desktop。本机直接编译后端时还需要 JDK 17+ 和 Maven 3.9.x。

首次构建后端镜像会下载 Maven 依赖，耗时可能较长；后续构建会复用 Docker 缓存。

### 首次启动

1. 复制环境变量文件。`.env` 已被 Git 忽略，不会上传：

   ```powershell
   Copy-Item .env.example .env
   ```

2. 构建并启动全部服务：

   ```powershell
   docker compose up -d --build
   ```

3. 查看服务状态：

   ```powershell
   docker compose ps
   ```

正常情况下会看到 4 个容器：

| 服务 | 容器 | 端口 |
| --- | --- | --- |
| MySQL | `firstsun-pharmacy-mysql` | `3307 -> 3306` |
| Redis | `firstsun-pharmacy-redis` | `6379 -> 6379` |
| 后端 | `firstsun-pharmacy-backend` | `48080 -> 48080` |
| 管理后台 | `firstsun-pharmacy-admin-ui` | `80 -> 80` |


## 日常命令

| 操作 | 命令 |
| --- | --- |
| 启动全部服务 | `docker compose up -d` |
| 构建并启动全部服务 | `docker compose up -d --build` |
| 停止全部服务并保留数据 | `docker compose down` |
| 查看全部日志 | `docker compose logs -f` |
| 查看后端日志 | `docker compose logs -f backend` |
| 查看前端日志 | `docker compose logs -f admin-ui` |
| 进入 MySQL | `docker exec -it firstsun-pharmacy-mysql mysql -uroot -p` |

日常前端开发优先使用本地 Vite 开发服务器，从而获得热更新，不需要每次修改代码后重新构建 Docker：

```powershell
cd admin-ui
pnpm dev
```

默认访问地址以终端输出为准，通常为：

```text
http://localhost:5173/
```



> `-v` 会删除 Docker 数据卷，数据库里的所有本地数据都会清空。执行前必须确认没有需要保留的数据。

普通前端样式和 Vue 页面修改不需要删除数据库卷，也不需要执行 `docker compose down -v`。

## 团队同步规则

- 所有成员从最新 `main` 同步公共代码，不直接相互合并个人功能分支。
- 数据库结构以 `backend/sql/mysql/ruoyi-vue-pro.sql` 和 `sql/firstsun_pharmacy_init.sql` 为准。
- `.env` 只保存本机配置，禁止提交。
- 后端统一使用 `48080`。
- 管理后台 Docker 版本统一使用 `80`。
- MySQL 统一使用 `3307`。
- Redis 统一使用 `6379`。
- 前端包管理统一使用 pnpm，不要提交 `package-lock.json`。
- 修改数据库脚本后，必须在本机重建数据库并验证服务能够启动，再提交 Pull Request。
- 每个 Pull Request 必须说明修改范围、真实接口验证结果和已知限制。
- 提交前检查是否包含测试账号、密码、临时截图、本地启动脚本或构建产物。

管理后台 Docker 容器使用 `admin-ui/.env.docker`，不要把个人 `admin-ui/.env.local` 提交到仓库。

## 环境变量说明

除 `docker-compose.yml` 中带默认值的变量外，以下变量需要按需填写到本机 `.env`（`.env` 已被 Git 忽略）：

| 变量名 | 是否必填 | 用途 | 说明 |
| --- | --- | --- | --- |
| `PHARMACY_DEV_SMS_CODE` | 使用小程序手机号登录时必填 | 会员手机号快捷登录的**开发测试验证码**（F 模块） | 只写在本地 `.env`，禁止写入代码/前端/SQL/Dockerfile，也禁止提交到 Git；仅 `local`/`dev` profile 生效 |

### 手机号快捷登录（小程序）

会员手机号快捷登录需要「手机号 + 验证码」，验证码由后端校验：

```text
POST /app-api/member/auth/send-sms-code?mobile=13800138000     # 获取验证码（不返回验证码内容）
POST /app-api/member/auth/login-or-register?mobile=13800138000 # 登录（不存在则自动注册）
     Header: X-Sms-Code: <验证码>
```

> 验证码走请求头 `X-Sms-Code` 而不是 query 参数或 JSON body：yudao 框架的访问日志拦截器在非 prod 环境会把请求参数与 body 原样打印，走请求头可以确保验证码不会出现在后端日志里。

- 本地开发自行在 `.env` 里设置 `PHARMACY_DEV_SMS_CODE`（任意 4-8 位数字/字母即可，团队各人可不同），然后 `docker compose up -d backend` 让变量生效；调用发码接口后，用 `.env` 里配置的这个值登录。
- 验证码与「手机号 + 登录用途」绑定，10 分钟内有效，且**一次性使用**：用过一次、过期、错误或不匹配当前手机号都会被拒绝。
- 生产环境（无 `local`/`dev` 覆盖）默认**关闭**手机号快捷登录，接口返回「当前环境未启用短信登录」，开发测试验证码在 prod 无效。手机号 + 密码登录不受影响。

### 会员积分规则（F 模块）

积分赠送、抵扣、退货回退与取消返还**全部由后端**按配置 + 会员等级计算，前端传入的积分值只表示「希望使用多少抵扣积分」，实际可用值由后端校验后确定。规则配置在 `backend/yudao-server/src/main/resources/application.yaml` 的 `yudao.pharmacy.member-point` 下：

| 配置项 | 默认值 | 含义 |
| --- | --- | --- |
| `earn-enabled` | true | 是否启用积分赠送 |
| `earn-per-yuan` | 1 | 每消费 1 元获得的基础积分 |
| `level-multiplier` | 1→1.0 / 2→1.2 / 3→1.5 / 4→2.0 | 按 `member_level.level` 的赠送倍率，未配置的等级按 1.0 |
| `deduct-enabled` | true | 是否启用积分抵扣 |
| `points-per-yuan` | 100 | 多少积分抵扣 1 元 |
| `max-deduct-percent` | 50 | 单笔订单最高抵扣比例（占应付金额的百分比） |
| `min-deduct-points` / `max-deduct-points` | 1 / 0 | 单笔最少使用积分 / 单笔上限（0 表示不额外限制） |

计算口径：`赠送积分 = ⌊实付金额 × earn-per-yuan × 等级倍率⌋`，`可用抵扣积分 = min(会员余额, 订单金额 × max-deduct-percent, 单笔上限)`，退货按「实际退货金额 ÷ 原单金额」比例回退，整单全退时结清剩余。

积分入口（小程序端，会员编号取自登录令牌）：

```text
GET  /app-api/member/point/summary                      # 积分余额 + 当前生效规则
POST /app-api/member/point/deduct-preview               # 抵扣试算（不传 usePoints 时返回本单最多可用积分）
POST /app-api/member/wx-order/create                    # 下单，请求体可带 usePoints 使用积分抵扣
```

所有积分变动都带业务幂等键（会员 + 业务类型 + 业务编码，对应唯一键 `uk_point_event`），重复支付回调、重复核销、重复退货、重复取消都不会重复变动积分；`member_point_record` 只允许由 F 的积分服务写入，D（POS）、E（支付）通过 `MemberPointFacade` 调用。
