import request from '@/config/axios'

// 处方明细项
export interface PrescItemVO {
  drugId?: number // 药品
  qty: number // 核准数量
  usage?: string // 用法
  dosage?: string // 用量
  drugName?: string // 药品名称快照
  specification?: string // 规格快照
}

// 处方记录 VO
export interface PrescRecordVO {
  id: number
  prescNo: string // 处方号
  storeId: number // 门店
  storeName?: string // 门店名称（关联填充）
  source: number // 0纸质拍照/1电子处方平台/2复诊续方
  hospital?: string // 开具医院
  doctorName?: string // 医师姓名
  patientName: string // 患者姓名
  patientAge?: number // 年龄
  patientIdNo?: string // 身份证(AES)
  diagnosis?: string // 诊断
  usageDesc?: string // 用法用量
  prescDate?: string // 开方日期
  imageUrl?: string // 处方影像URL
  reviewStatus: number // 0待审/1通过/2驳回
  pharmacistId?: number // 审方药师
  pharmacistName?: string // 审方药师姓名（关联填充）
  reviewAt?: string // 审方时间
  reviewOpinion?: string // 审方意见
  reviewSnapshot?: string // 电子签名
  isSpecial: number // 是否特管(双人复核)
  dblCheckBy?: number // 双人复核人
  limitCheck: number // 是否超量复核
  status: number // 0有效/1已完成/2作废
  wxMemberId?: number // 小程序上传人
  images?: string // JSON: 最多5张影像
  prescribedItems?: string // JSON: 药品明细
  createTime?: string
}

// 处方登记 Request VO
export interface PrescRecordSaveReqVO {
  prescNo?: string // 不传则后端生成
  storeId: number
  source: number
  hospital?: string
  doctorName?: string
  patientName: string
  patientAge?: number
  patientIdNo?: string
  diagnosis?: string
  usageDesc?: string
  prescDate?: string
  imageUrl?: string
  isSpecial?: number
  limitCheck?: number
  wxMemberId?: number
  images?: string[] // 影像URL列表(最多5张)
  items: PrescItemVO[] // 药品明细
}

// 审方 Request VO
export interface PrescRecordReviewReqVO {
  id: number
  reviewStatus: number // 1通过/2驳回
  reviewOpinion?: string // 驳回必填
  reviewSnapshot?: string
  dblCheckBy?: number // 特管处方必填
}

// 处方 API
export const PrescRecordApi = {
  // 登记处方
  createPrescRecord: async (data: PrescRecordSaveReqVO) => {
    return await request.post({ url: '/pharmacy/prescription/create', data })
  },
  // 药师审核
  reviewPrescRecord: async (data: PrescRecordReviewReqVO) => {
    return await request.post({ url: '/pharmacy/prescription/review', data })
  },
  // 作废处方
  invalidatePrescRecord: async (id: number) => {
    return await request.post({ url: `/pharmacy/prescription/invalidate?id=${id}` })
  },
  // 处方分页(台账)
  getPrescRecordPage: async (params: any) => {
    return await request.get({ url: '/pharmacy/prescription/page', params })
  },
  // 处方详情
  getPrescRecord: async (id: number) => {
    return await request.get({ url: `/pharmacy/prescription/get?id=${id}` })
  }
}
