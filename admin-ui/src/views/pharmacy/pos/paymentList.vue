<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="销售单" prop="orderId">
        <el-input-number v-model="queryParams.orderId" :min="1" :controls="false" class="!w-150px" />
      </el-form-item>
      <el-form-item label="支付方式" prop="payMethod">
        <el-select v-model="queryParams.payMethod" placeholder="请选择" clearable class="!w-140px">
          <el-option v-for="item in PAY_METHODS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-120px">
          <el-option label="支付中" :value="0" />
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="2" />
          <el-option label="已退款" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="销售单" align="center" prop="orderId" width="110" />
      <el-table-column label="支付方式" align="center" width="100">
        <template #default="scope">{{ payMethodLabel(scope.row.payMethod) }}</template>
      </el-table-column>
      <el-table-column label="金额(元)" align="center" width="110">
        <template #default="scope">{{ formatMoney(scope.row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="渠道" align="center" prop="channel" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付时间" align="center" prop="paidAt" width="170" :formatter="dateFormatter" />
      <el-table-column label="支付幂等号" align="center" prop="paymentNo" min-width="180" />
      <el-table-column label="外部交易号" align="center" prop="payNo" min-width="150" />
      <el-table-column label="退款时间" align="center" prop="refundAt" width="170" :formatter="dateFormatter" />
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { SalePaymentApi, SalePaymentVO } from '@/api/pharmacy/pos/payment'

/** POS 销售支付明细 */
defineOptions({ name: 'PharmacyPosPaymentList' })

const loading = ref(true)
const total = ref(0)
const list = ref<SalePaymentVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderId: undefined,
  payMethod: undefined,
  status: undefined
})
const queryFormRef = ref()

const PAY_METHODS = [
  { value: 1, label: '现金' },
  { value: 2, label: '微信' },
  { value: 3, label: '支付宝' },
  { value: 4, label: '银行卡' },
  { value: 5, label: '储值' },
  { value: 6, label: '医保' },
  { value: 7, label: '积分' }
]
const payMethodLabel = (method: number) => PAY_METHODS.find((item) => item.value === method)?.label ?? String(method)
const statusLabel = (status: number) => ({ 0: '支付中', 1: '成功', 2: '失败', 3: '已退款', 4: '已冲正' })[status] ?? String(status)
const statusTagType = (status: number) => (status === 1 ? 'success' : status === 3 ? 'warning' : 'info')
const formatMoney = (value: number) => (value ?? 0).toFixed(2)

const getList = async () => {
  loading.value = true
  try {
    const data = await SalePaymentApi.getSalePaymentPage(queryParams)
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
  handleQuery()
}

onMounted(() => {
  getList()
})
</script>
