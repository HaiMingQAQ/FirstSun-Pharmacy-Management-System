import request from '@/config/axios'

export interface DrugVO {
  id?: number
  drugCode: string
  categoryId: number
  genericName: string
  tradeName?: string
  spellCode?: string
  specification: string
  dosageForm?: string
  manufacturer?: string
  approvalNo?: string
  drugType: number
  isRx: number
  isSpecial: number
  isPseudoephedrine: number
  isColdChain: number
  unit: string
  conversionRatio?: number
  retailPrice: number
  memberPrice?: number
  costPrice?: number
  minSalePrice?: number
  taxRate: number
  insuranceType: number
  minStock: number
  maxStock: number
  storageCond: number
  needExpiry: number
  defaultLocationId?: number
  saleableOnline: number
  status: number
  remark?: string
  imageUrl?: string
  images?: string[]
  description?: string
  instructionsUrl?: string
  approveStatus?: number
}

// 查询药品分页
export const getDrugPage = (params: PageParam) => {
  return request.get({ url: '/pharmacy/base/drug/page', params })
}

// 查询药品详情
export const getDrug = (id: number) => {
  return request.get({ url: '/pharmacy/base/drug/get?id=' + id })
}

// 新增药品
export const createDrug = (data: DrugVO) => {
  return request.post({ url: '/pharmacy/base/drug/create', data })
}

// 修改药品
export const updateDrug = (data: DrugVO) => {
  return request.put({ url: '/pharmacy/base/drug/update', data })
}

// 删除药品
export const deleteDrug = (id: number) => {
  return request.delete({ url: '/pharmacy/base/drug/delete?id=' + id })
}

// 导出药品
export const exportDrug = (params: any) => {
  return request.download({ url: '/pharmacy/base/drug/export-excel', params })
}

// 审核药品
export const approveDrug = (id: number, approveStatus: number, auditOpinion?: string) => {
  return request.put({
    url: '/pharmacy/base/drug/approve',
    params: { id, approveStatus, auditOpinion }
  })
}

// 获取药品精简列表（下拉/条码页面选择药品）
export const getSimpleDrugList = (keyword?: string) => {
  return request.get({ url: '/pharmacy/base/drug/simple-list', params: { keyword } })
}
