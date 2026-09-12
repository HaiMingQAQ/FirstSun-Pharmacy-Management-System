import request from '@/config/axios'

export interface WarehouseVO {
  id: number
  storeId: number
  whCode: string
  whName: string
  tempZone: number
  isDefault: number
  status: number
}

export interface WarehouseQuery extends PageParam {
  code?: string
  name?: string
  status?: number
}

export interface WarehousePage {
  list: WarehouseVO[]
  total: number
}

export const getWarehousePage = (params: WarehouseQuery): Promise<WarehousePage> =>
  request.get({ url: '/pharmacy/inventory/warehouse/page', params })

export const getWarehouse = (id: number): Promise<WarehouseVO> =>
  request.get({ url: '/pharmacy/inventory/warehouse/get', params: { id } })

export interface WarehouseCreate {
  whCode: string
  whName: string
  tempZone: number
}

export const createWarehouse = (data: WarehouseCreate): Promise<number> =>
  request.post({ url: '/pharmacy/inventory/warehouse/create', data })

export interface WarehouseFields extends WarehouseCreate {
  status: number
}

export interface WarehouseUpdate {
  id: number
  expected: WarehouseFields
  value: WarehouseFields
}

export const updateWarehouse = (data: WarehouseUpdate): Promise<boolean> =>
  request.put({ url: '/pharmacy/inventory/warehouse/update', data })

export const deleteWarehouse = (id: number): Promise<boolean> =>
  request.delete({ url: '/pharmacy/inventory/warehouse/delete', params: { id } })
