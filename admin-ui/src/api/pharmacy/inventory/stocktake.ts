import request from '@/config/axios'

export interface StocktakeQuery extends PageParam {
  warehouseId?: number
  status?: number
}

export interface StocktakeSummary {
  id: number
  stocktakeNo: string
  warehouseId: number
  stocktakeType: number
  blindFlag: number
  freezeFlag: number
  totalItem: number
  doneItem: number
  status: number
  initiatorId: number
  auditBy?: number
  auditAt?: string
  adjustedAt?: string
  createTime: string
}

export interface StocktakeLine {
  id: number
  stocktakeId: number
  batchId: number
  locationId: number
  drugId: number
  batchNo: string
  bookQty: number | null
  realQty: number | null
  diffQty: number
  diffFlag: number
  scannedFlag: number
}

export interface StocktakeDetail {
  summary: StocktakeSummary
  lines: StocktakeLine[]
}

export const STOCKTAKE_STATUS: Record<number, string> = {
  0: '草稿',
  1: '进行中',
  2: '待审批',
  3: '已调整'
}

export const getStocktakePage = (
  params: StocktakeQuery
): Promise<{ list: StocktakeSummary[]; total: number }> =>
  request.get({ url: '/pharmacy/inventory/stocktake/page', params })

export const getStocktake = (id: number): Promise<StocktakeDetail> =>
  request.get({ url: '/pharmacy/inventory/stocktake/get', params: { id } })

export const createStocktake = (data: {
  warehouseId: number
  stocktakeType: number
  blindFlag: number
  freezeFlag: number
  scope?: string
}): Promise<number> => request.post({ url: '/pharmacy/inventory/stocktake/create', data })

export const startStocktake = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/stocktake/start', params: { id } })

export const recordStocktake = (data: {
  stocktakeId: number
  lineId: number
  realQty: number
}): Promise<boolean> => request.post({ url: '/pharmacy/inventory/stocktake/record', data })

export const completeStocktake = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/stocktake/complete', params: { id } })

export const approveStocktake = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/stocktake/approve', params: { id } })

export const cancelStocktake = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/stocktake/cancel', params: { id } })
