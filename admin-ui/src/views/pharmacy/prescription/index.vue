<template>
  <div class="pharmacy-modern-page">
    <PharmacyPageHeader
      title="处方台账"
      eyebrow="PRESCRIPTION LEDGER"
      icon="ep:document"
      total-label="处方总数"
      :total="total"
      :current-count="list.length"
      page-type="处方管理"
      :loading="loading"
      subtitle="FirstSun 药店管理系统 · 处方审核与统一支付"
    />
    <ContentWrap class="pharmacy-panel">
      <!-- 搜索工作栏 -->
      <el-form
        class="-mb-15px"
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="80px"
      >
        <el-form-item label="处方号" prop="prescNo">
          <el-input
            v-model="queryParams.prescNo"
            placeholder="处方号"
            clearable
            class="!w-200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="患者" prop="patientName">
          <el-input
            v-model="queryParams.patientName"
            placeholder="患者姓名"
            clearable
            class="!w-140px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-select v-model="queryParams.source" placeholder="请选择" clearable class="!w-130px">
            <el-option v-for="item in SOURCE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审方状态" prop="reviewStatus">
          <el-select v-model="queryParams.reviewStatus" placeholder="请选择" clearable class="!w-120px">
            <el-option v-for="item in REVIEW_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开方日期">
          <el-date-picker
            v-model="dateRange"
            value-format="YYYY-MM-DD"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="!w-260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
          <el-button type="primary" plain @click="router.push('/pharmacy-rx/prescription/create')">
            <Icon icon="ep:plus" class="mr-5px" /> 登记处方
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 列表 -->
    <ContentWrap class="pharmacy-panel">
      <el-table v-loading="loading" :data="list">
        <el-table-column label="处方号" align="center" prop="prescNo" min-width="190" />
        <el-table-column label="门店" align="center" prop="storeName" min-width="150" />
        <el-table-column label="来源" align="center" width="90">
          <template #default="scope">{{ sourceLabel(scope.row.source) }}</template>
        </el-table-column>
        <el-table-column label="患者" align="center" prop="patientName" width="90" />
        <el-table-column label="医师" align="center" prop="doctorName" width="90" />
        <el-table-column label="审方状态" align="center" width="100">
          <template #default="scope">
            <el-tag :type="reviewTagType(scope.row.reviewStatus)">{{ reviewLabel(scope.row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="特管" align="center" width="70">
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
        <el-table-column label="开方日期" align="center" prop="prescDate" width="110" />
        <el-table-column label="操作" align="center" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleDetail(scope.row.id)">详情</el-button>
            <el-button
              v-if="scope.row.reviewStatus === 0 && scope.row.status === 0"
              link
              type="success"
              @click="openReview(scope.row)"
            >
              审核
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              link
              type="danger"
              @click="handleInvalidate(scope.row)"
            >
              作废
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </ContentWrap>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="处方详情" size="min(760px, 90vw)" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="处方号">{{ detail.prescNo }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ detail.storeName || detail.storeId }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ detail.patientName }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ detail.patientAge ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="医师">{{ detail.doctorName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="医院">{{ detail.hospital ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="诊断">{{ detail.diagnosis ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="开方日期">{{ detail.prescDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="审方状态" :span="2">
          <el-tag :type="reviewTagType(detail.reviewStatus)">{{ reviewLabel(detail.reviewStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审方药师" :span="2">{{ detail.pharmacistName ?? (detail.pharmacistId ?? '-') }}</el-descriptions-item>
        <el-descriptions-item label="审方意见" :span="2">{{ detail.reviewOpinion ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="用法用量" :span="2">{{ detail.usageDesc ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="药品明细" :span="2">
          <el-table :data="parsedItems" size="small" border>
            <el-table-column label="药品" prop="drugName" min-width="120" />
            <el-table-column label="规格" prop="specification" width="120" />
            <el-table-column label="核准数量" prop="qty" width="80" align="center" />
            <el-table-column label="用法" prop="usage" width="100" />
          </el-table>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewVisible" title="药师审核" width="560px" destroy-on-close>
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="110px">
        <el-form-item label="处方号">
          <el-input :model-value="reviewTarget?.prescNo" disabled />
        </el-form-item>
        <el-form-item label="患者">
          <el-input :model-value="reviewTarget?.patientName" disabled />
        </el-form-item>
        <el-form-item label="审核结果" prop="reviewStatus">
          <el-radio-group v-model="reviewForm.reviewStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="reviewTarget?.isSpecial === 1"
          label="双人复核人"
          prop="dblCheckBy"
        >
          <el-input-number v-model="reviewForm.dblCheckBy" :min="1" :controls="false" placeholder="复核员工编号" class="!w-full" />
        </el-form-item>
        <el-form-item
          :label="reviewForm.reviewStatus === 2 || reviewTarget?.limitCheck === 1 ? '审方意见(必填)' : '审方意见'"
          prop="reviewOpinion"
        >
          <el-input v-model="reviewForm.reviewOpinion" type="textarea" :rows="3" placeholder="审核意见（驳回必填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :loading="reviewLoading" type="primary" @click="submitReview">提交</el-button>
        <el-button @click="reviewVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { PrescRecordApi, PrescRecordVO } from '@/api/pharmacy/prescription/prescRecord'

/** 处方台账（E 维护） */
defineOptions({ name: 'PharmacyPrescriptionIndex' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<PrescRecordVO[]>([])
const dateRange = ref<any[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  prescNo: '',
  patientName: '',
  source: undefined,
  reviewStatus: undefined
})
const queryFormRef = ref()

const SOURCE_OPTIONS = [
  { value: 0, label: '纸质拍照' },
  { value: 1, label: '电子处方平台' },
  { value: 2, label: '复诊续方' }
]
const REVIEW_OPTIONS = [
  { value: 0, label: '待审' },
  { value: 1, label: '通过' },
  { value: 2, label: '驳回' }
]

const sourceLabel = (v: number) => SOURCE_OPTIONS.find((item) => item.value === v)?.label ?? String(v)
const reviewLabel = (v: number) => REVIEW_OPTIONS.find((item) => item.value === v)?.label ?? String(v)
const reviewTagType = (v: number) => (v === 1 ? 'success' : v === 2 ? 'danger' : 'info')

const getList = async () => {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (dateRange.value?.length === 2) {
      params.prescDate = dateRange.value
    }
    const data = await PrescRecordApi.getPrescRecordPage(params)
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
  dateRange.value = []
  handleQuery()
}

/** 详情 */
const detailVisible = ref(false)
const detail = ref<PrescRecordVO | null>(null)
const parsedItems = computed(() => {
  const raw = detail.value?.prescribedItems
  if (!raw) return []
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
})
const handleDetail = async (id: number) => {
  detail.value = await PrescRecordApi.getPrescRecord(id)
  detailVisible.value = true
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
  reviewForm.reviewStatus = 1
  reviewForm.reviewOpinion = ''
  reviewForm.dblCheckBy = undefined
  reviewVisible.value = true
}
const submitReview = async () => {
  if (!reviewTarget.value) return
  const valid = await reviewFormRef.value?.validate().catch(() => false)
  if (!valid) return
  // 动态规则：非驳回且非超量时意见可不填，此处按必填兜底（后端亦校验）
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

/** 作废 */
const handleInvalidate = async (row: PrescRecordVO) => {
  const ok = await message.confirm(`确认作废处方「${row.prescNo}」？`)
  if (!ok) return
  await PrescRecordApi.invalidatePrescRecord(row.id)
  message.success('作废成功')
  getList()
}

onMounted(() => {
  getList()
})
</script>
