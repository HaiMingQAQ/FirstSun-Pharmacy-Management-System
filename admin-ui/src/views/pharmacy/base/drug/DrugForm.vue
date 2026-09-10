<template>
  <el-drawer
    v-model="drawerVisible"
    :title="dialogTitle"
    size="min(1000px, 92vw)"
    :destroy-on-close="true"
  >
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <!-- 分区一：基础信息 -->
      <el-divider content-position="left">基础信息</el-divider>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="药品编码" prop="drugCode">
            <el-input v-model="formData.drugCode" placeholder="请输入药品编码" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
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
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="通用名" prop="genericName">
            <el-input v-model="formData.genericName" placeholder="请输入通用名" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="商品名" prop="tradeName">
            <el-input v-model="formData.tradeName" placeholder="请输入商品名" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="拼音码" prop="spellCode">
            <el-input v-model="formData.spellCode" placeholder="如 amxljn" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="规格" prop="specification">
            <el-input v-model="formData.specification" placeholder="如 0.25g*24粒" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="剂型" prop="dosageForm">
            <el-input v-model="formData.dosageForm" placeholder="如 胶囊剂" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="生产厂家" prop="manufacturer">
            <el-input v-model="formData.manufacturer" placeholder="请输入生产厂家" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="批准文号" prop="approvalNo">
            <el-input v-model="formData.approvalNo" placeholder="如 国药准字H20043221" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="销售单位" prop="unit">
            <el-input v-model="formData.unit" placeholder="如 盒/瓶/支" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="转换比" prop="conversionRatio">
            <el-input-number
              v-model="formData.conversionRatio"
              :min="0"
              placeholder="如 12"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 分区二：监管属性 -->
      <el-divider content-position="left">监管属性</el-divider>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="12">
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
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="处方药" prop="isRx">
            <el-radio-group v-model="formData.isRx">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="特殊管理" prop="isSpecial">
            <el-radio-group v-model="formData.isSpecial">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="含麻黄碱" prop="isPseudoephedrine">
            <el-radio-group v-model="formData.isPseudoephedrine">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="冷链" prop="isColdChain">
            <el-radio-group v-model="formData.isColdChain">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="线上可售" prop="saleableOnline">
            <el-radio-group v-model="formData.saleableOnline">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
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
        <el-col :xs="24" :sm="24" :md="12">
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
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="效期管理" prop="needExpiry">
            <el-radio-group v-model="formData.needExpiry">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
                :key="dict.value"
                :value="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="启用状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
                :key="dict.value"
                :value="dict.value"
                >{{ dict.label }}</el-radio
              >
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 分区三：价格与库存参数 -->
      <el-divider content-position="left">价格与库存参数</el-divider>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="零售价" prop="retailPrice">
            <el-input-number
              v-model="formData.retailPrice"
              :min="0"
              :precision="2"
              :step="0.01"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="会员价" prop="memberPrice">
            <el-input-number
              v-model="formData.memberPrice"
              :min="0"
              :precision="2"
              :step="0.01"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="参考成本价" prop="costPrice">
            <el-input-number
              v-model="formData.costPrice"
              :min="0"
              :precision="2"
              :step="0.01"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="最低限售价" prop="minSalePrice">
            <el-input-number
              v-model="formData.minSalePrice"
              :min="0"
              :precision="2"
              :step="0.01"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="税率" prop="taxRate">
            <el-input-number
              v-model="formData.taxRate"
              :min="0"
              :max="100"
              :precision="2"
              :step="0.01"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="库存下限" prop="minStock">
            <el-input-number v-model="formData.minStock" :min="0" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <el-form-item label="库存上限" prop="maxStock">
            <el-input-number v-model="formData.maxStock" :min="0" class="!w-full" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 分区四：图片与说明 -->
      <el-divider content-position="left">图片与说明</el-divider>
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="主图" prop="imageUrl">
            <UploadImg v-model="formData.imageUrl" :file-size="2" width="120px" height="120px" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="多图" prop="images">
            <UploadImgs v-model="formData.images" :limit="5" :file-size="2" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="说明书" prop="instructionsUrl">
            <UploadFile
              v-model="formData.instructionsUrl"
              :limit="1"
              :file-type="['pdf', 'doc', 'docx']"
              :file-size="10"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="药品说明" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="3"
              placeholder="药品说明信息"
            />
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
      <el-button :disabled="formLoading" type="primary" @click="submitForm">保存</el-button>
      <el-button @click="drawerVisible = false">取消</el-button>
    </template>
  </el-drawer>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as DrugApi from '@/api/pharmacy/base/drug'
import * as CategoryApi from '@/api/pharmacy/base/category'
import { handleTree } from '@/utils/tree'
import { UploadImg, UploadImgs, UploadFile } from '@/components/UploadFile'

defineOptions({ name: 'PharmacyBaseDrugForm' })

const { t } = useI18n()
const message = useMessage()

const drawerVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')

/** 统一默认值，避免隐式 any 与类型漂移 */
const createDefaultFormData = (): DrugApi.DrugVO => ({
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
  instructionsUrl: '',
  approveStatus: undefined,
  auditBy: undefined,
  auditAt: undefined,
  auditOpinion: undefined
})

const formData = ref<DrugApi.DrugVO>(createDefaultFormData())

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

/** 分类树（药品分类选择仅需启用分类，使用 simple-list） */
const categoryTree = ref<any[]>([])
const treeProps = { label: 'catName', value: 'id', children: 'children' }
const getCategoryTree = async () => {
  const list = await CategoryApi.getSimpleCategoryList()
  categoryTree.value = handleTree(list, 'id', 'parentId')
}

/** 打开抽屉 */
const open = async (type: string, id?: number) => {
  drawerVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    await getCategoryTree()
    if (id) {
      const data = await DrugApi.getDrug(id)
      formData.value = { ...createDefaultFormData(), ...data }
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open })

/**
 * 构造保存载荷：显式逐字段返回允许提交的字段，剔除 id 与所有后端维护的审核字段
 * （approveStatus/auditBy/auditAt/auditOpinion 由独立审核接口维护，不通过保存接口回传）。
 * 不依赖解构未使用变量，避免 ESLint no-unused-vars。
 */
const createDrugPayload = (): DrugApi.DrugCreateVO => {
  const v = formData.value
  return {
    drugCode: v.drugCode,
    categoryId: v.categoryId,
    genericName: v.genericName,
    tradeName: v.tradeName,
    spellCode: v.spellCode,
    specification: v.specification,
    dosageForm: v.dosageForm,
    manufacturer: v.manufacturer,
    approvalNo: v.approvalNo,
    drugType: v.drugType,
    isRx: v.isRx,
    isSpecial: v.isSpecial,
    isPseudoephedrine: v.isPseudoephedrine,
    isColdChain: v.isColdChain,
    unit: v.unit,
    conversionRatio: v.conversionRatio,
    retailPrice: v.retailPrice,
    memberPrice: v.memberPrice,
    costPrice: v.costPrice,
    minSalePrice: v.minSalePrice,
    taxRate: v.taxRate,
    insuranceType: v.insuranceType,
    minStock: v.minStock,
    maxStock: v.maxStock,
    storageCond: v.storageCond,
    needExpiry: v.needExpiry,
    defaultLocationId: v.defaultLocationId,
    saleableOnline: v.saleableOnline,
    status: v.status,
    remark: v.remark,
    imageUrl: v.imageUrl,
    images: v.images,
    description: v.description,
    instructionsUrl: v.instructionsUrl
  }
}

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const payload = createDrugPayload()
    if (formType.value === 'create') {
      await DrugApi.createDrug(payload)
      message.success(t('common.createSuccess'))
    } else {
      const id = formData.value.id
      if (!id) return
      await DrugApi.updateDrug({ id, ...payload })
      message.success(t('common.updateSuccess'))
    }
    drawerVisible.value = false
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
