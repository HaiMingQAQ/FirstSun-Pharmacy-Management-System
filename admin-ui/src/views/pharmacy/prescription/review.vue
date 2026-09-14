<template>
  <div class="pharmacy-modern-page">
    <PharmacyPageHeader
      title="处方审核"
      eyebrow="PRESCRIPTION REVIEW"
      icon="ep:finished"
      total-label="待审处方"
      :total="total"
      :current-count="list.length"
      page-type="处方管理"
      :loading="loading"
      subtitle="FirstSun 药店管理系统 · 药师审方工作台"
    />
    <ContentWrap class="pharmacy-panel">
      <!-- 搜索工作栏 -->
      <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
        <el-form-item label="处方号" prop="prescNo">
          <el-input v-model="queryParams.prescNo" placeholder="处方号" clearable class="!w-200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="患者" prop="patientName">
          <el-input v-model="queryParams.patientName" placeholder="患者姓名" clearable class="!w-140px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="审方状态" prop="reviewStatus">
          <el-select v-model="queryParams.reviewStatus" placeholder="请选择" clearable class="!w-130px">
            <el-option v-for="item in REVIEW_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
          <el-button type="warning" plain @click="queryOnlyWaiting">
            <Icon icon="ep:alarm-clock" class="mr-5px" /> 只看待审
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 列表 -->
    <ContentWrap class="pharmacy-panel">
      <el-table v-loading="loading" :data="list">
        <el-table-column label="处方号" align="center" prop="prescNo" min-width="190" />
        <el-table-column label="患者" align="center" prop="patientName" width="90" />
        <el-table-column label="年龄" align="center" prop="patientAge" width="70" />
        <el-table-column label="医师" align="center" prop="doctorName" width="90" />
        <el-table-column label="诊断" align="center" prop="diagnosis" min-width="120" show-overflow-tooltip />
        <el-table-column label="特管" align="center" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row.isSpecial === 1" type="danger" size="small">双人复核</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="超量" align="center" width="70">
          <template #default="scope">
            <el-tag v-if="scope.row.limitCheck === 1" type="warning" size="small">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="审方状态" align="center" width="100">
          <template #default="scope">
            <el-tag :type="reviewTagType(scope.row.reviewStatus)">{{ reviewLabel(scope.row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="140" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.reviewStatus === 0 && scope.row.status === 0"
              link
              type="primary"
              @click="openReview(scope.row)"
            >
              审核
            </el-button>
            <el-button v-else link type="info" @click="openReview(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </ContentWrap>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewVisible" :title="reviewTarget?.reviewStatus === 0 ? '药师审核' : '审核记录'" width="560px" destroy-on-close>
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="110px">
        <el-form-item label="处方号">
          <el-input :model-value="reviewTarget?.prescNo" disabled />
        </el-form-item>
        <el-form-item label="患者">
          <el-input :model-value="reviewTarget?.patientName" disabled />
        </el-form-item>
        <el-form-item v-if="reviewTarget?.reviewStatus === 0" label="审核结果" prop="reviewStatus">
          <el-radio-group v-model="reviewForm.reviewStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="reviewTarget?.reviewStatus === 0 && reviewTarget?.isSpecial === 1"
          label="双人复核人"
          prop="dblCheckBy"
        >
          <el-input-number v-model="reviewForm.dblCheckBy" :min="1" :controls="false" placeholder="复核员工编号" class="!w-full" />
        </el-form-item>
        <el-form-item label="审方意见" prop="reviewOpinion">
          <el-input v-model="reviewForm.reviewOpinion" type="textarea" :rows="3" placeholder="审核意见（驳回必填）" :disabled="reviewTarget?.reviewStatus !== 0" />
        </el-form-item>
        <el-form-item v-if="reviewTarget?.reviewStatus !== 0" label="审方药师">
          <el-input :model-value="reviewTarget?.pharmacistName || String(reviewTarget?.pharmacistId ?? '-')" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <template v-if="reviewTarget?.reviewStatus === 0">
          <el-button :loading="reviewLoading" type="primary" @click="submitReview">提交</el-button>
          <el-button @click="reviewVisible = false">取消</el-button>
        </template>
        <el-button v-else @click="reviewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { PrescRecordApi, PrescRecordVO } from '@/api/pharmacy/prescription/prescRecord'

/** 处方审核工作台（E 维护） */
defineOptions({ name: 'PharmacyPrescriptionReview' })

const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<PrescRecordVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  prescNo: '',
  patientName: '',
  reviewStatus: undefined as number | undefined
})
const queryFormRef = ref()

const REVIEW_OPTIONS = [
  { value: 0, label: '待审' },
  { value: 1, label: '通过' },
  { value: 2, label: '驳回' }
]
const reviewLabel = (v: number) => REVIEW_OPTIONS.find((item) => item.value === v)?.label ?? String(v)
const reviewTagType = (v: number) => (v === 1 ? 'success' : v === 2 ? 'danger' : 'info')

const getList = async () => {
  loading.value = true
  try {
    const data = await PrescRecordApi.getPrescRecordPage({ ...queryParams })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}
const queryOnlyWaiting = () => {
  queryParams.reviewStatus = 0
  handleQuery()
}

/** 审核 */
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewTarget = ref<PrescRecordVO | null>(null)
const reviewFormRef = ref()
const reviewForm = reactive({
  reviewStatus: 1,
  reviewOpinion: '',
  reviewSnapshot: '',
  dblCheckBy: undefined
})
const reviewRules = {
  reviewStatus: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  reviewOpinion: [{ required: true, message: '请填写审方意见', trigger: 'blur' }],
  dblCheckBy: [{ required: true, message: '请选择双人复核人', trigger: 'change' }]
}
const openReview = (row: PrescRecordVO) => {
  reviewTarget.value = row
  reviewForm.reviewStatus = row.reviewStatus === 1 ? 1 : 2
  reviewForm.reviewOpinion = row.reviewOpinion ?? ''
  reviewForm.dblCheckBy = row.dblCheckBy
  reviewVisible.value = true
}
const submitReview = async () => {
  if (!reviewTarget.value) return
  const valid = await reviewFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (reviewForm.reviewStatus === 1 && reviewTarget.value.limitCheck !== 1 && !reviewForm.reviewOpinion) {
    message.warning('请填写审方意见')
    return
  }
  reviewLoading.value = true
  try {
    await PrescRecordApi.reviewPrescRecord({
      id: reviewTarget.value.id,
      reviewStatus: reviewForm.reviewStatus,
      reviewOpinion: reviewForm.reviewOpinion,
      reviewSnapshot: reviewForm.reviewSnapshot,
      dblCheckBy: reviewForm.dblCheckBy
    })
    message.success('审核成功')
    reviewVisible.value = false
    getList()
  } finally {
    reviewLoading.value = false
  }
}

onMounted(() => {
  // 默认只看待审
  queryParams.reviewStatus = 0
  getList()
})
</script>
