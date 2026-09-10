import request from '@/config/axios'

export interface DrugVO {
  id?: number
  drugCode: string
  /** 所属分类编号（表单初始态可为空，提交由前端校验 + 后端 @NotNull 兜底） */
  categoryId?: number
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
  remark: string
  imageUrl: string
  images: string[]
  description: string
  instructionsUrl: string
  approveStatus?: number
  /** 审核员工编号（后端维护，前端只读，不随保存接口回传） */
  auditBy?: number
  /** 审核时间（后端维护，前端只读） */
  auditAt?: Date
  /** 审核意见（由审核接口维护，前端只读） */
  auditOpinion?: string
}

/**
 * 药品创建载荷：剔除 id 与所有后端维护的审核字段
 * （后端 DrugSaveReqVO 不接收 approveStatus/auditBy/auditAt/auditOpinion，由独立审核接口维护）
 */
export type DrugCreateVO = Omit<
  DrugVO,
  'id' | 'approveStatus' | 'auditBy' | 'auditAt' | 'auditOpinion'
>

/** 药品修改载荷：创建载荷 + id */
export type DrugUpdateVO = DrugCreateVO & { id: number }

// 查询药品分页
export const getDrugPage = (params: PageParam) => {
  return request.get({ url: '/pharmacy/base/drug/page', params })
}

// 查询药品详情
export const getDrug = (id: number) => {
  return request.get({ url: '/pharmacy/base/drug/get?id=' + id })
}

// 新增药品
export const createDrug = (data: DrugCreateVO) => {
  return request.post({ url: '/pharmacy/base/drug/create', data })
}

// 修改药品
export const updateDrug = (data: DrugUpdateVO) => {
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
