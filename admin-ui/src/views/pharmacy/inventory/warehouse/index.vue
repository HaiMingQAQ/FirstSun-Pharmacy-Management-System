<template>
  <el-result v-if="!canQuery" icon="warning" title="无仓库查询权限" />
  <div v-else class="pharmacy-page pharmacy-modern-page">
    <PharmacyPageHeader
      v-if="loaded && !failed"
      title="仓库管理"
      eyebrow="WAREHOUSE"
      icon="ep:house"
      subtitle="本门店仓库 · 编码、温区与启停状态"
      total-label="符合条件的仓库"
      :total="total"
      :current-count="list.length"
      page-type="仓库查询"
      :loading="loading"
    />
    <ContentWrap v-else class="pharmacy-panel">
      <h1>仓库管理</h1>
      <el-alert
        v-if="failed"
        title="仓库数据加载失败，请重试。"
        type="error"
        :closable="false"
        show-icon
      />
      <el-skeleton v-else :rows="2" animated />
    </ContentWrap>
    <ContentWrap class="pharmacy-panel">
      <el-form :model="query" :inline="true" label-width="80px" @submit.prevent="search">
        <el-form-item label="仓库编码">
          <el-input v-model="query.code" clearable maxlength="64" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="仓库名称">
          <el-input v-model="query.name" clearable maxlength="64" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态" class="!w-160px">
            <el-option
              v-for="option in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :loading="loading" @click="search">
            <Icon icon="ep:search" class="mr-5px" /> 查询
          </el-button>
          <el-button :disabled="loading" @click="reset">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
          <el-button
            v-if="canCreate"
            type="primary"
            :disabled="loading"
            @click="createForm?.open()"
          >
            <Icon icon="ep:plus" class="mr-5px" /> 新增仓库
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>
    <ContentWrap class="pharmacy-panel">
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="仓库名称" prop="whName" min-width="180" show-overflow-tooltip />
        <el-table-column label="仓库编码" prop="whCode" min-width="140" />
        <el-table-column label="门店编号" prop="storeId" min-width="120" />
        <el-table-column label="温区" min-width="120">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.PHARMACY_STORAGE_COND" :value="row.tempZone" />
          </template>
        </el-table-column>
        <el-table-column label="默认收货仓" min-width="120">
          <template #default="{ row }">{{ row.isDefault === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.PHARMACY_STATUS" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column
          v-if="
            canCreateLocation ||
            canQueryLocation ||
            canUpdate ||
            canDelete ||
            canLedger ||
            canHealth ||
            canStocktake ||
            canDamage
          "
          label="操作"
          width="360"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button v-if="canLedger" link type="primary" @click="ledger?.open(row)"
              >台账</el-button
            >
            <el-button v-if="canHealth" link type="primary" @click="health?.open(row)"
              >健康</el-button
            >
            <el-button v-if="canStocktake" link type="primary" @click="stocktake?.open(row)"
              >盘点</el-button
            >
            <el-button v-if="canDamage" link type="primary" @click="damage?.open(row)"
              >报损</el-button
            >
            <el-button v-if="canUpdate" link type="primary" @click="editForm?.open(row)"
              >编辑</el-button
            >
            <el-button v-if="canDelete" link type="danger" @click="handleDelete(row)"
              >删除</el-button
            >
            <el-dropdown
              v-if="canQueryLocation || canCreateLocation"
              class="ml-8px"
              trigger="click"
            >
              <el-button link type="primary">更多</el-button>
              <template #dropdown
                ><el-dropdown-menu>
                  <el-dropdown-item v-if="canQueryLocation" @click="locationList?.open(row)"
                    >查看货位</el-dropdown-item
                  >
                  <el-dropdown-item
                    v-if="canCreateLocation"
                    :disabled="row.status !== 1"
                    @click="locationForm?.open(row)"
                    >新增货位</el-dropdown-item
                  >
                </el-dropdown-menu></template
              >
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-if="loaded && !failed"
        v-model:page="query.pageNo"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="getList"
      />
    </ContentWrap>
    <WarehouseCreateForm ref="createForm" @success="search" />
    <WarehouseEditForm ref="editForm" @success="getList" />
    <InventoryLedger ref="ledger" />
    <LocationCreateForm ref="locationForm" />
    <LocationList ref="locationList" />
    <InventoryHealth ref="health" />
    <InventoryStocktake ref="stocktake" />
    <InventoryDamage ref="damage" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PharmacyPageHeader from '@/components/Pharmacy/PharmacyPageHeader.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import WarehouseCreateForm from './WarehouseCreateForm.vue'
import WarehouseEditForm from './WarehouseEditForm.vue'
import InventoryLedger from './InventoryLedger.vue'
import LocationCreateForm from './LocationCreateForm.vue'
import LocationList from './LocationList.vue'
import InventoryHealth from './InventoryHealth.vue'
import InventoryStocktake from './InventoryStocktake.vue'
import InventoryDamage from './InventoryDamage.vue'
import {
  getWarehousePage,
  deleteWarehouse,
  type WarehouseQuery,
  type WarehouseVO
} from '@/api/pharmacy/inventory/warehouse'

defineOptions({ name: 'PharmacyInventoryWarehouse' })

const query = reactive<WarehouseQuery>({ pageNo: 1, pageSize: 10, code: '', name: '' })
const list = ref<WarehouseVO[]>([])
const total = ref(0)
const loading = ref(false)
const loaded = ref(false)
const failed = ref(false)
const canQuery = computed(() => checkPermi(['pharmacy:inventory-warehouse:query']))
const canCreate = computed(() => checkPermi(['pharmacy:inventory-warehouse:create']))
const createForm = ref<InstanceType<typeof WarehouseCreateForm>>()
const canUpdate = computed(() => checkPermi(['pharmacy:inventory-warehouse:update']))
const canDelete = computed(() => checkPermi(['pharmacy:inventory-warehouse:delete']))
const editForm = ref<InstanceType<typeof WarehouseEditForm>>()
const canLedger = computed(() =>
  checkPermi(['pharmacy:inventory-batch:query', 'pharmacy:inventory-flow:query'])
)
const ledger = ref<InstanceType<typeof InventoryLedger>>()
const canCreateLocation = computed(() => checkPermi(['pharmacy:inventory-location:create']))
const locationForm = ref<InstanceType<typeof LocationCreateForm>>()
const canQueryLocation = computed(() => checkPermi(['pharmacy:inventory-location:query']))
const locationList = ref<InstanceType<typeof LocationList>>()
const canHealth = computed(() =>
  checkPermi(['pharmacy:inventory-expiry:query', 'pharmacy:inventory-reconciliation:query'])
)
const health = ref<InstanceType<typeof InventoryHealth>>()
const canStocktake = computed(() =>
  checkPermi(['pharmacy:inventory-stocktake:query', 'pharmacy:inventory-stocktake:create'])
)
const stocktake = ref<InstanceType<typeof InventoryStocktake>>()
const canDamage = computed(() =>
  checkPermi(['pharmacy:inventory-damage:query', 'pharmacy:inventory-damage:create'])
)
const damage = ref<InstanceType<typeof InventoryDamage>>()

const getList = async () => {
  if (loading.value || !canQuery.value) return
  loading.value = true
  failed.value = false
  try {
    const page = await getWarehousePage({ ...query })
    list.value = page.list
    total.value = page.total
    loaded.value = true
  } catch {
    // The shared request interceptor displays the actual server/network error.
    // Clear stale records and expose a persistent failure state instead of presenting an empty success.
    list.value = []
    failed.value = true
  } finally {
    loading.value = false
  }
}

const handleDelete = async (row: WarehouseVO) => {
  if (loading.value || !canDelete.value) return
  const confirmed = await ElMessageBox.confirm(
    `确认删除仓库“${row.whName}”？仅已停用且没有货位、库存、历史业务引用的仓库可删除。`,
    '确认删除',
    { type: 'warning' }
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  try {
    await deleteWarehouse(row.id)
    ElMessage.success('仓库删除成功')
    await getList()
  } catch {
    // The shared request interceptor displays the server reason.
  }
}

const search = () => {
  if (loading.value) return
  query.pageNo = 1
  return getList()
}

const reset = () => {
  if (loading.value) return
  query.code = ''
  query.name = ''
  query.status = undefined
  return search()
}

onMounted(getList)
</script>
