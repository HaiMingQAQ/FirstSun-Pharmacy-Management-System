import request from '@/config/axios'

/** 药品分类 VO */
export interface CategoryVO {
  id?: number
  catCode: string
  catName: string
  parentId?: number
  catType: number
  sort: number
  status: number
  createTime?: Date
}

/** 药品分类精简信息 VO（下拉/树选择使用） */
export interface CategorySimpleVO {
  id: number
  catName: string
  catCode: string
  parentId?: number
}

// 查询药品分类分页
export const getCategoryPage = async (params: PageParam) => {
  return await request.get({ url: '/pharmacy/base/category/page', params })
}

// 获取启用的药品分类精简列表（下拉/树选择）
export const getSimpleCategoryList = async (): Promise<CategorySimpleVO[]> => {
  return await request.get({ url: '/pharmacy/base/category/simple-list' })
}

// 查询药品分类详情
export const getCategory = async (id: number) => {
  return await request.get({ url: '/pharmacy/base/category/get?id=' + id })
}

// 新增药品分类
export const createCategory = async (data: CategoryVO) => {
  return await request.post({ url: '/pharmacy/base/category/create', data })
}

// 修改药品分类
export const updateCategory = async (data: CategoryVO) => {
  return await request.put({ url: '/pharmacy/base/category/update', data })
}

// 删除药品分类
export const deleteCategory = async (id: number) => {
  return await request.delete({ url: '/pharmacy/base/category/delete?id=' + id })
}

// 导出药品分类
export const exportCategory = async (params) => {
  return await request.download({ url: '/pharmacy/base/category/export-excel', params })
}
