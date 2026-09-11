import request from '@/config/axios'

/** 会员档案 VO */
export interface MemberUserVO {
  id?: number
  mobile: string
  nickname: string
  avatar: string
  name: string
  sex: number
  birthday: string
  status: number
  point: number
  levelId: number
  experience: number
  registerIp: string
  registerTerminal: number
  loginIp: string
  loginDate: Date
  email: string
  mark: string
  createTime?: Date
}

// 查询会员档案分页
export const getMemberPage = async (params: PageParam) => {
  return await request.get({ url: '/pharmacy/member/user/page', params })
}

// 查询会员档案详情
export const getMember = async (id: number) => {
  return await request.get({ url: '/pharmacy/member/user/get?id=' + id })
}

// 修改会员档案
export const updateMember = async (data: MemberUserVO) => {
  return await request.put({ url: '/pharmacy/member/user/update', data })
}

// 修改会员状态
export const updateMemberStatus = async (id: number, status: number) => {
  return await request.put({ url: '/pharmacy/member/user/update-status', data: { id, status } })
}

// 导出会员档案
export const exportMember = async (params) => {
  return await request.download({ url: '/pharmacy/member/user/export-excel', params })
}
