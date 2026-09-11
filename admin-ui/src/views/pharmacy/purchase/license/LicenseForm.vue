<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="640">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="供应商" prop="supplierId">
        <el-select
          v-model="formData.supplierId"
          placeholder="请选择供应商"
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
      <el-form-item label="证照类型" prop="licenseType">
        <el-select v-model="formData.licenseType" placeholder="请选择证照类型" clearable>
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_LICENSE_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="证照号" prop="licenseNo">
        <el-input v-model="formData.licenseNo" placeholder="请输入证照号" />
      </el-form-item>
      <el-form-item label="发证日期" prop="issueDate">
        <el-date-picker
          v-model="formData.issueDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择发证日期"
          class="!w-full"
        />
      </el-form-item>
      <el-form-item label="到期日" prop="expireDate">
        <el-date-picker
          v-model="formData.expireDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择到期日"
          class="!w-full"
        />
      </el-form-item>
      <el-form-item label="影像地址" prop="fileUrl">
        <el-input v-model="formData.fileUrl" placeholder="请输入证照影像地址（可选）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">保存</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as LicenseApi from '@/api/pharmacy/purchase/license'
import { toLocalDateString } from '../utils/date'
import * as SupplierApi from '@/api/pharmacy/purchase/supplier'

defineOptions({ name: 'PharmacyPurchaseLicenseForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改

/** 统一默认值，避免隐式 any 与类型漂移 */
const createDefaultFormData = (): LicenseApi.SupplierLicenseVO => ({
  id: undefined,
  supplierId: undefined,
  licenseType: 0,
  licenseNo: '',
  issueDate: '',
  expireDate: '',
  fileUrl: ''
})

const formData = ref<LicenseApi.SupplierLicenseVO>(createDefaultFormData())

const formRules = reactive({
  supplierId: [{ required: true, message: '供应商不能为空', trigger: 'change' }],
  licenseType: [{ required: true, message: '证照类型不能为空', trigger: 'change' }],
  licenseNo: [{ required: true, message: '证照号不能为空', trigger: 'blur' }],
  expireDate: [{ required: true, message: '到期日不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 供应商下拉：使用全部供应商（首营资料登记发生在审核之前） */
const supplierList = ref<SupplierApi.SupplierSimpleVO[]>([])
const getSupplierList = async () => {
  supplierList.value = await SupplierApi.getAllSupplierList()
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    await getSupplierList()
    if (id) {
      const data = await LicenseApi.getLicense(id)
      formData.value = {
        ...createDefaultFormData(),
        ...data,
        // LocalDate 响应是数组 [y,m,d]，必须转成 YYYY-MM-DD 才能回填 el-date-picker
        issueDate: toLocalDateString(data.issueDate),
        expireDate: toLocalDateString(data.expireDate)
      }
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/**
 * 构造保存载荷：显式逐字段返回，剔除 id 与后端计算字段
 * （status 由后端按到期日计算，前端不回传）
 */
const buildSaveData = (): LicenseApi.SupplierLicenseCreateVO => {
  const v = formData.value
  const payload: LicenseApi.SupplierLicenseCreateVO = {
    supplierId: v.supplierId,
    licenseType: v.licenseType,
    licenseNo: v.licenseNo,
    issueDate: v.issueDate || undefined,
    expireDate: v.expireDate,
    fileUrl: v.fileUrl
  }
  return payload
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
      await LicenseApi.createLicense(data)
      message.success(t('common.createSuccess'))
    } else {
      const id = formData.value.id
      if (!id) return
      await LicenseApi.updateLicense({ id, ...data })
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
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
