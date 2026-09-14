<template>
  <el-drawer
    v-model="drawerVisible"
    :title="dialogTitle"
    size="min(1000px, 92vw)"
    :destroy-on-close="true"
  >
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-divider content-position="left">基础信息</el-divider>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="采购门店" prop="storeId">
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
          <el-form-item label="供应商" prop="supplierId">
            <el-select
              v-model="formData.supplierId"
              placeholder="请选择供应商（仅显示已审核通过）"
              filterable
              clearable
              class="!w-full"
            >
              <el-option
                v-for="item in supplierList"
                :key="item.id"
                :label="`${item.supplierName}（${item.supplierCode}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="下单日期" prop="orderDate">
            <el-date-picker
              v-model="formData.orderDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择下单日期"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="预计到货" prop="expectDate">
            <el-date-picker
              v-model="formData.expectDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择预计到货日期"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="采购建议" prop="isAuto">
            <el-radio-group v-model="formData.isAuto">
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
        <el-col :xs="24" :sm="24" :md="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">采购明细</el-divider>
      <el-button type="primary" plain @click="addLine">
        <Icon icon="ep:plus" class="mr-5px" /> 新增明细行
      </el-button>
      <el-table :data="formData.lines" class="mt-10px" :show-overflow-tooltip="true">
        <el-table-column label="药品" align="left" min-width="220">
          <template #default="scope">
            <el-select
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
          </template>
        </el-table-column>
        <el-table-column label="订购数量" align="right" width="130">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.orderQty"
              :min="1"
              :precision="0"
              controls-position="right"
              class="!w-110px"
            />
          </template>
        </el-table-column>
        <el-table-column label="含税单价" align="right" width="140">
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
        <el-table-column label="折扣率" align="right" width="130">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.discountRate"
              :min="0"
              :max="1"
              :precision="2"
              :step="0.01"
              controls-position="right"
              class="!w-110px"
            />
          </template>
        </el-table-column>
        <el-table-column label="行金额" align="right" width="120">
          <template #default="scope">
            <span>{{ formatAmount(calcLineAmount(scope.row)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="90" fixed="right">
          <template #default="scope">
            <el-button link type="danger" @click="removeLine(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 本地预览金额；正式金额由服务端按同一公式重算 -->
      <el-descriptions class="mt-10px" :column="4" border>
        <el-descriptions-item label="总数量">{{ previewTotalQty }}</el-descriptions-item>
        <el-descriptions-item label="含税总金额">{{ formatAmount(previewTotalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">{{ formatAmount(previewDiscountAmount) }}</el-descriptions-item>
        <el-descriptions-item label="应付金额">{{ formatAmount(previewPayableAmount) }}</el-descriptions-item>
      </el-descriptions>
      <div class="mt-5px">
        <span class="text-12px text-[var(--pharmacy-text-secondary)]">
          以上为本地预览，保存后由服务端按「数量 × 单价 × 折扣率」重新计算并落库。
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
import * as OrderApi from '@/api/pharmacy/purchase/order'
import { toLocalDateString } from '../utils/date'
import * as SupplierApi from '@/api/pharmacy/purchase/supplier'
import * as StoreApi from '@/api/pharmacy/base/store'
import * as DrugApi from '@/api/pharmacy/base/drug'

defineOptions({ name: 'PharmacyPurchaseOrderForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const drawerVisible = ref(false) // 抽屉的是否展示
const dialogTitle = ref('') // 抽屉的标题
const formLoading = ref(false) // 表单加载中
const formType = ref('') // create / update

/** 抽屉内明细行：drugId 允许为空（未选择），提交时收窄为必填 */
type OrderLineFormItem = Omit<OrderApi.PurchaseOrderLineCreateVO, 'drugId'> & { drugId?: number }

/** 抽屉表单数据：创建载荷 + 可选的 id（修改时回填） */
type OrderFormData = Omit<OrderApi.PurchaseOrderCreateVO, 'lines'> & {
  id?: number
  lines: OrderLineFormItem[]
}

/** 统一默认值，避免隐式 any 与类型漂移 */
const createDefaultLine = (): OrderLineFormItem => ({
  lineNo: undefined,
  drugId: undefined,
  orderQty: 1,
  unitPrice: 0,
  discountRate: 1,
  remark: ''
})

const createDefaultFormData = (): OrderFormData => ({
  id: undefined,
  storeId: undefined,
  warehouseId: undefined,
  supplierId: undefined,
  orderDate: '',
  expectDate: '',
  isAuto: 0,
  remark: '',
  lines: [createDefaultLine()]
})

const formData = ref<OrderFormData>(createDefaultFormData())
const formRules = reactive({
  storeId: [{ required: true, message: '采购门店不能为空', trigger: 'change' }],
  supplierId: [{ required: true, message: '供应商不能为空', trigger: 'change' }],
  orderDate: [{ required: true, message: '下单日期不能为空', trigger: 'change' }]
})
const formRef = ref()

/** 下拉数据：门店、可采购供应商、可采购药品 */
const storeList = ref<StoreApi.StoreSimpleVO[]>([])
const supplierList = ref<SupplierApi.SupplierSimpleVO[]>([])
const drugList = ref<DrugApi.DrugVO[]>([])
const loadOptions = async () => {
  const [stores, suppliers, drugs] = await Promise.all([
    StoreApi.getSimpleStoreList(),
    SupplierApi.getSimpleSupplierList(),
    DrugApi.getSimpleDrugList()
  ])
  storeList.value = stores
  supplierList.value = suppliers
  drugList.value = drugs
}

/** 行金额预览：数量 × 单价 × 折扣率，保留两位小数 */
const calcLineAmount = (line: OrderApi.PurchaseOrderLineCreateVO) => {
  const qty = Number(line.orderQty || 0)
  const price = Number(line.unitPrice || 0)
  const rate = line.discountRate === undefined ? 1 : Number(line.discountRate)
  return Number((qty * price * rate).toFixed(2))
}

const formatAmount = (value?: number) => {
  if (value === undefined || value === null) return '0.00'
  return Number(value).toFixed(2)
}

const previewTotalQty = computed(() =>
  formData.value.lines.reduce((sum, line) => sum + Number(line.orderQty || 0), 0)
)
const previewTotalAmount = computed(() =>
  formData.value.lines.reduce(
    (sum, line) => sum + Number(line.orderQty || 0) * Number(line.unitPrice || 0),
    0
  )
)
const previewPayableAmount = computed(() =>
  formData.value.lines.reduce((sum, line) => sum + calcLineAmount(line), 0)
)
const previewDiscountAmount = computed(() => previewTotalAmount.value - previewPayableAmount.value)

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
      const data = (await OrderApi.getPurchaseOrder(id)) as OrderApi.PurchaseOrderDetailVO
      formData.value = {
        ...createDefaultFormData(),
        id: data.id,
        storeId: data.storeId,
        warehouseId: data.warehouseId,
        supplierId: data.supplierId,
        // LocalDate 响应是数组 [y,m,d]，必须转成 YYYY-MM-DD 才能回填 el-date-picker
        orderDate: toLocalDateString(data.orderDate),
        expectDate: toLocalDateString(data.expectDate),
        isAuto: data.isAuto,
        remark: data.remark,
        lines: (data.lines || []).map((line) => ({
          lineNo: line.lineNo,
          drugId: line.drugId,
          orderQty: line.orderQty || 1,
          unitPrice: Number(line.unitPrice || 0),
          discountRate: line.discountRate === undefined ? 1 : Number(line.discountRate),
          remark: line.remark
        }))
      }
      if (formData.value.lines.length === 0) {
        formData.value.lines.push(createDefaultLine())
      }
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open })

/**
 * 构造保存载荷：显式逐字段返回，剔除 id 与后端维护字段
 * （orderNo/金额/status/auditBy/auditAt 由后端生成或重算）
 */
const buildSaveData = (): OrderApi.PurchaseOrderCreateVO => {
  const v = formData.value
  return {
    storeId: v.storeId,
    warehouseId: v.warehouseId,
    supplierId: v.supplierId,
    orderDate: v.orderDate,
    expectDate: v.expectDate,
    isAuto: v.isAuto,
    remark: v.remark,
    lines: v.lines.map((line, index) => {
      const drugId = line.drugId
      if (drugId === undefined) {
        // 前置校验已拦截空药品，这里只做类型收窄
        throw new Error('采购明细的药品不能为空')
      }
      return {
        lineNo: index + 1,
        drugId,
        orderQty: line.orderQty,
        unitPrice: line.unitPrice,
        discountRate: line.discountRate,
        remark: line.remark
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
  // 明细校验：药品必选、数量与单价合法
  const invalidLine = formData.value.lines.find(
    (line) => !line.drugId || !line.orderQty || line.orderQty <= 0 || line.unitPrice === undefined
  )
  if (invalidLine) {
    message.warning('请完整填写每条明细的药品、订购数量与含税单价')
    return
  }
  formLoading.value = true
  try {
    const data = buildSaveData()
    if (formType.value === 'create') {
      await OrderApi.createPurchaseOrder(data)
      message.success(t('common.createSuccess'))
    } else {
      const id = formData.value.id
      if (!id) return
      await OrderApi.updatePurchaseOrder({ id, ...data })
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
