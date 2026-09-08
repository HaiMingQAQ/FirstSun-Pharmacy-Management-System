<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="600">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="药品" prop="drugId">
            <el-select
              v-model="formData.drugId"
              filterable
              remote
              :remote-method="handleDrugSearch"
              placeholder="请选择药品"
              class="!w-full"
            >
              <el-option
                v-for="item in drugOptions"
                :key="item.id"
                :label="`${item.genericName}（${item.drugCode}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="条码" prop="barcode">
            <el-input v-model="formData.barcode" placeholder="请输入条码" maxlength="32" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="条码类型" prop="barcodeType">
            <el-select v-model="formData.barcodeType" placeholder="请选择条码类型" class="!w-full">
              <el-option label="商品条码" :value="0" />
              <el-option label="店内码" :value="1" />
              <el-option label="追溯码" :value="2" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否默认" prop="isDefault">
            <el-select v-model="formData.isDefault" placeholder="请选择是否默认" class="!w-full">
              <el-option label="否" :value="0" />
              <el-option label="是" :value="1" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as BarcodeApi from '@/api/pharmacy/base/barcode'
import * as DrugApi from '@/api/pharmacy/base/drug'

defineOptions({ name: 'BarcodeForm' })

const { t } = useI18n()
const message = useMessage()
const emit = defineEmits(['success'])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const drugOptions = ref<any[]>([])

const formData = ref({
  id: undefined as undefined | number,
  drugId: undefined as undefined | number,
  barcode: '',
  barcodeType: 0,
  isDefault: 0
})

const formRules = reactive({
  drugId: [{ required: true, message: '请选择药品', trigger: 'change' }],
  barcode: [{ required: true, message: '请输入条码', trigger: 'blur' }],
  barcodeType: [{ required: true, message: '请选择条码类型', trigger: 'change' }],
  isDefault: [{ required: true, message: '请选择是否默认', trigger: 'change' }]
})

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增条码' : '编辑条码'
  resetForm()
  await handleDrugSearch('')
  if (type === 'update' && id) {
    formLoading.value = true
    try {
      const data = await BarcodeApi.getBarcode(id)
      formData.value = data
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const handleDrugSearch = async (keyword: string) => {
  const list = await DrugApi.getSimpleDrugList(keyword)
  drugOptions.value = list
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const data = { ...formData.value } as BarcodeApi.BarcodeVO
      if (data.id) {
        await BarcodeApi.updateBarcode(data)
        message.success(t('common.updateSuccess'))
      } else {
        await BarcodeApi.createBarcode(data)
        message.success(t('common.createSuccess'))
      }
      dialogVisible.value = false
      emit('success')
    } finally {
      submitLoading.value = false
    }
  })
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    drugId: undefined,
    barcode: '',
    barcodeType: 0,
    isDefault: 0
  }
  formRef.value?.resetFields()
}
</script>
