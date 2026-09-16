<template>
  <el-dialog v-model="visible" title="上架移位" width="min(820px, 96vw)" @closed="close">
    <el-alert
      type="info"
      :closable="false"
      title="仅支持同仓库移位。冻结库存不能移动；目标容量由后端最终校验。"
    />
    <el-form class="mt-16px" label-width="100px">
      <el-form-item label="仓库"
        ><span>{{ warehouseName }}</span></el-form-item
      >
      <el-form-item label="来源库存" required>
        <el-select
          v-model="form.sourceKey"
          filterable
          class="!w-full"
          :loading="loading"
          @change="selectSource"
        >
          <el-option
            v-for="stock in stocks"
            :key="stockKey(stock)"
            :value="stockKey(stock)"
            :label="`${stock.batchNo} / ${stock.locationCode} / 可移 ${stock.qtyAvail}`"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="目标货位" required>
        <el-select v-model="form.targetLocationId" filterable class="!w-full" :loading="loading">
          <el-option
            v-for="location in targets"
            :key="location.id"
            :value="location.id"
            :label="location.locationCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="移位数量" required>
        <el-input-number
          v-model="form.qty"
          :min="1"
          :max="selected?.qtyAvail || 1"
          :precision="0"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">执行移位</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getLocationStockPage, type LocationStockVO } from '@/api/pharmacy/inventory/batch'
import { getLocationPage, type LocationVO } from '@/api/pharmacy/inventory/location'
import { executeMovement } from '@/api/pharmacy/inventory/movement'
import type { WarehouseVO } from '@/api/pharmacy/inventory/warehouse'

const emit = defineEmits<{ success: [] }>()
const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const warehouse = ref<WarehouseVO>()
const stocks = ref<LocationStockVO[]>([])
const targets = ref<LocationVO[]>([])
const form = reactive({ sourceKey: '', targetLocationId: undefined as number | undefined, qty: 1 })
const warehouseName = computed(() =>
  warehouse.value ? `${warehouse.value.whName}（${warehouse.value.whCode}）` : ''
)
const stockKey = (stock: LocationStockVO) => `${stock.batchId}:${stock.locationId}`
const selected = computed(() => stocks.value.find((stock) => stockKey(stock) === form.sourceKey))
const newBizNo = () =>
  `MV-${Date.now().toString(36).toUpperCase()}-${Math.random().toString(36).slice(2, 7).toUpperCase()}`
let bizNo = ''

const selectSource = () => {
  form.qty = 1
  if (form.targetLocationId === selected.value?.locationId) form.targetLocationId = undefined
}
const load = async (item: WarehouseVO) => {
  loading.value = true
  try {
    const [stockPage, locationPage] = await Promise.all([
      getLocationStockPage({ warehouseId: item.id, pageNo: 1, pageSize: 200 }),
      getLocationPage({ warehouseId: item.id, pageNo: 1, pageSize: 200, status: 1 })
    ])
    stocks.value = stockPage.list.filter((stock) => stock.qtyAvail > 0)
    targets.value = locationPage.list.filter((location) => location.status === 1)
  } catch {
    stocks.value = []
    targets.value = []
  } finally {
    loading.value = false
  }
}
const submit = async () => {
  const source = selected.value
  if (!source || !form.targetLocationId || !form.qty) {
    ElMessage.warning('请选择来源库存、目标货位并填写数量')
    return
  }
  if (source.locationId === form.targetLocationId) {
    ElMessage.warning('源货位和目标货位不能相同')
    return
  }
  if (form.qty > source.qtyAvail) {
    ElMessage.warning('移位数量超过可移动库存')
    return
  }
  submitting.value = true
  try {
    await executeMovement({
      warehouseId: warehouse.value!.id,
      bizNo,
      lines: [
        {
          bizLineId: 1,
          batchId: source.batchId,
          sourceLocationId: source.locationId,
          targetLocationId: form.targetLocationId,
          qty: form.qty
        }
      ]
    })
    ElMessage.success('移位成功')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}
const close = () => {
  form.sourceKey = ''
  form.targetLocationId = undefined
  form.qty = 1
  stocks.value = []
  targets.value = []
  bizNo = ''
}
const open = async (item: WarehouseVO) => {
  warehouse.value = item
  bizNo = newBizNo()
  visible.value = true
  await load(item)
}
defineExpose({ open })
</script>
