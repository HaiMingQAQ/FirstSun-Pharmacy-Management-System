import request from '@/config/axios'

export interface EmployeeVO {
  id?: number
  empNo: string
  empName: string
  phone?: string
  storeId: number
  position: number
  pharmacistNo?: string
  licenseExpire?: string
  healthCertExpire?: string
  hireDate?: string
  status: number
  userId?: number
}

// 查询员工分页
export const getEmployeePage = (params: PageParam) => {
  return request.get({ url: '/pharmacy/base/employee/page', params })
}

// 查询员工详情
export const getEmployee = (id: number) => {
  return request.get({ url: '/pharmacy/base/employee/get?id=' + id })
}

// 新增员工
export const createEmployee = (data: EmployeeVO) => {
  return request.post({ url: '/pharmacy/base/employee/create', data })
}

// 修改员工
export const updateEmployee = (data: EmployeeVO) => {
  return request.put({ url: '/pharmacy/base/employee/update', data })
}

// 删除员工
export const deleteEmployee = (id: number) => {
  return request.delete({ url: '/pharmacy/base/employee/delete?id=' + id })
}

// 导出员工
export const exportEmployee = (params: any) => {
  return request.download({ url: '/pharmacy/base/employee/export-excel', params })
}
