import request from '@/config/axios'

// 销售支付明细 VO
export interface SalePaymentVO {
  id: number
  orderId: number // 销售单
  payNo: string // 外部交易号
  payMethod: number // 1现金/2微信/3支付宝/4银行卡/5储值/6医保/7积分
  payAmount: number // 金额
  channel: string // 渠道(如 wxpay/alipay)
  status: number // 0 支付中 / 1 成功 / 2 失败 / 3 已退款 / 4 已冲正
  paidAt: Date // 支付时间
  refundNo: string // 退款流水号
  refundAt: Date // 退款时间
  paymentNo: string // 本系统分笔支付幂等号
  payOrderId: number // 关联渠道支付单(现金为空)
  createTime: Date
}

// 销售支付明细 API
export const SalePaymentApi = {
  // 获得支付明细分页
  getSalePaymentPage: async (params: any) => {
    return await request.get({ url: '/pharmacy/pos/payment/page', params })
  },
  // 按销售单获得支付明细列表
  getSalePaymentListByOrder: async (orderId: number) => {
    return await request.get({ url: `/pharmacy/pos/payment/list-by-order?orderId=${orderId}` })
  }
}
