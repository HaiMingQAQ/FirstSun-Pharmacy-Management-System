import request from '@/config/axios'

/** 购物车 VO */
export interface MemberCartVO {
  id?: number
  memberId: number
  drugId: number
  qty: number
  selectedFlag: number
  addTime: Date
  storeId: number
  createTime?: Date
}

// 查询购物车分页
export const getCartPage = async (params: PageParam) => {
  return await request.get({ url: '/pharmacy/member/cart/page', params })
}

// 查询购物车详情
export const getCart = async (id: number) => {
  return await request.get({ url: '/pharmacy/member/cart/get?id=' + id })
}

// 新增购物车
export const createCart = async (data: MemberCartVO) => {
  return await request.post({ url: '/pharmacy/member/cart/create', data })
}

// 修改购物车
export const updateCart = async (data: MemberCartVO) => {
  return await request.put({ url: '/pharmacy/member/cart/update', data })
}

// 删除购物车
export const deleteCart = async (id: number) => {
  return await request.delete({ url: '/pharmacy/member/cart/delete?id=' + id })
}
