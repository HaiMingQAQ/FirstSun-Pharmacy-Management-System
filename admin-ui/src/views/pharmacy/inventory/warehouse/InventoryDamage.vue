<template>
  <el-drawer v-model="visible" title="库存报损" size="min(1280px, 98vw)" @closed="close">
    <div class="pharmacy-page pharmacy-modern-page">
      <p>{{ warehouseName }}</p>
      <el-alert
        title="报损执行会在审批、双人复核后一次性扣减可用库存并写入报损流水；已冻结数量不可报损。"
        type="info"
        :closable="false"
      />
      <el-form :model="createForm" inline @submit.prevent="create">
        <el-form-item label="原因">
          <el-select v-model="createForm.reason" class="!w-160px">
            <el-option :value="0" label="过期" /><el-option :value="1" label="破损" />
            <el-option :value="2" label="污染" /><el-option :value="3" label="召回" />
            <el-option :value="4" label="质量问题" /><el-option :value="5" label="盘亏" />
            <el-option :value="6" label="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="销毁方式"
          ><el-input v-model="createForm.destroyMethod"
        /></el-form-item>
        <el-form-item label="销毁单位"
          ><el-input v-model="createForm.destroyCompany"
        /></el-form-item>
        <el-button v-if="canCreate" type="primary" :loading="creating" @click="create"
          >新建报损</el-button
        >
      </el-form>
      <el-table :data="createForm.lines" border class="mb-12px">
        <el-table-column label="批次 ID" min-width="150"
          ><template #default="{ row }"><el-input-number v-model="row.batchId" :min="1" /></template
        ></el-table-column>
        <el-table-column label="货位 ID" min-width="150"
          ><template #default="{ row }"
            ><el-input-number v-model="row.locationId" :min="1" /></template
        ></el-table-column>
        <el-table-column label="数量" min-width="130"
          ><template #default="{ row }"><el-input-number v-model="row.qty" :min="1" /></template
        ></el-table-column>
        <el-table-column label="处置" min-width="150"
          ><template #default="{ row }"
            ><el-select v-model="row.disposeType"
              ><el-option :value="0" label="销毁" /><el-option
                :value="1"
                label="退供应商" /><el-option :value="2" label="其他" /></el-select></template
        ></el-table-column>
        <el-table-column label="操作" width="80"
          ><template #default="{ $index }"
            ><el-button link type="danger" @click="removeLine($index)">移除</el-button></template
          ></el-table-column
        >
      </el-table>
      <el-button v-if="canCreate" link type="primary" @click="addLine">+ 添加明细</el-button>
      <el-alert v-if="failed" title="报损数据加载失败，请重试。" type="error" :closable="false" />
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="报损单号" prop="damageNo" min-width="220" />
        <el-table-column label="数量" prop="totalQty" width="80" />
        <el-table-column label="金额" prop="totalAmount" width="110" />
        <el-table-column label="状态" width="120"
          ><template #default="{ row }">{{ DAMAGE_STATUS[row.status] }}</template></el-table-column
        >
        <el-table-column label="创建时间" prop="createTime" min-width="170" />
        <el-table-column label="操作" width="430" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            <el-button
              v-if="canUpdate && row.status === 0"
              link
              type="primary"
              @click="run(row, submitDamage)"
              >提交</el-button
            >
            <el-button
              v-if="canApprove && row.status === 1"
              link
              type="primary"
              @click="run(row, approveDamage)"
              >审批</el-button
            >
            <el-button
              v-if="canApprove && row.status === 1"
              link
              type="danger"
              @click="run(row, rejectDamage)"
              >驳回</el-button
            >
            <el-button
              v-if="canReview && row.status === 2"
              link
              type="primary"
              @click="run(row, reviewDamage)"
              >复核</el-button
            >
            <el-button
              v-if="canExecute && row.status === 2"
              link
              type="primary"
              @click="run(row, executeDamage)"
              >执行</el-button
            >
            <el-button
              v-if="canCancel && row.status === 0"
              link
              type="danger"
              @click="run(row, cancelDamage)"
              >取消</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-if="loaded && !failed"
        v-model:page="query.pageNo"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="load"
      />
    </div>
  </el-drawer>
  <el-dialog v-model="detailVisible" title="报损明细" width="min(1000px, 95vw)">
    <template v-if="detail">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="报损单号">{{ detail.summary.damageNo }}</el-descriptions-item>
        <el-descriptions-item label="原因">{{ detail.summary.reason }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{
          DAMAGE_STATUS[detail.summary.status]
        }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.lines" class="mt-12px">
        <el-table-column label="批次 ID" prop="batchId" /><el-table-column
          label="货位 ID"
          prop="locationId"
        />
        <el-table-column label="数量" prop="qty" /><el-table-column
          label="成本"
          prop="costPrice"
        /><el-table-column label="金额" prop="amount" />
      </el-table>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { checkPermi } from '@/utils/permission'
import {
  approveDamage,
  cancelDamage,
  createDamage,
  DAMAGE_STATUS,
  executeDamage,
  getDamage,
  getDamagePage,
  rejectDamage,
  reviewDamage,
  submitDamage,
  type DamageDetail,
  type DamageQuery,
  type DamageSummary
} from '@/api/pharmacy/inventory/damage'
import type { WarehouseVO } from '@/api/pharmacy/inventory/warehouse'

const visible = ref(false)
const warehouseId = ref<number>()
const warehouseName = ref('')
const loading = ref(false)
const creating = ref(false)
const loaded = ref(false)
const failed = ref(false)
const rows = ref<DamageSummary[]>([])
const total = ref(0)
const query = reactive<DamageQuery>({ pageNo: 1, pageSize: 10 })
const createForm = reactive({
  damageType: 0,
  reason: 0,
  destroyMethod: '',
  destroyCompany: '',
  lines: [{ batchId: 0, locationId: 0, qty: 1, disposeType: 0 }]
})
const detailVisible = ref(false)
const detail = ref<DamageDetail>()
const canQuery = computed(() => checkPermi(['pharmacy:inventory-damage:query']))
const canCreate = computed(() => checkPermi(['pharmacy:inventory-damage:create']))
const canUpdate = computed(() => checkPermi(['pharmacy:inventory-damage:update']))
const canApprove = computed(() => checkPermi(['pharmacy:inventory-damage:approve']))
const canReview = computed(() => checkPermi(['pharmacy:inventory-damage:review']))
const canExecute = computed(() => checkPermi(['pharmacy:inventory-damage:execute']))
const canCancel = computed(() => checkPermi(['pharmacy:inventory-damage:cancel']))
let sequence = 0

const load = async () => {
  if (!visible.value || loading.value || !canQuery.value) return
  const requestId = ++sequence
  loading.value = true
  failed.value = false
  try {
    const page = await getDamagePage({ ...query, warehouseId: warehouseId.value })
    if (requestId !== sequence) return
    rows.value = page.list
    total.value = page.total
    loaded.value = true
  } catch {
    if (requestId !== sequence) return
    rows.value = []
    total.value = 0
    failed.value = true
  } finally {
    if (requestId === sequence) loading.value = false
  }
}
const create = async () => {
  if (
    !warehouseId.value ||
    creating.value ||
    createForm.lines.some((line) => !line.batchId || !line.locationId || !line.qty)
  )
    return
  creating.value = true
  try {
    await createDamage(createForm)
    ElMessage.success('报损草稿已创建')
    await load()
  } finally {
    creating.value = false
  }
}
const run = async (row: DamageSummary, action: (id: number) => Promise<boolean>) => {
  await action(row.id)
  ElMessage.success('报损操作已完成')
  await load()
}
const openDetail = async (row: DamageSummary) => {
  detail.value = await getDamage(row.id)
  detailVisible.value = true
}
const addLine = () => createForm.lines.push({ batchId: 0, locationId: 0, qty: 1, disposeType: 0 })
const removeLine = (index: number) => {
  if (createForm.lines.length > 1) createForm.lines.splice(index, 1)
}
const close = () => {
  sequence++
  rows.value = []
  loaded.value = false
  detailVisible.value = false
}
const open = (warehouse: WarehouseVO) => {
  if (!canQuery.value && !canCreate.value) return
  close()
  warehouseId.value = warehouse.id
  warehouseName.value = `${warehouse.whName}（${warehouse.whCode}）`
  visible.value = true
  return load()
}
defineExpose({ open })
</script>
