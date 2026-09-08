<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="900">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="药品编码" prop="drugCode">
            <el-input v-model="formData.drugCode" placeholder="请输入药品编码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分类" prop="categoryId">
            <el-tree-select
              v-model="formData.categoryId"
              :data="categoryTree"
              :props="treeProps"
              check-strictly
              placeholder="请选择分类"
              clearable
              node-key="id"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="通用名" prop="genericName">
            <el-input v-model="formData.genericName" placeholder="请输入通用名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商品名" prop="tradeName">
            <el-input v-model="formData.tradeName" placeholder="请输入商品名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="拼音码" prop="spellCode">
            <el-input v-model="formData.spellCode" placeholder="如 amxljn" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规格" prop="specification">
            <el-input v-model="formData.specification" placeholder="如 0.25g*24粒" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="剂型" prop="dosageForm">
            <el-input v-model="formData.dosageForm" placeholder="如 胶囊剂" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="生产厂家" prop="manufacturer">
            <el-input v-model="formData.manufacturer" placeholder="请输入生产厂家" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="批准文号" prop="approvalNo">
            <el-input v-model="formData.approvalNo" placeholder="如 国药准字H20043221" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="药品类型" prop="drugType">
            <el-select v-model="formData.drugType" placeholder="请选择" class="!w-full">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_DRUG_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="处方药" prop="isRx">
            <el-radio-group v-model="formData.isRx">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
              >{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="特殊管理" prop="isSpecial">
            <el-radio-group v-model="formData.isSpecial">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
              >{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="含麻黄碱" prop="isPseudoephedrine">
            <el-radio-group v-model="formData.isPseudoephedrine">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
              >{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="冷链" prop="isColdChain">
            <el-radio-group v-model="formData.isColdChain">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
              >{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="线上可售" prop="saleableOnline">
            <el-radio-group v-model="formData.saleableOnline">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
              >{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="效期管理" prop="needExpiry">
            <el-radio-group v-model="formData.needExpiry">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
              >{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="销售单位" prop="unit">
            <el-input v-model="formData.unit" placeholder="如 盒/瓶/支" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="转换比" prop="conversionRatio">
            <el-input-number v-model="formData.conversionRatio" :min="0" placeholder="如 12" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="零售价" prop="retailPrice">
            <el-input-number v-model="formData.retailPrice" :min="0" :precision="2" :step="0.01" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="会员价" prop="memberPrice">
            <el-input-number v-model="formData.memberPrice" :min="0" :precision="2" :step="0.01" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="参考成本价" prop="costPrice">
            <el-input-number v-model="formData.costPrice" :min="0" :precision="2" :step="0.01" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="最低限售价" prop="minSalePrice">
            <el-input-number v-model="formData.minSalePrice" :min="0" :precision="2" :step="0.01" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="税率" prop="taxRate">
            <el-input-number v-model="formData.taxRate" :min="0" :max="100" :precision="2" :step="0.01" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="医保类别" prop="insuranceType">
            <el-select v-model="formData.insuranceType" placeholder="请选择" class="!w-full">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_INSURANCE_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="储存条件" prop="storageCond">
            <el-select v-model="formData.storageCond" placeholder="请选择" class="!w-full">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_STORAGE_COND)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="库存下限" prop="minStock">
            <el-input-number v-model="formData.minStock" :min="0" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="库存上限" prop="maxStock">
            <el-input-number v-model="formData.maxStock" :min="0" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="启用状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
                :key="dict.value"
                :value="dict.value"
              >{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注信息" />
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
import * as DrugApi from '@/api/pharmacy/base/drug'
import * as CategoryApi from '@/api/pharmacy/base/category'
import { handleTree } from '@/utils/tree'

defineOptions({ name: 'PharmacyBaseDrugForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref({
  id: undefined,
  drugCode: '',
  categoryId: undefined,
  genericName: '',
  tradeName: '',
  spellCode: '',
  specification: '',
  dosageForm: '',
  manufacturer: '',
  approvalNo: '',
  drugType: 1,
  isRx: 0,
  isSpecial: 0,
  isPseudoephedrine: 0,
  isColdChain: 0,
  unit: '盒',
  conversionRatio: undefined,
  retailPrice: 0,
  memberPrice: undefined,
  costPrice: undefined,
  minSalePrice: undefined,
  taxRate: 0,
  insuranceType: 0,
  minStock: 0,
  maxStock: 0,
  storageCond: 0,
  needExpiry: 1,
  defaultLocationId: undefined,
  saleableOnline: 0,
  status: 1,
  remark: '',
  imageUrl: '',
  images: [],
  description: '',
  instructionsUrl: ''
})
const formRules = reactive({
  drugCode: [{ required: true, message: '药品编码不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '所属分类不能为空', trigger: 'change' }],
  genericName: [{ required: true, message: '通用名不能为空', trigger: 'blur' }],
  specification: [{ required: true, message: '规格不能为空', trigger: 'blur' }],
  drugType: [{ required: true, message: '药品类型不能为空', trigger: 'change' }],
  isRx: [{ required: true, message: '是否处方药不能为空', trigger: 'change' }],
  isSpecial: [{ required: true, message: '是否特殊管理不能为空', trigger: 'change' }],
  isPseudoephedrine: [{ required: true, message: '含麻黄碱类不能为空', trigger: 'change' }],
  isColdChain: [{ required: true, message: '是否冷链不能为空', trigger: 'change' }],
  unit: [{ required: true, message: '销售单位不能为空', trigger: 'blur' }],
  retailPrice: [{ required: true, message: '零售价不能为空', trigger: 'blur' }],
  taxRate: [{ required: true, message: '税率不能为空', trigger: 'blur' }],
  insuranceType: [{ required: true, message: '医保类别不能为空', trigger: 'change' }],
  minStock: [{ required: true, message: '库存下限不能为空', trigger: 'blur' }],
  maxStock: [{ required: true, message: '库存上限不能为空', trigger: 'blur' }],
  storageCond: [{ required: true, message: '储存条件不能为空', trigger: 'change' }],
  needExpiry: [{ required: true, message: '是否效期管理不能为空', trigger: 'change' }],
  saleableOnline: [{ required: true, message: '线上可售不能为空', trigger: 'change' }],
  status: [{ required: true, message: '启用状态不能为空', trigger: 'change' }]
})
const formRef = ref()

/** 分类树 */
const categoryTree = ref<any[]>([])
const treeProps = { label: 'catName', value: 'id', children: 'children' }
const getCategoryTree = async () => {
  const list = await CategoryApi.getSimpleCategoryList()
  categoryTree.value = handleTree(list, 'id', 'parentId')
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await getCategoryTree()
  if (id) {
    formLoading.value = true
    try {
      const data = await DrugApi.getDrug(id)
      formData.value = data
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as unknown as DrugApi.DrugVO
    if (formType.value === 'create') {
      await DrugApi.createDrug(data)
      message.success(t('common.createSuccess'))
    } else {
      await DrugApi.updateDrug(data)
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
    drugCode: '',
    categoryId: undefined,
    genericName: '',
    tradeName: '',
    spellCode: '',
    specification: '',
    dosageForm: '',
    manufacturer: '',
    approvalNo: '',
    drugType: 1,
    isRx: 0,
    isSpecial: 0,
    isPseudoephedrine: 0,
    isColdChain: 0,
    unit: '盒',
    conversionRatio: undefined,
    retailPrice: 0,
    memberPrice: undefined,
    costPrice: undefined,
    minSalePrice: undefined,
    taxRate: 0,
    insuranceType: 0,
    minStock: 0,
    maxStock: 0,
    storageCond: 0,
    needExpiry: 1,
    defaultLocationId: undefined,
    saleableOnline: 0,
    status: 1,
    remark: '',
    imageUrl: '',
    images: [],
    description: '',
    instructionsUrl: ''
  } as any
  formRef.value?.resetFields()
}
</script>
