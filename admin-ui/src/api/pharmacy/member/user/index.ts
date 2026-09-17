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

/** 会员档案新增/修改 VO（与后端 MemberUserSaveReqVO 对齐，注册/登录信息由服务端维护，不在提交载荷内） */
export interface MemberUserSaveVO {
  id?: number
  mobile: string
  nickname: string
  avatar?: string
  name?: string
  sex?: number
  status: number
  birthday?: string
  mark?: string
  point?: number
  levelId?: number
  experience?: number
  email?: string
}

// 查询会员档案分页
export const getMemberPage = async (params: PageParam) => {
  return await request.get({ url: '/pharmacy/member/user/page', params })
}

// 查询会员档案详情
export const getMember = async (id: number) => {
  return await request.get({ url: '/pharmacy/member/user/get?id=' + id })
}

// 创建会员档案
export const createMember = async (data: MemberUserSaveVO) => {
  return await request.post({ url: '/pharmacy/member/user/create', data })
}

// 修改会员档案
export const updateMember = async (data: MemberUserSaveVO) => {
  return await request.put({ url: '/pharmacy/member/user/update', data })
}

// 删除会员档案
export const deleteMember = async (id: number) => {
  return await request.delete({ url: '/pharmacy/member/user/delete?id=' + id })
}

// 修改会员状态（后端契约：Query 参数 id、status）
export const updateMemberStatus = async (id: number, status: number) => {
  return await request.put({ url: '/pharmacy/member/user/update-status', params: { id, status } })
}

// 导出会员档案
export const exportMember = async (params) => {
  return await request.download({ url: '/pharmacy/member/user/export-excel', params })
}
