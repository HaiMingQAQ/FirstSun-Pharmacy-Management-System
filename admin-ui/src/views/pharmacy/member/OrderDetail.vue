<template>
  <el-drawer
    v-model="drawerVisible"
    title="订单详情"
    direction="rtl"
    size="700px"
    :before-close="handleClose"
  >
    <div v-loading="loading">
      <!-- 订单基本信息 -->
      <el-divider content-position="left">订单信息</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ orderData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">
          <dict-tag :type="DICT_TYPE.TRADE_ORDER_TYPE" :value="orderData.orderType" />
        </el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <dict-tag :type="DICT_TYPE.TRADE_ORDER_STATUS" :value="orderData.status" />
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <dict-tag :type="DICT_TYPE.PAY_ORDER_STATUS" :value="orderData.payStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="会员ID">{{ orderData.memberId }}</el-descriptions-item>
        <el-descriptions-item label="门店ID">{{ orderData.storeId }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">
          {{ formatDateTime(orderData.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="支付单号">{{ orderData.payNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">
          {{ formatDateTime(orderData.paidAt) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="完成时间">
          {{ formatDateTime(orderData.finishAt) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="核销人">{{ orderData.verifyBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="核销时间">
          {{ formatDateTime(orderData.verifyAt) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="过期时间">
          {{ formatDateTime(orderData.expireAt) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="取货码">{{ orderData.pickupCode || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 金额信息 -->
      <el-divider content-position="left">金额信息</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品金额">
          ¥{{ formatMoney(orderData.goodsAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="优惠金额">
          <span class="text-green-500">-¥{{ formatMoney(orderData.discountAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="优惠券金额">
          <span class="text-green-500">-¥{{ formatMoney(orderData.couponAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="运费金额">
          ¥{{ formatMoney(orderData.freightAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="应付金额" :span="2">
          <span class="text-red-500 text-lg font-bold">
            ¥{{ formatMoney(orderData.payableAmount) }}
          </span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 其他信息 -->
      <el-divider content-position="left">其他信息</el-divider>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="处方ID">
          {{ orderData.prescId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="取消原因">
          {{ orderData.cancelReason || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="订单备注">
          {{ orderData.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 订单明细（预留展示区，实际明细数据需后端接口支持） -->
      <el-divider content-position="left">订单明细</el-divider>
      <el-empty description="暂无明细数据" v-if="!orderItems.length" />
      <el-table v-else :data="orderItems" size="small">
        <el-table-column label="商品名称" prop="drugName" />
        <el-table-column label="数量" align="center" prop="qty" width="80" />
        <el-table-column label="单价" align="center" prop="price" width="100">
          <template #default="scope">¥{{ formatMoney(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column label="小计" align="center" prop="subtotal" width="100">
          <template #default="scope">¥{{ formatMoney(scope.row.subtotal) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>
</template>
<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDateTime } from '@/utils/formatTime'
import * as OrderApi from '@/api/pharmacy/member/order'

defineOptions({ name: 'PharmacyMemberOrderDetail' })

const drawerVisible = ref(false)
const loading = ref(false)

const orderData = ref<OrderApi.MemberOrderVO>({
  id: undefined,
  orderNo: '',
  memberId: 0,
  storeId: 0,
  orderType: 0,
  goodsAmount: 0,
  couponAmount: 0,
  freightAmount: 0,
  discountAmount: 0,
  payableAmount: 0,
  payStatus: 0,
  status: 0,
  payNo: '',
  paidAt: new Date(),
  prescId: 0,
  cancelReason: '',
  remark: '',
  finishAt: new Date(),
  pickupCode: '',
  verifyBy: '',
  verifyAt: new Date(),
  expireAt: new Date()
})

const orderItems = ref<any[]>([])

/** 金额格式化 */
const formatMoney = (val: number) => {
  return val ? val.toFixed(2) : '0.00'
}

/** 打开详情抽屉 */
const open = async (id: number) => {
  drawerVisible.value = true
  loading.value = true
  try {
    const data = await OrderApi.getOrder(id)
    orderData.value = { ...orderData.value, ...data }
    // 订单明细暂为空，待后端接口支持后补充
    orderItems.value = []
  } finally {
    loading.value = false
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

/** 关闭前重置 */
const handleClose = (done: () => void) => {
  orderItems.value = []
  done()
}
</script>
