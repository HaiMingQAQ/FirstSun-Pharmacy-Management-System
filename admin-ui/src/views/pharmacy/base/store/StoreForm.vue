<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="680">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="门店编码" prop="storeCode">
            <el-input v-model="formData.storeCode" placeholder="请输入门店编码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="门店名称" prop="storeName">
            <el-input v-model="formData.storeName" placeholder="请输入门店名称" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="地址" prop="address">
            <el-input v-model="formData.address" placeholder="请输入地址" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入联系电话" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="营业时间" prop="businessHours">
            <el-input v-model="formData.businessHours" placeholder="如 08:00-22:00" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="经营范围" prop="manageScope">
            <el-input
              v-model="formData.manageScope"
              type="textarea"
              :rows="2"
              placeholder="如 中成药、化学药制剂、抗生素"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="许可证号" prop="licenseNo">
            <el-input v-model="formData.licenseNo" placeholder="药品经营许可证号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="证照到期日" prop="licenseExpire">
            <el-date-picker
              v-model="formData.licenseExpire"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否医保定点" prop="isMedical">
            <el-radio-group v-model="formData.isMedical">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
              >
                {{ dict.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="营业状态" prop="status">
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
        <el-col :span="24">
          <el-form-item label="关联部门" prop="deptId">
            <el-tree-select
              v-model="formData.deptId"
              :data="deptTree"
              :props="{ label: 'name', children: 'children' }"
              check-strictly
              placeholder="不选则不关联部门"
              clearable
              node-key="id"
              class="!w-full"
            />
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
import * as StoreApi from '@/api/pharmacy/base/store'
import * as DeptApi from '@/api/system/dept'
import { handleTree } from '@/utils/tree'

defineOptions({ name: 'PharmacyBaseStoreForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  storeCode: '',
  storeName: '',
  address: '',
  phone: '',
  manageScope: '',
  licenseNo: '',
  licenseExpire: undefined,
  isMedical: 0,
  businessHours: '',
  status: 1,
  deptId: undefined
})
const formRules = reactive({
  storeCode: [{ required: true, message: '门店编码不能为空', trigger: 'blur' }],
  storeName: [{ required: true, message: '门店名称不能为空', trigger: 'blur' }],
  address: [{ required: true, message: '地址不能为空', trigger: 'blur' }],
  isMedical: [{ required: true, message: '是否医保定点不能为空', trigger: 'change' }],
  status: [{ required: true, message: '营业状态不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 部门树 */
const deptTree = ref<any[]>([])
const getDeptTree = async () => {
  const list = await DeptApi.getSimpleDeptList()
  deptTree.value = handleTree(list)
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 加载部门树
  await getDeptTree()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      const data = await StoreApi.getStore(id)
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
    const data = formData.value as unknown as StoreApi.StoreVO
    if (formType.value === 'create') {
      await StoreApi.createStore(data)
      message.success(t('common.createSuccess'))
    } else {
      await StoreApi.updateStore(data)
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
    storeCode: '',
    storeName: '',
    address: '',
    phone: '',
    manageScope: '',
    licenseNo: '',
    licenseExpire: undefined,
    isMedical: 0,
    businessHours: '',
    status: 1,
    deptId: undefined
  } as any
  formRef.value?.resetFields()
}
</script>
