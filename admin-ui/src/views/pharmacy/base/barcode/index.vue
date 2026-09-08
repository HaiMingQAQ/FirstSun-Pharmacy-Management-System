<template>
  <div class="app-container">
    <el-row :gutter="8">
      <el-col :span="24">
        <!-- 搜索栏 -->
        <el-form
          class="-mb-15px"
          :model="queryParams"
          ref="queryFormRef"
          :inline="true"
          label-width="80px"
        >
          <el-form-item label="药品" prop="drugId">
            <el-select
              v-model="queryParams.drugId"
              filterable
              remote
              :remote-method="handleDrugSearch"
              clearable
              placeholder="请选择药品"
              class="!w-240px"
            >
              <el-option
                v-for="item in drugOptions"
                :key="item.id"
                :label="`${item.genericName}（${item.drugCode}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="条码" prop="barcode">
            <el-input
              v-model="queryParams.barcode"
              placeholder="请输入条码"
              clearable
              @keyup.enter="handleQuery"
              class="!w-200px"
            />
          </el-form-item>
          <el-form-item label="条码类型" prop="barcodeType">
            <el-select
              v-model="queryParams.barcodeType"
              placeholder="请选择"
              clearable
              class="!w-160px"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_BARCODE_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="默认" prop="isDefault">
            <el-select
              v-model="queryParams.isDefault"
              placeholder="请选择"
              clearable
              class="!w-120px"
            >
              <el-option label="否" :value="0" />
              <el-option label="是" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery"
              ><Icon icon="ep:search" class="mr-5px" />搜索</el-button
            >
            <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
          </el-form-item>
        </el-form>
      </el-col>
      <el-col :span="6" :xs="24" />
      <el-col :span="18" :xs="24">
        <!-- 操作栏 -->
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              @click="openForm('create')"
              v-hasPermi="['pharmacy:base:barcode:create']"
              ><Icon icon="ep:plus" class="mr-5px" />新增</el-button
            >
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="warning"
              plain
              @click="handleExport"
              :loading="exportLoading"
              v-hasPermi="['pharmacy:base:barcode:export']"
              ><Icon icon="ep:download" class="mr-5px" />导出</el-button
            >
          </el-col>
        </el-row>
      </el-col>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column
        label="药品"
        align="left"
        prop="drugName"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column label="条码" align="center" prop="barcode" width="160" />
      <el-table-column label="条码类型" align="center" prop="barcodeType" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.PHARMACY_BARCODE_TYPE" :value="scope.row.barcodeType" />
        </template>
      </el-table-column>
      <el-table-column label="默认" align="center" prop="isDefault" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isDefault === 1" type="success">是</el-tag>
          <el-tag v-else type="info">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['pharmacy:base:barcode:update']"
            >编辑</el-button
          >
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['pharmacy:base:barcode:delete']"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 表单弹窗 -->
    <BarcodeForm ref="formRef" @success="getList" />
  </div>
</template>

<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import * as BarcodeApi from '@/api/pharmacy/base/barcode'
import * as DrugApi from '@/api/pharmacy/base/drug'
import BarcodeForm from './BarcodeForm.vue'

defineOptions({ name: 'PharmacyBaseBarcode' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const exportLoading = ref(false)
const total = ref(0)
const list = ref<any[]>([])
const drugOptions = ref<any[]>([])
const queryFormRef = ref()
const formRef = ref()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  drugId: undefined as undefined | number,
  barcode: '',
  barcodeType: undefined as undefined | number,
  isDefault: undefined as undefined | number
})

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await BarcodeApi.getBarcodePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

/** 药品下拉远程搜索 */
const handleDrugSearch = async (keyword: string) => {
  const listData = await DrugApi.getSimpleDrugList(keyword)
  drugOptions.value = listData
}

/** 新增/编辑 */
const openForm = (type: 'create' | 'update', id?: number) => {
  formRef.value.open(type, id)
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await BarcodeApi.deleteBarcode(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 导出 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await BarcodeApi.exportBarcode(queryParams)
    download.excel(data, '药品条码.xls')
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 */
onMounted(async () => {
  await handleDrugSearch('')
  await getList()
})
</script>
