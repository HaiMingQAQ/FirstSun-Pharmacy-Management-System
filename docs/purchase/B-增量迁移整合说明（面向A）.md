# B 模块增量迁移 —— 面向 A 的基线整合说明

| 项 | 内容 |
| --- | --- |
| 提交方 | B（供应商与采购收货） |
| 接收方 | A（公共平台 / 初始化基线维护） |
| 分支 | `fix/b-purchase-3bugs`（未提交、未推送） |
| 背景 | B 本轮修复：部分收货累计错误（B-1）、采购订单驳回（B-2）、收货单日期异常（B-3）、采购域并发取号 |
| 约定 | `sql/firstsun_pharmacy_init.sql` 是公共初始化基线，**由 A 统一维护**；B 只提交增量迁移与执行顺序，不直接改写该汇总文件 |

---

## 一、新增的 B 模块增量迁移（2 个）

| 顺序 | 文件 | 作用 | 是否改既有表结构 |
| --- | --- | --- | --- |
| 3 | `sql/migrations/20260917_b_purchase_order_reject.sql` | 采购订单驳回：加 3 列、放开状态检查约束、注册「已驳回」字典 | 是（`ph_po_order` 加列 + 改 CHECK 约束） |
| 4 | `sql/migrations/20260917_b_purchase_doc_seq.sql` | 采购单号序列表（并发取号原子分配） | 否（仅新建 B 自有表 `ph_po_doc_seq`） |

---

## 二、执行顺序（整合到 A 的基线）

```text
1) 20260912_b_purchase_supplier_menu.sql           （B，既有）
2) 20260913_b_purchase_order_receipt_menu.sql      （B，既有）
3) 20260917_b_purchase_order_reject.sql            （B，本轮新增）
4) 20260917_b_purchase_doc_seq.sql                 （B，本轮新增）
```

> 依赖关系：第 4 个脚本**不依赖**第 3 个，两者可独立执行；但第 4 个脚本的历史回填读取 `ph_po_order` / `ph_po_receipt`，
> 因此必须在**数据已存在**之后执行（即业务数据导入之后）。第 3 个脚本无前置依赖。

---

## 三、脚本 3：`20260917_b_purchase_order_reject.sql`

### 3.1 变更内容

**① `ph_po_order` 新增 3 列**

| 列名 | 类型 | 允许 NULL | 注释 |
| --- | --- | --- | --- |
| `reject_reason` | `VARCHAR(500)` | 是 | 驳回原因（仅「已提交 → 已驳回」时写入） |
| `reject_by` | `BIGINT` | 是 | 驳回人员工编号 |
| `reject_at` | `DATETIME` | 是 | 驳回时间 |

**② 放开状态检查约束**

原约束（见 `firstsun_pharmacy_init.sql` 第 263 行）不含「已驳回」，不改会直接报
`Check constraint 'ck_po_order_status' is violated`：

```sql
-- 原
CONSTRAINT ck_po_order_status CHECK (status IN (-1,0,1,2,3,4,5))
-- 改为
CONSTRAINT ck_po_order_status CHECK (status IN (-1,0,1,2,3,4,5,6))
```

新增状态值 `6 = 已驳回`（见 `PurchaseOrderStatusEnum.REJECTED`）。

**③ 注册字典项**：`system_dict_type` 类型 `pharmacy_po_status` 下新增 `已驳回 = 6`（`color_type=danger`，`sort=8`）。

### 3.2 幂等实现

- 加列：先查 `information_schema.COLUMNS`，不存在才 `ALTER`；
- 约束：查 `information_schema.CHECK_CONSTRAINTS`，仅当存在的约束子句**尚不含 6** 时才 `DROP` + `ADD`；
- 字典：`system_dict_type` 用 `WHERE NOT EXISTS` 兜底；`system_dict_data` 用 `WHERE NOT EXISTS`（按 `dict_type + value` 判重）。

### 3.3 回滚语句

```sql
-- 回滚 20260917_b_purchase_order_reject.sql
-- ① 删除新增字典项
DELETE FROM `system_dict_data` WHERE `dict_type` = 'pharmacy_po_status' AND `value` = '6';

-- ② 约束还原（去掉 6）——注意：若库中已存在 status=6 的数据，必须先处理，否则 ADD 会失败
UPDATE `ph_po_order` SET `status` = -1 WHERE `status` = 6;   -- 按业务约定回退为「已取消」，或按需改为 0 草稿
ALTER TABLE `ph_po_order` DROP CHECK `ck_po_order_status`;
ALTER TABLE `ph_po_order` ADD CONSTRAINT `ck_po_order_status` CHECK (`status` IN (-1,0,1,2,3,4,5));

-- ③ 删除新增列
ALTER TABLE `ph_po_order` DROP COLUMN `reject_at`;
ALTER TABLE `ph_po_order` DROP COLUMN `reject_by`;
ALTER TABLE `ph_po_order` DROP COLUMN `reject_reason`;
```

> ⚠️ 回滚前必须确认没有 `status = 6` 的业务数据，否则第 ② 步加约束会失败。

**回滚语句实测结论**（在临时库 `firstsun_rollback_test` 上验证，验证后已删除该库）：
预置「三列 + 含 6 的约束 + 已驳回字典 + 一条 `status=6` 数据」后执行上述回滚，**8 条语句全部成功**，回滚后状态：

| 校验项 | 回滚后 |
| --- | --- |
| 驳回列残留 | 0 |
| `ck_po_order_status` | `` (`status` in (-(1),0,1,2,3,4,5)) ``（已还原） |
| 「已驳回」字典残留 | 0 |
| 原 `status=6` 数据 | 已回退为 `-1`（已取消） |

---

## 四、脚本 4：`20260917_b_purchase_doc_seq.sql`

### 4.1 为什么需要这张表

原取号方式为「`SELECT MAX(单号) + 1` 后插入」，存在两个并发缺陷（均有实测证据）：

1. MySQL 默认隔离级别 **REPEATABLE-READ** 下，`MAX()` 是不加锁的一致性读，
   **同一事务内重试读到的快照恒定不变**，5 次重试算出同一个号，最终抛「单号已存在」；
2. 并发插入同一 `receipt_no` / `order_no` 会争抢 `uk_receipt_no` / `uk_order_no` 的索引锁，产生死锁（`DeadlockLoserDataAccessException`），表现为 HTTP 500。

实测：同门店同业务日并发创建 5 张收货单 → **仅 1 张成功**，其余为 2 个「单号已存在」+ 3 个 500。

### 4.2 变更内容

**新建表 `ph_po_doc_seq`（B 模块自有，不改动任何既有表）**

| 列名 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `BIGINT` AUTO_INCREMENT | 主键 |
| `store_id` | `BIGINT` NOT NULL | 门店编号 |
| `biz_date` | `DATE` NOT NULL | 业务日期（单号中的 yyyyMMdd） |
| `biz_type` | `VARCHAR(16)` NOT NULL | 单据类型：`ORDER` / `RECEIPT` |
| `next_seq` | `INT` NOT NULL DEFAULT 0 | 已分配到的最大流水号 |
| `creator` / `create_time` / `updater` / `update_time` / `deleted` / `tenant_id` | — | 与库内既有表一致的审计与租户列 |

**唯一键**：`UNIQUE KEY uk_doc_seq (store_id, biz_date, biz_type)`
→ 保证每个「门店 + 业务日 + 单据类型」拥有独立且唯一的流水序列。

字符集：`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`（与库内既有表一致）。

### 4.3 历史数据衔接（回填）

按 `ph_po_order.order_no` / `ph_po_receipt.receipt_no` 解析出历史最大流水，回填 `next_seq`，
使新单号从「历史最大 + 1」继续，不与既有单号冲突。

**关键口径：业务日期取自「单号自身的日期段」，不取 `receive_date` / `order_date`。**

> 原因：单号格式为 `GR<门店>-<yyyyMMdd>-<流水>`，运行时注册键用的就是单号里的日期段；
> 而收货日期与单号日期段并不总一致（补录历史单据时 `receive_date` 是当天、单号日期段却是原始业务日）。
> 若按 `receive_date` 分组，会把 A 日期的流水算进 B 日期，导致该组 `next_seq` 虚高、新单号凭空跳号。
>
> **实测样例**：`GR407-20260911-0042` 的 `receive_date` 是 `2026-09-17`。
> 按 `receive_date` 分组会把 `0917` 组的 max 抬到 **42**（真实只有 **10**）。

### 4.4 幂等实现

- `CREATE TABLE IF NOT EXISTS`；
- 回填用 `INSERT ... ON DUPLICATE KEY UPDATE next_seq = GREATEST(existing.next_seq, VALUES(next_seq))`
  —— **只增不减**，重复执行不会把运行中已推进的序列改小。

### 4.5 回滚语句

```sql
-- 回滚 20260917_b_purchase_doc_seq.sql
DROP TABLE IF EXISTS `ph_po_doc_seq`;
```

> ⚠️ 回滚后取号会退回 `MAX(单号) + 1` 实现（需同时回滚后端代码），且并发缺陷会复现。
> 表内已分配的流水会丢失，回滚后由 `MAX` 重新推算，可能出现单号跳号。

**回滚语句实测结论**：在临时库 `firstsun_rollback_test` 上与脚本 3 的回滚一并执行，
`DROP TABLE IF EXISTS ph_po_doc_seq` **执行成功**，回滚后查 `information_schema.TABLES` 该表已不存在（计数 0）。

---

## 五、幂等性验证结果（B 侧实测）

两个脚本均在真实 MySQL 8.0.46 上**重复执行**验证：

```text
20260917_b_purchase_order_reject.sql   首次 18 条语句 0 失败；二次执行 18 条语句 0 失败
20260917_b_purchase_doc_seq.sql        首次  7 条语句 0 失败；二次执行  7 条语句 0 失败
```

重复执行后的状态未被破坏：

| 校验项 | 结果 |
| --- | --- |
| `ph_po_order` 驳回列数 | 3 |
| `ck_po_order_status` | `` (`status` in (-(1),0,1,2,3,4,5,6)) `` |
| 「已驳回」字典项数 | 1（未重复插入） |
| `ph_po_doc_seq` 回填值 | 与真实最大流水**逐条一致**，二次执行未被改小 |

### 执行后自检 SQL

```sql
-- 1) 脚本 3 校验
SELECT COUNT(*) AS `驳回列数` FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ph_po_order'
   AND COLUMN_NAME IN ('reject_reason','reject_by','reject_at');          -- 期望 3

SELECT CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS
 WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'ck_po_order_status';  -- 期望含 6

SELECT COUNT(*) AS `已驳回字典项` FROM system_dict_data
 WHERE dict_type = 'pharmacy_po_status' AND value = '6';                  -- 期望 1

-- 2) 脚本 4 校验：序列表回填值应等于「按单号日期段」的真实最大流水
SELECT s.biz_type, s.biz_date, s.next_seq AS `序列表`, x.mx AS `真实最大流水`,
       IF(s.next_seq = x.mx, '一致', '不一致') AS `结论`
  FROM ph_po_doc_seq s
  JOIN (
    SELECT 'RECEIPT' AS t, store_id,
           STR_TO_DATE(SUBSTRING_INDEX(SUBSTRING_INDEX(receipt_no,'-',2),'-',-1),'%Y%m%d') AS d,
           MAX(CAST(SUBSTRING_INDEX(receipt_no,'-',-1) AS UNSIGNED)) AS mx
      FROM ph_po_receipt WHERE receipt_no REGEXP '^GR[0-9]+-[0-9]{8}-[0-9]+$'
     GROUP BY store_id, d
    UNION ALL
    SELECT 'ORDER', store_id, order_date,
           MAX(CAST(SUBSTRING_INDEX(order_no,'-',-1) AS UNSIGNED))
      FROM ph_po_order WHERE order_no REGEXP '^PO[0-9]+-[0-9]{8}-[0-9]+$'
     GROUP BY store_id, order_date
  ) x ON x.t = s.biz_type AND x.store_id = s.store_id AND x.d = s.biz_date
 ORDER BY s.biz_date, s.biz_type;                                          -- 期望全部「一致」
```

---

## 六、对 `firstsun_pharmacy_init.sql` 的建议整合点（供 A 参考）

B **未直接修改**该文件。若 A 决定把本轮变更纳入基线，建议整合以下两处：

**① `ph_po_order` 建表语句**（当前第 255~263 行附近）

```sql
  audit_by        BIGINT DEFAULT NULL COMMENT '审批人',
  audit_at        DATETIME     DEFAULT NULL COMMENT '审批时间',
  -- 新增 ↓
  reject_reason   VARCHAR(500) DEFAULT NULL COMMENT '驳回原因（仅已提交→已驳回时写入）',
  reject_by       BIGINT DEFAULT NULL COMMENT '驳回人员工编号',
  reject_at       DATETIME     DEFAULT NULL COMMENT '驳回时间',
  -- 新增 ↑
  creator         VARCHAR(64)  NULL DEFAULT '' COMMENT '创建者(用户编号/系统标识)',
  ...
  -- 状态：-1取消/0草稿/1提交/2审批/3发出/4部分到货/5完成/6已驳回
  CONSTRAINT ck_po_order_status CHECK (status IN (-1,0,1,2,3,4,5,6)),
```

**② 新增 `ph_po_doc_seq` 建表语句**（放在采购域相关表之后，内容见脚本 4）

**③ 字典数据**：`pharmacy_po_status` 增补 `已驳回 / 6 / danger / sort=8`

---

## 七、需 A 协同的公共装配缺陷（B 已按约定只报告、未修改）

`FacadeFallbackConfiguration` 属**公共 Spring Bean 装配**，经与需求方确认，**主责转 A**。

| 项 | 内容 |
| --- | --- |
| 问题类 1 | `cn.iocoder.yudao.module.pharmacy.config.FacadeFallbackConfiguration` |
| 问题类 2 | `cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryFacadeAdapter`（C 已实现且带 `@Service`） |
| 现象 | 调用 `PUT /pharmacy/purchase/receipt/post` 返回 `code=1029002001`「库存服务未就绪」，即注入的是降级桩 `InventoryFacadeImpl` |
| 复现 | 建证照有效供应商 → 已审批订单 → 建收货单 → `submit` → `post` |
| 影响 | 采购收货永远无法入账，`received_qty` 不累计，订单无法推进到「部分到货/已完成」；**阻断采购域闭环** |
| 期望 | 容器内 `InventoryFacade` 只应有 C 的 `InventoryFacadeAdapter` 一个 Bean；降级桩仅在 C 未提供实现时生效 |
| 根因 | `@ConditionalOnMissingBean` 写在普通 `@Configuration` 的 `@Bean` 方法上不可靠：配置类先于组件扫描被解析，条件评估时看不到 `@Service` 适配器；随后存在两个同类型 Bean，Spring 按 `@Resource` 字段名回退，降级 Bean 方法名 `inventoryFacade` 与注入点字段名同名 → 选中降级桩 |
| 建议改法 | 降级实现迁到独立 `@AutoConfiguration`（配 `AutoConfiguration.imports`），或给 C 的实现加 `@Primary` |

> **待 A 修复后**，B 将补充 B-1 场景的完整 HTTP 端到端证据（首次收 40 真实入账 → 再收 60 真实入账 → 订单状态推进到「已完成」）。

---

## 八、相关代码与文档索引

| 类型 | 路径 |
| --- | --- |
| 迁移脚本 3 | `sql/migrations/20260917_b_purchase_order_reject.sql` |
| 迁移脚本 4 | `sql/migrations/20260917_b_purchase_doc_seq.sql` |
| 序列 DO / Mapper / Service | `backend/yudao-module-pharmacy/.../dal/dataobject/purchase/PurchaseDocSeqDO.java`、`.../dal/mysql/purchase/PurchaseDocSeqMapper.java`、`.../service/purchase/PurchaseDocSeqService.java` |
| 序列类型枚举 | `backend/yudao-module-pharmacy/.../enums/PurchaseDocSeqTypeEnum.java` |
| 驳回请求 VO | `backend/yudao-module-pharmacy/.../controller/admin/purchase/vo/order/PurchaseOrderRejectReqVO.java` |
| B 修复报告 | 本轮交付说明（含完整根因、测试结果与验证证据） |
