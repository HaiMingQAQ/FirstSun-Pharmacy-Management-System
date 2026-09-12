<template>
  <el-drawer v-model="visible" title="仓库货位" size="min(900px, 100vw)" @closed="close">
    <div class="pharmacy-page pharmacy-modern-page">
      <p>{{ warehouseName }}</p>
      <el-alert v-if="failed" title="货位加载失败，请重试。" type="error" :closable="false" />
      <el-form :model="query" inline @submit.prevent="search">
        <el-form-item label="货位编码"
          ><el-input v-model="query.code" clearable maxlength="64" @keyup.enter="search"
        /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" class="!w-120px">
            <el-option
              v-for="option in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          ><el-button :loading="loading" @click="search">查询</el-button
          ><el-button :disabled="loading" @click="reset">重置</el-button></el-form-item
        >
      </el-form>
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="货位编码" prop="locationCode" min-width="140" />
        <el-table-column label="类型" min-width="100"
          ><template #default="{ row }">{{
            LOCATION_TYPES[row.locationType] ?? '未知'
          }}</template></el-table-column
        >
        <el-table-column label="容量" min-width="120"
          ><template #default="{ row }">{{
            row.maxCapacity == null ? '不限制' : row.maxCapacity
          }}</template></el-table-column
        >
        <el-table-column label="状态" min-width="90"
          ><template #default="{ row }"
            ><dict-tag :type="DICT_TYPE.PHARMACY_STATUS" :value="row.status" /></template
        ></el-table-column>
        <el-table-column v-if="canUpdate" label="操作" width="80" fixed="right">
          <template #default="{ row }"
            ><el-button link type="primary" @click="editForm?.open(row)">编辑</el-button></template
          >
        </el-table-column>
      </el-table>
      <Pagination
        v-if="loaded && !failed"
        v-model:page="query.pageNo"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="getList"
      />
    </div>
  </el-drawer>
  <LocationEditForm ref="editForm" @success="getList" />
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import LocationEditForm from './LocationEditForm.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import {
  getLocationPage,
  LOCATION_TYPES,
  type LocationQuery,
  type LocationVO
} from '@/api/pharmacy/inventory/location'
import type { WarehouseVO } from '@/api/pharmacy/inventory/warehouse'

const visible = ref(false)
const canUpdate = computed(() => checkPermi(['pharmacy:inventory-location:update']))
const editForm = ref<InstanceType<typeof LocationEditForm>>()
const warehouseName = ref('')
const loading = ref(false)
const failed = ref(false)
const loaded = ref(false)
const list = ref<LocationVO[]>([])
const total = ref(0)
const query = reactive<LocationQuery>({ warehouseId: 0, pageNo: 1, pageSize: 10 })
let sequence = 0
const getList = async () => {
  if (!visible.value || !checkPermi(['pharmacy:inventory-location:query'])) return
  const requestId = ++sequence
  loading.value = true
  failed.value = false
  try {
    const page = await getLocationPage({ ...query })
    if (requestId !== sequence || !visible.value) return
    list.value = page.list
    total.value = page.total
    loaded.value = true
  } catch {
    if (requestId !== sequence || !visible.value) return
    list.value = []
    total.value = 0
    failed.value = true
  } finally {
    if (requestId === sequence) loading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  return getList()
}
const reset = () => {
  query.code = ''
  query.status = undefined
  return search()
}
const close = () => {
  sequence++
  loading.value = false
  list.value = []
  loaded.value = false
}
const open = (warehouse: WarehouseVO) => {
  if (!checkPermi(['pharmacy:inventory-location:query'])) return
  close()
  query.warehouseId = warehouse.id
  query.pageSize = 10
  warehouseName.value = `${warehouse.whName}（${warehouse.whCode}）`
  visible.value = true
  return reset()
}
defineExpose({ open })
</script>
