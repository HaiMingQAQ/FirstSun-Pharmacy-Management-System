<template>
  <ContentWrap class="pharmacy-panel">
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="手机号" prop="mobile">
        <el-input
          v-model="queryParams.mobile"
          placeholder="请输入手机号"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input
          v-model="queryParams.nickname"
          placeholder="请输入昵称"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="等级" prop="levelId">
        <el-select
          v-model="queryParams.levelId"
          placeholder="请选择会员等级"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="item in levelList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-240px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['pharmacy:member:user:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap class="pharmacy-panel">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="手机号" align="center" prop="mobile" width="130" />
      <el-table-column label="昵称" align="center" prop="nickname" />
      <el-table-column label="姓名" align="center" prop="name" width="100" />
      <el-table-column label="性别" align="center" prop="sex" width="80">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.SYSTEM_USER_SEX" :value="scope.row.sex" />
        </template>
      </el-table-column>
      <el-table-column label="等级" align="center" prop="levelId" width="120">
        <template #default="scope">
          {{ levelMap[scope.row.levelId]?.name ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="积分" align="center" prop="point" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.PHARMACY_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="注册时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openDetail(scope.row.id)"
            v-hasPermi="['pharmacy:member:user:query']"
          >
            查看详情
          </el-button>
          <el-button
            link
            :type="scope.row.status === 1 ? 'danger' : 'success'"
            @click="handleStatusChange(scope.row)"
            v-hasPermi="['pharmacy:member:user:update']"
          >
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 详情抽屉：查看/修改 -->
  <MemberForm ref="formRef" @success="handleFormSuccess" />
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import { formatDate } from '@/utils/formatTime'
import * as MemberApi from '@/api/pharmacy/member/user'
import * as LevelApi from '@/api/pharmacy/member/level'
import MemberForm from './MemberForm.vue'

defineOptions({ name: 'PharmacyMemberUser' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  mobile: '',
  nickname: '',
  levelId: undefined,
  status: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 会员等级列表与映射 */
const levelList = ref<LevelApi.MemberLevelSimpleVO[]>([])
const levelMap = ref<Record<number, LevelApi.MemberLevelSimpleVO>>({})
const loadLevelList = async () => {
  const data = await LevelApi.getSimpleLevelList()
  levelList.value = data
  const map: Record<number, LevelApi.MemberLevelSimpleVO> = {}
  for (const item of data) {
    map[item.id] = item
  }
  levelMap.value = map
}

/** 查询会员列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await MemberApi.getMemberPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 修改成功后刷新列表 */
const handleFormSuccess = async () => {
  await getList()
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 查看详情操作 */
const formRef = ref()
const openDetail = (id: number) => {
  formRef.value.open(id)
}

/** 启用/禁用操作 */
const handleStatusChange = async (row: MemberApi.MemberUserVO) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    const actionText = newStatus === 1 ? '启用' : '禁用'
    await message.confirm(`确定要${actionText}该会员吗？`)
    await MemberApi.updateMemberStatus(row.id!, newStatus)
    message.success(t('common.updateSuccess'))
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await MemberApi.exportMember(queryParams)
    download.excel(data, '会员档案.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  await loadLevelList()
  await getList()
})
</script>
