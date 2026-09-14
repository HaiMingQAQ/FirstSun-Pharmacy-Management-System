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

// ========== 业务接口 ==========

// 加购：同门店同药品累加数量
export const addToCart = async (memberId: number, drugId: number, qty: number, storeId: number) => {
  return await request.post({ url: '/pharmacy/member/cart/add', params: { memberId, drugId, qty, storeId } })
}

// 修改购物车数量
export const updateCartQty = async (id: number, qty: number) => {
  return await request.put({ url: '/pharmacy/member/cart/update-qty', params: { id, qty } })
}

// 勾选/取消勾选购物车
export const updateCartSelected = async (id: number, selectedFlag: number) => {
  return await request.put({ url: '/pharmacy/member/cart/update-selected', params: { id, selectedFlag } })
}

// 批量勾选/取消勾选
export const batchUpdateCartSelected = async (ids: number[], selectedFlag: number) => {
  return await request.put({ url: '/pharmacy/member/cart/batch-update-selected', data: ids, params: { selectedFlag } })
}

// 清空指定会员的购物车
export const clearCart = async (memberId: number) => {
  return await request.delete({ url: '/pharmacy/member/cart/clear', params: { memberId } })
}

// 获取指定会员的购物车列表
export const getCartListByMember = async (memberId: number) => {
  return await request.get({ url: '/pharmacy/member/cart/list-by-member', params: { memberId } })
}

// 获取指定会员已勾选的购物车列表
export const getSelectedCartListByMember = async (memberId: number) => {
  return await request.get({ url: '/pharmacy/member/cart/selected-list-by-member', params: { memberId } })
}
