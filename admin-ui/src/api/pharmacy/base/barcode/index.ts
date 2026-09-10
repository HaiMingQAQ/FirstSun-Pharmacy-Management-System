import request from '@/config/axios'

export interface BarcodeVO {
  id?: number
  drugId: number
  barcode: string
  barcodeType: number
  isDefault: number
}

// 查询条码分页
export const getBarcodePage = (params: PageParam) => {
  return request.get({ url: '/pharmacy/base/barcode/page', params })
}

// 查询条码详情
export const getBarcode = (id: number) => {
  return request.get({ url: '/pharmacy/base/barcode/get?id=' + id })
}

// 新增条码
export const createBarcode = (data: BarcodeVO) => {
  return request.post({ url: '/pharmacy/base/barcode/create', data })
}

// 修改条码
export const updateBarcode = (data: BarcodeVO) => {
  return request.put({ url: '/pharmacy/base/barcode/update', data })
}

// 删除条码
export const deleteBarcode = (id: number) => {
  return request.delete({ url: '/pharmacy/base/barcode/delete?id=' + id })
}

// 导出条码
export const exportBarcode = (params: any) => {
  return request.download({ url: '/pharmacy/base/barcode/export-excel', params })
}
