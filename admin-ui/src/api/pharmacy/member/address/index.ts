import request from '@/config/axios'

/** 收货地址 VO */
export interface MemberAddressVO {
  id?: number
  userId: number
  name: string
  mobile: string
  areaId: number
  detailAddress: string
  defaultStatus: number
  createTime?: Date
}

// 查询收货地址分页
export const getAddressPage = async (params: PageParam) => {
  return await request.get({ url: '/pharmacy/member/address/page', params })
}

// 查询收货地址详情
export const getAddress = async (id: number) => {
  return await request.get({ url: '/pharmacy/member/address/get?id=' + id })
}

/** 收货地址新增/修改 VO（与后端 MemberAddressSaveReqVO 对齐） */
export interface MemberAddressSaveVO {
  id?: number
  userId: number
  name: string
  mobile: string
  areaId: number
  detailAddress: string
  defaultStatus: number
}

// 创建收货地址
export const createAddress = async (data: MemberAddressSaveVO) => {
  return await request.post({ url: '/pharmacy/member/address/create', data })
}

// 更新收货地址
export const updateAddress = async (data: MemberAddressSaveVO) => {
  return await request.put({ url: '/pharmacy/member/address/update', data })
}

// 删除收货地址
export const deleteAddress = async (id: number) => {
  return await request.delete({ url: '/pharmacy/member/address/delete?id=' + id })
}
