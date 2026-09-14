<template>
  <el-dialog
    v-model="visible"
    title="编辑货位"
    width="520px"
    :close-on-click-modal="false"
    :before-close="beforeClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" :disabled="saving">
      <el-form-item label="货位编码" prop="locationCode"
        ><el-input v-model="form.locationCode" maxlength="32"
      /></el-form-item>
      <el-form-item label="货位类型" prop="locationType"
        ><el-select v-model="form.locationType"
          ><el-option
            v-for="(label, value) in LOCATION_TYPES"
            :key="value"
            :label="label"
            :value="value" /></el-select
      ></el-form-item>
      <el-form-item label="容量" prop="maxCapacity"
        ><el-input-number
          v-model="form.maxCapacity"
          :min="0"
          :max="4294967295"
          :precision="0"
          :controls="false"
          placeholder="留空表示不限制"
      /></el-form-item>
      <el-form-item label="状态" prop="status"
        ><el-select v-model="form.status"
          ><el-option
            v-for="option in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
            :key="option.value"
            :label="option.label"
            :value="option.value" /></el-select
      ></el-form-item>
      <el-alert
        title="容量不能低于占用总量；有库存时不能停用或改变类型。容量 0 表示零容量。"
        type="info"
        :closable="false"
      />
      <el-alert
        v-if="failed"
        class="mt-12px"
        title="未确认保存成功。若资料已变化，请关闭表单、刷新列表后重新编辑。"
        type="error"
        :closable="false"
      />
    </el-form>
    <template #footer
      ><el-button :disabled="saving" @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template
    >
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import {
  updateLocation,
  LOCATION_TYPES,
  type LocationFields,
  type LocationVO
} from '@/api/pharmacy/inventory/location'

const emit = defineEmits<{ success: [] }>()
const visible = ref(false)
const saving = ref(false)
const failed = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<LocationFields>({
  locationCode: '',
  locationType: 0,
  maxCapacity: null,
  status: 1
})
let id = 0
let warehouseId = 0
let expected: LocationFields = { ...form }
const rules: FormRules = {
  locationCode: [
    {
      required: true,
      whitespace: true,
      max: 32,
      message: '请输入 1–32 字符的编码',
      trigger: 'blur'
    }
  ],
  locationType: [
    { required: true, type: 'number', min: 0, max: 4, message: '请选择货位类型', trigger: 'change' }
  ],
  maxCapacity: [
    {
      type: 'integer',
      min: 0,
      max: 4294967295,
      message: '请输入有效的非负整数容量',
      trigger: 'blur'
    }
  ],
  status: [
    { required: true, type: 'number', min: 0, max: 1, message: '请选择状态', trigger: 'change' }
  ]
}
const open = async (row: LocationVO) => {
  if (saving.value || !checkPermi(['pharmacy:inventory-location:update'])) return
  id = row.id
  warehouseId = row.warehouseId
  expected = {
    locationCode: row.locationCode,
    locationType: row.locationType,
    maxCapacity: row.maxCapacity ?? null,
    status: row.status
  }
  Object.assign(form, expected)
  failed.value = false
  visible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}
const beforeClose = (done: () => void) => {
  if (!saving.value) done()
}
const submit = async () => {
  if (saving.value || !checkPermi(['pharmacy:inventory-location:update'])) return
  saving.value = true
  try {
    if (!(await formRef.value?.validate().catch(() => false))) return
    if (
      form.status !== expected.status ||
      form.locationType !== expected.locationType ||
      (form.maxCapacity ?? null) !== expected.maxCapacity
    ) {
      const confirmed = await ElMessageBox.confirm(
        '确认修改货位状态、类型或容量？后端会检查库存和未完成作业。',
        '确认修改',
        { type: 'warning' }
      )
        .then(() => true)
        .catch(() => false)
      if (!confirmed) return
    }
    failed.value = false
    try {
      await updateLocation({
        id,
        warehouseId,
        expected: { ...expected },
        value: { ...form, maxCapacity: form.maxCapacity ?? null }
      })
      visible.value = false
      ElMessage.success('货位保存成功')
      emit('success')
    } catch {
      failed.value = true
    }
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
