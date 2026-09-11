<template>
  <el-drawer
    v-model="drawerVisible"
    :title="dialogTitle"
    size="min(1100px, 94vw)"
    :destroy-on-close="true"
  >
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-divider content-position="left">收货信息</el-divider>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="关联采购单" prop="orderId">
            <el-select
              v-model="formData.orderId"
              placeholder="选择采购订单（不选则为无单收货）"
              filterable
              clearable
              class="!w-full"
              @change="handleOrderChange"
            >
              <el-option
                v-for="item in receivableOrders"
                :key="item.id"
                :label="`${item.orderNo}（${item.supplierName || '-'}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="收货门店" prop="storeId">
            <el-select v-model="formData.storeId" placeholder="请选择门店" clearable class="!w-full">
              <el-option
                v-for="item in storeList"
                :key="item.id"
                :label="item.storeName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="入库仓库" prop="warehouseId">
            <el-input-number
              v-model="formData.warehouseId"
              :min="1"
              :precision="0"
              controls-position="right"
              class="!w-full"
              placeholder="请输入仓库编号"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="收货时间" prop="receiveDate">
            <!-- value-format="x"：yudao 后端 LocalDateTime 按毫秒时间戳接收，传字符串会被解析为 0（1970） -->
            <el-date-picker
              v-model="formData.receiveDate"
              type="datetime"
              value-format="x"
              placeholder="请选择收货时间"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="无单收货" prop="isFreeReceipt">
            <el-radio-group v-model="formData.isFreeReceipt" @change="handleFreeReceiptChange">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
              >
                {{ dict.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <div class="-mt-10px mb-10px">
        <span class="text-12px text-[var(--pharmacy-text-secondary)]">
          仓库与货位编号属库存主数据（C 负责），当前以编号录入，待库存主数据下拉接口就绪后替换。
        </span>
      </div>

      <el-divider content-position="left">收货明细</el-divider>
      <el-button v-if="!formData.orderId" type="primary" plain @click="addLine">
        <Icon icon="ep:plus" class="mr-5px" /> 新增明细行
      </el-button>
      <el-button v-else type="primary" plain @click="loadOrderLines">
        <Icon icon="ep:refresh" class="mr-5px" /> 重新载入订单明细
      </el-button>

      <el-table :data="formData.lines" class="mt-10px" :show-overflow-tooltip="true">
        <el-table-column label="药品" align="left" min-width="200">
          <template #default="scope">
            <el-select
              v-if="!scope.row.orderLineId"
              v-model="scope.row.drugId"
              placeholder="请选择药品"
              filterable
              clearable
              class="!w-full"
            >
              <el-option
                v-for="drug in drugList"
                :key="drug.id"
                :label="`${drug.genericName}（${drug.drugCode}）`"
                :value="drug.id"
              />
            </el-select>
            <span v-else>{{ drugLabel(scope.row.drugId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="批号" align="left" width="150">
          <template #default="scope">
            <el-input v-model="scope.row.batchNo" placeholder="批号（必填）" />
          </template>
        </el-table-column>
        <el-table-column label="生产日期" align="center" width="150">
          <template #default="scope">
            <el-date-picker
              v-model="scope.row.manufactureDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="生产日期"
              class="!w-130px"
            />
          </template>
        </el-table-column>
        <el-table-column label="有效期至" align="center" width="150">
          <template #default="scope">
            <el-date-picker
              v-model="scope.row.expiryDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="有效期（必填）"
              class="!w-130px"
            />
          </template>
        </el-table-column>
        <el-table-column label="实收数量" align="right" width="130">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.qty"
              :min="1"
              :precision="0"
              controls-position="right"
              class="!w-110px"
            />
          </template>
        </el-table-column>
        <el-table-column label="采购单价" align="right" width="140">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.unitPrice"
              :min="0"
              :precision="2"
              :step="0.01"
              controls-position="right"
              class="!w-120px"
            />
          </template>
        </el-table-column>
        <el-table-column label="货位编号" align="right" width="120">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.locationId"
              :min="1"
              :precision="0"
              controls-position="right"
              class="!w-100px"
              placeholder="货位"
            />
          </template>
        </el-table-column>
        <el-table-column label="质检" align="center" width="130">
          <template #default="scope">
            <el-select v-model="scope.row.qualityFlag" placeholder="质检" class="!w-110px">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_QUALITY_FLAG)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="90" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="danger"
              :disabled="!!scope.row.orderLineId"
              @click="removeLine(scope.$index)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-descriptions class="mt-10px" :column="2" border>
        <el-descriptions-item label="实收总数量">{{ previewTotalQty }}</el-descriptions-item>
        <el-descriptions-item label="实收总金额">{{ formatAmount(previewTotalAmount) }}</el-descriptions-item>
      </el-descriptions>
      <div class="mt-5px">
        <span class="text-12px text-[var(--pharmacy-text-secondary)]">
          以上为本地预览，保存后由服务端重算；入账需库存服务（C）可用，入账前必须填写货位编号。
        </span>
      </div>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">保存</el-button>
      <el-button @click="drawerVisible = false">取消</el-button>
    </template>
  </el-drawer>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as ReceiptApi from '@/api/pharmacy/purchase/receipt'
import { toLocalDateString } from '../utils/date'
import * as OrderApi from '@/api/pharmacy/purchase/order'
import * as StoreApi from '@/api/pharmacy/base/store'
import * as DrugApi from '@/api/pharmacy/base/drug'

defineOptions({ name: 'PharmacyPurchaseReceiptForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const drawerVisible = ref(false) // 抽屉的是否展示
const dialogTitle = ref('') // 抽屉的标题
const formLoading = ref(false) // 表单加载中
const formType = ref('') // create / update

/** 抽屉内明细行：无单收货时 drugId 可为空（未选择），提交时收窄为必填 */
type ReceiptLineFormItem = Omit<ReceiptApi.PurchaseReceiptLineCreateVO, 'drugId'> & {
  drugId?: number
}

/** 抽屉表单数据：创建载荷 + 可选 id（修改时回填） */
type ReceiptFormData = Omit<ReceiptApi.PurchaseReceiptCreateVO, 'lines'> & {
  id?: number
  lines: ReceiptLineFormItem[]
}

const createDefaultLine = (): ReceiptLineFormItem => ({
  lineNo: undefined,
  orderLineId: undefined,
  drugId: undefined,
  batchNo: '',
  manufactureDate: '',
  expiryDate: '',
  qty: 1,
  unitPrice: 0,
  locationId: undefined,
  qualityFlag: 0,
  qaRemark: '',
  coldChainTemp: undefined
})

const createDefaultFormData = (): ReceiptFormData => ({
  id: undefined,
  orderId: undefined,
  storeId: undefined,
  warehouseId: undefined,
  // 默认取当前时间：后端要求毫秒时间戳（number）
  receiveDate: Date.now(),
  isFreeReceipt: 0,
  lines: [createDefaultLine()]
})

const formData = ref<ReceiptFormData>(createDefaultFormData())
const formRules = reactive({
  storeId: [{ required: true, message: '收货门店不能为空', trigger: 'change' }],
  warehouseId: [{ required: true, message: '入库仓库不能为空', trigger: 'blur' }],
  receiveDate: [{ required: true, message: '收货时间不能为空', trigger: 'change' }]
})
const formRef = ref()

/** 下拉数据 */
const storeList = ref<StoreApi.StoreSimpleVO[]>([])
const drugList = ref<DrugApi.DrugVO[]>([])
/** 可收货的采购订单：已审批(2)/已发出(3)/部分到货(4) */
const receivableOrders = ref<OrderApi.PurchaseOrderVO[]>([])

const loadOptions = async () => {
  const [stores, drugs, orders] = await Promise.all([
    StoreApi.getSimpleStoreList(),
    DrugApi.getSimpleDrugList(),
    OrderApi.getPurchaseOrderPage({ pageNo: 1, pageSize: 100 })
  ])
  storeList.value = stores
  drugList.value = drugs
  // 仅保留可继续收货的订单：已审批(2)/已发出(3)/部分到货(4)
  receivableOrders.value = (orders.list || []).filter((item: OrderApi.PurchaseOrderVO) =>
    item.status === 2 || item.status === 3 || item.status === 4
  )
}

const drugLabel = (drugId?: number) => {
  const drug = drugList.value.find((item) => item.id === drugId)
  return drug ? `${drug.genericName}（${drug.drugCode}）` : `药品#${drugId ?? '-'}`
}

const formatAmount = (value?: number) => {
  if (value === undefined || value === null) return '0.00'
  return Number(value).toFixed(2)
}

const previewTotalQty = computed(() =>
  formData.value.lines.reduce((sum, line) => sum + Number(line.qty || 0), 0)
)
const previewTotalAmount = computed(() =>
  formData.value.lines.reduce(
    (sum, line) => sum + Number(line.qty || 0) * Number(line.unitPrice || 0),
    0
  )
)

const addLine = () => {
  formData.value.lines.push(createDefaultLine())
}
const removeLine = (index: number) => {
  if (formData.value.lines.length <= 1) {
    message.warning('至少保留一行明细')
    return
  }
  formData.value.lines.splice(index, 1)
}

/** 切换无单收货标记时同步清空订单关联，避免参数矛盾 */
const handleFreeReceiptChange = (value: number | string | boolean | undefined) => {
  if (Number(value) === 1) {
    formData.value.orderId = undefined
    formData.value.lines = [createDefaultLine()]
  }
}

/** 选择采购订单后载入订单明细（剩余可收数量作为默认实收数量） */
const handleOrderChange = async (orderId?: number) => {
  if (!orderId) {
    formData.value.lines = [createDefaultLine()]
    return
  }
  formData.value.isFreeReceipt = 0
  await loadOrderLines()
}

const loadOrderLines = async () => {
  const orderId = formData.value.orderId
  if (!orderId) {
    message.warning('请先选择采购订单')
    return
  }
  formLoading.value = true
  try {
    const detail = (await OrderApi.getPurchaseOrder(orderId)) as OrderApi.PurchaseOrderDetailVO
    formData.value.storeId = detail.storeId
    formData.value.warehouseId = detail.warehouseId
    formData.value.lines = (detail.lines || []).map((line) => {
      const remaining = (line.orderQty || 0) - (line.receivedQty || 0)
      return {
        lineNo: line.lineNo,
        orderLineId: line.id,
        drugId: line.drugId,
        batchNo: '',
        manufactureDate: '',
        expiryDate: '',
        qty: remaining > 0 ? remaining : 1,
        unitPrice: Number(line.unitPrice || 0),
        locationId: undefined,
        qualityFlag: 0,
        qaRemark: '',
        coldChainTemp: undefined
      }
    })
    if (formData.value.lines.length === 0) {
      formData.value.lines = [createDefaultLine()]
    }
  } finally {
    formLoading.value = false
  }
}

/** 打开抽屉 */
const open = async (type: string, id?: number) => {
  drawerVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    await loadOptions()
    if (id) {
      const data = (await ReceiptApi.getPurchaseReceipt(id)) as ReceiptApi.PurchaseReceiptDetailVO
      formData.value = {
        ...createDefaultFormData(),
        id: data.id,
        orderId: data.orderId,
        storeId: data.storeId,
        warehouseId: data.warehouseId,
        receiveDate: data.receiveDate,
        isFreeReceipt: data.isFreeReceipt,
        lines: (data.lines || []).map((line) => ({
          lineNo: line.lineNo,
          orderLineId: line.orderLineId,
          drugId: line.drugId,
          batchNo: line.batchNo,
          // LocalDate 响应是数组 [y,m,d]，必须转成 YYYY-MM-DD 才能回填 el-date-picker
          manufactureDate: toLocalDateString(line.manufactureDate),
          expiryDate: toLocalDateString(line.expiryDate),
          qty: line.qty || 1,
          unitPrice: Number(line.unitPrice || 0),
          locationId: line.locationId,
          qualityFlag: line.qualityFlag,
          qaRemark: line.qaRemark,
          coldChainTemp: line.coldChainTemp
        }))
      }
      if (formData.value.lines.length === 0) {
        formData.value.lines = [createDefaultLine()]
      }
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open })

/**
 * 构造保存载荷：显式逐字段返回，剔除 id 与后端计算字段
 * （receiptNo/金额/diffType/qualityStatus/status/postedAt 由后端生成或计算）
 */
const buildSaveData = (): ReceiptApi.PurchaseReceiptCreateVO => {
  const v = formData.value
  return {
    orderId: v.orderId,
    storeId: v.storeId,
    warehouseId: v.warehouseId,
    receiveDate: v.receiveDate,
    isFreeReceipt: v.orderId ? 0 : v.isFreeReceipt,
    lines: v.lines.map((line, index) => {
      const drugId = line.drugId
      if (drugId === undefined) {
        // 前置校验已拦截空药品，这里只做类型收窄
        throw new Error('收货明细的药品不能为空')
      }
      return {
        lineNo: index + 1,
        orderLineId: line.orderLineId,
        drugId,
        batchNo: line.batchNo,
        manufactureDate: line.manufactureDate || undefined,
        expiryDate: line.expiryDate,
        qty: line.qty,
        unitPrice: line.unitPrice,
        locationId: line.locationId,
        qualityFlag: line.qualityFlag,
        qaRemark: line.qaRemark,
        coldChainTemp: line.coldChainTemp
      }
    })
  }
}

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 明细校验：药品、批号、有效期必填，数量大于 0
  const invalidLine = formData.value.lines.find(
    (line) => !line.drugId || !line.batchNo || !line.expiryDate || !line.qty || line.qty <= 0
  )
  if (invalidLine) {
    message.warning('请完整填写每条明细的药品、批号、有效期与实收数量')
    return
  }
  formLoading.value = true
  try {
    const data = buildSaveData()
    if (formType.value === 'create') {
      await ReceiptApi.createPurchaseReceipt(data)
      message.success(t('common.createSuccess'))
    } else {
      const id = formData.value.id
      if (!id) return
      await ReceiptApi.updatePurchaseReceipt({ id, ...data })
      message.success(t('common.updateSuccess'))
    }
    drawerVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = createDefaultFormData()
  formRef.value?.resetFields()
}
</script>
