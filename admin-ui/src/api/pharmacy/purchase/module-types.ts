/**
 * 采购域用到的「跨模块精简列表」前端类型。
 *
 * ## 为什么单独定义而不复用 `@/api/pharmacy/base/drug` 的 `DrugVO`
 *
 * 采购订单/收货的药品下拉调用的是精简列表接口：
 *
 * ```
 * GET /admin-api/pharmacy/base/drug/simple-list
 * → CommonResult<List<DrugSimpleRespVO>>        (DrugController#getSimpleDrugList)
 * ```
 *
 * 后端 `DrugSimpleRespVO` 只返回 6 个字段，且 `id` 标注为 REQUIRED：
 *
 * ```java
 * private Long id;              // 药品编号（REQUIRED）
 * private String drugCode;
 * private String genericName;
 * private String specification;
 * private String unit;
 * private BigDecimal retailPrice;
 * ```
 *
 * 而 `DrugVO` 是通用药品 VO，字段更多、且把主键声明成了可选（`id?: number`）。
 * 用 `DrugVO` 承接精简列表会有两个问题：
 *
 * 1. **契约不符**：精简列表不返回 `DrugVO` 的多数字段；
 * 2. **主键被误判可空**：`id?: number` 会让 `<el-option :value="drug.id">` 收到
 *    `number | undefined`，而 `el-option` 的 value prop 不接受 undefined，产生 TS2322。
 *
 * 前端 `getSimpleDrugList()` 目前没有声明返回类型（推断为 `any`），因此**赋值处**无法
 * 自动获得正确类型，需在采购域内按后端真实契约显式收窄。
 *
 * 说明：本文件不改动 `@/api/pharmacy/base/drug`（A 负责）。若 A 后续为
 * `getSimpleDrugList` 补上 `Promise<DrugSimpleVO[]>` 返回类型，本文件可移除并改用它。
 */
export interface DrugSimpleVO {
  /** 药品编号（后端主键，必定返回） */
  id: number
  /** 药品编码 */
  drugCode: string
  /** 通用名 */
  genericName: string
  /** 规格 */
  specification?: string
  /** 销售单位 */
  unit?: string
  /** 零售价 */
  retailPrice?: number
}
