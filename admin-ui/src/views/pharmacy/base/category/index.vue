<template>
  <div class="pharmacy-page pharmacy-modern-page">
    <PharmacyPageHeader
      title="药品分类"
      eyebrow="CATEGORY TREE"
      icon="ep:folder-opened"
      total-label="分类总数"
      :total="total"
      :current-count="list.length"
      page-type="树形档案"
      :loading="loading"
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
        <el-form-item label="分类编码" prop="catCode">
          <el-input
            v-model="queryParams.catCode"
            placeholder="请输入分类编码"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="分类名" prop="catName">
          <el-input
            v-model="queryParams.catName"
            placeholder="请输入分类名"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="分类类型" prop="catType">
          <el-select
            v-model="queryParams.catType"
            placeholder="请选择分类类型"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_CATEGORY_TYPE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="请选择状态"
            clearable
            class="!w-240px"
          >
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
            type="primary"
            plain
            @click="openForm('create')"
            v-hasPermi="['pharmacy:base:category:create']"
          >
            <Icon icon="ep:plus" class="mr-5px" /> 新增
          </el-button>
          <el-button
            type="success"
            plain
            @click="handleExport"
            :loading="exportLoading"
            v-hasPermi="['pharmacy:base:category:export']"
          >
            <Icon icon="ep:download" class="mr-5px" /> 导出
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 列表 -->
    <ContentWrap class="pharmacy-panel">
      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column label="分类名称" align="left" prop="catName" min-width="180" />
        <el-table-column label="分类编码" align="left" prop="catCode" min-width="140" />
        <el-table-column label="上级分类" align="center" prop="parentId">
          <template #default="scope">
            {{ parentMap[scope.row.parentId]?.catName ?? '顶级分类' }}
          </template>
        </el-table-column>
        <el-table-column label="分类类型" align="center" prop="catType">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_CATEGORY_TYPE" :value="scope.row.catType" />
          </template>
        </el-table-column>
        <el-table-column label="排序" align="center" prop="sort" width="80" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_STATUS" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="160">
          <template #default="scope">
            <el-button
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
              v-hasPermi="['pharmacy:base:category:update']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(scope.row.id)"
              v-hasPermi="['pharmacy:base:category:delete']"
            >
              删除
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

    <!-- 表单弹窗：添加/修改 -->
    <CategoryForm ref="formRef" @success="handleFormSuccess" />
  </div>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import { handleTree } from '@/utils/tree'
import * as CategoryApi from '@/api/pharmacy/base/category'
import CategoryForm from './CategoryForm.vue'

defineOptions({ name: 'PharmacyBaseCategory' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<CategoryApi.CategoryVO[]>([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  catCode: '',
  catName: '',
  catType: undefined,
  status: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 上级分类映射：用完整列表（含停用）构建，避免停用父分类显示为「顶级分类」 */
const parentMap = ref<Record<number, CategoryApi.CategoryVO>>({})
const loadAllCategories = async () => {
  const pageSize = 100
  const firstPage = await CategoryApi.getCategoryPage({ pageNo: 1, pageSize })
  const all = [...((firstPage.list || []) as CategoryApi.CategoryVO[])]
  const pageCount = Math.ceil(firstPage.total / pageSize)
  for (let pageNo = 2; pageNo <= pageCount; pageNo++) {
    const page = await CategoryApi.getCategoryPage({ pageNo, pageSize })
    all.push(...((page.list || []) as CategoryApi.CategoryVO[]))
  }
  const map: Record<number, CategoryApi.CategoryVO> = {}
  for (const item of all) {
    if (item.id != null) map[item.id] = item
  }
  parentMap.value = map
}

/** 查询药品分类列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await CategoryApi.getCategoryPage(queryParams)
    list.value = handleTree(data.list, 'id', 'parentId')
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 新增/修改成功后同时刷新列表与上级分类映射，保证父分类名不显示旧数据 */
const handleFormSuccess = async () => {
  await getList()
  await loadAllCategories()
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

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await CategoryApi.deleteCategory(id)
    message.success(t('common.delSuccess'))
    // 刷新列表与上级分类映射
    await getList()
    await loadAllCategories()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await CategoryApi.exportCategory(queryParams)
    download.excel(data, '药品分类.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  await Promise.allSettled([loadAllCategories(), getList()])
})
</script>
