<template>
  <ContentWrap class="pharmacy-panel">
    <!-- 搜索 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="工号" prop="empNo">
        <el-input v-model="queryParams.empNo" placeholder="请输入工号" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="姓名" prop="empName">
        <el-input v-model="queryParams.empName" placeholder="请输入姓名" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="门店" prop="storeId">
        <el-select v-model="queryParams.storeId" placeholder="请选择门店" clearable class="!w-180px">
          <el-option
            v-for="store in storeList"
            :key="store.id"
            :label="store.storeName"
            :value="store.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="岗位" prop="position">
        <el-select v-model="queryParams.position" placeholder="请选择岗位" clearable class="!w-180px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_EMPLOYEE_POSITION)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-180px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_EMPLOYEE_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap class="pharmacy-panel">
    <el-button type="primary" :icon="Plus" plain @click="openForm('create')" v-hasPermi="['pharmacy:base:employee:create']">
      新增
    </el-button>
    <el-button type="success" :icon="Download" plain @click="handleExport" v-hasPermi="['pharmacy:base:employee:export']" class="ml-10px">
      导出
    </el-button>

    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" class="mt-10px">
      <el-table-column label="员工编号" align="center" prop="id" width="100" />
      <el-table-column label="工号" align="center" prop="empNo" width="120" />
      <el-table-column label="姓名" align="center" prop="empName" width="100" />
      <el-table-column label="所属门店" align="center" prop="storeName" width="160" />
      <el-table-column label="岗位" align="center" prop="position" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.PHARMACY_EMPLOYEE_POSITION" :value="scope.row.position" />
        </template>
      </el-table-column>
      <el-table-column label="在职状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.PHARMACY_EMPLOYEE_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="关联用户" align="center" prop="userNickname" width="120" />
      <el-table-column label="药师注册证号" align="center" prop="pharmacistNo" width="140" />
      <el-table-column label="入职日期" align="center" prop="hireDate" width="120" />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['pharmacy:base:employee:update']">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['pharmacy:base:employee:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <EmployeeForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { Search, Refresh, Plus, Download } from '@element-plus/icons-vue'
import * as EmployeeApi from '@/api/pharmacy/base/employee'
import * as StoreApi from '@/api/pharmacy/base/store'
import EmployeeForm from './EmployeeForm.vue'

defineOptions({ name: 'PharmacyBaseEmployee' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表加载中
const total = ref(0) // 数据总条数
const list = ref<any[]>([]) // 列表数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  empNo: undefined,
  empName: undefined,
  storeId: undefined,
  position: undefined,
  status: undefined
})
const queryFormRef = ref() // 搜索表单 Ref

/** 门店下拉 */
const storeList = ref<any[]>([])
const getStoreList = async () => {
  storeList.value = await StoreApi.getSimpleStoreList()
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await EmployeeApi.getEmployeePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 新增/修改弹窗 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await EmployeeApi.deleteEmployee(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 导出 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    await EmployeeApi.exportEmployee(queryParams)
    message.success(t('common.exportSuccess'))
  } catch {}
}

/** 初始化 */
onMounted(async () => {
  await getStoreList()
  await getList()
})
</script>
