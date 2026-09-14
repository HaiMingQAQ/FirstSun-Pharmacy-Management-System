<template>
  <div class="pharmacy-page pharmacy-modern-page">
    <PharmacyPageHeader
      title="供应商证照"
      eyebrow="SUPPLIER LICENSE"
      icon="ep:document"
      total-label="证照总数"
      :total="total"
      :current-count="list.length"
      page-type="资质档案"
      :loading="loading"
    />
    <ContentWrap class="pharmacy-panel">
      <!-- 搜索工作栏：高频条件默认展示，低频条件收入「更多筛选」 -->
      <el-form
        class="-mb-15px"
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="96px"
      >
        <el-form-item label="供应商" prop="supplierId">
          <el-select
            v-model="queryParams.supplierId"
            placeholder="请选择供应商"
            filterable
            clearable
            class="!w-260px"
          >
            <el-option
              v-for="item in supplierList"
              :key="item.id"
              :label="`${item.supplierName}（${item.supplierCode}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="证照类型" prop="licenseType">
          <el-select
            v-model="queryParams.licenseType"
            placeholder="请选择证照类型"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_LICENSE_TYPE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="证照状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="请选择证照状态"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_LICENSE_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-show="expandQuery" label="证照号" prop="licenseNo">
          <el-input
            v-model="queryParams.licenseNo"
            placeholder="请输入证照号"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item v-show="expandQuery" label="到期日" prop="expireDate">
          <el-date-picker
            v-model="queryParams.expireDate"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="!w-260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
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
        plain
        @click="openForm('create')"
        v-hasPermi="['pharmacy:purchase:license:create']"
      >
        <Icon icon="ep:plus" class="mr-5px" /> 新增
      </el-button>
      <el-button
        class="ml-10px"
        type="warning"
        plain
        @click="handleExpiring"
        v-hasPermi="['pharmacy:purchase:license:query']"
      >
        <Icon icon="ep:alarm-clock" class="mr-5px" /> 到期提醒
      </el-button>
      <el-button
        class="ml-10px"
        plain
        :loading="refreshLoading"
        @click="handleRefreshStatus"
        v-hasPermi="['pharmacy:purchase:license:update']"
      >
        <Icon icon="ep:refresh-right" class="mr-5px" /> 刷新状态
      </el-button>
      <el-button
        class="ml-10px"
        type="success"
        plain
        @click="handleExport"
        :loading="exportLoading"
        v-hasPermi="['pharmacy:purchase:license:export']"
      >
        <Icon icon="ep:download" class="mr-5px" /> 导出
      </el-button>

      <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" class="mt-10px">
        <el-table-column label="供应商名称" align="left" prop="supplierName" min-width="180" />
        <el-table-column label="证照类型" align="center" prop="licenseType" width="120">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_LICENSE_TYPE" :value="scope.row.licenseType" />
          </template>
        </el-table-column>
        <el-table-column label="证照号" align="left" prop="licenseNo" min-width="160" />
        <el-table-column
          label="发证日期"
          align="center"
          prop="issueDate"
          width="120"
          :formatter="localDateFormatter"
        />
        <el-table-column
          label="到期日"
          align="center"
          prop="expireDate"
          width="120"
          :formatter="localDateFormatter"
        />
        <el-table-column label="证照状态" align="center" prop="status" width="110">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_LICENSE_STATUS" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="距到期" align="right" prop="daysToExpire" width="120">
          <template #default="scope">
            <span>{{ formatDaysToExpire(scope.row.daysToExpire) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="130" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
              v-hasPermi="['pharmacy:purchase:license:update']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(scope.row.id)"
              v-hasPermi="['pharmacy:purchase:license:delete']"
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
    <LicenseForm ref="formRef" @success="getList" />

    <!-- 到期提醒弹窗：默认 30 天窗口，含已过期证照 -->
    <Dialog v-model="expiringDialogVisible" title="证照到期提醒（30 天内及已过期）" width="900">
      <el-table v-loading="expiringLoading" :data="expiringList" :show-overflow-tooltip="true">
        <el-table-column label="供应商名称" align="left" prop="supplierName" min-width="180" />
        <el-table-column label="证照类型" align="center" prop="licenseType" width="120">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_LICENSE_TYPE" :value="scope.row.licenseType" />
          </template>
        </el-table-column>
        <el-table-column label="证照号" align="left" prop="licenseNo" min-width="150" />
        <el-table-column
          label="到期日"
          align="center"
          prop="expireDate"
          width="120"
          :formatter="localDateFormatter"
        />
        <el-table-column label="距到期" align="right" prop="daysToExpire" width="120">
          <template #default="scope">
            <span>{{ formatDaysToExpire(scope.row.daysToExpire) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="expiringDialogVisible = false">关 闭</el-button>
      </template>
    </Dialog>
  </div>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import { localDateFormatter } from '../utils/date'
import * as LicenseApi from '@/api/pharmacy/purchase/license'
import * as SupplierApi from '@/api/pharmacy/purchase/supplier'
import LicenseForm from './LicenseForm.vue'

defineOptions({ name: 'PharmacyPurchaseLicense' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<LicenseApi.SupplierLicenseVO[]>([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  supplierId: undefined,
  licenseType: undefined,
  status: undefined,
  licenseNo: '',
  expireDate: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const refreshLoading = ref(false) // 刷新证照状态的加载中
/** 查询区「更多筛选」展开状态：默认仅展示高频条件 */
const expandQuery = ref(false)

/** 供应商下拉：使用全部供应商，便于按供应商筛选证照 */
const supplierList = ref<SupplierApi.SupplierSimpleVO[]>([])
const getSupplierList = async () => {
  supplierList.value = await SupplierApi.getAllSupplierList()
}

/** 距到期天数展示：负数表示已过期 */
const formatDaysToExpire = (days?: number) => {
  if (days === undefined || days === null) return '-'
  return days < 0 ? `已过期 ${-days} 天` : `${days} 天`
}

/** 查询证照列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await LicenseApi.getLicensePage(queryParams)
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
    await LicenseApi.deleteLicense(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 刷新证照有效/过期状态 */
const handleRefreshStatus = async () => {
  try {
    await message.confirm('确认按当天日期刷新全部证照的有效/过期状态?', '刷新证照状态')
    refreshLoading.value = true
    const count = await LicenseApi.refreshLicenseStatus()
    message.success(`已刷新 ${count ?? 0} 条证照状态`)
    await getList()
  } catch {
  } finally {
    refreshLoading.value = false
  }
}

/** 到期提醒弹窗 */
const expiringDialogVisible = ref(false)
const expiringLoading = ref(false)
const expiringList = ref<LicenseApi.SupplierLicenseVO[]>([])
const handleExpiring = async () => {
  expiringDialogVisible.value = true
  expiringLoading.value = true
  try {
    expiringList.value = await LicenseApi.getExpiringLicenseList(30)
  } finally {
    expiringLoading.value = false
  }
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await LicenseApi.exportLicense(queryParams)
    download.excel(data, '供应商证照.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  await Promise.allSettled([getSupplierList(), getList()])
})
</script>
