# FirstSun Pharmacy Management System

医药门店管理系统。数据库初始化脚本位于 [`sql/`](sql/firstsun_pharmacy_init.sql)（46 张业务表：药店 `ph_*` + 会员 `member_*` + 支付 `pay_*`）。

> 数据库文件本身不入库，通过 Docker 让每位成员得到一致的开发库，见下文。

## 项目结构

本仓库采用单仓库管理，三端代码和数据库脚本放在同一个 GitHub 项目中：

`	ext
backend/       基于 ruoyi-vue-pro 的 Java 后端
admin-ui/      基于 yudao-ui-admin-vue3 的管理后台
mall-uniapp/   基于 yudao-mall-uniapp 的会员小程序
sql/           FirstSun 药店业务初始化 SQL
docker-compose.yml  团队统一 MySQL 开发环境
`
## 本地开发数据库（Docker MySQL）

### 前提

安装并启动 [Docker Desktop](https://www.docker.com/products/docker-desktop/)（Windows）。

### 首次启动

1. 复制环境变量文件（`.env` 已被 git 忽略，不会上传）：
   ```powershell
   Copy-Item .env.example .env
   ```
2. 启动容器：
   ```powershell
   docker compose up -d
   ```
   首次启动会自动执行 `sql/firstsun_pharmacy_init.sql`，建好全部 46 张业务表（约 10~30 秒）。
3. 验证：
   ```powershell
   docker compose ps                      # STATUS 应为 healthy
   docker exec -it firstsun-pharmacy-mysql mysql -uroot -p   # 密码见 .env
   ```
   ```sql
   SHOW DATABASES;                        -- 应有 firstsun_pharmacy
   USE firstsun_pharmacy; SHOW TABLES;    -- 应有 ph_store 等 46 张业务表
   ```

### 应用连接配置

| 项 | 值 |
|---|---|
| 地址 | `localhost`（`127.0.0.1`） |
| 端口 | `.env` 中 `MYSQL_PORT`，默认 `3307`（避开本机原生 MySQL 占用的 3306，应用统一连 3307） |
| 库名 | `.env` 中 `MYSQL_DATABASE`，默认 `firstsun_pharmacy` |
| 账号 | `.env` 中 `MYSQL_USER`，默认 `pharmacy` |
| 密码 | `.env` 中 `MYSQL_PASSWORD`，默认 `pharmacy123` |

应用连接请用 `MYSQL_USER` 而不是 root。示例 JDBC 连接串：

```
jdbc:mysql://localhost:3307/firstsun_pharmacy?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

### 日常命令

| 操作 | 命令 |
|---|---|
| 停止（保留数据） | `docker compose down` |
| 再次启动 | `docker compose up -d` |
| 查看日志 | `docker compose logs -f mysql` |
| 进入容器 | `docker exec -it firstsun-pharmacy-mysql mysql -uroot -p` |

### ⚠️ 重建数据库（删数据，仅在 sql 脚本更新后需要重新初始化时执行）

```powershell
docker compose down -v
docker compose up -d
```

`-v` 会删除数据卷，**所有数据丢失**，请只在确认无需要保留的数据时执行。

### 团队如何同步数据库结构

1. 修改或新增 `sql/*.sql`（用新文件名如 `alter_v2.sql`，保证可按顺序重复执行）。
2. 提交并推送（`main` 分支受保护，需走分支 + Pull Request）。
3. 其他成员 `git pull` 后，执行上面的「重建数据库」命令即可获得一致结构。




