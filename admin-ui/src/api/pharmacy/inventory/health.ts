import request from '@/config/axios'

export interface ExpiryQuery extends PageParam {
  warehouseId?: number
  drugId?: number
  batchId?: number
  alertLevel?: number
  handleType?: number
  alertDate?: string
}

export interface ExpiryAlertVO {
  id: number
  batchId: number
  drugId: number
  warehouseId: number
  batchNo: string
  expiryDate: string
  qtyTotal: number
  qtyAvail: number
  qtyFrozen: number
  alertLevel: number
  expireDays: number
  handleType: number
  handleBy?: number
  handleAt?: string
  alertDate: string
}

export interface ReconciliationQuery extends PageParam {
  warehouseId?: number
  drugId?: number
  onlyDifference?: boolean
}

export interface ReconciliationVO {
  batchId: number
  warehouseId: number
  drugId: number
  batchNo: string
  batchTotal: number
  batchAvail: number
  batchFrozen: number
  locationTotal: number
  locationFrozen: number
  flowNet: number
  openingFlowCount: number
  activeLockQty: number
  lastFlowId?: number
  locationMatches: boolean
  flowMatches: boolean
  frozenMatches: boolean
  openingBaselinePresent: boolean
  difference: boolean
}

export const EXPIRY_HANDLE_TYPES: Record<number, string> = {
  0: '未处理',
  1: '促销',
  2: '退货',
  3: '报损',
  4: '继续销售（店长确认）'
}

export const getExpiryPage = (
  params: ExpiryQuery
): Promise<{ list: ExpiryAlertVO[]; total: number }> =>
  request.get({ url: '/pharmacy/inventory/expiry/page', params })

export const refreshExpiry = (): Promise<{ alertDate: string; affectedRows: number }> =>
  request.post({ url: '/pharmacy/inventory/expiry/refresh' })

export const handleExpiry = (id: number, handleType: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/expiry/handle', data: { id, handleType } })

export const getReconciliationPage = (
  params: ReconciliationQuery
): Promise<{ list: ReconciliationVO[]; total: number }> =>
  request.get({ url: '/pharmacy/inventory/reconciliation/page', params })
