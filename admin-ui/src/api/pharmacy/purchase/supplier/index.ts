import request from '@/config/axios'

/** 供应商 VO */
export interface SupplierVO {
  id?: number
  supplierCode: string
  supplierName: string
  creditCode?: string
  scopeCode?: string
  contact?: string
  phone?: string
  address?: string
  bankName?: string
  /** 银行账号原文：仅详情接口返回，供编辑表单回填 */
  bankAccount?: string
  /** 银行账号掩码：列表接口返回（NFR-12） */
  bankAccountMasked?: string
  paymentTerms?: string
  defaultDiscount?: number
  /** 首营审核状态 0待审/1通过/2驳回（后端维护） */
  approveStatus?: number
  status: number
  /** 审核员工编号（后端维护，前端只读） */
  auditBy?: number
  /** 审核时间：毫秒时间戳（后端维护，前端只读） */
  auditAt?: number
  /** 审核意见（由审核接口维护，前端只读） */
  auditOpinion?: string
  /** 创建时间：毫秒时间戳（只读） */
  createTime?: number
}

/**
 * 供应商创建载荷：剔除 id 与所有后端维护字段
 * （后端 SupplierSaveReqVO 不接收 approveStatus/auditBy/auditAt/auditOpinion，由独立审核接口维护）
 */
export type SupplierCreateVO = Omit<
  SupplierVO,
  | 'id'
  | 'bankAccountMasked'
  | 'approveStatus'
  | 'auditBy'
  | 'auditAt'
  | 'auditOpinion'
  | 'createTime'
>

/** 供应商修改载荷：创建载荷 + id */
export type SupplierUpdateVO = SupplierCreateVO & { id: number }

/** 供应商精简 VO（下拉选择） */
export interface SupplierSimpleVO {
  id: number
  supplierCode: string
  supplierName: string
  defaultDiscount?: number
}

// 查询供应商分页
export const getSupplierPage = (params: PageParam) => {
  return request.get({ url: '/pharmacy/purchase/supplier/page', params })
}

// 查询供应商详情（返回完整银行账号）
export const getSupplier = (id: number) => {
  return request.get({ url: '/pharmacy/purchase/supplier/get?id=' + id })
}

// 新增供应商
export const createSupplier = (data: SupplierCreateVO) => {
  return request.post({ url: '/pharmacy/purchase/supplier/create', data })
}

// 修改供应商
export const updateSupplier = (data: SupplierUpdateVO) => {
  return request.put({ url: '/pharmacy/purchase/supplier/update', data })
}

// 删除供应商
export const deleteSupplier = (id: number) => {
  return request.delete({ url: '/pharmacy/purchase/supplier/delete?id=' + id })
}

// 供应商首营审核：approveStatus 1通过 / 2驳回
export const approveSupplier = (id: number, approveStatus: number, auditOpinion?: string) => {
  return request.put({
    url: '/pharmacy/purchase/supplier/approve',
    params: { id, approveStatus, auditOpinion }
  })
}

// 获取可采购供应商精简列表（启用 + 首营通过），用于采购订单/收货单下拉
export const getSimpleSupplierList = (keyword?: string) => {
  return request.get({ url: '/pharmacy/purchase/supplier/simple-list', params: { keyword } })
}

// 获取全部供应商精简列表（不限启停与审核状态），用于证照登记与筛选
export const getAllSupplierList = () => {
  return request.get({ url: '/pharmacy/purchase/supplier/all-simple-list' })
}

// 导出供应商
export const exportSupplier = (params: any) => {
  return request.download({ url: '/pharmacy/purchase/supplier/export-excel', params })
}

