<template>
  <el-drawer v-model="visible" title="库存健康" size="min(1280px, 98vw)" @closed="close">
    <div class="pharmacy-page pharmacy-modern-page">
      <p>{{ warehouseName }}</p>
      <el-tabs v-model="tab" @tab-change="search">
        <el-tab-pane v-if="canExpiry" label="效期预警" name="expiry" />
        <el-tab-pane v-if="canReconciliation" label="三账核对" name="reconciliation" />
      </el-tabs>
      <template v-if="tab === 'expiry'">
        <el-form :model="expiryQuery" inline @submit.prevent="search">
          <el-form-item label="预警级别">
            <el-select v-model="expiryQuery.alertLevel" clearable class="!w-130px">
              <el-option :value="1" label="红色（≤30天）" />
              <el-option :value="2" label="橙色（≤60天）" />
              <el-option :value="3" label="黄色（≤90天）" />
            </el-select>
          </el-form-item>
          <el-form-item label="处理状态">
            <el-select v-model="expiryQuery.handleType" clearable class="!w-160px">
              <el-option
                v-for="(label, value) in EXPIRY_HANDLE_TYPES"
                :key="value"
                :value="Number(value)"
                :label="label"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="预警日期">
            <el-date-picker v-model="expiryQuery.alertDate" value-format="YYYY-MM-DD" type="date" />
          </el-form-item>
          <el-form-item>
            <el-button :loading="loading" @click="search">查询</el-button>
            <el-button v-if="canRefresh" :loading="refreshing" type="primary" @click="refresh"
              >刷新今日预警</el-button
            >
          </el-form-item>
        </el-form>
        <el-alert
          title="预警只记录效期提醒，不改变库存数量，也不替代销售时的实时效期校验。"
          type="info"
          :closable="false"
        />
        <el-alert v-if="failed" title="效期预警加载失败，请重试。" type="error" :closable="false" />
        <el-table v-loading="loading" :data="expiryRows" row-key="id">
          <el-table-column label="批次" prop="batchNo" min-width="140" />
          <el-table-column label="药品编号" prop="drugId" min-width="100" />
          <el-table-column label="效期" prop="expiryDate" min-width="120" />
          <el-table-column label="剩余天数" prop="expireDays" min-width="100" />
          <el-table-column label="在库" prop="qtyTotal" min-width="80" />
          <el-table-column label="级别" min-width="100">
            <template #default="{ row }">{{
              row.alertLevel === 1 ? '红' : row.alertLevel === 2 ? '橙' : '黄'
            }}</template>
          </el-table-column>
          <el-table-column label="处理" min-width="180">
            <template #default="{ row }">
              <el-tag v-if="row.handleType !== 0" type="success">{{
                EXPIRY_HANDLE_TYPES[row.handleType]
              }}</el-tag>
              <el-dropdown
                v-else-if="canHandle"
                trigger="click"
                @command="(value: number) => handle(row, value)"
              >
                <el-button link type="primary">选择处理方式</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="value in [1, 2, 3, 4]" :key="value" :command="value">{{
                      EXPIRY_HANDLE_TYPES[value]
                    }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <span v-else>未处理</span>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          v-if="loaded && !failed"
          v-model:page="expiryQuery.pageNo"
          v-model:limit="expiryQuery.pageSize"
          :total="expiryTotal"
          @pagination="load"
        />
      </template>
      <template v-else>
        <el-form :model="reconciliationQuery" inline @submit.prevent="search">
          <el-checkbox v-model="reconciliationQuery.onlyDifference">只看异常</el-checkbox>
          <el-form-item
            ><el-button :loading="loading" @click="search">核对</el-button></el-form-item
          >
        </el-form>
        <el-alert
          title="对账采用同一只读快照，比较批次账、货位账、流水账和有效库存锁；缺少期初流水会标为异常。"
          type="info"
          :closable="false"
        />
        <el-alert v-if="failed" title="三账核对加载失败，请重试。" type="error" :closable="false" />
        <el-table v-loading="loading" :data="reconciliationRows" row-key="batchId">
          <el-table-column label="批次" prop="batchNo" min-width="140" />
          <el-table-column label="批次账" prop="batchTotal" min-width="90" />
          <el-table-column label="货位账" prop="locationTotal" min-width="90" />
          <el-table-column label="流水净额" prop="flowNet" min-width="100" />
          <el-table-column label="批次冻结" prop="batchFrozen" min-width="100" />
          <el-table-column label="有效锁" prop="activeLockQty" min-width="90" />
          <el-table-column label="基线" min-width="90"
            ><template #default="{ row }">{{
              row.openingBaselinePresent ? '有' : '缺失'
            }}</template></el-table-column
          >
          <el-table-column label="结果" min-width="90"
            ><template #default="{ row }"
              ><el-tag :type="row.difference ? 'danger' : 'success'">{{
                row.difference ? '异常' : '一致'
              }}</el-tag></template
            ></el-table-column
          >
        </el-table>
        <Pagination
          v-if="loaded && !failed"
          v-model:page="reconciliationQuery.pageNo"
          v-model:limit="reconciliationQuery.pageSize"
          :total="reconciliationTotal"
          @pagination="load"
        />
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { checkPermi } from '@/utils/permission'
import {
  EXPIRY_HANDLE_TYPES,
  getExpiryPage,
  getReconciliationPage,
  handleExpiry,
  refreshExpiry,
  type ExpiryAlertVO,
  type ExpiryQuery,
  type ReconciliationQuery,
  type ReconciliationVO
} from '@/api/pharmacy/inventory/health'
import type { WarehouseVO } from '@/api/pharmacy/inventory/warehouse'

type Tab = 'expiry' | 'reconciliation'
const visible = ref(false)
const tab = ref<Tab>('expiry')
const warehouseName = ref('')
const warehouseId = ref<number>()
const loading = ref(false)
const refreshing = ref(false)
const loaded = ref(false)
const failed = ref(false)
const expiryRows = ref<ExpiryAlertVO[]>([])
const reconciliationRows = ref<ReconciliationVO[]>([])
const expiryTotal = ref(0)
const reconciliationTotal = ref(0)
const canExpiry = computed(() => checkPermi(['pharmacy:inventory-expiry:query']))
const canRefresh = computed(() => checkPermi(['pharmacy:inventory-expiry:refresh']))
const canHandle = computed(() => checkPermi(['pharmacy:inventory-expiry:handle']))
const canReconciliation = computed(() => checkPermi(['pharmacy:inventory-reconciliation:query']))
const expiryQuery = reactive<ExpiryQuery>({ pageNo: 1, pageSize: 10 })
const reconciliationQuery = reactive<ReconciliationQuery>({ pageNo: 1, pageSize: 10 })
let sequence = 0

const load = async () => {
  if (!visible.value || loading.value) return
  const requestId = ++sequence
  loading.value = true
  failed.value = false
  try {
    if (tab.value === 'expiry') {
      const page = await getExpiryPage({ ...expiryQuery, warehouseId: warehouseId.value })
      if (requestId !== sequence) return
      expiryRows.value = page.list
      expiryTotal.value = page.total
    } else {
      const page = await getReconciliationPage({
        ...reconciliationQuery,
        warehouseId: warehouseId.value
      })
      if (requestId !== sequence) return
      reconciliationRows.value = page.list
      reconciliationTotal.value = page.total
    }
    loaded.value = true
  } catch {
    if (requestId !== sequence) return
    expiryRows.value = []
    reconciliationRows.value = []
    expiryTotal.value = 0
    reconciliationTotal.value = 0
    failed.value = true
  } finally {
    if (requestId === sequence) loading.value = false
  }
}
const search = () => {
  if (tab.value === 'expiry') expiryQuery.pageNo = 1
  else reconciliationQuery.pageNo = 1
  return load()
}
const refresh = async () => {
  if (refreshing.value) return
  refreshing.value = true
  try {
    const result = await refreshExpiry()
    ElMessage.success(`已刷新 ${result.alertDate} 预警，新增或更新 ${result.affectedRows} 条`)
    await load()
  } finally {
    refreshing.value = false
  }
}
const handle = async (row: ExpiryAlertVO, value: number) => {
  await handleExpiry(row.id, value)
  row.handleType = value
  ElMessage.success('效期预警已处理')
}
const close = () => {
  sequence++
  expiryRows.value = []
  reconciliationRows.value = []
  loaded.value = false
}
const open = (warehouse: WarehouseVO) => {
  if (!canExpiry.value && !canReconciliation.value) return
  close()
  warehouseId.value = warehouse.id
  warehouseName.value = `${warehouse.whName}（${warehouse.whCode}）`
  tab.value = canExpiry.value ? 'expiry' : 'reconciliation'
  visible.value = true
  return load()
}
defineExpose({ open })
</script>
