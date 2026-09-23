# FirstSun Pharmacy — SSH 隧道受限演示部署指南

> 源码基线：`codex/admin-delivery`（基于 `origin/main` @ f0068958，以当前交付提交为准）
> 镜像标签：后端 `codex-20260922`，管理后台 `codex-20260923-demo-data`（非 latest）
> 适用场景：无域名、无公网 HTTPS 条件下的 SSH 隧道受限演示
> **本配置不是生产部署方案**

## 镜像标签策略

- 交付镜像使用 Compose 中的明确版本标签，不使用 `latest`。
- `latest` 不指向确定源码版本，无法保证与交付分支一致。
- 升级时构建新标签（如 `codex-YYYYMMDD`）并修改 compose 中 `image` 字段，不覆盖旧标签。
- 独立项目名 `firstsun-admin-delivery` 确保与现有容器/卷不冲突。

## 已验证构建产物

| 镜像 | 标签 | 架构 | 镜像 ID | 大小 |
|------|------|------|---------|------|
| firstsun-admin-delivery-backend | codex-20260922 | linux/amd64 | sha256:4c198a89d8e9 | ~447MB |
| firstsun-admin-delivery-admin-ui | codex-20260923-demo-data | linux/amd64 | sha256:7fcecf09ba46 | ~116MB |

> 以上 ID 为 manifest list digest；源码未提交，无发布提交号。

## 构建命令

```bash
# 在项目根目录（codex/admin-delivery worktree）执行

# 后端
docker buildx build --platform linux/amd64 \
  -t firstsun-admin-delivery-backend:codex-20260922 \
  -f backend/Dockerfile \
  --load \
  backend/

# 管理后台前端
docker buildx build --platform linux/amd64 \
  -t firstsun-admin-delivery-admin-ui:codex-20260923-demo-data \
  -f admin-ui/Dockerfile \
  --build-arg VITE_BASE_URL=http://localhost:48080 \
  --load \
  admin-ui/
```

## 内存预算

| 服务 | mem_limit | 实际约束 | 说明 |
|------|-----------|----------|------|
| MySQL 8.0 | 512m | innodb_buffer_pool=192M, performance_schema=OFF | 38 个 init 脚本需要执行空间 |
| Redis 7 | 96m | maxmemory=64mb | 仅缓存用途 |
| Java Backend | 768m | -Xms128m -Xmx512m | JRE + 非堆需额外 ~100m |
| Nginx (admin-ui) | 64m | 静态文件服务 | 占用极小 |
| **合计容器** | **~1440m** | | |
| OS + Docker | ~300-400m | | 内核、systemd、sshd、dockerd |
| **总需求** | **~1.8-1.9GiB** | | 建议 2GiB 以上或添加 Swap |

---

## 前置条件

- Ubuntu 22.04，2 核 CPU，≥2GiB RAM
- Docker 24+ 与 Compose V2 插件
- 本地构建机需 Docker Desktop（Windows/macOS）或 Docker Buildx（Linux）
- 所有端口仅绑定 `127.0.0.1`，通过 SSH 隧道访问，不暴露公网

---

## 第 1 步：准备环境变量

在 `deploy/` 目录下：

```bash
cp .env.prod.example .env.prod
```

编辑 `.env.prod`，**必须修改**：
- `MYSQL_ROOT_PASSWORD` — 设置强密码
- `MYSQL_PASSWORD` — 设置强密码（不要使用 `pharmacy123`）

可选设置：
- `PHARMACY_DEV_SMS_CODE` — 演示测试验证码（仅 SSH 隧道下使用；留空则禁用）
- `PHARMACY_AI_API_KEY` — AI 功能密钥（见下文 AI 密钥配置）

> `.env.prod` 已在 `.gitignore` 排除范围，不应提交。

---

## 第 2 步：导出镜像为 tar 文件

```bash
docker save firstsun-admin-delivery-backend:codex-20260922 | gzip > deploy/backend-codex-20260922.tar.gz
docker save firstsun-admin-delivery-admin-ui:codex-20260923-demo-data | gzip > deploy/admin-ui-codex-20260923-demo-data.tar.gz
```

预估大小：后端 ~450MB，前端 ~30MB。

---

## 第 3 步：上传到服务器

```bash
SERVER=user@YOUR_SERVER_IP

ssh $SERVER "mkdir -p ~/firstsun"
scp deploy/backend-codex-20260922.tar.gz deploy/admin-ui-codex-20260923-demo-data.tar.gz $SERVER:~/firstsun/

# 上传部署配置与 SQL（compose 文件引用 ../sql 和 ../backend，需保持目录结构）
ssh $SERVER "mkdir -p ~/firstsun/FirstSun/{deploy,backend/sql/mysql,sql/migrations}"
scp deploy/docker-compose.prod.yml deploy/.env.prod \
  $SERVER:~/firstsun/FirstSun/deploy/
scp backend/sql/mysql/ruoyi-vue-pro.sql \
  $SERVER:~/firstsun/FirstSun/backend/sql/mysql/
scp sql/firstsun_pharmacy_init.sql \
  $SERVER:~/firstsun/FirstSun/sql/
scp sql/migrations/*.sql \
  $SERVER:~/firstsun/FirstSun/sql/migrations/
```

---

## 第 4 步：服务器加载镜像

```bash
ssh $SERVER
cd ~/firstsun

gunzip -c backend-codex-20260922.tar.gz | docker load
gunzip -c admin-ui-codex-20260923-demo-data.tar.gz | docker load
docker images | grep firstsun-admin-delivery

# 清理 tar（可选）
rm -f backend-codex-20260922.tar.gz admin-ui-codex-20260923-demo-data.tar.gz
```

---

## 第 5 步：首次启动与初始化

```bash
cd ~/firstsun/FirstSun/deploy

# 使用独立项目名 -p firstsun-admin-delivery，避免与现有容器冲突
docker compose -p firstsun-admin-delivery \
  -f docker-compose.prod.yml --env-file .env.prod up -d

# 观察 MySQL 初始化（38 个 SQL 文件首次执行需 1-3 分钟）
docker compose -p firstsun-admin-delivery -f docker-compose.prod.yml logs -f mysql

# MySQL ready 后观察后端启动
docker compose -p firstsun-admin-delivery -f docker-compose.prod.yml logs -f backend
```

首次创建空数据卷时，MySQL 会按 `01` 至 `38` 的顺序执行初始化脚本。第 38 份脚本
`20260922_p_admin_delivery_demo_data.sql` 为 FirstSun 租户（`tenant_id=163`）补充管理后台
A-F 板块的关联演示数据，推荐仅在首次初始化的空数据卷中执行。

**前置数据依赖（重要）**：第 38 份脚本不自行创建门店、分类、仓库、货位、供应商、员工、
班次及会员等基础记录，而是引用此前迁移已写入的固定 ID 演示记录——主要来自第 23 份
`20260914_l_pharmacy_demo_data.sql`，门店 `407` 来自第 9 份共享账号脚本。脚本开头用
`NOT EXISTS` 逐项校验这些前置记录，**任一前置记录缺失时，会在写入任何数据前明确失败并整体
回滚**，不会插入指向空记录的孤儿数据。因此该脚本只能在完整执行过 `01`–`37`、且演示基线
未被删除的库上运行；**不要在无演示数据的生产库、仅部分初始化或手动清理过演示记录的库中
执行**。

固定 ID 或业务唯一键若已被其他记录占用，脚本同样会报错并回滚，不会改写其他租户或继续
引用冲突记录。仅当同一批演示记录已存在且尚未产生真实业务流水时，才可重跑核验；重跑不会
重置已有库存、订单状态或会员状态。业务运行后的库存与状态以实际流水为准，不要把此脚本当作
恢复数据的工具。脚本不使用 `DROP`、`TRUNCATE`，也不删除既有数据。

演示药品图片位于 `admin-ui/public/pharmacy-demo/`，由管理后台 Nginx 同源提供
`/pharmacy-demo/*.svg`。这些图片是项目内原创的通用分类占位图，不含真实药品包装或外链资源。
包含该图片列和静态资源的管理后台镜像标签为
`firstsun-admin-delivery-admin-ui:codex-20260923-demo-data`；不要用旧前端镜像验证本节。

等待 backend 日志出现 `Started YudaoServerApplication` 即启动成功。

### 端口冲突

- 默认 `BACKEND_PORT=48080`、`ADMIN_PORT=80`，均绑定 `127.0.0.1`。
- 如端口被占用，修改 `.env.prod` 中 `BACKEND_PORT=48081` 或 `ADMIN_PORT=8080`。
- 修改后执行 `docker compose -p firstsun-admin-delivery -f docker-compose.prod.yml up -d` 重建端口映射。

---

## 第 6 步：SSH 隧道访问

在本地电脑执行：

```bash
ssh -L 48080:127.0.0.1:48080 -L 8080:127.0.0.1:80 -N user@YOUR_SERVER_IP
```

浏览器访问：
- **管理后台**：`http://localhost:8080`
- **后端 API / Swagger**：`http://localhost:48080/doc.html`

### 租户与角色

- 登录页默认进入 **FirstSun 药店管理系统** 入口，租户为 `FirstSun`。
- 点击底部链接可切换到 **芋道平台租户登录** 入口（租户名 `芋道源码`）。
- 首次登录后请立即修改默认密码。
- 各业务模块（A 采购、B 仓储、C 库存、D POS、E 处方、F 会员）的菜单权限由对应角色控制，新部署后需在平台租户下为药店角色分配菜单。

---

## AI 密钥配置

在 `.env.prod` 中设置：

```
PHARMACY_AI_API_KEY=<你的 AI API Key>
```

未配置时 AI 助手相关功能返回未授权提示，不影响其他业务。配置后需重启后端容器使环境变量生效。

---

## 第 7 步：健康检查

```bash
cd ~/firstsun/FirstSun/deploy

# 容器状态（应全部 healthy/running）
docker compose -p firstsun-admin-delivery -f docker-compose.prod.yml ps

# 后端健康端点
docker exec firstsun-admin-delivery-backend curl -fsS http://localhost:48080/actuator/health

# MySQL 连通性
docker exec firstsun-admin-delivery-mysql mysqladmin ping -uroot -p"$MYSQL_ROOT_PASSWORD" --silent

# Redis 连通性
docker exec firstsun-admin-delivery-redis redis-cli ping

# 内存使用
docker stats --no-stream
free -h
```

---

## 第 8 步：停止与回滚

### 停止服务

```bash
cd ~/firstsun/FirstSun/deploy
docker compose -p firstsun-admin-delivery -f docker-compose.prod.yml --env-file .env.prod down
```

> **不要执行 `docker compose down -v`**，这会删除 MySQL 数据卷。

### 旧库备份（升级前必做）

```bash
docker exec firstsun-admin-delivery-mysql \
  mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" firstsun_pharmacy \
  --single-transaction --routines --triggers \
  | gzip > ~/firstsun/backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

### 增量迁移

新版本如有新增 SQL 迁移脚本：
1. 上传新的 `.sql` 文件到 `sql/migrations/`
2. 在 MySQL 容器中手动执行（幂等脚本可重复执行）：
   ```bash
   docker exec -i firstsun-admin-delivery-mysql \
     mysql -uroot -p"$MYSQL_ROOT_PASSWORD" firstsun_pharmacy \
     < sql/migrations/<new-migration>.sql
   ```
3. 重启后端容器使代码生效：
   ```bash
   docker compose -p firstsun-admin-delivery -f docker-compose.prod.yml restart backend
   ```

> **关于第 38 份脚本（`20260922_p_admin_delivery_demo_data.sql`）**：
> - 新数据库会在首次初始化时随 `01`–`38` 自动执行，无需手动操作。
> - **已有数据库升级时，只有在该库已完整包含第 23 份等前置演示记录时才可手动补跑第 38
>   份**；脚本开头的前置检查会在缺失时直接失败回滚。
> - 该脚本仅用于演示/验收环境。**没有演示数据基线的生产库不要执行**——它不会补建基础
>   数据，前置检查失败即终止。
> - **升级不得通过删除数据卷重建来完成**，否则会丢失既有业务数据。

### 回滚到上一次备份

```bash
cd ~/firstsun/FirstSun/deploy
docker compose -p firstsun-admin-delivery -f docker-compose.prod.yml --env-file .env.prod down

# 先恢复备份（仅在明确需要时执行）
gunzip -c ~/firstsun/backup_YYYYMMDD_HHMMSS.sql.gz | \
  docker exec -i firstsun-admin-delivery-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" firstsun_pharmacy

docker compose -p firstsun-admin-delivery -f docker-compose.prod.yml --env-file .env.prod up -d
```

### 回滚到上一版镜像

```bash
# 加载旧标签镜像
gunzip -c backend-codex-<old-tag>.tar.gz | docker load
# 修改 compose 中 image 标签后重建
docker compose -p firstsun-admin-delivery -f docker-compose.prod.yml up -d --force-recreate backend
```

---

## 构建 Swap（内存不足时可选）

```bash
swapon --show
free -h
if [ ! -f /swapfile ] && [ "$(swapon --show | wc -l)" -eq 0 ]; then
  sudo fallocate -l 1G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi
```

---

## 文件清单

```
deploy/
├── docker-compose.prod.yml   ← SSH 隧道演示 compose（独立项目名 firstsun-admin-delivery）
├── .env.prod.example          ← 环境变量模板（复制为 .env.prod）
├── .env.prod                  ← 实际环境变量（不提交 Git）
└── README-deploy.md           ← 本文档
```

## 验证范围

### 已验证（本地构建）
- [x] 前端 `vite build --mode docker` 构建成功（1m 1s，0 错误）
- [x] 后端 `mvn compile -pl yudao-server -am` 编译成功
- [x] `ApiAccessLogFilterSanitizeTest` 9 个测试全部通过（0 失败，0 错误），含改密请求链路明文不落日志、AI token 数量字段不被误伤
- [x] Docker 镜像 `firstsun-admin-delivery-backend:codex-20260922` linux/amd64 构建成功
- [x] Docker 镜像 `firstsun-admin-delivery-admin-ui:codex-20260923-demo-data` linux/amd64 构建成功，包含药品图片列表功能与仓库内占位图
- [x] `docker compose config` 语法校验通过
- [x] 现有 `firstsun-pharmacy-*:latest` 镜像未被覆盖
- [x] 无真实密钥、tar 归档或 .env.prod 被纳入

### 管理后台演示数据本地验收（2026-09-23）
- [x] 独立项目与新数据卷从零顺序执行 38 个 init SQL，`20260922_p_admin_delivery_demo_data.sql` 成功完成
- [x] 在保留的独立数据库重复执行演示迁移，关键表计数及固定 ID 校验和保持一致
- [x] backend、MySQL、Redis 健康，admin-ui 可登录 FirstSun 租户
- [x] A-F 页面显示药品、采购、库存、销售、处方和会员关联数据；药品分类占位图可访问并实际显示
- [x] 验收使用的临时 Compose 项目、网络和数据卷在完成后删除；保留卷 `firstsun-admin-delivery_mysql-data` 未删除

### 冲突 / 前置缺失测试（2026-09-23，一次性 MySQL）
- [x] 一次性 `mysql:8.0` 容器（无宿主端口、无命名卷，768MiB）实际运行 `Test-AdminDeliveryDemoData.ps1`，退出码 0
- [x] 全新迁移、不重置状态重跑均保持 20 药品且库存数量不被重置（幂等）
- [x] 错误引用、跨租户主键冲突、业务唯一键冲突均安全失败回滚，冲突行保留且无新数据
- [x] 删除前置供应商后迁移在写入前失败，无守卫行、无孤儿药品（详见 `sql/tests/AdminDeliveryDemoData-result.md`）

### 未验证（需独立环境复现）
- [ ] 服务器实际内存是否足够启动全部 4 个容器
- [ ] MySQL 38 个 init 脚本在低内存下能否完整执行
- [ ] Spring Boot 在 `-Xmx512m` 下的运行时稳定性
- [ ] SSH 隧道下 admin-ui 前端 API 请求代理是否正常
- [ ] 管理后台双入口登录页在浏览器中的实际渲染与登录流程
- [ ] 容器启动后健康检查是否全部通过
