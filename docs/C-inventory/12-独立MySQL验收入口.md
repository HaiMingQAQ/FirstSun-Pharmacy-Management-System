# 独立 MySQL 持久化验收入口

日期：2026-09-12。测试源码已提供，尚未执行真实 MySQL 验证。

已验证默认保护：未提供 `inventory.mysql.config` 时，显式选择 InventoryMysqlIT 的 Maven 命令正常结束，5 项全部 skipped，BeforeAll 未执行，没有数据库连接。该结果仅证明默认不连接保护生效，**不是 5 项数据库测试通过**。测试源码已随本模块编译。

## 目的与限制

`InventoryMysqlIT` 使用真实 MySQL JDBC、C 原始 MyBatis XML 和 Spring TransactionTemplate，验证主键/无符号容量映射、数据库唯一约束、插入/更新失败回滚，以及通过 performance_schema 确认的真实行锁等待和旧值冲突。

本入口不启动应用，不测试真实 Token、A 员工查询、Controller 权限代理或业务单据闭环；测试中的门店解析使用明确的 Mock。数据库、Mapper 和事务不是 Mock。不得将这 5 个数据库测试尚未执行的状态写成通过。

## 执行前提

1. 用户确认 C 独立本机实例。仅监听 127.0.0.1 的非默认端口，使用新数据目录和随机凭据；不注册系统服务、不读取小组环境配置。
2. 实例数据目录及凭据文件均位于本项目 `backend/yudao-module-pharmacy/target/inventory-mysql` 下，路径真实解析后也必须留在该目录，不能用符号链接指向共享数据目录。
3. 使用 MySQL 8.x，且满足项目 SQL 基线要求（8.0.28+）。账号能够创建独立测试 schema、读 performance_schema 以确认锁等待；仅为 C 自建实例授权，不能把共享 root 配置填入。

默认执行 Maven 测试不会启动本入口。测试类要求显式 `inventory.mysql.config` 系统属性；没有默认地址、默认密码或失败后回退到开发库的行为。

## 本地凭据文件格式

以下是字段说明，**不是可直接使用的密码或配置**。实际文件只写入上述 Git 忽略目录；dataDir 建议用正斜杠，避免 Java properties 的反斜杠转义。

```properties
owner=C-local-disposable
port=<独立端口，不得为3306或3307>
dataDir=<该C专属实例真实数据目录>
user=<独立实例测试账号>
password=<仅在本机生成的随机密码>
```

测试先校验配置文件位置、归属标记、端口、目录；随后只读查询目标服务器的 `@@datadir` 和 `@@version`。目录或版本不符即中止，发生任何写操作前完成此核对。

通过核对后，每次创建一个新的随机 schema `c_inventory_it_<随机标识>`，执行原始 46 表空库基线。没有 DROP/TRUNCATE、没有清库或重复初始化已有 schema；测试数据保留在独立实例，方便复查。此操作不属于生产增量迁移，也不修改原始 SQL。

## 执行命令

确认实例归属并准备本地配置后，在 backend 执行：

```powershell
mvn -o -pl yudao-module-pharmacy -am test '-Dtest=InventoryMysqlIT' '-Dinventory.mysql.config=<本机专属配置文件绝对路径>' '-Dsurefire.failIfNoSpecifiedTests=false'
```

这一步目前**未执行**。不能把编译测试类、未提供配置导致跳过，或普通单元测试通过当作 MySQL 测试通过。

## 预期证据

- 实际返回自增主键，4294967295 容量正确映射 Long，他店/他租户查询为空，审计用户正确。
- 相同编码及已删除编码仍触发唯一冲突，不出现第二行。
- 在实际 INSERT/UPDATE 后抛异常，事务提交后查询确认数据回滚。
- 并发编辑者确实进入 `performance_schema.data_lock_waits`，前一事务修改并提交后，后一事务发现旧值不符并拒绝，不覆盖新值。

仍需扩展真实 FEFO、容量竞争、收货/销售/退货事务等测试；这些服务未完成时不能预先声称已验收。完整应用级校验还需要 A 登录权限、B/D 业务契约及经确认的迁移。
