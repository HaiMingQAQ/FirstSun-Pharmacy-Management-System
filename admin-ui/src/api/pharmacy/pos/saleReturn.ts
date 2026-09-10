import request from '@/config/axios'

// 退货单 VO
export interface SaleReturnVO {
  id: number
  returnNo: string // 退货单号
  saleOrderId: number // 原销售单
  storeId: number // 门店
  returnType: number // 0 退货 / 1 换货
  reason: number // 原因字典
  totalAmount: number // 退款金额
  refundMethod: number // 0 原路 / 1 现金 / 2 余额
  status: number // 0 草稿 / 1 待审批 / 2 已审核 / 3 已完成 / 4 已取消
  cashierId: number // 经办人
  pharmacistConfirm: number // 药师复核(处方药退货)
  auditBy: number // 审批人
  returnAt: Date // 完成时间
  refundStatus: number // 0 未退款 / 1 退款中 / 2 成功 / 3 失败
  refundError: string // 退款失败原因
  createTime: Date
}

// 退货单创建 Request VO
export interface SaleReturnSaveReqVO {
  saleOrderId: number // 原销售单
  returnType: number // 0 退货 / 1 换货
  reason: number // 原因字典
  refundMethod?: number // 0 原路 / 1 现金 / 2 余额
  pharmacistConfirm?: number // 药师复核(处方药退货必须)
  cashierId?: number // 经办人(为空时取原销售单收银员)
  remark?: string // 备注
  items: SaleReturnItemVO[] // 退货明细
}

// 退货单商品行
export interface SaleReturnItemVO {
  saleOrderLineId: number // 销售明细行
  qty: number // 退货数量
  locationId?: number // 验收回库货位
}

// 退货单 API
export const SaleReturnApi = {
  // 创建退货单（全退/部分退）
  createSaleReturn: async (data: SaleReturnSaveReqVO) => {
    return await request.post({ url: '/pharmacy/pos/sale-return/create', data })
  },
  // 获得退货单分页
  getSaleReturnPage: async (params: any) => {
    return await request.get({ url: '/pharmacy/pos/sale-return/page', params })
  },
  // 获得退货单详情
  getSaleReturn: async (id: number) => {
    return await request.get({ url: `/pharmacy/pos/sale-return/get?id=${id}` })
  },
  // 获得退货单详情（含明细）
  getSaleReturnDetail: async (id: number) => {
    return await request.get({ url: `/pharmacy/pos/sale-return/detail?id=${id}` })
  }
}
