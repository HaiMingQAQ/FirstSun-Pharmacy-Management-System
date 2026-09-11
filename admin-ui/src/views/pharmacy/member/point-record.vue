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
      <el-form-item label="会员ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入会员ID"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="业务类型" prop="bizType">
        <el-select
          v-model="queryParams.bizType"
          placeholder="请选择业务类型"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.MEMBER_POINT_BIZ_TYPE)"
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
          v-hasPermi="['pharmacy:member:point-record:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap class="pharmacy-panel">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="会员ID" align="center" prop="userId" width="100" />
      <el-table-column label="业务类型" align="center" prop="bizType" width="150">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.MEMBER_POINT_BIZ_TYPE" :value="scope.row.bizType" />
        </template>
      </el-table-column>
      <el-table-column label="标题" align="center" prop="title" />
      <el-table-column label="描述" align="center" prop="description" show-overflow-tooltip />
      <el-table-column label="积分变动" align="center" prop="point" width="120">
        <template #default="scope">
          <span :class="scope.row.point > 0 ? 'text-green-500' : 'text-red-500'">
            {{ scope.row.point > 0 ? '+' : '' }}{{ scope.row.point }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="变动后总积分" align="center" prop="totalPoint" width="140" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.createTime) }}
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
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import { formatDateTime } from '@/utils/formatTime'
import * as PointRecordApi from '@/api/pharmacy/member/point-record'

defineOptions({ name: 'PharmacyMemberPointRecord' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  userId: undefined,
  bizType: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询积分明细列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await PointRecordApi.getPointRecordPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
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

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await PointRecordApi.exportPointRecord(queryParams)
    download.excel(data, '积分明细.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
