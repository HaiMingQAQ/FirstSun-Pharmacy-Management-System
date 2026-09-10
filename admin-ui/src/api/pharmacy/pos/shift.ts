import request from '@/config/axios'

// 收银班次 VO
export interface PosShiftVO {
  id: number
  shiftNo: string // 班次号
  storeId: number // 门店
  posNo: string // 收银台号
  cashierId: number // 收银员
  openAt: Date // 开台时间
  closeAt: Date // 交班时间
  cashExpected: number // 系统应收现金
  cashActual: number // 实盘现金
  diffAmount: number // 短长款
  diffReason: string // 差异原因
  saleCount: number // 交易笔数
  saleAmount: number // 销售额
  status: number // 0 营业中 / 1 已交班
  createTime: Date
}

// 收银班次 API
export const PosShiftApi = {
  // 开台（创建营业中班次）
  openShift: async (storeId: number, posNo: string, cashierId: number) => {
    return await request.post({
      url: '/pharmacy/pos/shift/open',
      params: { storeId, posNo, cashierId }
    })
  },
  // 交班（核对现金应收与实盘，差异必填原因）
  closeShift: async (shiftId: number, cashActual?: number, diffReason?: string) => {
    return await request.post({
      url: '/pharmacy/pos/shift/close',
      params: { shiftId, cashActual, diffReason }
    })
  },
  // 获得班次分页
  getShiftPage: async (params: any) => {
    return await request.get({ url: '/pharmacy/pos/shift/page', params })
  },
  // 获得班次详情
  getShift: async (id: number) => {
    return await request.get({ url: `/pharmacy/pos/shift/get?id=${id}` })
  }
}
