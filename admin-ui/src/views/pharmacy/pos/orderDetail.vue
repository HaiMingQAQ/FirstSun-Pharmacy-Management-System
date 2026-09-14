<template>
  <div class="pharmacy-modern-page">
    <PharmacyPageHeader
      title="销售单详情"
      eyebrow="ORDER DETAIL"
      icon="ep:document"
      total-label="销售单号"
      :total="Number(detail?.order?.id ?? 0)"
      :current-count="detail?.lines?.length ?? 0"
      page-type="销售档案"
      :loading="!detail"
      subtitle="FirstSun 药店管理系统 · 药店 POS"
    />
    <ContentWrap class="pharmacy-panel">
      <el-page-header @back="router.back()" :content="'销售单详情'">
        <template #title>
          <span>返回</span>
        </template>
      </el-page-header>
    </ContentWrap>

    <ContentWrap v-if="detail" class="pharmacy-panel">
      <!-- 主单信息 -->
      <el-descriptions :column="3" border title="销售单信息" class="mb-15px">
        <el-descriptions-item label="单号">{{ detail.order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.order.status)">{{ statusLabel(detail.order.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="销售时间">{{ formatDateTime(detail.order.saleTime) }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ detail.order.storeId }}</el-descriptions-item>
        <el-descriptions-item label="收银台">{{ detail.order.posNo }}</el-descriptions-item>
        <el-descriptions-item label="收银员">{{ detail.order.cashierId }}</el-descriptions-item>
        <el-descriptions-item label="顾客">{{ detail.order.customerName || '散客' }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detail.order.totalQty }}</el-descriptions-item>
        <el-descriptions-item label="退货标志">
          {{ returnFlagLabel(detail.order.returnFlag) }}
        </el-descriptions-item>
        <el-descriptions-item label="原价合计">{{ formatMoney(detail.order.subtotal) }}</el-descriptions-item>
        <el-descriptions-item label="应付">{{ formatMoney(detail.order.payableAmount) }}</el-descriptions-item>
        <el-descriptions-item label="实收">{{ formatMoney(detail.order.paidAmount) }}</el-descriptions-item>
        <el-descriptions-item label="找零">{{ formatMoney(detail.order.changeAmount) }}</el-descriptions-item>
        <el-descriptions-item label="积分抵扣">{{ formatMoney(detail.order.pointsDeduct) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 商品明细 -->
      <div class="mb-8px font-600">商品明细</div>
      <el-table :data="detail.lines" border class="mb-15px">
        <el-table-column label="行号" align="center" prop="lineNo" width="70" />
        <el-table-column label="药品" align="center" prop="drugName" min-width="150" />
        <el-table-column label="规格" align="center" prop="specification" min-width="110" />
        <el-table-column label="单位" align="center" prop="unit" width="60" />
        <el-table-column label="单价(元)" align="center" width="100">
          <template #default="scope">{{ formatMoney(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column label="数量" align="center" prop="qty" width="70" />
        <el-table-column label="金额(元)" align="center" width="100">
          <template #default="scope">{{ formatMoney(scope.row.lineAmount) }}</template>
        </el-table-column>
        <el-table-column label="已退" align="center" prop="returnedQty" width="70" />
        <el-table-column label="处方" align="center" width="70">
          <template #default="scope">
            <el-tag v-if="scope.row.isRx === 1" type="warning">处方药</el-tag>
            <span v-else>OTC</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 支付明细 -->
      <div class="mb-8px font-600">支付明细</div>
      <el-table :data="detail.payments" border>
        <el-table-column label="支付方式" align="center" width="100">
          <template #default="scope">{{ payMethodLabel(scope.row.payMethod) }}</template>
        </el-table-column>
        <el-table-column label="金额(元)" align="center" width="110">
          <template #default="scope">{{ formatMoney(scope.row.payAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '成功' : '其他' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付时间" align="center" prop="paidAt" width="170" :formatter="(row) => formatDateTime(row.paidAt)" />
        <el-table-column label="支付单号" align="center" prop="paymentNo" min-width="170" />
      </el-table>

      <div class="mt-15px">
        <el-button type="warning" plain @click="router.push(`/pharmacy-pos/pos/returnList?orderId=${detail.order.id}`)">
          创建退货
        </el-button>
      </div>
    </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import { SaleOrderApi } from '@/api/pharmacy/pos/saleOrder'

/** POS 销售单详情 */
defineOptions({ name: 'PharmacyPosOrderDetail' })

const router = useRouter()
const route = useRoute()

const detail = ref<any>()

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
const returnFlagLabel = (flag: number) => (flag === 0 ? '正常' : flag === 1 ? '部分退' : '全退')
const payMethodLabel = (method: number) =>
  ({ 1: '现金', 2: '微信', 3: '支付宝', 4: '银行卡', 5: '储值', 6: '医保', 7: '积分' })[method] ?? String(method)
const formatMoney = (value: number) => (value ?? 0).toFixed(2)
const formatDateTime = (value: any) => (value ? new Date(value).toLocaleString() : '-')

const getDetail = async () => {
  const id = Number(route.query.id)
  if (!id) return
  detail.value = await SaleOrderApi.getSaleOrderDetail(id)
}

onMounted(() => {
  getDetail()
})
</script>
