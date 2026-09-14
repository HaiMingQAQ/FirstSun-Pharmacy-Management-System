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

// 删除收货地址
export const deleteAddress = async (id: number) => {
  return await request.delete({ url: '/pharmacy/member/address/delete?id=' + id })
}
