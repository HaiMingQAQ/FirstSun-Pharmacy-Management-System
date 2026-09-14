<template>
  <div class="pharmacy-modern-page">
    <PharmacyPageHeader
      title="处方登记"
      eyebrow="PRESCIPTION CREATE"
      icon="ep:edit-pen"
      page-type="处方管理"
      :loading="formLoading"
      subtitle="FirstSun 药店管理系统 · 登记纸质/电子处方，提交后进入待审"
    />
    <ContentWrap class="pharmacy-panel">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <!-- 患者与处方信息 -->
        <el-divider content-position="left">患者与处方信息</el-divider>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="患者姓名" prop="patientName">
              <el-input v-model="formData.patientName" placeholder="请输入患者姓名" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="年龄" prop="patientAge">
              <el-input-number v-model="formData.patientAge" :min="0" :max="150" :controls="false" class="!w-full" placeholder="年龄" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="身份证号" prop="patientIdNo">
              <el-input v-model="formData.patientIdNo" placeholder="患者身份证（AES加密）" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="门店" prop="storeId">
              <el-input-number v-model="formData.storeId" :min="1" :controls="false" class="!w-full" placeholder="门店编号" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="处方来源" prop="source">
              <el-select v-model="formData.source" placeholder="请选择" class="!w-full">
                <el-option v-for="item in SOURCE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="开方日期" prop="prescDate">
              <el-date-picker v-model="formData.prescDate" value-format="YYYY-MM-DD" type="date" placeholder="开方日期" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="开具医院" prop="hospital">
              <el-input v-model="formData.hospital" placeholder="开具医院" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="医师姓名" prop="doctorName">
              <el-input v-model="formData.doctorName" placeholder="医师姓名" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="诊断" prop="diagnosis">
              <el-input v-model="formData.diagnosis" placeholder="诊断" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="用法用量" prop="usageDesc">
              <el-input v-model="formData.usageDesc" type="textarea" :rows="2" placeholder="如：一日三次，每次一片，饭后服用" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 监管标记 -->
        <el-divider content-position="left">监管标记</el-divider>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="特管登记" prop="isSpecial">
              <el-radio-group v-model="formData.isSpecial">
                <el-radio :value="0">否</el-radio>
                <el-radio :value="1">是（需双人复核）</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="超量复核" prop="limitCheck">
              <el-radio-group v-model="formData.limitCheck">
                <el-radio :value="0">否</el-radio>
                <el-radio :value="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 药品明细 -->
        <el-divider content-position="left">药品明细</el-divider>
        <el-table :data="formData.items" border>
          <el-table-column label="药品ID" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.drugId" :min="1" :controls="false" class="!w-full" placeholder="药品ID" />
            </template>
          </el-table-column>
          <el-table-column label="药品名称快照" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.drugName" placeholder="药品名称" />
            </template>
          </el-table-column>
          <el-table-column label="规格" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.specification" placeholder="规格" />
            </template>
          </el-table-column>
          <el-table-column label="核准数量" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.qty" :min="1" :controls="false" class="!w-full" />
            </template>
          </el-table-column>
          <el-table-column label="用法" min-width="110">
            <template #default="{ row }">
              <el-input v-model="row.usage" placeholder="如 口服" />
            </template>
          </el-table-column>
          <el-table-column label="用量" min-width="110">
            <template #default="{ row }">
              <el-input v-model="row.dosage" placeholder="如 每次一片" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="formData.items.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 12px">
          <el-button type="primary" plain @click="addItem">
            <Icon icon="ep:plus" class="mr-5px" /> 添加药品
          </el-button>
        </div>

        <!-- 处方影像 -->
        <el-divider content-position="left">处方影像（最多 5 张）</el-divider>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="主图" prop="imageUrl">
              <UploadImg v-model="formData.imageUrl" :file-size="5" width="120px" height="120px" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="影像列表" prop="images">
              <UploadImgs v-model="formData.images" :limit="5" :file-size="5" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div style="margin-top: 16px; text-align: right">
        <el-button :loading="formLoading" type="primary" @click="submitForm">提交登记</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import { PrescRecordApi } from '@/api/pharmacy/prescription/prescRecord'
import { UploadImg, UploadImgs } from '@/components/UploadFile'

/** 处方登记（E 维护） */
defineOptions({ name: 'PharmacyPrescriptionCreate' })

const router = useRouter()
const message = useMessage()

const formLoading = ref(false)
const formRef = ref()
const SOURCE_OPTIONS = [
  { value: 0, label: '纸质拍照' },
  { value: 1, label: '电子处方平台' },
  { value: 2, label: '复诊续方' }
]

interface PrescItemRow {
  drugId?: number
  qty: number
  usage?: string
  dosage?: string
  drugName?: string
  specification?: string
}

const formData = reactive({
  storeId: undefined as number | undefined,
  source: 0,
  patientName: '',
  patientAge: undefined as number | undefined,
  patientIdNo: '',
  hospital: '',
  doctorName: '',
  diagnosis: '',
  usageDesc: '',
  prescDate: '',
  imageUrl: '',
  images: [] as string[],
  isSpecial: 0,
  limitCheck: 0,
  items: [] as PrescItemRow[]
})

const formRules = {
  patientName: [{ required: true, message: '患者姓名不能为空', trigger: 'blur' }],
  storeId: [{ required: true, message: '门店不能为空', trigger: 'blur' }],
  source: [{ required: true, message: '处方来源不能为空', trigger: 'change' }]
}

const addItem = () => {
  formData.items.push({ qty: 1, usage: '', dosage: '', drugName: '', specification: '' })
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (formData.items.length === 0) {
    message.warning('请至少添加一种药品')
    return
  }
  formLoading.value = true
  try {
    await PrescRecordApi.createPrescRecord({
      storeId: formData.storeId!,
      source: formData.source,
      patientName: formData.patientName,
      patientAge: formData.patientAge,
      patientIdNo: formData.patientIdNo || undefined,
      hospital: formData.hospital || undefined,
      doctorName: formData.doctorName || undefined,
      diagnosis: formData.diagnosis || undefined,
      usageDesc: formData.usageDesc || undefined,
      prescDate: formData.prescDate || undefined,
      imageUrl: formData.imageUrl || undefined,
      images: formData.images,
      isSpecial: formData.isSpecial,
      limitCheck: formData.limitCheck,
      items: formData.items
    })
    message.success('登记成功，处方已进入待审')
    router.push('/pharmacy-rx/prescription/index')
  } finally {
    formLoading.value = false
  }
}

// 默认添加一行药品
onMounted(() => {
  addItem()
})
</script>
