import request from '@/config/axios'

/** 供应商证照 VO */
export interface SupplierLicenseVO {
  id?: number
  supplierId?: number
  /** 供应商名称（列表回填，只读） */
  supplierName?: string
  /** 证照类型 0经营许可证/1生产许可证/2GSP证/3营业执照/4其他 */
  licenseType: number
  licenseNo: string
  /** 发证日期：响应为 LocalDate 数组 [y,m,d]，表单绑定用 YYYY-MM-DD 字符串 */
  issueDate?: string | number[]
  /** 到期日：同上 */
  expireDate: string | number[]
  fileUrl?: string
  /** 证照状态 1有效/0过期（后端按到期日计算，前端只读） */
  status?: number
  /** 距到期天数，负数表示已过期（后端计算，只读） */
  daysToExpire?: number
  /** 创建时间：毫秒时间戳（只读） */
  createTime?: number
}

/**
 * 证照创建载荷：剔除 id 与后端计算字段
 * （status 由后端按到期日计算，daysToExpire/supplierName 为回填字段）
 */
export type SupplierLicenseCreateVO = Omit<
  SupplierLicenseVO,
  'id' | 'supplierName' | 'status' | 'daysToExpire' | 'createTime'
>

/** 证照修改载荷：创建载荷 + id */
export type SupplierLicenseUpdateVO = SupplierLicenseCreateVO & { id: number }

// 查询证照分页
export const getLicensePage = (params: PageParam) => {
  return request.get({ url: '/pharmacy/purchase/license/page', params })
}

// 查询证照详情
export const getLicense = (id: number) => {
  return request.get({ url: '/pharmacy/purchase/license/get?id=' + id })
}

// 查询某供应商的全部证照
export const getLicenseListBySupplier = (supplierId: number) => {
  return request.get({ url: '/pharmacy/purchase/license/list-by-supplier?supplierId=' + supplierId })
}

// 查询即将到期的证照（含已过期）
export const getExpiringLicenseList = (days?: number) => {
  return request.get({ url: '/pharmacy/purchase/license/expiring-list', params: { days } })
}

// 新增证照
export const createLicense = (data: SupplierLicenseCreateVO) => {
  return request.post({ url: '/pharmacy/purchase/license/create', data })
}

// 修改证照
export const updateLicense = (data: SupplierLicenseUpdateVO) => {
  return request.put({ url: '/pharmacy/purchase/license/update', data })
}

// 删除证照
export const deleteLicense = (id: number) => {
  return request.delete({ url: '/pharmacy/purchase/license/delete?id=' + id })
}

// 刷新证照有效/过期状态
export const refreshLicenseStatus = () => {
  return request.put({ url: '/pharmacy/purchase/license/refresh-status' })
}

// 导出证照
export const exportLicense = (params: any) => {
  return request.download({ url: '/pharmacy/purchase/license/export-excel', params })
}

