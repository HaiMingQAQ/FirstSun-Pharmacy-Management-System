<template>
  <el-drawer v-model="visible" title="库存台账" size="min(1280px, 98vw)" @closed="close">
    <div class="pharmacy-page pharmacy-modern-page">
      <p>{{ warehouseName }}</p>
      <el-tabs v-model="tab" @tab-change="changeTab">
        <el-tab-pane v-if="canStock" label="批次库存" name="batches" />
        <el-tab-pane v-if="canStock" label="货位库存" name="locations" />
        <el-tab-pane v-if="canFlow" label="库存流水" name="flows" />
      </el-tabs>
      <el-form :model="query" inline @submit.prevent="search">
        <el-form-item label="药品编号"
          ><el-input-number v-model="query.drugId" :min="1" :precision="0" :controls="false"
        /></el-form-item>
        <el-form-item label="批号"
          ><el-input v-model="query.batchNo" clearable maxlength="64"
        /></el-form-item>
        <el-form-item label="货位编号"
          ><el-input-number v-model="query.locationId" :min="1" :precision="0" :controls="false"
        /></el-form-item>
        <template v-if="tab !== 'flows'">
          <el-form-item label="质量状态"
            ><el-select v-model="query.qualityStatus" clearable class="!w-130px"
              ><el-option
                v-for="(label, value) in qualityLabels"
                :key="value"
                :value="value"
                :label="label" /></el-select
          ></el-form-item>
          <el-form-item label="效期从"
            ><el-date-picker v-model="query.expiryFrom" value-format="YYYY-MM-DD" type="date"
          /></el-form-item>
          <el-form-item label="效期至"
            ><el-date-picker v-model="query.expiryTo" value-format="YYYY-MM-DD" type="date"
          /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="业务单号"
            ><el-input v-model="query.bizNo" clearable maxlength="32"
          /></el-form-item>
          <el-form-item label="流水类型"
            ><el-select v-model="query.flowType" clearable class="!w-150px"
              ><el-option
                v-for="(label, value) in FLOW_TYPES"
                :key="value"
                :label="label"
                :value="Number(value)" /></el-select
          ></el-form-item>
          <el-form-item label="来源业务"
            ><el-select v-model="query.bizType" clearable class="!w-150px"
              ><el-option
                v-for="(label, value) in BIZ_TYPES"
                :key="value"
                :label="label"
                :value="Number(value)" /></el-select
          ></el-form-item>
          <el-form-item label="时间从"
            ><el-date-picker
              v-model="query.flowFrom"
              value-format="YYYY-MM-DDTHH:mm:ss"
              type="datetime"
          /></el-form-item>
          <el-form-item label="时间早于"
            ><el-date-picker
              v-model="query.flowBefore"
              value-format="YYYY-MM-DDTHH:mm:ss"
              type="datetime"
          /></el-form-item>
        </template>
        <el-form-item
          ><el-button :loading="loading" @click="search">查询</el-button
          ><el-button :disabled="loading" @click="reset">重置</el-button></el-form-item
        >
      </el-form>
      <el-alert v-if="failed" title="台账加载失败，请重试。" type="error" :closable="false" />
      <el-alert
        v-if="tab !== 'flows'"
        title="账面可用量不等于可售量；过期和质量停售库存仍在台账中展示。"
        type="info"
        :closable="false"
      />
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="编号" prop="id" min-width="100" />
        <el-table-column label="药品编号" prop="drugId" min-width="100" />
        <el-table-column label="批号" prop="batchNo" min-width="150" />
        <template v-if="tab === 'batches'">
          <el-table-column label="生产日期" prop="manufactureDate" min-width="120" />
          <el-table-column label="效期" prop="expiryDate" min-width="120" />
          <el-table-column label="质量状态" min-width="100"
            ><template #default="{ row }">{{
              qualityLabels[row.qualityStatus] ?? '未知'
            }}</template></el-table-column
          >
          <el-table-column label="总量" prop="qtyTotal" min-width="90" />
          <el-table-column label="账面可用" prop="qtyAvail" min-width="100" />
          <el-table-column label="冻结" prop="qtyFrozen" min-width="90" />
          <el-table-column label="累计净销售" prop="qtySold" min-width="110" />
          <el-table-column label="操作" width="110" fixed="right"
            ><template #default="{ row }"
              ><el-button link type="primary" @click="preview?.open(row)"
                >选批预览</el-button
              ></template
            ></el-table-column
          >
        </template>
        <template v-else-if="tab === 'locations'">
          <el-table-column label="货位" prop="locationCode" min-width="120" />
          <el-table-column label="批次编号" prop="batchId" min-width="100" />
          <el-table-column label="在库" prop="qty" min-width="90" />
          <el-table-column label="账面可用" prop="qtyAvail" min-width="100" />
          <el-table-column label="冻结" prop="qtyFrozen" min-width="90" />
        </template>
        <template v-else>
          <el-table-column label="货位编号" prop="locationId" min-width="100" />
          <el-table-column label="流水类型" min-width="120"
            ><template #default="{ row }">{{
              FLOW_TYPES[row.flowType] ?? `未知（${row.flowType}）`
            }}</template></el-table-column
          >
          <el-table-column label="来源业务" min-width="100"
            ><template #default="{ row }">{{
              BIZ_TYPES[row.bizType] ?? `未知（${row.bizType}）`
            }}</template></el-table-column
          >
          <el-table-column label="入库" prop="inQty" min-width="80" />
          <el-table-column label="出库" prop="outQty" min-width="80" />
          <el-table-column label="冻结变化" prop="frozenDelta" min-width="100" />
          <el-table-column label="批次余额" prop="balanceQty" min-width="100" />
          <el-table-column label="业务单号" prop="bizNo" min-width="160" />
          <el-table-column label="业务明细" prop="bizLineId" min-width="100" />
          <el-table-column label="时间" min-width="170"
            ><template #default="{ row }">{{ formatDate(row.flowTime) }}</template></el-table-column
          >
        </template>
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
  <FefoPreview ref="preview" />
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { checkPermi } from '@/utils/permission'
import { formatDate } from '@/utils/formatTime'
import {
  getBatchPage,
  getLocationStockPage,
  type BatchVO,
  type LocationStockVO,
  type StockQuery
} from '@/api/pharmacy/inventory/batch'
import {
  getFlowPage,
  FLOW_TYPES,
  BIZ_TYPES,
  type FlowQuery,
  type FlowVO
} from '@/api/pharmacy/inventory/flow'
import type { WarehouseVO } from '@/api/pharmacy/inventory/warehouse'
import FefoPreview from './FefoPreview.vue'

type Tab = 'batches' | 'locations' | 'flows'
const canStock = computed(() => checkPermi(['pharmacy:inventory-batch:query']))
const canFlow = computed(() => checkPermi(['pharmacy:inventory-flow:query']))
const visible = ref(false)
const tab = ref<Tab>('batches')
const warehouseName = ref('')
const loading = ref(false)
const loaded = ref(false)
const failed = ref(false)
const total = ref(0)
const rows = ref<Array<BatchVO | LocationStockVO | FlowVO>>([])
const preview = ref<InstanceType<typeof FefoPreview>>()
const qualityLabels = ['正常', '质量停售', '召回']
const query = reactive<StockQuery & FlowQuery>({ warehouseId: 0, pageNo: 1, pageSize: 10 })
let sequence = 0
const rejectFilters = (message: string) => {
  sequence++
  rows.value = []
  total.value = 0
  loaded.value = false
  failed.value = true
  loading.value = false
  ElMessage.warning(message)
}
const load = async () => {
  if (!visible.value || (tab.value === 'flows' ? !canFlow.value : !canStock.value)) return
  if (
    tab.value !== 'flows' &&
    query.expiryFrom &&
    query.expiryTo &&
    query.expiryFrom > query.expiryTo
  ) {
    rejectFilters('效期起始日期不能晚于结束日期')
    return
  }
  if (
    tab.value === 'flows' &&
    query.flowFrom &&
    query.flowBefore &&
    query.flowFrom >= query.flowBefore
  ) {
    rejectFilters('流水起始时间必须早于截止时间')
    return
  }
  const requestId = ++sequence
  loading.value = true
  failed.value = false
  const base = {
    warehouseId: query.warehouseId,
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    drugId: query.drugId ?? undefined,
    locationId: query.locationId ?? undefined,
    batchNo: query.batchNo || undefined
  }
  try {
    const page =
      tab.value === 'flows'
        ? await getFlowPage({
            ...base,
            bizNo: query.bizNo || undefined,
            flowType: query.flowType ?? undefined,
            bizType: query.bizType ?? undefined,
            flowFrom: query.flowFrom || undefined,
            flowBefore: query.flowBefore || undefined
          })
        : await (tab.value === 'batches' ? getBatchPage : getLocationStockPage)({
            ...base,
            qualityStatus: query.qualityStatus ?? undefined,
            expiryFrom: query.expiryFrom || undefined,
            expiryTo: query.expiryTo || undefined
          })
    if (requestId !== sequence || !visible.value) return
    rows.value = page.list
    total.value = page.total
    loaded.value = true
  } catch {
    if (requestId !== sequence || !visible.value) return
    rows.value = []
    total.value = 0
    failed.value = true
  } finally {
    if (requestId === sequence) loading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  return load()
}
const reset = () => {
  Object.assign(query, {
    drugId: undefined,
    locationId: undefined,
    batchNo: '',
    qualityStatus: undefined,
    expiryFrom: undefined,
    expiryTo: undefined,
    bizNo: '',
    flowType: undefined,
    bizType: undefined,
    flowFrom: undefined,
    flowBefore: undefined
  })
  return search()
}
const changeTab = () => {
  sequence++
  rows.value = []
  loaded.value = false
  return reset()
}
const close = () => {
  sequence++
  loading.value = false
  rows.value = []
  loaded.value = false
}
const open = (warehouse: WarehouseVO) => {
  if (!canStock.value && !canFlow.value) return
  close()
  query.warehouseId = warehouse.id
  query.pageSize = 10
  warehouseName.value = `${warehouse.whName}（${warehouse.whCode}）`
  tab.value = canStock.value ? 'batches' : 'flows'
  visible.value = true
  return reset()
}
defineExpose({ open })
</script>
