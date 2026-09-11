import request from '@/config/axios'

/** 采购订单 VO */
export interface PurchaseOrderVO {
  id?: number
  orderNo?: string
  storeId?: number
  storeName?: string
  warehouseId?: number
  supplierId?: number
  supplierName?: string
  /** 下单日期：响应为 LocalDate 数组 [y,m,d]，表单绑定用 YYYY-MM-DD 字符串 */
  orderDate?: string | number[]
  /** 预计到货：同上 */
  expectDate?: string | number[]
  /** 服务端重算字段，前端只读 */
  totalQty?: number
  totalAmount?: number
  discountAmount?: number
  payableAmount?: number
  /** 订单状态 -1取消/0草稿/1提交/2审批/3发出/4部分到货/5完成（后端维护） */
  status?: number
  isAuto?: number
  remark?: string
  /** 审批人（后端维护，前端只读） */
  auditBy?: number
  /** 审批时间：毫秒时间戳（后端维护，前端只读） */
  auditAt?: number
  /** 创建时间：毫秒时间戳（只读） */
  createTime?: number
}

/** 采购订单明细 VO */
export interface PurchaseOrderLineVO {
  id?: number
  orderId?: number
  lineNo?: number
  drugId?: number
  /** 药品编码/名称/规格/单位（后端回填，只读） */
  drugCode?: string
  drugName?: string
  specification?: string
  unit?: string
  orderQty?: number
  /** 已收数量（收货入账累计，后端维护） */
  receivedQty?: number
  unitPrice?: number
  discountRate?: number
  /** 行金额（后端重算，只读） */
  lineAmount?: number
  remark?: string
}

/** 采购明细提交载荷：只提交可编辑字段 */
export interface PurchaseOrderLineCreateVO {
  lineNo?: number
  drugId: number
  orderQty: number
  unitPrice: number
  discountRate?: number
  remark?: string
}

/**
 * 采购订单创建载荷：剔除 id 与所有后端维护字段
 * （orderNo/金额/status/auditBy/auditAt 由后端生成或重算）
 */
export type PurchaseOrderCreateVO = Omit<
  PurchaseOrderVO,
  | 'id'
  | 'orderNo'
  | 'storeName'
  | 'supplierName'
  | 'totalQty'
  | 'totalAmount'
  | 'discountAmount'
  | 'payableAmount'
  | 'status'
  | 'auditBy'
  | 'auditAt'
  | 'createTime'
> & { lines: PurchaseOrderLineCreateVO[] }

/** 采购订单修改载荷：创建载荷 + id */
export type PurchaseOrderUpdateVO = PurchaseOrderCreateVO & { id: number }

/** 采购订单详情 VO */
export type PurchaseOrderDetailVO = PurchaseOrderVO & { lines: PurchaseOrderLineVO[] }

// 查询采购订单分页
export const getPurchaseOrderPage = (params: PageParam) => {
  return request.get({ url: '/pharmacy/purchase/order/page', params })
}

// 查询采购订单详情（含明细）
export const getPurchaseOrder = (id: number) => {
  return request.get({ url: '/pharmacy/purchase/order/get?id=' + id })
}

// 新增采购订单
export const createPurchaseOrder = (data: PurchaseOrderCreateVO) => {
  return request.post({ url: '/pharmacy/purchase/order/create', data })
}

// 修改采购订单（仅草稿）
export const updatePurchaseOrder = (data: PurchaseOrderUpdateVO) => {
  return request.put({ url: '/pharmacy/purchase/order/update', data })
}

// 删除采购订单（仅草稿且无收货记录）
export const deletePurchaseOrder = (id: number) => {
  return request.delete({ url: '/pharmacy/purchase/order/delete?id=' + id })
}

// 提交审批：草稿 → 已提交
export const submitPurchaseOrder = (id: number) => {
  return request.put({ url: '/pharmacy/purchase/order/submit?id=' + id })
}

// 审批通过：已提交 → 已审批
export const approvePurchaseOrder = (id: number) => {
  return request.put({ url: '/pharmacy/purchase/order/approve?id=' + id })
}

// 标记已发出：已审批 → 已发出
export const issuePurchaseOrder = (id: number) => {
  return request.put({ url: '/pharmacy/purchase/order/issue?id=' + id })
}

// 取消订单：草稿/已提交/已审批 → 已取消
export const cancelPurchaseOrder = (id: number) => {
  return request.put({ url: '/pharmacy/purchase/order/cancel?id=' + id })
}

// 导出采购订单
export const exportPurchaseOrder = (params: any) => {
  return request.download({ url: '/pharmacy/purchase/order/export-excel', params })
}

