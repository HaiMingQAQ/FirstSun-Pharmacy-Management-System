<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="600">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="等级名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入等级名称" />
      </el-form-item>
      <el-form-item label="等级值" prop="level">
        <el-input-number
          v-model="formData.level"
          :min="0"
          controls-position="right"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="升级经验" prop="experience">
        <el-input-number
          v-model="formData.experience"
          :min="0"
          controls-position="right"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="折扣率(%)" prop="discountPercent">
        <el-input-number
          v-model="formData.discountPercent"
          :min="0"
          :max="100"
          :precision="2"
          controls-position="right"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="等级图标" prop="icon">
        <el-input v-model="formData.icon" placeholder="请输入图标地址" />
      </el-form-item>
      <el-form-item label="背景图" prop="backgroundUrl">
        <el-input v-model="formData.backgroundUrl" placeholder="请输入背景图地址" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number
          v-model="formData.sort"
          :min="0"
          controls-position="right"
          class="!w-200px"
        />
      </el-form-item>
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
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as LevelApi from '@/api/pharmacy/member/level'

defineOptions({ name: 'PharmacyMemberLevelForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中
const formType = ref('') // 表单的类型：create - 新增；update - 修改

/** 统一默认值 */
const createDefaultFormData = (): LevelApi.MemberLevelVO => ({
  id: undefined,
  name: '',
  level: 0,
  experience: 0,
  discountPercent: 100,
  icon: '',
  backgroundUrl: '',
  status: 1,
  sort: 0
})

const formData = ref<LevelApi.MemberLevelVO>(createDefaultFormData())

const formRules = reactive({
  name: [{ required: true, message: '等级名称不能为空', trigger: 'blur' }],
  level: [{ required: true, message: '等级值不能为空', trigger: 'blur' }],
  experience: [{ required: true, message: '升级经验不能为空', trigger: 'blur' }],
  discountPercent: [{ required: true, message: '折扣率不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    if (id) {
      const data = await LevelApi.getLevel(id)
      formData.value = { ...createDefaultFormData(), ...data }
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 构造保存载荷 */
const buildSaveData = (): LevelApi.MemberLevelVO => {
  const v = formData.value
  const payload: LevelApi.MemberLevelVO = {
    name: v.name,
    level: v.level,
    experience: v.experience,
    discountPercent: v.discountPercent,
    icon: v.icon,
    backgroundUrl: v.backgroundUrl,
    status: v.status,
    sort: v.sort
  }
  if (formType.value === 'update') {
    payload.id = v.id
  }
  return payload
}

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = buildSaveData()
    if (formType.value === 'create') {
      await LevelApi.createLevel(data)
      message.success(t('common.createSuccess'))
    } else {
      await LevelApi.updateLevel(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
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
