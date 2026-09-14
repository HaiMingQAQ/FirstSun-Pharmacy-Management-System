<template>
  <ContentWrap class="pharmacy-panel">
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="订单号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          placeholder="请输入订单号"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员ID" prop="memberId">
        <el-input
          v-model="queryParams.memberId"
          placeholder="请输入会员ID"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择订单状态" clearable class="!w-240px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.TRADE_ORDER_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="支付状态" prop="payStatus">
        <el-select v-model="queryParams.payStatus" placeholder="请选择支付状态" clearable class="!w-240px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.PAY_ORDER_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="订单类型" prop="orderType">
        <el-select v-model="queryParams.orderType" placeholder="请选择订单类型" clearable class="!w-240px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.TRADE_ORDER_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
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
      <el-table-column label="订单号" align="center" prop="orderNo" width="180" />
      <el-table-column label="会员ID" align="center" prop="memberId" width="100" />
      <el-table-column label="门店ID" align="center" prop="storeId" width="100" />
      <el-table-column label="订单类型" align="center" prop="orderType" width="120">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.TRADE_ORDER_TYPE" :value="scope.row.orderType" />
        </template>
      </el-table-column>
      <el-table-column label="商品金额" align="center" prop="goodsAmount" width="110">
        <template #default="scope">
          ¥{{ formatMoney(scope.row.goodsAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="应付金额" align="center" prop="payableAmount" width="110">
        <template #default="scope">
          <span class="text-red-500 font-medium">¥{{ formatMoney(scope.row.payableAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" align="center" prop="payStatus" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.PAY_ORDER_STATUS" :value="scope.row.payStatus" />
        </template>
      </el-table-column>
      <el-table-column label="订单状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.TRADE_ORDER_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="下单时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          {{ formatDateTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openDetail(scope.row.id)"
            v-hasPermi="['pharmacy:member:order:query']"
          >
            详情
          </el-button>
          <el-button
            link
            type="warning"
            @click="handleCancel(scope.row.id)"
            v-hasPermi="['pharmacy:member:order:update']"
            :disabled="scope.row.status !== 0 && scope.row.status !== 1"
          >
            取消
          </el-button>
          <el-button
            link
            type="success"
            @click="handleVerify(scope.row.id)"
            v-hasPermi="['pharmacy:member:order:update']"
            :disabled="scope.row.status !== 2"
          >
            核销
          </el-button>
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
  </ContentWrap>

  <!-- 订单详情抽屉 -->
  <OrderDetail ref="detailRef" @success="handleDetailSuccess" />
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { formatDateTime } from '@/utils/formatTime'
import * as OrderApi from '@/api/pharmacy/member/order'
import OrderDetail from './OrderDetail.vue'

defineOptions({ name: 'PharmacyMemberOrder' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: '',
  memberId: undefined,
  status: undefined,
  payStatus: undefined,
  orderType: undefined
})
const queryFormRef = ref() // 搜索的表单

/** 金额格式化 */
const formatMoney = (val: number) => {
  return val ? val.toFixed(2) : '0.00'
}

/** 查询订单列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await OrderApi.getOrderPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 操作成功后刷新列表 */
const handleDetailSuccess = async () => {
  await getList()
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 查看详情 */
const detailRef = ref()
const openDetail = (id: number) => {
  detailRef.value.open(id)
}

/** 取消订单 */
const handleCancel = async (id: number) => {
  try {
    await message.confirm('确定要取消该订单吗？')
    await OrderApi.cancelOrder(id, '后台取消')
    message.success('订单取消成功')
    await getList()
  } catch {}
}

/** 核销订单 */
const handleVerify = async (id: number) => {
  try {
    await message.confirm('确定要核销该订单吗？')
    await OrderApi.verifyOrder(id)
    message.success('订单核销成功')
    await getList()
  } catch {}
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
