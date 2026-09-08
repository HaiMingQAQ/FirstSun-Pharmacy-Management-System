import request from '@/config/axios'

/** 门店 VO */
export interface StoreVO {
  id?: number
  storeCode: string
  storeName: string
  address: string
  phone?: string
  manageScope?: string
  licenseNo?: string
  licenseExpire?: string
  isMedical: number
  businessHours?: string
  status: number
  deptId?: number
  createTime?: Date
}

/** 门店精简信息 VO（下拉使用） */
export interface StoreSimpleVO {
  id: number
  storeCode: string
  storeName: string
}

// 查询门店分页
export const getStorePage = async (params: PageParam) => {
  return await request.get({ url: '/pharmacy/base/store/page', params })
}

// 获取营业状态的门店精简列表
export const getSimpleStoreList = async (): Promise<StoreSimpleVO[]> => {
  return await request.get({ url: '/pharmacy/base/store/simple-list' })
}

// 查询门店详情
export const getStore = async (id: number) => {
  return await request.get({ url: '/pharmacy/base/store/get?id=' + id })
}

// 新增门店
export const createStore = async (data: StoreVO) => {
  return await request.post({ url: '/pharmacy/base/store/create', data })
}

// 修改门店
export const updateStore = async (data: StoreVO) => {
  return await request.put({ url: '/pharmacy/base/store/update', data })
}

// 删除门店
export const deleteStore = async (id: number) => {
  return await request.delete({ url: '/pharmacy/base/store/delete?id=' + id })
}

// 导出门店
export const exportStore = async (params) => {
  return await request.download({ url: '/pharmacy/base/store/export-excel', params })
}
