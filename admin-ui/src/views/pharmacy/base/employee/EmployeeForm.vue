<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="720">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="工号" prop="empNo">
            <el-input v-model="formData.empNo" placeholder="请输入工号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="姓名" prop="empName">
            <el-input v-model="formData.empName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属门店" prop="storeId">
            <el-select v-model="formData.storeId" placeholder="请选择门店" class="!w-full" filterable>
              <el-option
                v-for="store in storeList"
                :key="store.id"
                :label="store.storeName"
                :value="store.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="岗位" prop="position">
            <el-select v-model="formData.position" placeholder="请选择岗位" class="!w-full">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_EMPLOYEE_POSITION)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入加密手机号密文" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="关联用户" prop="userId">
            <el-select
              v-model="formData.userId"
              placeholder="请选择关联系统用户"
              filterable
              clearable
              class="!w-full"
            >
              <el-option
                v-for="u in userList"
                :key="u.id"
                :label="u.nickname"
                :value="u.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="药师注册证号" prop="pharmacistNo">
            <el-input v-model="formData.pharmacistNo" placeholder="执业药师注册证号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="入职日期" prop="hireDate">
            <el-date-picker v-model="formData.hireDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="药师资质到期" prop="licenseExpire">
            <el-date-picker v-model="formData.licenseExpire" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="健康证到期" prop="healthCertExpire">
            <el-date-picker v-model="formData.healthCertExpire" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="在职状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_EMPLOYEE_STATUS)"
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
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as EmployeeApi from '@/api/pharmacy/base/employee'
import * as StoreApi from '@/api/pharmacy/base/store'
import * as UserApi from '@/api/system/user'

defineOptions({ name: 'PharmacyBaseEmployeeForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中
const formType = ref('') // 表单的类型
const formData = ref({
  id: undefined,
  empNo: '',
  empName: '',
  phone: '',
  storeId: undefined,
  position: undefined,
  pharmacistNo: '',
  licenseExpire: undefined,
  healthCertExpire: undefined,
  hireDate: undefined,
  status: 1,
  userId: undefined
})
const formRules = reactive({
  empNo: [{ required: true, message: '工号不能为空', trigger: 'blur' }],
  empName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  storeId: [{ required: true, message: '所属门店不能为空', trigger: 'change' }],
  position: [{ required: true, message: '岗位不能为空', trigger: 'change' }],
  status: [{ required: true, message: '在职状态不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 门店列表 */
const storeList = ref<any[]>([])
const getStoreList = async () => {
  storeList.value = await StoreApi.getSimpleStoreList()
}

/** 系统用户列表 */
const userList = ref<any[]>([])
const getUserList = async () => {
  userList.value = await UserApi.getSimpleUserList()
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await getStoreList()
  await getUserList()
  if (id) {
    formLoading.value = true
    try {
      const data = await EmployeeApi.getEmployee(id)
      formData.value = data
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as unknown as EmployeeApi.EmployeeVO
    if (formType.value === 'create') {
      await EmployeeApi.createEmployee(data)
      message.success(t('common.createSuccess'))
    } else {
      await EmployeeApi.updateEmployee(data)
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
  formData.value = {
    id: undefined,
    empNo: '',
    empName: '',
    phone: '',
    storeId: undefined,
    position: undefined,
    pharmacistNo: '',
    licenseExpire: undefined,
    healthCertExpire: undefined,
    hireDate: undefined,
    status: 1,
    userId: undefined
  } as any
  formRef.value?.resetFields()
}
</script>
