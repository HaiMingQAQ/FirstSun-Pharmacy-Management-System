<template>
  <ContentWrap class="pharmacy-panel">
    <!-- 搜索 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="药品编码" prop="drugCode">
        <el-input
          v-model="queryParams.drugCode"
          placeholder="请输入编码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="通用名" prop="genericName">
        <el-input
          v-model="queryParams.genericName"
          placeholder="请输入通用名"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="商品名" prop="tradeName">
        <el-input
          v-model="queryParams.tradeName"
          placeholder="请输入商品名"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-tree-select
          v-model="queryParams.categoryId"
          :data="categoryTree"
          :props="treeProps"
          check-strictly
          placeholder="请选择分类"
          clearable
          node-key="id"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="药品类型" prop="drugType">
        <el-select v-model="queryParams.drugType" placeholder="请选择" clearable class="!w-180px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_DRUG_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="启用状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-180px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-show="expandQuery" label="审核状态" prop="approveStatus">
        <el-select
          v-model="queryParams.approveStatus"
          placeholder="请选择"
          clearable
          class="!w-180px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_DRUG_APPROVE_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button text @click="expandQuery = !expandQuery">
          {{ expandQuery ? '收起' : '更多筛选' }}
          <Icon :icon="expandQuery ? 'ep:arrow-up' : 'ep:arrow-down'" class="ml-5px" />
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap class="pharmacy-panel">
    <el-button
      type="primary"
      :icon="Plus"
      plain
      @click="openForm('create')"
      v-hasPermi="['pharmacy:base:drug:create']"
    >
      新增
    </el-button>
    <el-button
      type="success"
      :icon="Download"
      plain
      @click="handleExport"
      v-hasPermi="['pharmacy:base:drug:export']"
      class="ml-10px"
    >
      导出
    </el-button>

    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" class="mt-10px">
      <el-table-column label="药品编码" align="left" prop="drugCode" width="120" />
      <el-table-column label="通用名" align="left" prop="genericName" min-width="140" />
      <el-table-column label="商品名" align="left" prop="tradeName" min-width="120" />
      <el-table-column label="规格" align="left" prop="specification" min-width="120" />
      <el-table-column label="分类" align="left" prop="categoryName" min-width="100" />
      <el-table-column label="药品类型" align="center" prop="drugType" width="150">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.PHARMACY_DRUG_TYPE" :value="scope.row.drugType" />
          <el-tag
            v-if="isDrugTypeInconsistent(scope.row)"
            type="warning"
            size="small"
            class="ml-5px"
            >属性不一致</el-tag
          >
          <el-tag
            v-else
            :type="scope.row.isRx === 1 ? 'danger' : 'info'"
            size="small"
            class="ml-5px"
          >
            {{ scope.row.isRx === 1 ? 'Rx' : '非处方' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="零售价" align="right" prop="retailPrice" width="100" />
      <el-table-column label="审核状态" align="center" prop="approveStatus" width="100">
        <template #default="scope">
          <dict-tag
            :type="DICT_TYPE.PHARMACY_DRUG_APPROVE_STATUS"
            :value="scope.row.approveStatus"
          />
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" prop="status" width="80">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.PHARMACY_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['pharmacy:base:drug:update']"
            >编辑</el-button
          >
          <el-dropdown
            v-hasPermi="['pharmacy:base:drug:approve', 'pharmacy:base:drug:delete']"
            @command="(command) => handleCommand(command, scope.row)"
          >
            <el-button link type="primary">
              更多<Icon icon="ep:arrow-down" class="ml-5px" />
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-if="scope.row.approveStatus === 0 && checkPermi(['pharmacy:base:drug:approve'])"
                  command="approve"
                >
                  通过审核
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="scope.row.approveStatus === 0 && checkPermi(['pharmacy:base:drug:approve'])"
                  command="reject"
                >
                  驳回
                </el-dropdown-item>
                <el-dropdown-item v-if="checkPermi(['pharmacy:base:drug:delete'])" command="delete">
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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

  <DrugForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { Search, Refresh, Plus, Download } from '@element-plus/icons-vue'
import { checkPermi } from '@/utils/permission'
import * as DrugApi from '@/api/pharmacy/base/drug'
import * as CategoryApi from '@/api/pharmacy/base/category'
import { handleTree } from '@/utils/tree'
import DrugForm from './DrugForm.vue'

defineOptions({ name: 'PharmacyBaseDrug' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true)
const total = ref(0)
const list = ref<any[]>([])
/** 查询区「更多筛选」展开状态：默认仅展示高频条件，审核状态收入展开区 */
const expandQuery = ref(false)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  drugCode: undefined,
  genericName: undefined,
  tradeName: undefined,
  categoryId: undefined,
  drugType: undefined,
  approveStatus: undefined,
  status: undefined
})
const queryFormRef = ref()

/**
 * 药品类型与处方药一致性判断（不静默隐藏不一致数据）：
 * drugType=0(处方) 期望 isRx=1；drugType=1/2(OTC) 期望 isRx=0；其余类型不强制。
 */
const isDrugTypeInconsistent = (row: any): boolean => {
  if (row.drugType === 0 && row.isRx !== 1) return true
  if ((row.drugType === 1 || row.drugType === 2) && row.isRx === 1) return true
  return false
}

/** 「更多」下拉操作分发 */
const handleCommand = (command: string, row: any) => {
  if (command === 'approve') handleApprove(row.id, 1)
  else if (command === 'reject') handleApprove(row.id, 2)
  else if (command === 'delete') handleDelete(row.id)
}

/** 分类树 */
const categoryTree = ref<any[]>([])
const treeProps = { label: 'catName', value: 'id', children: 'children' }
const getCategoryTree = async () => {
  const list = await CategoryApi.getSimpleCategoryList()
  categoryTree.value = handleTree(list, 'id', 'parentId')
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await DrugApi.getDrugPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 新增/修改弹窗 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await DrugApi.deleteDrug(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 审核 */
const handleApprove = async (id: number, approveStatus: number) => {
  const tip = approveStatus === 1 ? '通过' : '驳回'
  try {
    let auditOpinion = ''
    if (approveStatus === 2) {
      const res = await message.prompt('请输入驳回意见', '驳回')
      auditOpinion = res.value
    } else {
      await message.confirm(`确认${tip}该药品?`, '审核')
    }
    await DrugApi.approveDrug(id, approveStatus, auditOpinion)
    message.success('审核完成')
    await getList()
  } catch {}
}

/** 导出 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    await DrugApi.exportDrug(queryParams)
    message.success(t('common.exportSuccess'))
  } catch {}
}

/** 初始化 */
onMounted(async () => {
  await getCategoryTree()
  await getList()
})
</script>
