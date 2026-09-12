import request from '@/config/axios'

// Existing baseline values only; proposed movement types are intentionally absent.
export const FLOW_TYPES: Record<number, string> = {
  10: '采购入库',
  20: '销售出库',
  21: '销售退货',
  40: '盘点',
  50: '报损',
  70: '期初',
  80: '锁定',
  81: '释放',
  82: '锁定转出库'
}
export const BIZ_TYPES: Record<number, string> = {
  1: '收货',
  2: '销售',
  3: '销售退货',
  4: '盘点',
  5: '报损',
  6: '线上订单',
  7: '期初'
}

export interface FlowVO {
  id: number
  batchId: number
  batchNo: string
  drugId: number
  locationId: number
  flowType: number
  inQty: number
  outQty: number
  frozenDelta: number
  balanceQty: number
  bizType: number
  bizNo: string
  bizLineId: number
  flowTime: string
}

export interface FlowQuery extends PageParam {
  warehouseId?: number
  drugId?: number
  locationId?: number
  batchId?: number
  batchNo?: string
  bizNo?: string
  flowType?: number
  bizType?: number
  flowFrom?: string
  flowBefore?: string
}

export const getFlowPage = (params: FlowQuery): Promise<{ list: FlowVO[]; total: number }> =>
  request.get({ url: '/pharmacy/inventory/flow/page', params })

export const getFlow = (id: number): Promise<FlowVO> =>
  request.get({ url: '/pharmacy/inventory/flow/get', params: { id } })
