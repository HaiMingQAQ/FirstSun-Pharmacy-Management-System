<template>
  <div class="pos-cashier">
    <!-- 页头 + 实时统计 -->
    <div class="pos-header">
      <div class="pos-header-left">
        <div class="pos-title">药店 POS 管理 · 收银台</div>
        <div class="pos-subtitle">OTC 现金销售 → 小票预览 → 交班；扣库/支付/积分由 C·E·F 服务提供，未就绪时后端将拦截并提示</div>
      </div>
      <div class="pos-stats">
        <div class="stat-card">
          <div class="stat-label">当前班次</div>
          <div class="stat-value" :class="{ 'stat-muted': !shiftId || shiftId < 1 }">{{ shiftId && shiftId > 0 ? '班次 #' + shiftId : '未开台' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">购物车</div>
          <div class="stat-value">{{ cart.length }} 件</div>
        </div>
        <div class="stat-card stat-card-accent">
          <div class="stat-label">应收合计</div>
          <div class="stat-value stat-amount">¥ {{ formatMoney(payableAmount) }}</div>
        </div>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：购物车 + 结算 -->
      <el-col :span="16">
        <ContentWrap class="mb-15px">
          <div class="panel-head">
            <span class="panel-title"><Icon icon="ep:shopping-cart" class="mr-5px" />销售商品</span>
            <div>
              <el-button type="primary" plain size="small" @click="openAddDialog">
                <Icon icon="ep:plus" class="mr-5px" /> 添加商品
              </el-button>
              <el-button type="danger" plain size="small" :disabled="cart.length === 0" @click="clearCart">
                <Icon icon="ep:delete" class="mr-5px" /> 清空
              </el-button>
            </div>
          </div>
          <el-table :data="cart" border max-height="380" empty-text="暂无商品，点击「添加商品」手动录入（库存选批依赖 C 服务）">
            <el-table-column label="药品名称" align="center" prop="drugName" min-width="140" />
            <el-table-column label="规格" align="center" prop="specification" min-width="100" />
            <el-table-column label="单位" align="center" prop="unit" width="60" />
            <el-table-column label="单价(元)" align="center" width="110">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.price"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  size="small"
                  @change="recalc"
                />
              </template>
            </el-table-column>
            <el-table-column label="数量" align="center" width="120">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.qty"
                  :min="1"
                  :max="999"
                  size="small"
                  @change="recalc"
                />
              </template>
            </el-table-column>
            <el-table-column label="金额(元)" align="center" width="110">
              <template #default="scope">
                <span class="money-cell">{{ formatMoney(scope.row.qty * scope.row.price) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="70">
              <template #default="scope">
                <el-button link type="danger" @click="removeItem(scope.$index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </ContentWrap>

        <ContentWrap>
          <div class="panel-head">
            <span class="panel-title"><Icon icon="ep:money" class="mr-5px" />现金结算</span>
          </div>
          <div class="settle-panel">
            <div class="settle-row">
              <span class="settle-label">应收合计</span>
              <span class="settle-amount">¥ {{ formatMoney(payableAmount) }}</span>
            </div>
            <div class="settle-grid">
              <div class="settle-field">
                <div class="settle-label">实收现金(元)</div>
                <el-input-number
                  v-model="paidAmount"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  class="!w-full"
                  placeholder="实收金额"
                />
              </div>
              <div class="settle-field">
                <div class="settle-label">找零(元)</div>
                <div class="settle-change">{{ formatMoney(changeAmount) }}</div>
              </div>
            </div>
            <div class="settle-field mb-10px">
              <div class="settle-label">备注</div>
              <el-input v-model="remark" type="textarea" :rows="2" placeholder="备注（选填）" />
            </div>
            <div class="settle-actions">
              <el-button type="primary" size="large" :loading="submitLoading" @click="handleSubmit">
                <Icon icon="ep:money" class="mr-5px" /> 现金结算并提交
              </el-button>
              <el-button size="large" :disabled="!lastOrder" @click="handlePrint">
                <Icon icon="ep:printer" class="mr-5px" /> 重打小票
              </el-button>
            </div>
          </div>
        </ContentWrap>
      </el-col>

      <!-- 右侧：班次 + 小票预览 -->
      <el-col :span="8">
        <ContentWrap class="mb-15px">
          <div class="panel-head">
            <span class="panel-title"><Icon icon="ep:timer" class="mr-5px" />当前班次</span>
          </div>
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="门店编号">{{ storeId }}</el-descriptions-item>
            <el-descriptions-item label="收银台号">{{ posNo }}</el-descriptions-item>
            <el-descriptions-item label="收银员ID">{{ cashierId }}</el-descriptions-item>
            <el-descriptions-item label="班次ID">
              <el-tag v-if="shiftId && shiftId > 0" type="success" effect="light">{{ shiftId }}</el-tag>
              <el-tag v-else type="info" effect="plain">未开台</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div class="mt-12px shift-actions">
            <el-button type="success" plain @click="handleOpenShift" :loading="openShiftLoading">
              <Icon icon="ep:switch" class="mr-5px" /> 开台
            </el-button>
            <el-button type="warning" plain @click="handleCloseShift" :loading="closeShiftLoading">
              <Icon icon="ep:switch-button" class="mr-5px" /> 交班
            </el-button>
          </div>
        </ContentWrap>

        <ContentWrap>
          <div class="panel-head">
            <span class="panel-title"><Icon icon="ep:list" class="mr-5px" />小票预览</span>
          </div>
          <div class="pos-receipt">
            <div class="receipt-head">
              <div class="receipt-title">FirstSun 药店</div>
              <div class="receipt-sub">收银小票</div>
            </div>
            <div v-if="lastOrder" class="receipt-meta">单号：{{ lastOrder.orderNo }}</div>
            <div v-if="lastOrder" class="receipt-meta">时间：{{ formatDate(lastOrder.saleTime) }}</div>
            <el-table :data="lastOrderLines" size="small" class="receipt-table">
              <el-table-column label="品名" prop="drugName" />
              <el-table-column label="数量" prop="qty" width="56" align="center" />
              <el-table-column label="金额" width="80" align="right">
                <template #default="scope">
                  {{ formatMoney(scope.row.lineAmount) }}
                </template>
              </el-table-column>
            </el-table>
            <div v-if="lastOrder" class="receipt-total">应付：{{ formatMoney(lastOrder.payableAmount) }}</div>
            <div v-if="lastOrder" class="receipt-total">实收：{{ formatMoney(lastOrder.paidAmount) }}</div>
            <div v-if="lastOrder" class="receipt-total">找零：{{ formatMoney(lastOrder.changeAmount) }}</div>
            <div class="receipt-foot">谢谢惠顾，祝您健康！</div>
          </div>
        </ContentWrap>
      </el-col>
    </el-row>

    <!-- 添加商品弹窗 -->
    <el-dialog v-model="addDialogVisible" title="添加商品（手动录入）" width="560px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="药品ID" prop="drugId">
          <el-input-number v-model="addForm.drugId" :min="1" :controls="false" class="!w-200px" />
        </el-form-item>
        <el-form-item label="批次ID" prop="batchId">
          <el-input-number v-model="addForm.batchId" :min="1" :controls="false" class="!w-200px" />
        </el-form-item>
        <el-form-item label="药品名称" prop="drugName">
          <el-input v-model="addForm.drugName" placeholder="商品名称快照(小票显示)" class="!w-320px" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="addForm.specification" placeholder="如 0.25g*24粒" class="!w-320px" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="addForm.unit" placeholder="如 盒" class="!w-120px" />
        </el-form-item>
        <el-form-item label="单价(元)" prop="price">
          <el-input-number v-model="addForm.price" :min="0" :precision="2" :controls="false" class="!w-200px" />
        </el-form-item>
        <el-form-item label="数量" prop="qty">
          <el-input-number v-model="addForm.qty" :min="1" :max="999" class="!w-120px" />
        </el-form-item>
        <el-form-item label="出库货位ID">
          <el-input-number v-model="addForm.locationId" :min="1" :controls="false" class="!w-200px" />
        </el-form-item>
        <el-form-item label="是否处方药">
          <el-switch v-model="addForm.isRx" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="addForm.isRx === 1" label="处方ID" prop="prescId">
          <el-input-number v-model="addForm.prescId" :min="1" :controls="false" class="!w-200px" />
          <div class="text-12px color-#909399">处方药必须关联已审方通过的处方（E 服务），否则后端拒绝</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="handleAddItem">加入购物车</el-button>
        <el-button @click="addDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 交班弹窗 -->
    <el-dialog v-model="closeShiftDialogVisible" title="交班" width="460px">
      <el-form label-width="100px">
        <el-form-item label="实盘现金(元)">
          <el-input-number v-model="closeShiftForm.cashActual" :min="0" :precision="2" :controls="false" class="!w-200px" />
        </el-form-item>
        <el-form-item label="差异原因">
          <el-input v-model="closeShiftForm.diffReason" type="textarea" :rows="2" placeholder="现金长短款必填原因" class="!w-300px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="handleCloseShiftConfirm">确认交班</el-button>
        <el-button @click="closeShiftDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 打印区（隐藏） -->
    <div class="hidden">
      <button ref="printButtonRef" v-print="printObj" type="button"></button>
      <div id="posReceiptPrint">
        <div class="text-center font-700">FirstSun 药店</div>
        <div class="text-center">收银小票</div>
        <div v-if="lastOrder">单号：{{ lastOrder.orderNo }}</div>
        <div v-if="lastOrder">时间：{{ formatDate(lastOrder.saleTime) }}</div>
        <div v-if="lastOrder">收银员：{{ lastOrder.cashierId }}</div>
        <table class="w-full">
          <thead>
            <tr>
              <th>品名</th>
              <th>数量</th>
              <th>单价</th>
              <th>金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="line in lastOrderLines" :key="line.id">
              <td>{{ line.drugName }}</td>
              <td>{{ line.qty }}</td>
              <td>{{ formatMoney(line.price) }}</td>
              <td>{{ formatMoney(line.lineAmount) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="lastOrder">应付：{{ formatMoney(lastOrder.payableAmount) }}</div>
        <div v-if="lastOrder">实收：{{ formatMoney(lastOrder.paidAmount) }}</div>
        <div v-if="lastOrder">找零：{{ formatMoney(lastOrder.changeAmount) }}</div>
        <div class="text-center">谢谢惠顾，祝您健康！</div>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { SaleOrderApi, SaleOrderSaveReqVO, SaleOrderItemVO, SaleOrderVO } from '@/api/pharmacy/pos/saleOrder'
import { PosShiftApi } from '@/api/pharmacy/pos/shift'

/** 收银台 */
defineOptions({ name: 'PharmacyPosIndex' })

const message = useMessage()

/** 购物车行 */
interface CartItem {
  drugId: number
  batchId: number
  drugName: string
  specification: string
  unit: string
  price: number
  qty: number
  locationId?: number
  isRx: number
  prescId?: number
}

const storeId = ref(1)
const posNo = ref('POS-01')
const cashierId = ref(1)
const shiftId = ref(1)
const remark = ref('')
const cart = ref<CartItem[]>([])
const paidAmount = ref(0)
const submitLoading = ref(false)
const openShiftLoading = ref(false)
const closeShiftLoading = ref(false)

/** 小票数据 */
const lastOrder = ref<SaleOrderVO>()
const lastOrderLines = ref<any[]>([])
const printButtonRef = ref<HTMLButtonElement>()
const printObj = ref({
  id: 'posReceiptPrint',
  popTitle: '收银小票',
  extraCss: '/print.css',
  zIndex: 20003
})

/** 金额计算（服务端为准，此处仅供展示） */
const subtotal = computed(() => cart.value.reduce((sum, item) => sum + item.qty * item.price, 0))
const payableAmount = computed(() => subtotal.value)
const changeAmount = computed(() => (paidAmount.value >= payableAmount.value ? paidAmount.value - payableAmount.value : 0))

const formatMoney = (value: number) => (value ?? 0).toFixed(2)
const formatDate = (value: any) => (value ? new Date(value).toLocaleString() : '-')

const recalc = () => {
  // 计算属性自动更新
}

const openAddDialog = () => {
  addForm.value = {
    drugId: undefined,
    batchId: undefined,
    drugName: '',
    specification: '',
    unit: '',
    price: 0,
    qty: 1,
    locationId: undefined,
    isRx: 0,
    prescId: undefined
  }
  addDialogVisible.value = true
}

/** 添加商品弹窗 */
const addDialogVisible = ref(false)
const addFormRef = ref()
const addForm = ref<any>({})
const addRules = {
  drugId: [{ required: true, message: '药品ID必填', trigger: 'blur' }],
  batchId: [{ required: true, message: '批次ID必填（库存选批由 C 服务提供）', trigger: 'blur' }],
  drugName: [{ required: true, message: '药品名称必填', trigger: 'blur' }],
  price: [{ required: true, message: '单价必填', trigger: 'blur' }],
  qty: [{ required: true, message: '数量必填', trigger: 'blur' }],
  prescId: [{ required: true, message: '处方药必须关联处方ID', trigger: 'blur' }]
}

const handleAddItem = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  cart.value.push({ ...addForm.value })
  addDialogVisible.value = false
  message.success('已加入购物车')
}

const removeItem = (index: number) => {
  cart.value.splice(index, 1)
}

const clearCart = () => {
  cart.value = []
  genIdempotency() // 放弃本次结算意图，重置幂等键
}

/** 生成订单号：SO-门店-yyyyMMddHHmmss-流水 */
const genOrderNo = () => {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const ts = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
  return `SO-${storeId.value}-${ts}-${String(Math.floor(Math.random() * 1000)).padStart(3, '0')}`
}

/** 幂等键：一次结算意图固定订单号/支付号，提交成功后重置；失败保留以便重试（防重复提交重复扣库） */
let pendingOrderNo = ''
let pendingPaymentNo = ''
const genIdempotency = () => {
  pendingOrderNo = genOrderNo()
  pendingPaymentNo = `PAY-${genOrderNo()}`
}
genIdempotency()

/** 提交现金销售单 */
const handleSubmit = async () => {
  if (cart.value.length === 0) {
    message.warning('请先添加商品')
    return
  }
  if (paidAmount.value < payableAmount.value) {
    message.warning('实收现金不足')
    return
  }
  submitLoading.value = true
  try {
    const items: SaleOrderItemVO[] = cart.value.map((item) => ({
      drugId: item.drugId,
      batchId: item.batchId,
      qty: item.qty,
      price: item.price,
      isRx: item.isRx,
      prescId: item.prescId,
      locationId: item.locationId,
      isGift: 0,
      drugName: item.drugName,
      specification: item.specification,
      unit: item.unit
    }))
    const data: SaleOrderSaveReqVO = {
      orderNo: pendingOrderNo,
      storeId: storeId.value,
      shiftId: shiftId.value,
      cashierId: cashierId.value,
      posNo: posNo.value,
      customerName: '散客',
      items,
      payments: [
        {
          payMethod: 1, // 现金
          payAmount: paidAmount.value,
          paymentNo: pendingPaymentNo // 幂等号
        }
      ],
      remark: remark.value
    }
    const orderId = await SaleOrderApi.createSaleOrder(data)
    message.success('销售成功')
    genIdempotency() // 新订单新幂等键
    // 加载小票数据
    lastOrder.value = await SaleOrderApi.getSaleOrder(orderId)
    lastOrderLines.value = (lastOrder.value as any).lines || []
    cart.value = []
    paidAmount.value = 0
    remark.value = ''
  } catch (err) {
    // 后端错误（如库存服务未就绪、处方未审）已由拦截器提示
  } finally {
    submitLoading.value = false
  }
}

/** 重打小票 */
const handlePrint = () => {
  printButtonRef.value?.click()
}

/** 开台 */
const handleOpenShift = async () => {
  openShiftLoading.value = true
  try {
    const id = await PosShiftApi.openShift(storeId.value, posNo.value, cashierId.value)
    shiftId.value = id
    message.success(`开台成功，班次ID：${id}`)
  } catch {} finally {
    openShiftLoading.value = false
  }
}

/** 交班弹窗 */
const closeShiftDialogVisible = ref(false)
const closeShiftForm = ref({ cashActual: 0, diffReason: '' })
const handleCloseShift = () => {
  closeShiftDialogVisible.value = true
}
const handleCloseShiftConfirm = async () => {
  closeShiftLoading.value = true
  try {
    await PosShiftApi.closeShift(shiftId.value, closeShiftForm.value.cashActual, closeShiftForm.value.diffReason)
    message.success('交班成功')
    closeShiftDialogVisible.value = false
  } catch {} finally {
    closeShiftLoading.value = false
  }
}
</script>

<style scoped>
.pos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.pos-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}
.pos-subtitle {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.pos-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.stat-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 8px 18px;
  min-width: 96px;
  text-align: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}
.stat-card-accent {
  background: linear-gradient(135deg, #fef0f0, #fff);
  border-color: #f8d0d0;
}
.stat-label {
  font-size: 12px;
  color: #909399;
}
.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin-top: 2px;
}
.stat-amount {
  color: #f56c6c;
}
.stat-muted {
  color: #c0c4cc;
  font-size: 14px;
  font-weight: 400;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.money-cell {
  color: #f56c6c;
  font-weight: 600;
}
.settle-panel {
  background: linear-gradient(135deg, #fdf6ec, #fff);
  border: 1px solid #f5e3c4;
  border-radius: 10px;
  padding: 14px;
}
.settle-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 10px;
}
.settle-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}
.settle-amount {
  font-size: 26px;
  font-weight: 700;
  color: #f56c6c;
}
.settle-grid {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
}
.settle-field {
  flex: 1;
}
.settle-change {
  font-size: 18px;
  font-weight: 600;
  color: #67c23a;
  line-height: 32px;
}
.settle-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.shift-actions {
  display: flex;
  gap: 8px;
}
.shift-actions .el-button {
  flex: 1;
}
.pos-receipt {
  border: 1px dashed #c0c4cc;
  border-radius: 8px;
  background: #fdfdfd;
  padding: 12px;
  font-family: 'Courier New', 'SimSun', monospace;
  font-size: 13px;
  color: #303133;
  max-height: 380px;
  overflow: auto;
}
.receipt-head {
  text-align: center;
  margin-bottom: 8px;
}
.receipt-title {
  font-weight: 700;
  font-size: 14px;
}
.receipt-sub {
  font-size: 12px;
  color: #606266;
}
.receipt-meta {
  margin-bottom: 2px;
}
.receipt-table {
  --el-table-border-color: #e4e7ed;
  --el-table-header-bg-color: #f5f7fa;
}
.receipt-total {
  margin-top: 6px;
}
.receipt-foot {
  text-align: center;
  margin-top: 10px;
  color: #909399;
}
</style>
