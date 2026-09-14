<template>
  <el-dialog
    v-model="visible"
    title="新增货位"
    width="520px"
    :close-on-click-modal="false"
    :before-close="beforeClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" :disabled="saving">
      <el-form-item label="所属仓库">{{ warehouseName }}</el-form-item>
      <el-form-item label="货位编码" prop="locationCode">
        <el-input v-model="form.locationCode" maxlength="32" />
      </el-form-item>
      <el-form-item label="货位类型" prop="locationType">
        <el-select v-model="form.locationType">
          <el-option
            v-for="(label, value) in locationTypes"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="容量" prop="maxCapacity">
        <el-input-number
          v-model="form.maxCapacity"
          :min="0"
          :max="4294967295"
          :precision="0"
          :controls="false"
          placeholder="留空表示不限制"
        />
      </el-form-item>
      <el-alert title="容量留空表示不限制；0 表示不允许存放库存。" type="info" :closable="false" />
      <el-alert
        v-if="failed"
        class="mt-12px"
        title="未确认创建成功，请核对货位记录后再重试；编码相同不会重复创建。"
        type="error"
        :closable="false"
      />
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { checkPermi } from '@/utils/permission'
import {
  createLocation,
  LOCATION_TYPES,
  type LocationCreate
} from '@/api/pharmacy/inventory/location'
import type { WarehouseVO } from '@/api/pharmacy/inventory/warehouse'

const visible = ref(false)
const saving = ref(false)
const failed = ref(false)
const warehouseName = ref('')
const formRef = ref<FormInstance>()
const form = reactive<LocationCreate>({ warehouseId: 0, locationCode: '', locationType: 0 })
// Existing ph_location enumeration; no new dictionary values or types are introduced.
const locationTypes = LOCATION_TYPES
const rules: FormRules = {
  locationCode: [
    {
      required: true,
      whitespace: true,
      max: 32,
      message: '请输入 1–32 字符的货位编码',
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
  ]
}
const open = async (warehouse: WarehouseVO) => {
  if (saving.value || warehouse.status !== 1 || !checkPermi(['pharmacy:inventory-location:create']))
    return
  Object.assign(form, {
    warehouseId: warehouse.id,
    locationCode: '',
    locationType: 0,
    maxCapacity: undefined
  })
  warehouseName.value = `${warehouse.whName}（${warehouse.whCode}）`
  failed.value = false
  visible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}
const beforeClose = (done: () => void) => {
  if (!saving.value) done()
}
const submit = async () => {
  if (saving.value || !checkPermi(['pharmacy:inventory-location:create'])) return
  saving.value = true
  try {
    if (!(await formRef.value?.validate().catch(() => false))) return
    failed.value = false
    try {
      await createLocation({ ...form, maxCapacity: form.maxCapacity ?? undefined })
      visible.value = false
      ElMessage.success('货位创建成功')
    } catch {
      failed.value = true
    }
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
