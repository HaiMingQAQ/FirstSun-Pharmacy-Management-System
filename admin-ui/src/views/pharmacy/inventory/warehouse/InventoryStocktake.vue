<template>
  <el-drawer v-model="visible" title="库存盘点" size="min(1280px, 98vw)" @closed="close">
    <div class="pharmacy-page pharmacy-modern-page">
      <p>{{ warehouseName }}</p>
      <el-form :model="createForm" inline @submit.prevent="create">
        <el-form-item label="盘点类型">
          <el-select v-model="createForm.stocktakeType" class="!w-130px"
            ><el-option :value="0" label="全盘" /><el-option :value="1" label="循环抽盘"
          /></el-select>
        </el-form-item>
        <el-form-item label="盘点方式">
          <el-select v-model="createForm.blindFlag" class="!w-130px"
            ><el-option :value="0" label="明盘" /><el-option :value="1" label="盲盘"
          /></el-select>
        </el-form-item>
        <el-button v-if="canCreate" type="primary" :loading="creating" @click="create"
          >创建盘点</el-button
        >
      </el-form>
      <el-alert
        title="当前盘点不启用冻结策略；审批调整会校验盘点期间库存、货位容量和冻结量。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="failed" title="盘点数据加载失败，请重试。" type="error" :closable="false" />
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="盘点单号" prop="stocktakeNo" min-width="220" />
        <el-table-column label="进度" min-width="100"
          ><template #default="{ row }"
            >{{ row.doneItem }}/{{ row.totalItem }}</template
          ></el-table-column
        >
        <el-table-column label="方式" min-width="100"
          ><template #default="{ row }">{{
            row.blindFlag === 1 ? '盲盘' : '明盘'
          }}</template></el-table-column
        >
        <el-table-column label="状态" min-width="100"
          ><template #default="{ row }">{{
            STOCKTAKE_STATUS[row.status]
          }}</template></el-table-column
        >
        <el-table-column label="创建时间" prop="createTime" min-width="170" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            <el-button
              v-if="canUpdate && row.status === 0"
              link
              type="primary"
              @click="run(row, startStocktake)"
              >开始</el-button
            >
            <el-button
              v-if="canUpdate && row.status === 1"
              link
              type="primary"
              @click="run(row, completeStocktake)"
              >完成</el-button
            >
            <el-button
              v-if="canApprove && row.status === 2"
              link
              type="primary"
              @click="run(row, approveStocktake)"
              >审批调整</el-button
            >
            <el-button
              v-if="canCancel && row.status === 0"
              link
              type="danger"
              @click="run(row, cancelStocktake)"
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
  <el-dialog
    v-model="detailVisible"
    title="盘点明细"
    width="min(1100px, 95vw)"
    @closed="detail = undefined"
  >
    <template v-if="detail">
      <el-alert
        v-if="detail.summary.blindFlag === 1 && detail.summary.status === 1"
        title="盲盘进行中，不显示账面数量。"
        type="warning"
        :closable="false"
      />
      <el-table :data="detail.lines" row-key="id">
        <el-table-column label="批号" prop="batchNo" min-width="140" />
        <el-table-column label="药品" prop="drugId" min-width="90" />
        <el-table-column label="账面数" min-width="90"
          ><template #default="{ row }">{{ row.bookQty ?? '—' }}</template></el-table-column
        >
        <el-table-column label="实盘数" min-width="150"
          ><template #default="{ row }"
            ><el-input-number
              v-model="row.realQty"
              :min="0"
              :precision="0"
              :disabled="detail.summary.status !== 1"
              controls-position="right" /></template
        ></el-table-column>
        <el-table-column label="差异" prop="diffQty" min-width="80" />
        <el-table-column label="货位" prop="locationId" min-width="90" />
        <el-table-column label="操作" width="90"
          ><template #default="{ row }"
            ><el-button
              v-if="canUpdate && detail.summary.status === 1"
              link
              type="primary"
              @click="record(row)"
              >保存</el-button
            ></template
          ></el-table-column
        >
      </el-table>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { checkPermi } from '@/utils/permission'
import {
  approveStocktake,
  cancelStocktake,
  completeStocktake,
  createStocktake,
  getStocktake,
  getStocktakePage,
  recordStocktake,
  startStocktake,
  STOCKTAKE_STATUS,
  type StocktakeDetail,
  type StocktakeLine,
  type StocktakeQuery,
  type StocktakeSummary
} from '@/api/pharmacy/inventory/stocktake'
import type { WarehouseVO } from '@/api/pharmacy/inventory/warehouse'

const visible = ref(false)
const warehouseId = ref<number>()
const warehouseName = ref('')
const loading = ref(false)
const creating = ref(false)
const loaded = ref(false)
const failed = ref(false)
const rows = ref<StocktakeSummary[]>([])
const total = ref(0)
const query = reactive<StocktakeQuery>({ pageNo: 1, pageSize: 10 })
const createForm = reactive({ stocktakeType: 0, blindFlag: 0, freezeFlag: 0 })
const detailVisible = ref(false)
const detail = ref<StocktakeDetail>()
const canQuery = computed(() => checkPermi(['pharmacy:inventory-stocktake:query']))
const canCreate = computed(() => checkPermi(['pharmacy:inventory-stocktake:create']))
const canUpdate = computed(() => checkPermi(['pharmacy:inventory-stocktake:update']))
const canApprove = computed(() => checkPermi(['pharmacy:inventory-stocktake:approve']))
const canCancel = computed(() => checkPermi(['pharmacy:inventory-stocktake:cancel']))
let sequence = 0

const load = async () => {
  if (!visible.value || loading.value || !canQuery.value) return
  const requestId = ++sequence
  loading.value = true
  failed.value = false
  try {
    const page = await getStocktakePage({ ...query, warehouseId: warehouseId.value })
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
  if (!warehouseId.value || creating.value) return
  creating.value = true
  try {
    await createStocktake({ warehouseId: warehouseId.value, ...createForm })
    ElMessage.success('盘点单已创建')
    await load()
  } finally {
    creating.value = false
  }
}
const run = async (row: StocktakeSummary, action: (id: number) => Promise<boolean>) => {
  await action(row.id)
  ElMessage.success('盘点操作已完成')
  await load()
}
const openDetail = async (row: StocktakeSummary) => {
  detail.value = await getStocktake(row.id)
  detailVisible.value = true
}
const record = async (line: StocktakeLine) => {
  if (line.realQty == null) return ElMessage.warning('请输入实盘数量')
  await recordStocktake({ stocktakeId: line.stocktakeId, lineId: line.id, realQty: line.realQty })
  ElMessage.success('盘点明细已保存')
  detail.value = await getStocktake(line.stocktakeId)
  await load()
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
