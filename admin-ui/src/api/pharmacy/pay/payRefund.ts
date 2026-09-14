import request from '@/config/axios'

// 退款单 VO（pay 模块管理端）
export interface PayRefundVO {
  id: number
  payOrderId?: number // 支付单
  channelCode?: string // 渠道编码
  merchantRefundId?: string // 商户退款编号
  merchantOrderId?: string // 商户订单编号
  reason?: string // 退款原因
  price?: number // 退款金额(分)
  status?: number // 退款状态
  successTime?: string
  createTime?: string
}

// 退款单查询 API（复用 yudao-module-pay 管理端）
export const PayRefundApi = {
  // 退款单分页
  getPayRefundPage: async (params: any) => {
    return await request.get({ url: '/pay/refund/page', params })
  },
  // 退款单详情
  getPayRefund: async (id: number) => {
    return await request.get({ url: `/pay/refund/get?id=${id}` })
  }
}
