# 药店模块迁移脚本执行顺序（A 成员维护）

## 采购与库存增量迁移顺序

首次初始化时，`docker-compose.yml` 按以下依赖顺序挂载并执行：

1. `20260912_b_purchase_supplier_menu.sql`
2. `20260913_b_purchase_order_receipt_menu.sql`
3. `20260917_b_purchase_order_reject.sql`
4. `20260917_b_purchase_doc_seq.sql`
5. `20260914_k_pharmacy_inventory_menu.sql` 及其后续库存基础迁移
6. `20260914_l_pharmacy_demo_data.sql`
7. `20260918_l_fix_demo_location_163032.sql`

采购驳回迁移以信息_schema 检查列和约束后再变更，采购单号序列表使用单号中的业务日期回填，并以 `GREATEST` 保证重复执行只增不减。演示货位迁移只更新仍处于错误仓库且未删除的目标货位，重复执行无副作用。已有持久化数据库不会自动重跑 init 目录脚本，应按上述顺序手工执行尚未落地的幂等迁移。

`20260922_f_pharmacy_wechat_identity.sql` 只创建药店微信身份绑定表，不写入身份数据。启用后端微信登录前，需在后端受保护配置中提供 `PHARMACY_WECHAT_TENANT_ID` 和 `PHARMACY_WECHAT_APP_ID`，并保证后者与现有 `wx.miniapp.appid` 一致；缺任一配置时接口拒绝登录。本文件尚未在本轮执行。

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
| 8 | `20260909_h_pharmacy_barcode_menu_icon_fix.sql` | 修复药品条码菜单使用不存在的 `ep:barcode` 导致图标空白 |
| 9 | `20260908_d_pos_menu.sql` | D 模块 POS 管理菜单与按钮权限 |
| 10 | `20260908_d_pos_menu_bind_role.sql` | 将 POS 菜单绑定至超级管理员角色 |
| 11 | `20260909_d_pos_tables_fix.sql` | POS 销售与退货明细表兼容性修正 |
| 12 | `fix_pos_menu_name.sql` | 修正 POS 菜单中文名称 |
| 13 | `20260912_b_purchase_supplier_menu.sql` | B 模块采购管理、供应商与供应商证照菜单及字典 |
| 14 | `20260913_b_purchase_order_receipt_menu.sql` | B 模块采购订单、采购收货菜单及字典 |
| 15 | `20260917_b_purchase_order_reject.sql` | B：采购订单驳回字段、状态约束与字典 |
| 16 | `20260917_b_purchase_doc_seq.sql` | B：采购订单与收货单并发流水序列表 |
| 17 | `20260914_k_pharmacy_inventory_menu.sql` | C 模块库存管理菜单与全部库存按钮权限 |
| 18 | `20260911_f_pharmacy_member_menu.sql` | F 模块会员管理菜单、按钮权限与字典 |
| 19 | `20260910_i_firstsun_shared_account.sql` | 创建 FirstSun 药店测试租户与共享账号，并汇总药店菜单权限 |
| 20 | `20260911_j_pos_menu_path_fix.sql` | 将 POS 一级菜单改为 `/pharmacy-pos`，避免与药店业务路由冲突 |
| 21 | `20260914_l_pharmacy_demo_data.sql` | 为 FirstSun 租户写入覆盖基础资料、采购、库存、POS 与会员页面的关联演示数据 |
| 22 | `20260914_m_pharmacy_rx_pay_menu.sql` | E 模块处方登记/审核/台账、支付单/退款单查询菜单与按钮权限 |
| 23 | `20260914_n_pay_app_init.sql` | E 模块支付应用（app_key=firstsun）与模拟渠道（mock）初始化 |
| 24 | `20260915_c_inventory_facade_flow_ref.sql` | C 库存门面：销售退货关联原销售出库流水，支持并发下的累计回补校验 |
| 25 | `20260916_f_wx_order_line_alloc.sql` | F：小程序订单行分配信息 |
| 26 | `20260916_f_wx_order_alloc_frozen_stage.sql` | F：小程序订单冻结分配阶段 |
| 27 | `20260916_f_wx_order_alloc_out_ref.sql` | F：小程序订单出库引用 |
| 28 | `20260917_f_member_address_update_menu.sql` | F：会员地址更新菜单与权限 |
| 29 | `20260918_f_member_sms_code.sql` | F：会员短信验证码表 |
| 30 | `20260918_f_wx_order_points.sql` | F：小程序订单积分字段与约束 |
| 31 | `20260922_f_pharmacy_wechat_identity.sql` | F：药店微信身份绑定表；执行前备份，应用启动前按需手工执行 |
| 32 | `20260917_c_inventory_shared_role_menu.sql` | C-1：共享租户套餐、角色167与上架移位权限 |
| 33 | `20260918_c_inventory_flow_comment_utf8.sql` | C-3：纠正库存流水原出库引用列中文注释 |
| 34 | `20260918_l_fix_demo_location_163032.sql` | E：修复演示数据货位关联 |
| 35 | `20260916_c_inventory_movement_menu.sql` | C/P1 同仓货位移位权限 |
| 36 | `20260917_o_pharmacy_ai_prototype.sql` | AI 助手原型表、权限与角色授权 |
| 37 | `20260918_a_ai_menu_fix.sql` | A：恢复采购管理目录并迁移 AI 权限 |

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
  "20260909_g_pharmacy_role_menu_tenant_fix.sql",
  "20260909_h_pharmacy_barcode_menu_icon_fix.sql",
  "20260908_d_pos_menu.sql",
  "20260908_d_pos_menu_bind_role.sql",
  "20260909_d_pos_tables_fix.sql",
  "fix_pos_menu_name.sql",
  "20260912_b_purchase_supplier_menu.sql",
  "20260913_b_purchase_order_receipt_menu.sql",
  "20260914_k_pharmacy_inventory_menu.sql",
  "20260911_f_pharmacy_member_menu.sql",
  "20260910_i_firstsun_shared_account.sql",
  "20260911_j_pos_menu_path_fix.sql",
  "20260914_l_pharmacy_demo_data.sql",
  "20260914_m_pharmacy_rx_pay_menu.sql",
  "20260914_n_pay_app_init.sql",
  "20260915_c_inventory_facade_flow_ref.sql",
  "20260916_c_inventory_movement_menu.sql",
  "20260917_b_purchase_order_reject.sql",
  "20260917_b_purchase_doc_seq.sql",
  "20260917_c_inventory_shared_role_menu.sql",
  "20260918_c_inventory_flow_comment_utf8.sql",
  "20260916_f_wx_order_line_alloc.sql",
  "20260916_f_wx_order_alloc_frozen_stage.sql",
  "20260916_f_wx_order_alloc_out_ref.sql",
  "20260917_f_member_address_update_menu.sql",
  "20260918_f_member_sms_code.sql",
  "20260918_f_wx_order_points.sql",
  "20260922_f_pharmacy_wechat_identity.sql",
  "20260918_l_fix_demo_location_163032.sql",
  "20260917_o_pharmacy_ai_prototype.sql",
  "20260918_a_ai_menu_fix.sql"
)
foreach ($s in $scripts) {
  docker cp "sql/migrations/$s" firstsun-pharmacy-mysql:/tmp/mig.sql
  docker exec firstsun-pharmacy-mysql mysql -uroot -proot123456 --default-character-set=utf8mb4 -D firstsun_pharmacy -e "source /tmp/mig.sql"
}
```

执行 C-1 后，已有运行中的后端应只清理该按钮的 Redis 缓存，使后端重新加载角色授权；不要执行 `FLUSHDB`：

```powershell
docker exec firstsun-pharmacy-redis redis-cli DEL "menu_role_ids:163:22229" "permission_menu_ids:pharmacy:inventory-movement:execute"
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
- 团队共享开发账号为租户 `FirstSun`、用户名 `0407`、密码 `123456`，并已绑定演示门店和员工身份。
- 其他角色需手动分配对应权限。
