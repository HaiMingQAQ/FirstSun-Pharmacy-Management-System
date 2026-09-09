import request from '@/config/axios'

// 按日销售统计行
export interface DailySaleRowVO {
  bizDate: string // 业务日期
  orderCount: number // 订单笔数
  saleAmount: number // 销售额
  costAmount: number // 成本
}

// 销售统计 API
export const SalesStatisticsApi = {
  // 按日统计销售（笔数/销售额/成本）
  getDailyStatistics: async (storeId: number, startTime: string, endTime: string) => {
    return await request.get({
      url: '/pharmacy/pos/statistics/daily',
      params: { storeId, startTime, endTime }
    })
  }
}
