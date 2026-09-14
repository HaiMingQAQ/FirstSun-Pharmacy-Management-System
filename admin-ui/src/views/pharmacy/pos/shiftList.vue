<template>
  <div class="pharmacy-modern-page">
    <PharmacyPageHeader
      title="收银班次"
      eyebrow="CASHIER SHIFTS"
      icon="ep:timer"
      total-label="班次总数"
      :total="total"
      :current-count="list.length"
      page-type="班次管理"
      :loading="loading"
      subtitle="FirstSun 药店管理系统 · 药店 POS"
    />
    <ContentWrap class="pharmacy-panel">
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="门店" prop="storeId">
        <el-input-number v-model="queryParams.storeId" :min="1" :controls="false" class="!w-140px" />
      </el-form-item>
      <el-form-item label="收银台" prop="posNo">
        <el-input v-model="queryParams.posNo" placeholder="如 POS-01" clearable class="!w-140px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-120px">
          <el-option label="营业中" :value="0" />
          <el-option label="已交班" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="开台时间">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="datetimerange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="!w-360px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="success" plain @click="openOpenShiftDialog">
          <Icon icon="ep:switch" class="mr-5px" /> 开台
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap class="pharmacy-panel">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="班次号" align="center" prop="shiftNo" min-width="170" />
      <el-table-column label="门店" align="center" prop="storeId" width="70" />
      <el-table-column label="收银台" align="center" prop="posNo" width="90" />
      <el-table-column label="收银员" align="center" prop="cashierId" width="80" />
      <el-table-column label="开台时间" align="center" prop="openAt" width="170" :formatter="dateFormatter" />
      <el-table-column label="交班时间" align="center" prop="closeAt" width="170" :formatter="dateFormatter" />
      <el-table-column label="笔数" align="center" prop="saleCount" width="70" />
      <el-table-column label="销售额(元)" align="center" width="110">
        <template #default="scope">{{ formatMoney(scope.row.saleAmount) }}</template>
      </el-table-column>
      <el-table-column label="应收现金(元)" align="center" width="120">
        <template #default="scope">{{ formatMoney(scope.row.cashExpected) }}</template>
      </el-table-column>
      <el-table-column label="实盘现金(元)" align="center" width="120">
        <template #default="scope">{{ formatMoney(scope.row.cashActual) }}</template>
      </el-table-column>
      <el-table-column label="长短款(元)" align="center" width="110">
        <template #default="scope">
          <span :class="scope.row.diffAmount !== 0 ? 'color-#f56c6c' : ''">
            {{ formatMoney(scope.row.diffAmount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="差异原因" align="center" prop="diffReason" min-width="100" />
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
            {{ scope.row.status === 1 ? '已交班' : '营业中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="90">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 0"
            link
            type="warning"
            @click="openCloseShiftDialog(scope.row)"
          >
            交班
          </el-button>
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

  <!-- 开台弹窗 -->
  <el-dialog v-model="openShiftDialogVisible" title="开台" width="440px">
    <el-form label-width="90px">
      <el-form-item label="门店">
        <el-input-number v-model="openShiftForm.storeId" :min="1" :controls="false" class="!w-180px" />
      </el-form-item>
      <el-form-item label="收银台号">
        <el-input v-model="openShiftForm.posNo" placeholder="如 POS-01" class="!w-180px" />
      </el-form-item>
      <el-form-item label="收银员">
        <el-input-number v-model="openShiftForm.cashierId" :min="1" :controls="false" class="!w-180px" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="submitLoading" @click="handleOpenShift">确认开台</el-button>
      <el-button @click="openShiftDialogVisible = false">取消</el-button>
    </template>
  </el-dialog>

  <!-- 交班弹窗 -->
  <el-dialog v-model="closeShiftDialogVisible" title="交班" width="480px">    <el-form label-width="110px">
      <el-form-item label="班次号">
        <span>{{ closeShiftForm.shiftNo }}</span>
      </el-form-item>
      <el-form-item label="系统应收现金">
        <span>{{ formatMoney(closeShiftForm.cashExpected) }}</span>
      </el-form-item>
      <el-form-item label="实盘现金(元)">
        <el-input-number
          v-model="closeShiftForm.cashActual"
          :min="0"
          :precision="2"
          :controls="false"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="差异原因">
        <el-input
          v-model="closeShiftForm.diffReason"
          type="textarea"
          :rows="2"
          placeholder="现金长短款差异必须填写原因"
          class="!w-300px"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="submitLoading" @click="handleCloseShift">确认交班</el-button>
      <el-button @click="closeShiftDialogVisible = false">取消</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { PosShiftApi, PosShiftVO } from '@/api/pharmacy/pos/shift'

/** POS 收银班次 */
defineOptions({ name: 'PharmacyPosShiftList' })

const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<PosShiftVO[]>([])
const dateRange = ref<any[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined,
  posNo: '',
  status: undefined
})
const queryFormRef = ref()
const submitLoading = ref(false)

const formatMoney = (value: number) => (value ?? 0).toFixed(2)

const getList = async () => {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (dateRange.value?.length === 2) {
      params.openTime = dateRange.value
    }
    const data = await PosShiftApi.getShiftPage(params)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  dateRange.value = []
  handleQuery()
}

/** 开台 */
const openShiftDialogVisible = ref(false)
const openShiftForm = reactive({ storeId: 1, posNo: 'POS-01', cashierId: 1 })
const openOpenShiftDialog = () => {
  openShiftDialogVisible.value = true
}
const handleOpenShift = async () => {
  submitLoading.value = true
  try {
    const id = await PosShiftApi.openShift(openShiftForm.storeId, openShiftForm.posNo, openShiftForm.cashierId)
    message.success(`开台成功，班次ID：${id}`)
    openShiftDialogVisible.value = false
    getList()
  } catch {
  } finally {
    submitLoading.value = false
  }
}

/** 交班 */
const closeShiftDialogVisible = ref(false)
const closeShiftForm = reactive<any>({ shiftId: undefined, shiftNo: '', cashExpected: 0, cashActual: 0, diffReason: '' })
const openCloseShiftDialog = (row: any) => {
  closeShiftForm.shiftId = row.id
  closeShiftForm.shiftNo = row.shiftNo
  closeShiftForm.cashExpected = row.cashExpected
  closeShiftForm.cashActual = row.cashExpected ?? 0
  closeShiftForm.diffReason = ''
  closeShiftDialogVisible.value = true
}
const handleCloseShift = async () => {
  submitLoading.value = true
  try {
    await PosShiftApi.closeShift(closeShiftForm.shiftId, closeShiftForm.cashActual, closeShiftForm.diffReason)
    message.success('交班成功')
    closeShiftDialogVisible.value = false
    getList()
  } catch {
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
