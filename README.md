# FirstSun Pharmacy Management System

医药门店管理系统，基于 Yudao / RuoYi-Vue-Pro 二次开发。本仓库采用单仓库管理，包含后端、管理后台、会员小程序和数据库初始化脚本。

> 数据库文件本身不上传 GitHub，通过 Docker 初始化脚本让每位成员得到一致的开发环境。

## 统一技术版本

| 组件 | 版本 |
|---|---|
| Java | 17（本机编译与 Docker 运行） |
| Spring Boot | 3.5.15 |
| Maven | 3.9.x |
| Node.js | 22 |
| pnpm | 11.22.0 |
| MySQL | 8.0 |
| Redis | 7 |

后端基于 RuoYi-Vue-Pro 官方 `master-jdk17` 分支。成员本机的 JDK 安装路径可以不同，但 `java -version` 和 `mvn -version` 必须显示 Java 17 或更高版本；也可以完全使用 Docker 构建运行。

## 项目结构

```text
backend/       基于 ruoyi-vue-pro 的 Java 后端
admin-ui/      基于 yudao-ui-admin-vue3 的管理后台
mall-uniapp/   基于 yudao-mall-uniapp 的会员小程序
sql/           FirstSun 药店业务初始化 SQL
docs/          团队分工、开发规范和协作文档
docker-compose.yml  团队统一 Docker 开发环境
```

## 开发文档

所有成员开始开发前，应按以下顺序阅读：

1. [团队开发须知](./docs/团队开发须知.md)：环境启动、Git 分支、提交、数据库迁移和冲突处理。
2. [六人全栈开发方案](./docs/六人全栈开发方案.md)：A～F 的模块、页面、数据表及跨模块接口分工。
3. [开发规范与 AI 协作规则](./docs/开发规范与AI协作规则.md)：成员和 AI 工具必须遵守的修改范围及交付要求。
4. [前端界面统一规范](./docs/前端界面统一规范.md)：管理端页面结构、组件、样式和交互约定。

开发文档索引见 [docs/README.md](./docs/README.md)。每位成员应从最新 `main` 创建自己的功能分支，并且只修改本人负责范围。若使用 AI 编程工具，需要同时提供本人分工、AI 协作规则和本次任务的验收标准。

## 一键启动开发环境

### 前提

安装并启动 Docker Desktop。本机直接编译后端时还需要 JDK 17+ 和 Maven 3.9.x。首次构建后端镜像会下载 Maven 依赖，耗时会比较久；之后会复用 Docker 缓存。

### 首次启动

1. 复制环境变量文件，`.env` 已被 Git 忽略，不会上传：

   ```powershell
   Copy-Item .env.example .env
   ```

2. 构建并启动全部服务：

   ```powershell
   docker compose up -d --build
   ```

3. 查看状态：

   ```powershell
   docker compose ps
   ```

   正常情况下会看到 4 个容器：

   | 服务 | 容器 | 端口 |
   |---|---|---|
   | MySQL | `firstsun-pharmacy-mysql` | `3307 -> 3306` |
   | Redis | `firstsun-pharmacy-redis` | `6379 -> 6379` |
   | 后端 | `firstsun-pharmacy-backend` | `48080 -> 48080` |
   | 管理后台 | `firstsun-pharmacy-admin-ui` | `80 -> 80` |

4. 访问管理后台：

   ```text
   http://localhost/
   ```

   团队共享开发账号：

   | 租户 | 用户名 | 密码 |
   |---|---|---|
   | `FirstSun` | `407` | `123456` |

   该账号仅用于本地开发和联调，拥有开发所需权限，并已绑定 FirstSun 演示门店和员工身份。

后端接口地址：

```text
http://localhost:48080
```

数据库连接串：

```text
jdbc:mysql://localhost:3307/firstsun_pharmacy?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

数据库账号使用 `.env` 中的普通应用账号，默认是 `pharmacy` / `pharmacy123`，不要在业务代码里使用 root。

## 日常命令

| 操作 | 命令 |
|---|---|
| 启动全部服务 | `docker compose up -d` |
| 构建并启动全部服务 | `docker compose up -d --build` |
| 停止全部服务并保留数据 | `docker compose down` |
| 查看全部日志 | `docker compose logs -f` |
| 查看后端日志 | `docker compose logs -f backend` |
| 查看前端日志 | `docker compose logs -f admin-ui` |
| 进入 MySQL | `docker exec -it firstsun-pharmacy-mysql mysql -uroot -p` |

## 重建数据库

MySQL 初始化脚本只会在数据卷为空时执行。首次拉取本次统一数据库配置，或初始化 SQL 更新后需要重建本地数据库时，执行：

```powershell
docker compose down -v
docker compose up -d --build
```

`-v` 会删除 Docker 数据卷，数据库里的所有本地数据都会清空。组员执行前先确认没有需要保留的数据。

## 团队同步规则

- 数据库结构以 `backend/sql/mysql/ruoyi-vue-pro.sql` 和 `sql/firstsun_pharmacy_init.sql` 为准。
- `.env` 只保存本机配置，禁止提交。
- 后端统一使用 `48080`，管理后台统一使用 `80`，MySQL 统一使用 `3307`，Redis 统一使用 `6379`。
- 前端包管理统一使用 pnpm；如果本机 npm 可用但 pnpm 不稳定，先联系组长处理环境，不要提交 `package-lock.json`。
- 修改数据库脚本后，必须在本机重建数据库并验证服务能启动，再提交 Pull Request。

管理后台 Docker 容器使用 `admin-ui/.env.docker`，不要把个人 `admin-ui/.env.local` 提交到仓库。
