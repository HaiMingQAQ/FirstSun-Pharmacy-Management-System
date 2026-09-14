<template>
  <div class="pharmacy-page pharmacy-modern-page">
    <PharmacyPageHeader
      title="供应商"
      eyebrow="SUPPLIER"
      icon="ep:shop"
      total-label="供应商总数"
      :total="total"
      :current-count="list.length"
      page-type="采购档案"
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
        <el-form-item label="供应商编码" prop="supplierCode">
          <el-input
            v-model="queryParams.supplierCode"
            placeholder="请输入供应商编码"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="供应商名称" prop="supplierName">
          <el-input
            v-model="queryParams.supplierName"
            placeholder="请输入供应商名称"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
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
        <el-form-item v-show="expandQuery" label="信用代码" prop="creditCode">
          <el-input
            v-model="queryParams.creditCode"
            placeholder="请输入统一社会信用代码"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item v-show="expandQuery" label="联系人" prop="contact">
          <el-input
            v-model="queryParams.contact"
            placeholder="请输入联系人"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item v-show="expandQuery" label="联系电话" prop="phone">
          <el-input
            v-model="queryParams.phone"
            placeholder="请输入联系电话"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item v-show="expandQuery" label="首营审核" prop="approveStatus">
          <el-select
            v-model="queryParams.approveStatus"
            placeholder="请选择首营审核状态"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_SUPPLIER_APPROVE_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
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
        v-hasPermi="['pharmacy:purchase:supplier:create']"
      >
        <Icon icon="ep:plus" class="mr-5px" /> 新增
      </el-button>
      <el-button
        class="ml-10px"
        type="success"
        plain
        @click="handleExport"
        :loading="exportLoading"
        v-hasPermi="['pharmacy:purchase:supplier:export']"
      >
        <Icon icon="ep:download" class="mr-5px" /> 导出
      </el-button>

      <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" class="mt-10px">
        <el-table-column label="供应商名称" align="left" prop="supplierName" min-width="180" />
        <el-table-column label="供应商编码" align="left" prop="supplierCode" min-width="130" />
        <el-table-column label="联系人" align="left" prop="contact" width="110" />
        <el-table-column label="联系电话" align="left" prop="phone" width="130" />
        <el-table-column
          label="统一社会信用代码"
          align="left"
          prop="creditCode"
          min-width="170"
        />
        <el-table-column label="银行账号" align="left" prop="bankAccountMasked" width="150" />
        <el-table-column label="账期" align="left" prop="paymentTerms" width="120" />
        <el-table-column label="默认折扣率" align="right" prop="defaultDiscount" width="110">
          <template #default="scope">
            {{ formatDiscount(scope.row.defaultDiscount) }}
          </template>
        </el-table-column>
        <el-table-column label="首营审核" align="center" prop="approveStatus" width="110">
          <template #default="scope">
            <dict-tag
              :type="DICT_TYPE.PHARMACY_SUPPLIER_APPROVE_STATUS"
              :value="scope.row.approveStatus"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="90">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_STATUS" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
              v-hasPermi="['pharmacy:purchase:supplier:update']"
            >
              编辑
            </el-button>
            <el-dropdown
              v-hasPermi="[
                'pharmacy:purchase:supplier:approve',
                'pharmacy:purchase:supplier:delete',
                'pharmacy:purchase:license:query'
              ]"
              @command="(command) => handleCommand(command, scope.row)"
            >
              <el-button link type="primary">
                更多<Icon icon="ep:arrow-down" class="ml-5px" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="
                      scope.row.approveStatus === 0 &&
                      checkPermi(['pharmacy:purchase:supplier:approve'])
                    "
                    command="approve"
                  >
                    通过审核
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="
                      scope.row.approveStatus === 0 &&
                      checkPermi(['pharmacy:purchase:supplier:approve'])
                    "
                    command="reject"
                  >
                    驳回
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="checkPermi(['pharmacy:purchase:license:query'])"
                    command="license"
                  >
                    查看证照
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="checkPermi(['pharmacy:purchase:supplier:delete'])"
                    command="delete"
                  >
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <!-- 表单抽屉：添加/修改 -->
    <SupplierForm ref="formRef" @success="getList" />

    <!-- 证照查看弹窗：只读展示该供应商的证照清单 -->
    <Dialog v-model="licenseDialogVisible" :title="licenseDialogTitle" width="900">
      <el-table v-loading="licenseLoading" :data="licenseList" :show-overflow-tooltip="true">
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
        <el-table-column label="距到期" align="right" prop="daysToExpire" width="110">
          <template #default="scope">
            <span>{{ formatDaysToExpire(scope.row.daysToExpire) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="licenseDialogVisible = false">关 闭</el-button>
      </template>
    </Dialog>
  </div>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import { checkPermi } from '@/utils/permission'
import { localDateFormatter } from '../utils/date'
import * as SupplierApi from '@/api/pharmacy/purchase/supplier'
import * as LicenseApi from '@/api/pharmacy/purchase/license'
import SupplierForm from './SupplierForm.vue'

defineOptions({ name: 'PharmacyPurchaseSupplier' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<SupplierApi.SupplierVO[]>([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  supplierCode: '',
  supplierName: '',
  status: undefined,
  creditCode: '',
  contact: '',
  phone: '',
  approveStatus: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
/** 查询区「更多筛选」展开状态：默认仅展示高频条件 */
const expandQuery = ref(false)

/** 折扣率展示：保留两位小数，空值显示占位符 */
const formatDiscount = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return Number(value).toFixed(2)
}

/** 距到期天数展示：负数表示已过期 */
const formatDaysToExpire = (days?: number) => {
  if (days === undefined || days === null) return '-'
  return days < 0 ? `已过期 ${-days} 天` : `${days} 天`
}

/** 查询供应商列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await SupplierApi.getSupplierPage(queryParams)
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

/** 「更多」下拉操作分发 */
const handleCommand = (command: string, row: SupplierApi.SupplierVO) => {
  if (!row.id) return
  if (command === 'approve') handleApprove(row.id, 1)
  else if (command === 'reject') handleApprove(row.id, 2)
  else if (command === 'license') openLicenseDialog(row)
  else if (command === 'delete') handleDelete(row.id)
}

/** 首营审核：通过需二次确认，驳回需填写意见 */
const handleApprove = async (id: number, approveStatus: number) => {
  const tip = approveStatus === 1 ? '通过' : '驳回'
  try {
    let auditOpinion = ''
    if (approveStatus === 2) {
      const res = await message.prompt('请输入驳回意见', '驳回')
      auditOpinion = res.value
    } else {
      await message.confirm(`确认${tip}该供应商的首营审核?`, '首营审核')
    }
    await SupplierApi.approveSupplier(id, approveStatus, auditOpinion)
    message.success('审核完成')
    await getList()
  } catch {}
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await SupplierApi.deleteSupplier(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
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
    const data = await SupplierApi.exportSupplier(queryParams)
    download.excel(data, '供应商.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 证照查看弹窗 */
const licenseDialogVisible = ref(false)
const licenseDialogTitle = ref('')
const licenseLoading = ref(false)
const licenseList = ref<LicenseApi.SupplierLicenseVO[]>([])
const openLicenseDialog = async (row: SupplierApi.SupplierVO) => {
  if (!row.id) return
  licenseDialogTitle.value = `${row.supplierName} - 证照清单`
  licenseDialogVisible.value = true
  licenseLoading.value = true
  try {
    licenseList.value = await LicenseApi.getLicenseListBySupplier(row.id)
  } finally {
    licenseLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  await getList()
})
</script>
