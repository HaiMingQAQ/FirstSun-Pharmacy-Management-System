<template>
  <div class="pharmacy-modern-page">
    <PharmacyPageHeader
      title="退款单查询"
      eyebrow="PAY REFUND"
      icon="ep:refresh-left"
      total-label="退款单总数"
      :total="total"
      :current-count="list.length"
      page-type="统一支付"
      :loading="loading"
      subtitle="FirstSun 药店管理系统 · 复用 yudao-module-pay 退款单"
    />
    <ContentWrap class="pharmacy-panel">
      <!-- 搜索工作栏 -->
      <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
        <el-form-item label="退款编号" prop="merchantRefundId">
          <el-input v-model="queryParams.merchantRefundId" placeholder="商户退款编号" clearable class="!w-200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="支付单ID" prop="payOrderId">
          <el-input-number v-model="queryParams.payOrderId" :min="1" :controls="false" class="!w-140px" placeholder="支付单ID" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-130px">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 列表 -->
    <ContentWrap class="pharmacy-panel">
      <el-table v-loading="loading" :data="list">
        <el-table-column label="退款单ID" align="center" prop="id" width="90" />
        <el-table-column label="支付单ID" align="center" prop="payOrderId" width="90" />
        <el-table-column label="商户退款编号" align="center" prop="merchantRefundId" min-width="180" show-overflow-tooltip />
        <el-table-column label="退款原因" align="center" prop="reason" min-width="140" show-overflow-tooltip />
        <el-table-column label="退款金额(元)" align="center" width="110">
          <template #default="scope">{{ fenToYuan(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" align="center" width="80" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleDetail(scope.row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </ContentWrap>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="退款单详情" size="min(640px, 90vw)" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="退款单ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="支付单ID">{{ detail.payOrderId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="商户退款编号" :span="2">{{ detail.merchantRefundId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="商户订单号" :span="2">{{ detail.merchantOrderId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="退款金额(元)">{{ fenToYuan(detail.price) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退款原因" :span="2">{{ detail.reason ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="成功时间" :span="2">{{ detail.successTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { PayRefundApi, PayRefundVO } from '@/api/pharmacy/pay/payRefund'

/** 退款单查询（E 维护） */
defineOptions({ name: 'PharmacyPayRefund' })

const loading = ref(true)
const total = ref(0)
const list = ref<PayRefundVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  merchantRefundId: '',
  payOrderId: undefined as number | undefined,
  status: undefined as number | undefined
})
const queryFormRef = ref()

const STATUS_OPTIONS = [
  { value: 0, label: '待退款' },
  { value: 1, label: '退款中' },
  { value: 2, label: '退款成功' },
  { value: 3, label: '退款失败' }
]
const statusLabel = (v: number) => STATUS_OPTIONS.find((item) => item.value === v)?.label ?? String(v)
const statusTagType = (v: number) => (v === 2 ? 'success' : v === 3 ? 'danger' : v === 1 ? 'warning' : 'info')
const fenToYuan = (v?: number) => (v ?? 0 / 100).toFixed(2)

const getList = async () => {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (!params.merchantRefundId) delete params.merchantRefundId
    if (params.payOrderId === undefined) delete params.payOrderId
    if (params.status === undefined) delete params.status
    const data = await PayRefundApi.getPayRefundPage(params)
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
  queryFormRef.value?.resetFields()
  handleQuery()
}

const detailVisible = ref(false)
const detail = ref<any>(null)
const handleDetail = async (id: number) => {
  detail.value = await PayRefundApi.getPayRefund(id)
  detailVisible.value = true
}

onMounted(() => {
  getList()
})
</script>
