import request from '@/config/axios'

/** 采购收货单 VO */
export interface PurchaseReceiptVO {
  id?: number
  receiptNo?: string
  orderId?: number
  orderNo?: string
  storeId?: number
  storeName?: string
  warehouseId?: number
  receiveBy?: number
  receiveByName?: string
  /** 收货时间：yudao 全局把 LocalDateTime 映射为毫秒时间戳（表单用 value-format="x" 提交数字） */
  receiveDate?: number | string
  /** 服务端重算字段，前端只读 */
  totalQty?: number
  totalAmount?: number
  /** 差异标记 0无/1数量差异/2价格差异（后端计算，只读） */
  diffType?: number
  isFreeReceipt?: number
  /** 收货单状态 0待提交/1已提交/2已入账/3已作废（后端维护） */
  status?: number
  /** 质检结果 0未检/1合格/2有异常（后端计算，只读） */
  qualityStatus?: number
  /** 入账时间：毫秒时间戳（只读） */
  postedAt?: number
  /** 创建时间：毫秒时间戳（只读） */
  createTime?: number
}

/** 采购收货明细 VO */
export interface PurchaseReceiptLineVO {
  id?: number
  receiptId?: number
  lineNo?: number
  orderLineId?: number
  drugId?: number
  /** 药品编码/名称/规格/单位（后端回填，只读） */
  drugCode?: string
  drugName?: string
  specification?: string
  unit?: string
  batchNo?: string
  manufactureDate?: string
  expiryDate?: string
  qty?: number
  unitPrice?: number
  /** 金额（后端重算，只读） */
  amount?: number
  locationId?: number
  qualityFlag?: number
  qaRemark?: string
  coldChainTemp?: number
  /** 入账生成的批次编号（库存服务回写，只读） */
  createBatchId?: number
}

/** 收货明细提交载荷：只提交可编辑字段 */
export interface PurchaseReceiptLineCreateVO {
  lineNo?: number
  orderLineId?: number
  drugId: number
  batchNo: string
  manufactureDate?: string
  expiryDate: string
  qty: number
  unitPrice: number
  locationId?: number
  qualityFlag?: number
  qaRemark?: string
  coldChainTemp?: number
}

/**
 * 收货单创建载荷：剔除 id 与后端计算字段
 * （receiptNo/金额/diffType/qualityStatus/status/postedAt 由后端生成或计算）
 */
export type PurchaseReceiptCreateVO = Omit<
  PurchaseReceiptVO,
  | 'id'
  | 'receiptNo'
  | 'orderNo'
  | 'storeName'
  | 'receiveBy'
  | 'receiveByName'
  | 'totalQty'
  | 'totalAmount'
  | 'diffType'
  | 'status'
  | 'qualityStatus'
  | 'postedAt'
  | 'createTime'
> & { lines: PurchaseReceiptLineCreateVO[] }

/** 收货单修改载荷：创建载荷 + id */
export type PurchaseReceiptUpdateVO = PurchaseReceiptCreateVO & { id: number }

/** 收货单详情 VO */
export type PurchaseReceiptDetailVO = PurchaseReceiptVO & { lines: PurchaseReceiptLineVO[] }

// 查询收货单分页
export const getPurchaseReceiptPage = (params: PageParam) => {
  return request.get({ url: '/pharmacy/purchase/receipt/page', params })
}

// 查询收货单详情（含明细）
export const getPurchaseReceipt = (id: number) => {
  return request.get({ url: '/pharmacy/purchase/receipt/get?id=' + id })
}

// 新增收货单
export const createPurchaseReceipt = (data: PurchaseReceiptCreateVO) => {
  return request.post({ url: '/pharmacy/purchase/receipt/create', data })
}

// 修改收货单（仅待提交）
export const updatePurchaseReceipt = (data: PurchaseReceiptUpdateVO) => {
  return request.put({ url: '/pharmacy/purchase/receipt/update', data })
}

// 删除收货单（仅待提交或已作废）
export const deletePurchaseReceipt = (id: number) => {
  return request.delete({ url: '/pharmacy/purchase/receipt/delete?id=' + id })
}

// 提交收货单：待提交 → 已提交
export const submitPurchaseReceipt = (id: number) => {
  return request.put({ url: '/pharmacy/purchase/receipt/submit?id=' + id })
}

// 收货入账：已提交 → 已入账（调用库存服务，重复调用会被拒绝）
export const postPurchaseReceipt = (id: number) => {
  return request.put({ url: '/pharmacy/purchase/receipt/post?id=' + id })
}

// 作废收货单：待提交 → 已作废
export const voidPurchaseReceipt = (id: number) => {
  return request.put({ url: '/pharmacy/purchase/receipt/void?id=' + id })
}

// 导出收货单
export const exportPurchaseReceipt = (params: any) => {
  return request.download({ url: '/pharmacy/purchase/receipt/export-excel', params })
}

