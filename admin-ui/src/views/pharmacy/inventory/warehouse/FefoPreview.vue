<template>
  <el-dialog
    v-model="visible"
    title="FEFO 选批预览"
    width="min(740px, 96vw)"
    :close-on-click-modal="false"
    :before-close="beforeClose"
  >
    <p>{{ warehouseName }} · 药品编号 {{ drugId }}</p>
    <el-alert
      title="仅模拟分配，不锁定或扣减库存。实际销售需重新校验。"
      type="info"
      :closable="false"
    />
    <el-form class="mt-16px" label-width="110px" :disabled="loading">
      <el-form-item label="需求数量"
        ><el-input-number v-model="quantity" :min="1" :max="2147483647" :precision="0"
      /></el-form-item>
      <el-form-item label="效期当天口径">
        <el-select v-model="policy" placeholder="请选择本次模拟口径">
          <el-option label="效期当天不参与分配" value="BLOCK_ON_EXPIRY_DATE" />
          <el-option label="效期当天参与分配" value="ALLOW_ON_EXPIRY_DATE" />
        </el-select>
      </el-form-item>
    </el-form>
    <el-alert
      v-if="failed"
      title="预览失败，请核对库存、药品状态和输入条件。"
      type="error"
      :closable="false"
    />
    <template v-if="result">
      <p>计算时间：{{ result.evaluatedAt }} · 未预占库存</p>
      <el-table :data="result.allocations" :row-key="(row) => `${row.batchId}:${row.locationId}`">
        <el-table-column label="批次编号" prop="batchId" />
        <el-table-column label="货位编号" prop="locationId" />
        <el-table-column label="分配数量" prop="quantity" />
      </el-table>
    </template>
    <template #footer
      ><el-button :disabled="loading" @click="visible = false">关闭</el-button
      ><el-button type="primary" :loading="loading" :disabled="!policy || !quantity" @click="run"
        >计算预览</el-button
      ></template
    >
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { checkPermi } from '@/utils/permission'
import {
  previewFefo,
  type BatchVO,
  type ExpiryDayPolicy,
  type FefoPreview
} from '@/api/pharmacy/inventory/batch'

const visible = ref(false)
const loading = ref(false)
const failed = ref(false)
const warehouseName = ref('')
const drugId = ref(0)
const quantity = ref<number | undefined>(1)
const policy = ref<ExpiryDayPolicy>()
const result = ref<FefoPreview>()
let warehouseId = 0
watch([quantity, policy], () => {
  result.value = undefined
  failed.value = false
})
const open = (row: BatchVO) => {
  if (loading.value || !checkPermi(['pharmacy:inventory-batch:query'])) return
  warehouseId = row.warehouseId
  warehouseName.value = row.warehouseName
  drugId.value = row.drugId
  quantity.value = 1
  policy.value = undefined
  result.value = undefined
  failed.value = false
  visible.value = true
}
const beforeClose = (done: () => void) => {
  if (!loading.value) done()
}
const run = async () => {
  if (
    loading.value ||
    !policy.value ||
    !quantity.value ||
    !checkPermi(['pharmacy:inventory-batch:query'])
  )
    return
  loading.value = true
  result.value = undefined
  failed.value = false
  try {
    result.value = await previewFefo({
      warehouseId,
      drugId: drugId.value,
      quantity: quantity.value,
      expiryDayPolicy: policy.value
    })
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}
defineExpose({ open })
</script>
