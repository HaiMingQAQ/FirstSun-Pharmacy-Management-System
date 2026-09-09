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
      <el-form-item label="退货单号" prop="returnNo">
        <el-input
          v-model="queryParams.returnNo"
          placeholder="退货单号"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="原销售单" prop="saleOrderId">
        <el-input-number v-model="queryParams.saleOrderId" :min="1" :controls="false" class="!w-150px" />
      </el-form-item>
      <el-form-item label="门店" prop="storeId">
        <el-input-number v-model="queryParams.storeId" :min="1" :controls="false" class="!w-120px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="warning" plain @click="openReturnDialog">
          <Icon icon="ep:refresh-left" class="mr-5px" /> 新建退货
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="退货单号" align="center" prop="returnNo" min-width="180" />
      <el-table-column label="原销售单" align="center" prop="saleOrderId" width="110" />
      <el-table-column label="门店" align="center" prop="storeId" width="70" />
      <el-table-column label="类型" align="center" width="80">
        <template #default="scope">{{ scope.row.returnType === 0 ? '退货' : '换货' }}</template>
      </el-table-column>
      <el-table-column label="退款金额(元)" align="center" width="120">
        <template #default="scope">{{ formatMoney(scope.row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="退款方式" align="center" width="90">
        <template #default="scope">
          {{ scope.row.refundMethod === 0 ? '原路' : scope.row.refundMethod === 1 ? '现金' : '余额' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 3 ? 'success' : 'info'">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="经办人" align="center" prop="cashierId" width="80" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="80">
        <template #default="scope">
          <el-button link type="primary" @click="handleDetail(scope.row.id)">详情</el-button>
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

  <!-- 新建退货弹窗 -->
  <el-dialog v-model="returnDialogVisible" title="新建退货" width="720px" :close-on-click-modal="false">
    <el-form ref="returnFormRef" :model="returnForm" :rules="returnRules" label-width="110px">
      <el-form-item label="原销售单ID" prop="saleOrderId">
        <el-input-number
          v-model="returnForm.saleOrderId"
          :min="1"
          :controls="false"
          class="!w-200px"
          @change="loadSaleOrderLines"
        />
        <el-button class="ml-10px" @click="loadSaleOrderLines">加载原单明细</el-button>
      </el-form-item>
      <template v-if="saleOrderLines.length > 0">
        <el-form-item label="原单信息">
          <span class="text-13px">
            单号：{{ saleOrder.orderNo }}｜状态：{{ statusLabel(saleOrder.status) }}｜
            已退：{{ returnFlagLabel(saleOrder.returnFlag) }}
          </span>
        </el-form-item>
        <el-form-item label="退货明细">
          <el-table :data="returnForm.items" border max-height="260" size="small">
            <el-table-column label="药品" align="center" min-width="130">
              <template #default="scope">
                {{ lineOf(scope.row.saleOrderLineId)?.drugName || scope.row.saleOrderLineId }}
              </template>
            </el-table-column>
            <el-table-column label="已售/已退" align="center" width="100">
              <template #default="scope">
                {{ lineOf(scope.row.saleOrderLineId)?.qty ?? '-' }} /
                {{ lineOf(scope.row.saleOrderLineId)?.returnedQty ?? '-' }}
              </template>
            </el-table-column>
            <el-table-column label="本次退货数量" align="center" width="130">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.qty"
                  :min="1"
                  :max="maxReturnable(scope.row.saleOrderLineId)"
                  size="small"
                  :disabled="maxReturnable(scope.row.saleOrderLineId) <= 0"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="60">
              <template #default="scope">
                <el-button link type="danger" @click="removeReturnItem(scope.$index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="mt-8px" size="small" plain @click="addAllReturnable">一键加全部可退</el-button>
        </el-form-item>
        <el-form-item label="退货类型" prop="returnType">
          <el-radio-group v-model="returnForm.returnType">
            <el-radio :value="0">退货</el-radio>
            <el-radio :value="1">换货</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="退货原因" prop="reason">
          <el-select v-model="returnForm.reason" placeholder="选择退货原因" class="!w-200px">
            <el-option v-for="item in REASON_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款方式" prop="refundMethod">
          <el-radio-group v-model="returnForm.refundMethod">
            <el-radio :value="0">原路退回</el-radio>
            <el-radio :value="1">现金退款</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="药师复核">
          <el-switch v-model="returnForm.pharmacistConfirm" :active-value="1" :inactive-value="0" />
          <span class="ml-10px text-12px color-#909399">含处方药时必须复核，否则后端拒绝</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="returnForm.remark" type="textarea" :rows="2" class="!w-400px" />
        </el-form-item>
      </template>
      <el-empty v-else-if="returnForm.saleOrderId && !orderLoading" description="请输入原销售单ID并加载明细" />
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交退货</el-button>
      <el-button @click="returnDialogVisible = false">取消</el-button>
    </template>
  </el-dialog>

  <!-- 详情弹窗 -->
  <el-dialog v-model="detailVisible" title="退货单详情" width="640px">
    <template v-if="returnDetail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="退货单号">{{ returnDetail.returnOrder.returnNo }}</el-descriptions-item>
        <el-descriptions-item label="原销售单">{{ returnDetail.returnOrder.saleOrderId }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">{{ formatMoney(returnDetail.returnOrder.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="退款方式">
          {{ returnDetail.returnOrder.refundMethod === 0 ? '原路' : '现金' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(returnDetail.returnOrder.status) }}</el-descriptions-item>
        <el-descriptions-item label="经办人">{{ returnDetail.returnOrder.cashierId }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="returnDetail.lines" border class="mt-15px" size="small">
        <el-table-column label="销售行" align="center" prop="saleLineId" width="90" />
        <el-table-column label="数量" align="center" prop="qty" width="80" />
        <el-table-column label="单价(元)" align="center" width="100">
          <template #default="scope">{{ formatMoney(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column label="金额(元)" align="center" width="100">
          <template #default="scope">{{ formatMoney(scope.row.lineAmount) }}</template>
        </el-table-column>
      </el-table>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { SaleReturnApi, SaleReturnVO } from '@/api/pharmacy/pos/saleReturn'
import { SaleOrderApi } from '@/api/pharmacy/pos/saleOrder'

/** POS 退货单列表 */
defineOptions({ name: 'PharmacyPosReturnList' })

const route = useRoute()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<SaleReturnVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  returnNo: '',
  saleOrderId: undefined,
  storeId: undefined
})
const queryFormRef = ref()

const REASON_OPTIONS = [
  { value: 1, label: '质量问题' },
  { value: 2, label: '顾客原因' },
  { value: 3, label: '错售' },
  { value: 4, label: '处方变更' },
  { value: 9, label: '其他' }
]

const statusLabel = (status: number) =>
  ({ 0: '草稿', 1: '待审批', 2: '已审核', 3: '已完成', 4: '已取消' })[status] ?? String(status)
const returnFlagLabel = (flag: number) => (flag === 0 ? '正常' : flag === 1 ? '部分退' : '全退')
const formatMoney = (value: number) => (value ?? 0).toFixed(2)

const getList = async () => {
  loading.value = true
  try {
    const data = await SaleReturnApi.getSaleReturnPage(queryParams)
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

/** 新建退货 */
const returnDialogVisible = ref(false)
const returnFormRef = ref()
const returnForm = reactive<any>({
  saleOrderId: undefined,
  returnType: 0,
  reason: undefined,
  refundMethod: 1,
  pharmacistConfirm: 0,
  remark: '',
  items: []
})
const returnRules = {
  saleOrderId: [{ required: true, message: '原销售单ID必填', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择退货原因', trigger: 'change' }]
}
const saleOrder = ref<any>({})
const saleOrderLines = ref<any[]>([])
const orderLoading = ref(false)
const submitLoading = ref(false)

const lineOf = (saleOrderLineId: number) => saleOrderLines.value.find((line) => line.id === saleOrderLineId)
const maxReturnable = (saleOrderLineId: number) => {
  const line = lineOf(saleOrderLineId)
  return line ? line.qty - (line.returnedQty ?? 0) : 0
}

const openReturnDialog = () => {
  returnDialogVisible.value = true
  returnForm.saleOrderId = route.query.orderId ? Number(route.query.orderId) : undefined
  returnForm.items = []
  saleOrderLines.value = []
  if (returnForm.saleOrderId) {
    loadSaleOrderLines()
  }
}

const loadSaleOrderLines = async () => {
  if (!returnForm.saleOrderId) return
  orderLoading.value = true
  try {
    const data = await SaleOrderApi.getSaleOrderDetail(returnForm.saleOrderId)
    saleOrder.value = data.order
    saleOrderLines.value = data.lines
    returnForm.items = []
  } catch {
    saleOrderLines.value = []
  } finally {
    orderLoading.value = false
  }
}

const addAllReturnable = () => {
  const items: any[] = []
  saleOrderLines.value.forEach((line) => {
    const max = line.qty - (line.returnedQty ?? 0)
    if (max > 0) {
      items.push({ saleOrderLineId: line.id, qty: max })
    }
  })
  returnForm.items = items
}

const removeReturnItem = (index: number) => {
  returnForm.items.splice(index, 1)
}

const handleSubmit = async () => {
  const valid = await returnFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (returnForm.items.length === 0) {
    message.warning('请先添加退货明细')
    return
  }
  submitLoading.value = true
  try {
    await SaleReturnApi.createSaleReturn(returnForm)
    message.success('退货成功')
    returnDialogVisible.value = false
    getList()
  } catch {
  } finally {
    submitLoading.value = false
  }
}

/** 详情 */
const detailVisible = ref(false)
const returnDetail = ref<any>()
const handleDetail = async (id: number) => {
  returnDetail.value = await SaleReturnApi.getSaleReturnDetail(id)
  detailVisible.value = true
}

onMounted(() => {
  getList()
})
</script>
