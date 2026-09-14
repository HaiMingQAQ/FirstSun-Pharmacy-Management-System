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
          <el-form-item label="供应商编码" prop="supplierCode">
            <el-input v-model="formData.supplierCode" placeholder="请输入供应商编码，如 SUP001" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="供应商名称" prop="supplierName">
            <el-input v-model="formData.supplierName" placeholder="请输入供应商名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="信用代码" prop="creditCode">
            <el-input v-model="formData.creditCode" placeholder="请输入统一社会信用代码" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="许可证号" prop="scopeCode">
            <el-input v-model="formData.scopeCode" placeholder="请输入药品经营/生产许可证号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="联系人" prop="contact">
            <el-input v-model="formData.contact" placeholder="请输入联系人" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入联系电话" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="24">
          <el-form-item label="地址" prop="address">
            <el-input v-model="formData.address" placeholder="请输入供应商地址" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">账务信息</el-divider>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="开户行" prop="bankName">
            <el-input v-model="formData.bankName" placeholder="请输入开户行" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="银行账号" prop="bankAccount">
            <el-input
              v-model="formData.bankAccount"
              placeholder="请输入银行账号（列表页按掩码显示）"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="账期" prop="paymentTerms">
            <el-input v-model="formData.paymentTerms" placeholder="如 月结30天" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="默认折扣率" prop="defaultDiscount">
            <el-input-number
              v-model="formData.defaultDiscount"
              :min="0"
              :max="1"
              :step="0.01"
              :precision="2"
              controls-position="right"
              class="!w-200px"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="24">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
                :key="dict.value"
                :value="dict.value"
              >
                {{ dict.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">保存</el-button>
      <el-button @click="drawerVisible = false">取消</el-button>
    </template>
  </el-drawer>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as SupplierApi from '@/api/pharmacy/purchase/supplier'

defineOptions({ name: 'PharmacyPurchaseSupplierForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const drawerVisible = ref(false) // 抽屉的是否展示
const dialogTitle = ref('') // 抽屉的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改

/** 统一默认值，避免隐式 any 与类型漂移 */
const createDefaultFormData = (): SupplierApi.SupplierVO => ({
  id: undefined,
  supplierCode: '',
  supplierName: '',
  creditCode: '',
  scopeCode: '',
  contact: '',
  phone: '',
  address: '',
  bankName: '',
  bankAccount: '',
  paymentTerms: '',
  defaultDiscount: 1,
  status: 1
})

const formData = ref<SupplierApi.SupplierVO>(createDefaultFormData())

const formRules = reactive({
  supplierCode: [{ required: true, message: '供应商编码不能为空', trigger: 'blur' }],
  supplierName: [{ required: true, message: '供应商名称不能为空', trigger: 'blur' }],
  defaultDiscount: [
    {
      validator: (_rule: any, value: number, callback: any) => {
        if (value === undefined || value === null) {
          callback()
          return
        }
        if (value < 0 || value > 1) {
          callback(new Error('默认折扣率必须在 0 到 1 之间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 打开抽屉 */
const open = async (type: string, id?: number) => {
  drawerVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    if (id) {
      const data = await SupplierApi.getSupplier(id)
      formData.value = { ...createDefaultFormData(), ...data }
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开抽屉

/**
 * 构造保存载荷：显式逐字段返回允许提交的字段，剔除 id 与后端维护字段
 * （approveStatus/auditBy/auditAt/auditOpinion 由独立审核接口维护，不通过保存接口回传）
 */
const buildSaveData = (): SupplierApi.SupplierCreateVO => {
  const v = formData.value
  return {
    supplierCode: v.supplierCode,
    supplierName: v.supplierName,
    creditCode: v.creditCode,
    scopeCode: v.scopeCode,
    contact: v.contact,
    phone: v.phone,
    address: v.address,
    bankName: v.bankName,
    bankAccount: v.bankAccount,
    paymentTerms: v.paymentTerms,
    defaultDiscount: v.defaultDiscount,
    status: v.status
  }
}

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = buildSaveData()
    if (formType.value === 'create') {
      await SupplierApi.createSupplier(data)
      message.success(t('common.createSuccess'))
    } else {
      const id = formData.value.id
      if (!id) return
      await SupplierApi.updateSupplier({ id, ...data })
      message.success(t('common.updateSuccess'))
    }
    drawerVisible.value = false
    // 发送操作成功的事件
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
