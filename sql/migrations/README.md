# 药店模块迁移脚本执行顺序（A 成员维护）

> 所有脚本均使用 `ON DUPLICATE KEY UPDATE` 或先删后插，**幂等可重复执行**。
> 字典 type 已与后端 `DictTypeConstants.java`、前端 `utils/dict.ts` 保持一致。
> 菜单 ID 段 22000+，字典类型 ID 300+，字典数据 ID 1500+，避免与 system 已有数据冲突。

## 执行顺序

| 顺序 | 文件 | 说明 |
|---|---|---|
| 1 | `20260908_a_pharmacy_module_init.sql` | 模块骨架：一级目录"药店业务"+ 二级目录"基础资料"+ 药品分类菜单 + 字典 `pharmacy_status`、`pharmacy_category_type` |
| 2 | `20260908_b_pharmacy_store_menu.sql` | 门店菜单 + 字典 `pharmacy_yes_no` |
| 3 | `20260908_c_pharmacy_employee_menu.sql` | 员工菜单 + 字典 `pharmacy_employee_status`、`pharmacy_employee_position` |
| 4 | `20260908_d_pharmacy_drug_menu.sql` | 药品档案菜单（含审核按钮）+ 字典 `pharmacy_drug_type`、`pharmacy_insurance_type`、`pharmacy_storage_cond`、`pharmacy_drug_approve_status` |
| 5 | `20260908_e_pharmacy_barcode_menu.sql` | 药品条码菜单 + 字典 `pharmacy_barcode_type` |
| 6 | `20260909_f_pharmacy_menu_path_fix.sql` | 修复 22010「基础资料」二级目录 path 前导 `/` 导致前端动态路由 404 |
| 7 | `20260909_g_pharmacy_role_menu_tenant_fix.sql` | 修复 system_role_menu 33 条关系 tenant_id=0 → 1，幂等校验含 tenant_id |

## 统一执行方法

所有脚本幂等，可重复执行。**必须指定 `--default-character-set=utf8mb4`** 防止中文乱码。

```powershell
# 拉取最新迁移后，按顺序执行（替换库名和密码为本机配置）
$scripts = @(
  "20260908_a_pharmacy_module_init.sql",
  "20260908_b_pharmacy_store_menu.sql",
  "20260908_c_pharmacy_employee_menu.sql",
  "20260908_d_pharmacy_drug_menu.sql",
  "20260908_e_pharmacy_barcode_menu.sql",
  "20260909_f_pharmacy_menu_path_fix.sql",
  "20260909_g_pharmacy_role_menu_tenant_fix.sql"
)
foreach ($s in $scripts) {
  docker cp "sql/migrations/$s" firstsun-pharmacy-mysql:/tmp/mig.sql
  docker exec firstsun-pharmacy-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -D firstsun_yudao_test_20260908 -e "source /tmp/mig.sql"
}
```

执行后清 Redis 菜单缓存使后端重新加载权限：

```powershell
docker exec firstsun-pharmacy-redis redis-cli FLUSHDB
```

## 权限标识清单

### 菜单（type=2）
| 菜单 ID | 名称 | 路径 | 组件 | 标识 |
|---|---|---|---|---|
| 22000 | 药店业务 | /pharmacy | - | - |
| 22010 | 基础资料 | base | - | - |
| 22011 | 药品分类 | category | pharmacy/base/category/index | - |
| 22020 | 门店管理 | store | pharmacy/base/store/index | - |
| 22030 | 员工管理 | employee | pharmacy/base/employee/index | - |
| 22040 | 药品档案 | drug | pharmacy/base/drug/index | - |
| 22050 | 药品条码 | barcode | pharmacy/base/barcode/index | - |

### 按钮（type=3，权限标识）
| 模块 | query | create | update | delete | export | approve |
|---|---|---|---|---|---|---|
| 药品分类 | ✓ | ✓ | ✓ | ✓ | ✓ | - |
| 门店管理 | ✓ | ✓ | ✓ | ✓ | ✓ | - |
| 员工管理 | ✓ | ✓ | ✓ | ✓ | ✓ | - |
| 药品档案 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 药品条码 | ✓ | ✓ | ✓ | ✓ | ✓ | - |

## 字典清单

| 字典 type | 名称 | 取值 |
|---|---|---|
| pharmacy_status | 药店通用启停 | 1=启用 / 0=停用 |
| pharmacy_category_type | 药品分类类型 | 0药品/1保健品/2医疗器械/3中药饮片/4日化/5其他 |
| pharmacy_yes_no | 通用是否 | 1=是 / 0=否 |
| pharmacy_employee_status | 员工在职状态 | 1在职/0离职/2休假 |
| pharmacy_employee_position | 员工岗位 | 1店长/2药师/3收银员/4库管员/5采购/6财务/9系统管理员 |
| pharmacy_drug_type | 药品类型 | 0处方/1OTC甲/2OTC乙/3特管/4饮片/5保健/6器械/7日化/8其他 |
| pharmacy_insurance_type | 医保类别 | 0自费/1甲类/2乙类 |
| pharmacy_storage_cond | 储存条件 | 0常温/1阴凉/2冷藏/3冷冻 |
| pharmacy_drug_approve_status | 药品审核状态 | 0待审/1通过/2驳回 |
| pharmacy_barcode_type | 条码类型 | 0商品条码/1店内码/2追溯码 |

## 角色

- 超管角色（role_id=1，tenant_id=1）已绑定所有上述菜单，便于联调验证。
- 其他角色需手动分配对应权限。
