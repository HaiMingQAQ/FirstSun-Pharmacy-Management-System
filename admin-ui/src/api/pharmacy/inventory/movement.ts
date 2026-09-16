import request from '@/config/axios'

export interface InventoryMovementLine {
  bizLineId: number
  batchId: number
  sourceLocationId: number
  targetLocationId: number
  qty: number
}

export interface InventoryMovement {
  warehouseId: number
  bizNo: string
  lines: InventoryMovementLine[]
}

export const executeMovement = (data: InventoryMovement): Promise<boolean> =>
  request.post({ url: '/pharmacy/inventory/movement/execute', data })
