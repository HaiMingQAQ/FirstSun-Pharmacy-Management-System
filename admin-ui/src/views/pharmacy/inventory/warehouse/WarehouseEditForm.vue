<template>
  <el-dialog
    v-model="visible"
    title="编辑仓库"
    width="520px"
    :close-on-click-modal="false"
    :before-close="beforeClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" :disabled="saving">
      <el-form-item label="仓库编码" prop="whCode"
        ><el-input v-model="form.whCode" maxlength="16"
      /></el-form-item>
      <el-form-item label="仓库名称" prop="whName"
        ><el-input v-model="form.whName" maxlength="64"
      /></el-form-item>
      <el-form-item label="温区" prop="tempZone"
        ><el-select v-model="form.tempZone"
          ><el-option
            v-for="option in getIntDictOptions(DICT_TYPE.PHARMACY_STORAGE_COND)"
            :key="option.value"
            :label="option.label"
            :value="option.value" /></el-select
      ></el-form-item>
      <el-form-item label="状态" prop="status"
        ><el-select v-model="form.status" :disabled="isDefault"
          ><el-option
            v-for="option in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
            :key="option.value"
            :label="option.label"
            :value="option.value" /></el-select
      ></el-form-item>
      <el-alert
        title="停用或变更温区前，需清空库存并完成相关作业。"
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
  updateWarehouse,
  type WarehouseFields,
  type WarehouseVO
} from '@/api/pharmacy/inventory/warehouse'

const emit = defineEmits<{ success: [] }>()
const visible = ref(false)
const saving = ref(false)
const failed = ref(false)
const isDefault = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<WarehouseFields>({ whCode: '', whName: '', tempZone: 0, status: 1 })
let id = 0
let expected: WarehouseFields = { ...form }
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
  ],
  status: [
    { required: true, type: 'number', min: 0, max: 1, message: '请选择状态', trigger: 'change' }
  ]
}
const open = async (row: WarehouseVO) => {
  if (saving.value || !checkPermi(['pharmacy:inventory-warehouse:update'])) return
  id = row.id
  expected = { whCode: row.whCode, whName: row.whName, tempZone: row.tempZone, status: row.status }
  Object.assign(form, expected)
  isDefault.value = row.isDefault !== 0
  failed.value = false
  visible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}
const beforeClose = (done: () => void) => {
  if (!saving.value) done()
}
const submit = async () => {
  if (saving.value || !checkPermi(['pharmacy:inventory-warehouse:update'])) return
  saving.value = true
  try {
    if (!(await formRef.value?.validate().catch(() => false))) return
    if (form.status !== expected.status || form.tempZone !== expected.tempZone) {
      const confirmed = await ElMessageBox.confirm(
        '确认修改仓库状态或温区？后端会检查库存和未完成作业。',
        '确认修改',
        { type: 'warning' }
      )
        .then(() => true)
        .catch(() => false)
      if (!confirmed) return
    }
    failed.value = false
    try {
      await updateWarehouse({ id, expected: { ...expected }, value: { ...form } })
      visible.value = false
      ElMessage.success('仓库保存成功')
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
