import request from '@/config/axios'

/** 小程序订单 VO */
export interface MemberOrderVO {
  id?: number
  orderNo: string
  memberId: number
  storeId: number
  orderType: number
  goodsAmount: number
  couponAmount: number
  freightAmount: number
  discountAmount: number
  payableAmount: number
  payStatus: number
  status: number
  payNo: string
  paidAt: Date
  prescId: number
  cancelReason: string
  remark: string
  finishAt: Date
  pickupCode: string
  verifyBy: string
  verifyAt: Date
  expireAt: Date
  createTime?: Date
}

// 查询小程序订单分页
export const getOrderPage = async (params: PageParam) => {
  return await request.get({ url: '/pharmacy/member/order/page', params })
}

// 查询小程序订单详情
export const getOrder = async (id: number) => {
  return await request.get({ url: '/pharmacy/member/order/get?id=' + id })
}

// 修改订单状态
export const updateOrderStatus = async (id: number, status: number) => {
  return await request.put({ url: '/pharmacy/member/order/update-status', data: { id, status } })
}

// 取消订单
export const cancelOrder = async (id: number, cancelReason: string) => {
  return await request.put({ url: '/pharmacy/member/order/cancel', data: { id, cancelReason } })
}

// 核销订单
export const verifyOrder = async (id: number) => {
  return await request.put({ url: '/pharmacy/member/order/verify?id=' + id })
}
