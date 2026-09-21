# FirstSun Pharmacy — SSH 隧道受限演示部署指南

> 目标服务器：Ubuntu 22.04, 2核, 1.7Gi RAM, Docker 29.8.1 + Compose V2
> 分支：`feat/miniapp-client`

## 重要声明

- **内存不保证充足**：4 个容器合计预算约 1056m（MySQL 512m + Redis 96m + Java 384m + Nginx 64m），加上 OS 和 Docker 本身开销，1.7Gi 可能不够用。建议添加 Swap 作为缓冲（见第 7 步）。
- **以下服务器运行步骤均未实测**，仅根据服务器信息和项目配置推导。首次启动请逐一执行并观察日志。
- 所有端口仅绑定 `127.0.0.1`，通过 SSH 隧道访问，不暴露到公网。
- 本配置仅用于无域名、无公网 HTTPS 条件下的 SSH 隧道受限演示，不是生产部署方案。
- 真实支付保持关闭；本机回调地址仅用于本地演示链路，外部支付渠道无法访问，因此真实支付、退款、转账通知不可用。

---

## 内存预算

| 服务 | mem_limit | 实际约束 | 说明 |
|------|-----------|----------|------|
| MySQL 8.0 | 512m | innodb_buffer_pool=192M, performance_schema=OFF | 37 个 init 脚本需要执行空间 |
| Redis 7 | 96m | maxmemory=64mb | 仅缓存用途 |
| Java Backend | 384m | -Xms128m -Xmx256m | JRE + 非堆需额外 ~100m |
| Nginx (admin-ui) | 64m | 静态文件服务 | 占用极小 |
| **合计容器** | **1056m** | | |
| OS + Docker | ~300-400m | | 内核、systemd、sshd、dockerd |
| **总需求** | **~1.4-1.5Gi** | | 理论可行，留余约 200-300m |

> 如果 MySQL 初始化（37 个 SQL 文件）时出现 OOM，可能需要 Swap 或分批挂载。

---

## 第 1 步：准备环境变量

在本地 `deploy/` 目录下：

```bash
cp .env.prod.example .env.prod
```

编辑 `.env.prod`，**必须修改**：
- `MYSQL_ROOT_PASSWORD` — 设置强密码
- `MYSQL_PASSWORD` — 设置强密码（不要使用 `pharmacy123`）

可选设置：
- `PHARMACY_DEV_SMS_CODE` — 演示测试验证码（仅 SSH 隧道下使用；留空则禁用）
- `PHARMACY_AI_API_KEY` — AI 功能密钥

> `.env.prod` 已在 `.gitignore` 排除范围，不应提交。

---

## 第 2 步：本地构建 Linux/amd64 镜像

在项目根目录 `D:\github-2\FirstSun-miniapp` 执行：

```bash
# 后端（Maven 构建 + JRE 打包，约 5-10 分钟）
docker buildx build --platform linux/amd64 \
  -t firstsun-pharmacy-backend:latest \
  -f backend/Dockerfile \
  --load \
  backend/

# 管理后台前端（Node + pnpm 构建 + Nginx 打包）
# VITE_BASE_URL 设为 http://localhost:48080，通过 SSH 隧道时浏览器会直连本地端口
docker buildx build --platform linux/amd64 \
  -t firstsun-pharmacy-admin-ui:latest \
  -f admin-ui/Dockerfile \
  --build-arg VITE_BASE_URL=http://localhost:48080 \
  --load \
  admin-ui/
```

> Windows 上 `docker buildx` 需要 Docker Desktop 启用 BuildKit。如果 `--load` 失败，尝试先 `docker buildx create --use` 创建 builder。

---

## 第 3 步：导出镜像为 tar 文件

```bash
docker save firstsun-pharmacy-backend:latest | gzip > deploy/backend.tar.gz
docker save firstsun-pharmacy-admin-ui:latest | gzip > deploy/admin-ui.tar.gz
```

预估大小：后端 ~200-300MB，前端 ~30-50MB。

---

## 第 4 步：上传到服务器

```bash
# 替换 YOUR_SERVER_IP 和用户名
SERVER=user@YOUR_SERVER_IP

# 创建远程工作目录
ssh $SERVER "mkdir -p ~/firstsun"

# 上传镜像
scp deploy/backend.tar.gz deploy/admin-ui.tar.gz $SERVER:~/firstsun/

# 上传部署配置（compose 文件引用 ../sql 和 ../backend，需保持目录结构）
# 方法 A：上传整个项目（简单但体积大）
rsync -avz --exclude='node_modules' --exclude='.git' --exclude='target' \
  . $SERVER:~/firstsun/FirstSun-miniapp/

# 方法 B：只上传必要文件（推荐，节省带宽）
ssh $SERVER "mkdir -p ~/firstsun/FirstSun-miniapp/{deploy,backend/sql/mysql,sql/migrations}"
scp deploy/docker-compose.prod.yml deploy/.env.prod \
  $SERVER:~/firstsun/FirstSun-miniapp/deploy/
scp backend/sql/mysql/ruoyi-vue-pro.sql \
  $SERVER:~/firstsun/FirstSun-miniapp/backend/sql/mysql/
scp sql/firstsun_pharmacy_init.sql \
  $SERVER:~/firstsun/FirstSun-miniapp/sql/
scp sql/migrations/*.sql \
  $SERVER:~/firstsun/FirstSun-miniapp/sql/migrations/
```

---

## 第 5 步：服务器加载镜像

```bash
ssh $SERVER

cd ~/firstsun

# 加载镜像
gunzip -c backend.tar.gz | docker load
gunzip -c admin-ui.tar.gz | docker load

# 验证
docker images | grep firstsun

# 清理 tar（可选，节省磁盘）
rm -f backend.tar.gz admin-ui.tar.gz
```

---

## 第 6 步：启动服务

```bash
cd ~/firstsun/FirstSun-miniapp/deploy

# 首次启动（MySQL 初始化 37 个 SQL 文件可能需要 1-3 分钟）
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 观察启动日志（等待 MySQL healthy）
docker compose -f docker-compose.prod.yml logs -f mysql

# MySQL ready 后观察后端启动
docker compose -f docker-compose.prod.yml logs -f backend
```

等待 backend 日志出现 `Started YudaoServerApplication` 即启动成功。

---

## 第 7 步：（可选）添加 Swap 缓冲

> 仅在内存不足时使用。先检查是否已存在 Swap。

```bash
# 检查现有 swap
swapon --show
free -h

# 仅在无 swap 且 /swapfile 不存在时创建
if [ ! -f /swapfile ] && [ "$(swapon --show | wc -l)" -eq 0 ]; then
  sudo fallocate -l 1G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
  echo "Swap 已创建并启用"
else
  echo "已有 swap 或 /swapfile，跳过"
fi
```

---

## 第 8 步：健康检查与验证

```bash
# 查看各容器状态（应全部 healthy/running）
docker compose -f docker-compose.prod.yml ps

# 后端健康端点
docker exec firstsun-pharmacy-backend curl -fsS http://localhost:48080/actuator/health

# MySQL 连通性
docker exec firstsun-pharmacy-mysql mysqladmin ping -uroot -p"$MYSQL_ROOT_PASSWORD" --silent

# Redis 连通性
docker exec firstsun-pharmacy-redis redis-cli ping

# 内存使用概览
docker stats --no-stream
free -h
```

---

## 第 9 步：SSH 隧道访问

在本地电脑（Windows）执行：

```bash
# 同时转发后端 API 和管理后台
ssh -L 48080:127.0.0.1:48080 -L 8080:127.0.0.1:80 -N user@YOUR_SERVER_IP
```

保持终端开启，然后在浏览器访问：
- **管理后台**：`http://localhost:8080`
- **后端 API**：`http://localhost:48080`（含 Swagger 文档 `/doc.html`）

> 关于默认登录凭据：admin-ui 构建时嵌入了默认用户名/密码（admin/admin123），但由于服务仅通过 SSH 隧道访问，不暴露公网，风险受控。首次登录后建议立即修改密码。

---

## 第 10 步：数据库备份

```bash
# 手动备份
docker exec firstsun-pharmacy-mysql \
  mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" firstsun_pharmacy \
  --single-transaction --routines --triggers \
  | gzip > ~/firstsun/backup_$(date +%Y%m%d_%H%M%S).sql.gz

# 查看备份
ls -lh ~/firstsun/backup_*.sql.gz
```

---

## 第 11 步：升级与回滚

已有数据库升级前必须先备份，并按项目迁移说明执行增量 SQL。升级不得以删除数据库卷作为常规方案；不要执行 `docker compose down -v`。

### 回滚到上一次备份

```bash
cd ~/firstsun/FirstSun-miniapp/deploy

# 停止服务
docker compose -f docker-compose.prod.yml --env-file .env.prod down

# 先执行对应的增量迁移，再重启服务；不要删除数据卷
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 仅在明确需要恢复且已确认备份可用时执行
gunzip -c ~/firstsun/backup_YYYYMMDD_HHMMSS.sql.gz | \
  docker exec -i firstsun-pharmacy-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" firstsun_pharmacy
```

### 回滚到上一版镜像

```bash
# 如果保留了旧的 tar.gz
gunzip -c backend-old.tar.gz | docker load

cd ~/firstsun/FirstSun-miniapp/deploy
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --force-recreate backend
```

---

## 小程序前端（mall-uniapp）— 当前状态说明

小程序前端不在本次服务器部署范围内。部署到微信需要：

1. 将 `mall-uniapp/.env.production` 中的 `SHOPRO_BASE_URL` 改为公网可达地址
2. 执行 `pnpm build:mp-weixin` 编译
3. 用微信开发者工具上传到微信后台
4. 提交审核

当前无域名、服务仅 SSH 隧道可达，小程序无法直连后端。待配置域名 + HTTPS 后再处理此部分。

---

## 文件清单

```
deploy/
├── docker-compose.prod.yml   ← SSH 隧道演示 compose（本文件所述）
├── .env.prod.example          ← 环境变量模板（复制为 .env.prod）
├── .env.prod                  ← 实际环境变量（不提交 Git）
└── README-deploy.md           ← 本文档
```

## 尚未验证的事项

- [ ] 服务器实际内存是否足够启动全部 4 个容器
- [ ] MySQL 37 个 init 脚本在低内存下能否完整执行
- [ ] Spring Boot 在 `-Xmx256m` 下的稳定性（原始默认 512m）
- [ ] Docker Compose volume 路径 `../` 在服务器上的解析行为
- [ ] `docker buildx --platform linux/amd64` 在 Windows 上的镜像兼容性
- [ ] SSH 隧道下 admin-ui 前端 API 请求是否正常代理
