import request from '@/config/axios'

export interface DamageQuery extends PageParam {
  warehouseId?: number
  status?: number
  damageType?: number
}

export interface DamageSummary {
  id: number
  damageNo: string
  storeId: number
  damageType: number
  reason: number
  totalQty: number
  totalAmount: number
  status: number
  auditBy?: number
  auditAt?: string
  executeBy?: number
  executeAt?: string
  reviewBy?: number
  createTime: string
}

export interface DamageLine {
  id: number
  damageId: number
  batchId: number
  locationId: number
  drugId: number
  batchNo: string
  qty: number
  costPrice: number
  amount: number
  disposeType: number
}

export interface DamageDetail {
  summary: DamageSummary
  lines: DamageLine[]
}

export const DAMAGE_STATUS: Record<number, string> = {
  0: '草稿',
  1: '待审批',
  2: '待复核/执行',
  3: '已执行',
  4: '已取消/驳回'
}

export const getDamagePage = (
  params: DamageQuery
): Promise<{ list: DamageSummary[]; total: number }> =>
  request.get({ url: '/pharmacy/inventory/damage/page', params })

export const getDamage = (id: number): Promise<DamageDetail> =>
  request.get({ url: '/pharmacy/inventory/damage/get', params: { id } })

export const createDamage = (data: {
  damageType: number
  reason: number
  destroyMethod?: string
  destroyCompany?: string
  lines: Array<{ batchId: number; locationId: number; qty: number; disposeType: number }>
}): Promise<number> => request.post({ url: '/pharmacy/inventory/damage/create', data })

export const submitDamage = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/damage/submit', params: { id } })
export const approveDamage = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/damage/approve', params: { id } })
export const rejectDamage = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/damage/reject', params: { id } })
export const reviewDamage = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/damage/review', params: { id } })
export const executeDamage = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/damage/execute', params: { id } })
export const cancelDamage = (id: number): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/damage/cancel', params: { id } })
