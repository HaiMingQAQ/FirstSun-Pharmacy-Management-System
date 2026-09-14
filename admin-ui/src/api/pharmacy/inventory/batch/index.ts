import request from '@/config/axios'

export interface BatchVO {
  id: number
  warehouseId: number
  warehouseName: string
  drugId: number
  batchNo: string
  manufactureDate: string | null
  expiryDate: string
  qtyTotal: number
  qtyAvail: number
  qtyFrozen: number
  qtySold: number
  qualityStatus: number
}

export interface StockQuery extends PageParam {
  warehouseId: number
  drugId?: number
  batchNo?: string
  locationId?: number
  qualityStatus?: number
  expiryFrom?: string
  expiryTo?: string
}

export interface LocationStockVO {
  id: number
  batchId: number
  batchNo: string
  warehouseId: number
  locationId: number
  locationCode: string
  drugId: number
  qty: number
  qtyFrozen: number
  qtyAvail: number
}

export const getBatchPage = (params: StockQuery): Promise<{ list: BatchVO[]; total: number }> =>
  request.get({ url: '/pharmacy/inventory/batch/page', params })

export const getBatch = (id: number): Promise<BatchVO> =>
  request.get({ url: '/pharmacy/inventory/batch/get', params: { id } })

export const getLocationStockPage = (
  params: StockQuery
): Promise<{ list: LocationStockVO[]; total: number }> =>
  request.get({ url: '/pharmacy/inventory/batch/location-stock', params })

export type ExpiryDayPolicy = 'ALLOW_ON_EXPIRY_DATE' | 'BLOCK_ON_EXPIRY_DATE'

export interface FefoPreviewQuery {
  warehouseId: number
  drugId: number
  quantity: number
  expiryDayPolicy: ExpiryDayPolicy
}

export interface FefoPreview {
  evaluatedAt: string
  expiryDayPolicy: ExpiryDayPolicy
  reserved: false
  allocations: { batchId: number; locationId: number; quantity: number }[]
}

/** Simulation only: this response cannot be used as a stock reservation or sale authorization. */
export const previewFefo = (params: FefoPreviewQuery): Promise<FefoPreview> =>
  request.get({ url: '/pharmacy/inventory/batch/fefo-preview', params })
