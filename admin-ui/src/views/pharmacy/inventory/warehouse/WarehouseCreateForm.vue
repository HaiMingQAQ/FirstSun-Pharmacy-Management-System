<template>
  <el-dialog
    v-model="visible"
    title="新增仓库"
    width="520px"
    :close-on-click-modal="false"
    :before-close="beforeClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" :disabled="saving">
      <el-form-item label="仓库编码" prop="whCode">
        <el-input v-model="form.whCode" maxlength="16" />
      </el-form-item>
      <el-form-item label="仓库名称" prop="whName">
        <el-input v-model="form.whName" maxlength="64" />
      </el-form-item>
      <el-form-item label="温区" prop="tempZone">
        <el-select v-model="form.tempZone">
          <el-option
            v-for="option in getIntDictOptions(DICT_TYPE.PHARMACY_STORAGE_COND)"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-alert title="创建后为本门店启用仓库。" type="info" :closable="false" />
      <el-alert
        v-if="failed"
        class="mt-12px"
        title="未确认创建成功，请核对列表后再重试；编码相同不会重复创建。"
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
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import { createWarehouse, type WarehouseCreate } from '@/api/pharmacy/inventory/warehouse'

const emit = defineEmits<{ success: [] }>()
const visible = ref(false)
const saving = ref(false)
const failed = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<WarehouseCreate>({ whCode: '', whName: '', tempZone: 0 })
const rules: FormRules = {
  whCode: [
    {
      required: true,
      whitespace: true,
      max: 16,
      message: '请输入 1–16 字符的编码',
      trigger: 'blur'
    }
  ],
  whName: [
    {
      required: true,
      whitespace: true,
      max: 64,
      message: '请输入 1–64 字符的名称',
      trigger: 'blur'
    }
  ],
  tempZone: [
    { required: true, type: 'number', min: 0, max: 3, message: '请选择温区', trigger: 'change' }
  ]
}
const open = async () => {
  if (!checkPermi(['pharmacy:inventory-warehouse:create']) || saving.value) return
  Object.assign(form, { whCode: '', whName: '', tempZone: 0 })
  failed.value = false
  visible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}
const beforeClose = (done: () => void) => {
  if (!saving.value) done()
}
const submit = async () => {
  if (saving.value || !checkPermi(['pharmacy:inventory-warehouse:create'])) return
  saving.value = true
  try {
    if (!(await formRef.value?.validate().catch(() => false))) return
    failed.value = false
    try {
      await createWarehouse({ ...form })
      visible.value = false
      ElMessage.success('仓库创建成功')
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
