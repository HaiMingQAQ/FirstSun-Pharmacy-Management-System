import request from '@/config/axios'

// 支付单 VO（pay 模块管理端）
export interface PayOrderVO {
  id: number
  channelCode?: string // 渠道编码
  merchantOrderId?: string // 商户订单号
  subject?: string // 商品标题
  price?: number // 支付金额(分)
  status?: number // 0未支付/10成功/20退款/30关闭
  successTime?: string
  createTime?: string
}

// 支付单详情 VO
export interface PayOrderDetailVO extends PayOrderVO {
  channelId?: number
  channelOrderNo?: string
  refundPrice?: number
  expireTime?: string
  userIp?: string
  body?: string
  notifyUrl?: string
}

// 支付单查询 API（复用 yudao-module-pay 管理端）
export const PayOrderApi = {
  // 支付单分页
  getPayOrderPage: async (params: any) => {
    return await request.get({ url: '/pay/order/page', params })
  },
  // 支付单详情
  getPayOrder: async (id: number) => {
    return await request.get({ url: `/pay/order/get?id=${id}` })
  },
  // 支付单详情(含扩展)
  getPayOrderDetail: async (id: number) => {
    return await request.get({ url: `/pay/order/get-detail?id=${id}` })
  }
}
