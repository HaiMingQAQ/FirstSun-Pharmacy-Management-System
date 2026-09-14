import request from '@/config/axios'

/** 积分明细 VO */
export interface PointRecordVO {
  id?: number
  userId: number
  bizId: number
  bizType: number
  title: string
  description: string
  point: number
  totalPoint: number
  createTime?: Date
}

// 查询积分明细分页
export const getPointRecordPage = async (params: PageParam) => {
  return await request.get({ url: '/pharmacy/member/point-record/page', params })
}

// 导出积分明细
export const exportPointRecord = async (params) => {
  return await request.download({ url: '/pharmacy/member/point-record/export-excel', params })
}
