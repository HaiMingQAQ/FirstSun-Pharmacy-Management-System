import request from '@/config/axios'

/** 会员等级 VO */
export interface MemberLevelVO {
  id?: number
  name: string
  level: number
  experience: number
  discountPercent: number
  icon: string
  backgroundUrl: string
  status: number
  sort: number
  createTime?: Date
}

/** 会员等级精简信息 VO（下拉选择使用） */
export interface MemberLevelSimpleVO {
  id: number
  name: string
  level: number
}

// 查询会员等级分页
export const getLevelPage = async (params: PageParam) => {
  return await request.get({ url: '/pharmacy/member/level/page', params })
}

// 查询会员等级详情
export const getLevel = async (id: number) => {
  return await request.get({ url: '/pharmacy/member/level/get?id=' + id })
}

// 新增会员等级
export const createLevel = async (data: MemberLevelVO) => {
  return await request.post({ url: '/pharmacy/member/level/create', data })
}

// 修改会员等级
export const updateLevel = async (data: MemberLevelVO) => {
  return await request.put({ url: '/pharmacy/member/level/update', data })
}

// 删除会员等级
export const deleteLevel = async (id: number) => {
  return await request.delete({ url: '/pharmacy/member/level/delete?id=' + id })
}

// 获取会员等级精简列表（下拉选择）
export const getSimpleLevelList = async (): Promise<MemberLevelSimpleVO[]> => {
  return await request.get({ url: '/pharmacy/member/level/simple-list' })
}

// 导出会员等级
export const exportLevel = async (params) => {
  return await request.download({ url: '/pharmacy/member/level/export-excel', params })
}
