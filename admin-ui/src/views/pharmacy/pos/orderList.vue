<template>
  <div class="pharmacy-modern-page">
    <PharmacyPageHeader
      title="销售单"
      eyebrow="SALE ORDERS"
      icon="ep:tickets"
      total-label="销售单总数"
      :total="total"
      :current-count="list.length"
      page-type="销售档案"
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
      <el-form-item label="单号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          placeholder="销售单号"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="门店" prop="storeId">
        <el-input-number v-model="queryParams.storeId" :min="1" :controls="false" class="!w-140px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-140px">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="销售时间">
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
        <el-button type="primary" plain @click="router.push('/pharmacy-pos/pos/index')">
          <Icon icon="ep:plus" class="mr-5px" /> 去收银
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap class="pharmacy-panel">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="单号" align="center" prop="orderNo" min-width="190" />
      <el-table-column label="门店" align="center" prop="storeId" width="70" />
      <el-table-column label="收银台" align="center" prop="posNo" width="90" />
      <el-table-column label="收银员" align="center" prop="cashierId" width="80" />
      <el-table-column label="顾客" align="center" prop="customerName" width="100" />
      <el-table-column label="总数量" align="center" prop="totalQty" width="80" />
      <el-table-column label="应付(元)" align="center" width="100">
        <template #default="scope">{{ formatMoney(scope.row.payableAmount) }}</template>
      </el-table-column>
      <el-table-column label="实收(元)" align="center" width="100">
        <template #default="scope">{{ formatMoney(scope.row.paidAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="销售时间" align="center" prop="saleTime" width="170" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="180">
        <template #default="scope">
          <el-button link type="primary" @click="handleDetail(scope.row.id)">详情</el-button>
          <el-button
            link
            type="warning"
            :disabled="![1, 3].includes(scope.row.status)"
            @click="router.push(`/pharmacy-pos/pos/returnList?orderId=${scope.row.id}`)"
          >
            退货
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
  </div>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { SaleOrderApi, SaleOrderVO } from '@/api/pharmacy/pos/saleOrder'

/** POS 销售单列表 */
defineOptions({ name: 'PharmacyPosOrderList' })

const router = useRouter()

const loading = ref(true)
const total = ref(0)
const list = ref<SaleOrderVO[]>([])
const dateRange = ref<any[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: '',
  storeId: undefined,
  status: undefined
})
const queryFormRef = ref()

const STATUS_OPTIONS = [
  { value: 0, label: '待支付' },
  { value: 1, label: '完成' },
  { value: 2, label: '全额退款' },
  { value: 3, label: '部分退款' },
  { value: -1, label: '已取消' }
]

const statusLabel = (status: number) => STATUS_OPTIONS.find((item) => item.value === status)?.label ?? String(status)
const statusTagType = (status: number) => {
  switch (status) {
    case 1:
      return 'success'
    case 2:
      return 'danger'
    case 3:
      return 'warning'
    default:
      return 'info'
  }
}
const formatMoney = (value: number) => (value ?? 0).toFixed(2)

const getList = async () => {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (dateRange.value?.length === 2) {
      params.saleTime = dateRange.value
    }
    const data = await SaleOrderApi.getSaleOrderPage(params)
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

const handleDetail = (id: number) => {
  router.push(`/pharmacy-pos/pos/orderDetail?id=${id}`)
}

onMounted(() => {
  getList()
})
</script>
