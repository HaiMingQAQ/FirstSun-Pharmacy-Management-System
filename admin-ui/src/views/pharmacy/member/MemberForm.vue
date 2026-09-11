<template>
  <el-drawer
    v-model="drawerVisible"
    :title="drawerTitle"
    direction="rtl"
    size="600px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-divider content-position="left">基本信息</el-divider>
      <el-form-item label="头像" prop="avatar">
        <el-avatar :src="formData.avatar" size="80">
          <Icon icon="ep:user" />
        </el-avatar>
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="formData.mobile" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="formData.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="formData.name" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="性别" prop="sex">
        <el-radio-group v-model="formData.sex">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.SYSTEM_USER_SEX)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="生日" prop="birthday">
        <el-date-picker
          v-model="formData.birthday"
          type="date"
          placeholder="请选择生日"
          value-format="YYYY-MM-DD"
          class="!w-full"
        />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="formData.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="备注" prop="mark">
        <el-input
          v-model="formData.mark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注"
        />
      </el-form-item>

      <el-divider content-position="left">账户信息</el-divider>
      <el-form-item label="会员等级" prop="levelId">
        <el-select v-model="formData.levelId" placeholder="请选择会员等级" class="!w-full">
          <el-option
            v-for="item in levelList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="积分" prop="point">
        <el-input-number
          v-model="formData.point"
          :min="0"
          controls-position="right"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="经验值" prop="experience">
        <el-input-number
          v-model="formData.experience"
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

      <el-divider content-position="left">注册与登录信息</el-divider>
      <el-form-item label="注册IP">
        <span>{{ formData.registerIp || '-' }}</span>
      </el-form-item>
      <el-form-item label="注册终端">
        <dict-tag :type="DICT_TYPE.TERMINAL" :value="formData.registerTerminal" />
      </el-form-item>
      <el-form-item label="登录IP">
        <span>{{ formData.loginIp || '-' }}</span>
      </el-form-item>
      <el-form-item label="最后登录">
        <span>{{ formatDate(formData.loginDate) || '-' }}</span>
      </el-form-item>
      <el-form-item label="注册时间">
        <span>{{ formatDate(formData.createTime) || '-' }}</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">保 存</el-button>
      <el-button @click="drawerVisible = false">取 消</el-button>
    </template>
  </el-drawer>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as MemberApi from '@/api/pharmacy/member/user'
import * as LevelApi from '@/api/pharmacy/member/level'

defineOptions({ name: 'PharmacyMemberUserForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const drawerVisible = ref(false) // 抽屉的是否展示
const drawerTitle = ref('') // 抽屉的标题
const formLoading = ref(false) // 表单的加载中
const formRef = ref() // 表单 Ref

/** 统一默认值，避免隐式 any 与类型漂移 */
const createDefaultFormData = (): MemberApi.MemberUserVO => ({
  id: undefined,
  mobile: '',
  nickname: '',
  avatar: '',
  name: '',
  sex: 0,
  birthday: '',
  status: 1,
  point: 0,
  levelId: 0,
  experience: 0,
  registerIp: '',
  registerTerminal: 0,
  loginIp: '',
  loginDate: new Date(),
  email: '',
  mark: '',
  createTime: undefined
})

const formData = ref<MemberApi.MemberUserVO>(createDefaultFormData())

const formRules = reactive({
  mobile: [{ required: true, message: '手机号不能为空', trigger: 'blur' }],
  nickname: [{ required: true, message: '昵称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

/** 会员等级列表 */
const levelList = ref<LevelApi.MemberLevelSimpleVO[]>([])
const loadLevelList = async () => {
  const data = await LevelApi.getSimpleLevelList()
  levelList.value = data
}

/** 打开抽屉 */
const open = async (id: number) => {
  drawerVisible.value = true
  drawerTitle.value = '会员详情'
  formLoading.value = true
  try {
    await loadLevelList()
    const data = await MemberApi.getMember(id)
    formData.value = { ...createDefaultFormData(), ...data }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开抽屉

/** 构造保存载荷，显式逐字段返回 */
const buildSaveData = (): MemberApi.MemberUserVO => {
  const v = formData.value
  const payload: MemberApi.MemberUserVO = {
    id: v.id,
    mobile: v.mobile,
    nickname: v.nickname,
    name: v.name,
    sex: v.sex,
    birthday: v.birthday,
    status: v.status,
    point: v.point,
    levelId: v.levelId,
    experience: v.experience,
    email: v.email,
    mark: v.mark,
    avatar: v.avatar
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
    await MemberApi.updateMember(data)
    message.success(t('common.updateSuccess'))
    drawerVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 关闭前重置 */
const handleClose = (done: () => void) => {
  formData.value = createDefaultFormData()
  formRef.value?.resetFields()
  done()
}
</script>
