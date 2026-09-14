<template>
  <div class="pharmacy-modern-page">
    <PharmacyPageHeader
      title="支付单查询"
      eyebrow="PAY ORDER"
      icon="ep:wallet"
      total-label="支付单总数"
      :total="total"
      :current-count="list.length"
      page-type="统一支付"
      :loading="loading"
      subtitle="FirstSun 药店管理系统 · 复用 yudao-module-pay 支付单"
    />
    <ContentWrap class="pharmacy-panel">
      <!-- 搜索工作栏 -->
      <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
        <el-form-item label="商户单号" prop="merchantOrderId">
          <el-input v-model="queryParams.merchantOrderId" placeholder="商户订单号" clearable class="!w-200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="渠道" prop="channelCode">
          <el-input v-model="queryParams.channelCode" placeholder="如 wx_native/mock" clearable class="!w-150px" @keyup.enter="handleQuery" />
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
        <el-table-column label="支付单ID" align="center" prop="id" width="90" />
        <el-table-column label="商户订单号" align="center" prop="merchantOrderId" min-width="180" show-overflow-tooltip />
        <el-table-column label="商品标题" align="center" prop="subject" min-width="140" show-overflow-tooltip />
        <el-table-column label="金额(元)" align="center" width="100">
          <template #default="scope">{{ fenToYuan(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column label="渠道" align="center" prop="channelCode" width="110" />
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
    <el-drawer v-model="detailVisible" title="支付单详情" size="min(640px, 90vw)" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="支付单ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ detail.channelCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="商户订单号" :span="2">{{ detail.merchantOrderId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品标题" :span="2">{{ detail.subject ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额(元)">{{ fenToYuan(detail.price) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="渠道订单号" :span="2">{{ detail.channelOrderNo ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="成功时间" :span="2">{{ detail.successTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { PayOrderApi, PayOrderVO } from '@/api/pharmacy/pay/payOrder'

/** 支付单查询（E 维护） */
defineOptions({ name: 'PharmacyPayIndex' })

const loading = ref(true)
const total = ref(0)
const list = ref<PayOrderVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  merchantOrderId: '',
  channelCode: '',
  status: undefined as number | undefined
})
const queryFormRef = ref()

const STATUS_OPTIONS = [
  { value: 0, label: '未支付' },
  { value: 10, label: '已支付' },
  { value: 20, label: '已退款' },
  { value: 30, label: '已关闭' }
]
const statusLabel = (v: number) => STATUS_OPTIONS.find((item) => item.value === v)?.label ?? String(v)
const statusTagType = (v: number) => (v === 10 ? 'success' : v === 20 ? 'warning' : v === 30 ? 'info' : 'danger')
const fenToYuan = (v?: number) => (v ?? 0 / 100).toFixed(2)

const getList = async () => {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (!params.merchantOrderId) delete params.merchantOrderId
    if (!params.channelCode) delete params.channelCode
    if (params.status === undefined) delete params.status
    const data = await PayOrderApi.getPayOrderPage(params)
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
  detail.value = await PayOrderApi.getPayOrder(id)
  detailVisible.value = true
}

onMounted(() => {
  getList()
})
</script>
