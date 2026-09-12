import request from '@/config/axios'

export const LOCATION_TYPES = ['常规', '处方药区', '特管柜', '拆零区', '近效期区'] as const

export interface LocationVO {
  id: number
  warehouseId: number
  warehouseName: string
  locationCode: string
  locationType: number
  maxCapacity: number | null
  status: number
}

export interface LocationQuery extends PageParam {
  warehouseId: number
  code?: string
  status?: number
}

export const getLocationPage = (
  params: LocationQuery
): Promise<{ list: LocationVO[]; total: number }> =>
  request.get({ url: '/pharmacy/inventory/location/page', params })

export interface LocationCreate {
  warehouseId: number
  locationCode: string
  locationType: number
  maxCapacity?: number
}

export const createLocation = (data: LocationCreate): Promise<number> =>
  request.post({ url: '/pharmacy/inventory/location/create', data })

export interface LocationFields {
  locationCode: string
  locationType: number
  maxCapacity: number | null
  status: number
}

export interface LocationUpdate {
  id: number
  warehouseId: number
  expected: LocationFields
  value: LocationFields
}

export const updateLocation = (data: LocationUpdate): Promise<boolean> =>
  request.put({ url: '/pharmacy/inventory/location/update', data })
