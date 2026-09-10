import request from '@/config/axios'

// 销售单 VO
export interface SaleOrderVO {
  id: number
  orderNo: string // 销售单号
  storeId: number // 门店
  posNo: string // 收银台
  shiftId: number // 班次
  cashierId: number // 收银员
  pharmacistId: number // 审方药师
  memberId: number // 会员
  customerName: string // 顾客姓名(散客)
  source: number // 来源:0 柜台 / 1 小程序
  wxOrderId: number // 线上订单来源
  saleType: number // 0 销售 / 1 换货
  returnFlag: number // 0 正常 / 1 部分退 / 2 全退
  totalQty: number // 总数量
  subtotal: number // 原价合计
  discountAmount: number // 折扣金额
  couponAmount: number // 券抵扣
  pointsDeduct: number // 积分抵扣
  payableAmount: number // 应付
  paidAmount: number // 实收
  changeAmount: number // 找零
  costAmount: number // 成本合计
  pointsEarned: number // 奖励积分
  status: number // 0 待支付 / 1 完成 / 2 全额退款 / 3 部分退款 / -1 取消
  offlineFlag: number // 是否离线单
  remark: string // 备注
  saleTime: Date // 销售时间
  createTime: Date
}

// 销售单创建 Request VO
export interface SaleOrderSaveReqVO {
  orderNo: string // 销售单号(前端预生成, 幂等键)
  storeId: number // 门店编号
  shiftId?: number // 班次编号
  cashierId?: number // 收银员编号
  posNo?: string // 收银台号
  memberId?: number // 会员编号
  customerName?: string // 顾客姓名(散客)
  items: SaleOrderItemVO[] // 商品明细
  payments: SalePaymentItemVO[] // 支付明细
  remark?: string // 备注
}

// 销售单商品行
export interface SaleOrderItemVO {
  drugId: number // 药品
  batchId: number // 批次(先进先出)
  qty: number // 数量
  price: number // 成交价
  isRx?: number // 是否处方药 0/1
  prescId?: number // 关联处方
  locationId?: number // 出库货位
  isGift?: number // 是否赠品
  drugName?: string // 商品名称快照
  specification?: string // 规格快照
  unit?: string // 单位快照
}

// 销售单支付行
export interface SalePaymentItemVO {
  payMethod: number // 1现金/2微信/3支付宝/4银行卡/5储值/6医保/7积分
  payAmount: number // 支付金额
  channel?: string // 渠道(如 wxpay/alipay)
  payNo?: string // 外部交易号(现金可不填)
  paymentNo: string // 本系统支付幂等号(现金也生成)
}

// 销售单 API
export const SaleOrderApi = {
  // 创建销售单（收银）
  createSaleOrder: async (data: SaleOrderSaveReqVO) => {
    return await request.post({ url: '/pharmacy/pos/sale-order/create', data })
  },
  // 获得销售单分页
  getSaleOrderPage: async (params: any) => {
    return await request.get({ url: '/pharmacy/pos/sale-order/page', params })
  },
  // 获得销售单详情
  getSaleOrder: async (id: number) => {
    return await request.get({ url: `/pharmacy/pos/sale-order/get?id=${id}` })
  },
  // 获得销售单详情（含明细与支付）
  getSaleOrderDetail: async (id: number) => {
    return await request.get({ url: `/pharmacy/pos/sale-order/detail?id=${id}` })
  }
}
