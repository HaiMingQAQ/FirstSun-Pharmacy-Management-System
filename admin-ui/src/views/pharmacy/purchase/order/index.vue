<template>
  <div class="pharmacy-page pharmacy-modern-page">
    <PharmacyPageHeader
      title="采购订单"
      eyebrow="PURCHASE ORDER"
      icon="ep:document-copy"
      total-label="订单总数"
      :total="total"
      :current-count="list.length"
      page-type="采购流程"
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
        <el-form-item label="订单号" prop="orderNo">
          <el-input
            v-model="queryParams.orderNo"
            placeholder="请输入订单号"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
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
        <el-form-item label="订单状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="请选择订单状态"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_PO_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-show="expandQuery" label="门店" prop="storeId">
          <el-select
            v-model="queryParams.storeId"
            placeholder="请选择门店"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="item in storeList"
              :key="item.id"
              :label="item.storeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-show="expandQuery" label="下单日期" prop="orderDate">
          <el-date-picker
            v-model="queryParams.orderDate"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="!w-260px"
          />
        </el-form-item>
        <el-form-item v-show="expandQuery" label="采购建议" prop="isAuto">
          <el-select
            v-model="queryParams.isAuto"
            placeholder="请选择"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
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
        v-hasPermi="['pharmacy:purchase:order:create']"
      >
        <Icon icon="ep:plus" class="mr-5px" /> 新增
      </el-button>
      <el-button
        class="ml-10px"
        type="success"
        plain
        @click="handleExport"
        :loading="exportLoading"
        v-hasPermi="['pharmacy:purchase:order:export']"
      >
        <Icon icon="ep:download" class="mr-5px" /> 导出
      </el-button>

      <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" class="mt-10px">
        <el-table-column label="订单号" align="left" prop="orderNo" min-width="170" />
        <el-table-column label="供应商" align="left" prop="supplierName" min-width="160" />
        <el-table-column label="门店" align="left" prop="storeName" min-width="140" />
        <el-table-column label="下单日期" align="center" prop="orderDate" width="110" :formatter="localDateFormatter" />
        <el-table-column label="预计到货" align="center" prop="expectDate" width="110" :formatter="localDateFormatter" />
        <el-table-column label="总数量" align="right" prop="totalQty" width="90" />
        <el-table-column label="应付金额" align="right" prop="payableAmount" width="120">
          <template #default="scope">
            <span>{{ formatAmount(scope.row.payableAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" align="center" prop="status" width="110">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_PO_STATUS" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
              v-if="scope.row.status === 0"
              v-hasPermi="['pharmacy:purchase:order:update']"
            >
              编辑
            </el-button>
            <el-dropdown
              v-hasPermi="[
                'pharmacy:purchase:order:query',
                'pharmacy:purchase:order:submit',
                'pharmacy:purchase:order:approve',
                'pharmacy:purchase:order:issue',
                'pharmacy:purchase:order:cancel',
                'pharmacy:purchase:order:delete'
              ]"
              @command="(command) => handleCommand(command, scope.row)"
            >
              <el-button link type="primary">
                更多<Icon icon="ep:arrow-down" class="ml-5px" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="checkPermi(['pharmacy:purchase:order:query'])"
                    command="detail"
                  >
                    查看详情
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="scope.row.status === 0 && checkPermi(['pharmacy:purchase:order:submit'])"
                    command="submit"
                  >
                    提交审批
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="
                      scope.row.status === 1 && checkPermi(['pharmacy:purchase:order:approve'])
                    "
                    command="approve"
                  >
                    审批通过
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="scope.row.status === 2 && checkPermi(['pharmacy:purchase:order:issue'])"
                    command="issue"
                  >
                    标记已发出
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="canCancel(scope.row.status) && checkPermi(['pharmacy:purchase:order:cancel'])"
                    command="cancel"
                  >
                    取消订单
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="scope.row.status === 0 && checkPermi(['pharmacy:purchase:order:delete'])"
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
    <OrderForm ref="formRef" @success="getList" />

    <!-- 详情弹窗：订单头 + 明细 -->
    <Dialog v-model="detailVisible" :title="detailTitle" width="1000">
      <el-descriptions v-if="detail" :column="3" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <dict-tag :type="DICT_TYPE.PHARMACY_PO_STATUS" :value="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ detail.storeName }}</el-descriptions-item>
        <el-descriptions-item label="下单日期">{{ formatLocalDate(detail.orderDate) }}</el-descriptions-item>
        <el-descriptions-item label="预计到货">{{ formatLocalDate(detail.expectDate) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detail.totalQty }}</el-descriptions-item>
        <el-descriptions-item label="含税总金额">
          {{ formatAmount(detail.totalAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="应付金额">
          {{ formatAmount(detail.payableAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="detail?.lines || []" class="mt-10px" :show-overflow-tooltip="true">
        <el-table-column label="行号" align="center" prop="lineNo" width="70" />
        <el-table-column label="药品编码" align="left" prop="drugCode" width="120" />
        <el-table-column label="药品名称" align="left" prop="drugName" min-width="150" />
        <el-table-column label="规格" align="left" prop="specification" min-width="120" />
        <el-table-column label="订购数量" align="right" prop="orderQty" width="100" />
        <el-table-column label="已收数量" align="right" prop="receivedQty" width="100" />
        <el-table-column label="含税单价" align="right" prop="unitPrice" width="110">
          <template #default="scope">
            <span>{{ formatAmount(scope.row.unitPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="折扣率" align="right" prop="discountRate" width="90" />
        <el-table-column label="行金额" align="right" prop="lineAmount" width="110">
          <template #default="scope">
            <span>{{ formatAmount(scope.row.lineAmount) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </Dialog>
  </div>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import { checkPermi } from '@/utils/permission'
import { localDateFormatter, formatLocalDate } from '../utils/date'
import * as OrderApi from '@/api/pharmacy/purchase/order'
import * as SupplierApi from '@/api/pharmacy/purchase/supplier'
import * as StoreApi from '@/api/pharmacy/base/store'
import OrderForm from './OrderForm.vue'

defineOptions({ name: 'PharmacyPurchaseOrder' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<OrderApi.PurchaseOrderVO[]>([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: '',
  supplierId: undefined,
  status: undefined,
  storeId: undefined,
  orderDate: undefined,
  isAuto: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
/** 查询区「更多筛选」展开状态：默认仅展示高频条件 */
const expandQuery = ref(false)

/** 下拉数据：门店与供应商 */
const storeList = ref<StoreApi.StoreSimpleVO[]>([])
const supplierList = ref<SupplierApi.SupplierSimpleVO[]>([])
const loadOptions = async () => {
  const [stores, suppliers] = await Promise.all([
    StoreApi.getSimpleStoreList(),
    SupplierApi.getAllSupplierList()
  ])
  storeList.value = stores
  supplierList.value = suppliers
}

const formatAmount = (value?: number) => {
  if (value === undefined || value === null) return '0.00'
  return Number(value).toFixed(2)
}

/** 与后端状态机一致：草稿/已提交/已审批可取消 */
const canCancel = (status?: number) => status === 0 || status === 1 || status === 2

/** 查询采购订单列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await OrderApi.getPurchaseOrderPage(queryParams)
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
const handleCommand = (command: string, row: OrderApi.PurchaseOrderVO) => {
  if (!row.id) return
  if (command === 'detail') openDetail(row.id)
  else if (command === 'submit') handleChangeStatus('submit', row.id)
  else if (command === 'approve') handleChangeStatus('approve', row.id)
  else if (command === 'issue') handleChangeStatus('issue', row.id)
  else if (command === 'cancel') handleChangeStatus('cancel', row.id)
  else if (command === 'delete') handleDelete(row.id)
}

/** 状态流转：提交/审批/发出/取消，均二次确认，权限由后端再校验 */
const handleChangeStatus = async (
  action: 'submit' | 'approve' | 'issue' | 'cancel',
  id: number
) => {
  const actionText: Record<string, string> = {
    submit: '提交审批',
    approve: '审批通过',
    issue: '标记已发出',
    cancel: '取消订单'
  }
  try {
    await message.confirm(`确认${actionText[action]}该采购订单?`, actionText[action])
    if (action === 'submit') await OrderApi.submitPurchaseOrder(id)
    else if (action === 'approve') await OrderApi.approvePurchaseOrder(id)
    else if (action === 'issue') await OrderApi.issuePurchaseOrder(id)
    else await OrderApi.cancelPurchaseOrder(id)
    message.success(`${actionText[action]}成功`)
    await getList()
  } catch {}
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await OrderApi.deletePurchaseOrder(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailTitle = ref('')
const detail = ref<OrderApi.PurchaseOrderDetailVO>()
const openDetail = async (id: number) => {
  detailVisible.value = true
  const data = (await OrderApi.getPurchaseOrder(id)) as OrderApi.PurchaseOrderDetailVO
  detail.value = data
  detailTitle.value = `采购订单详情 - ${data.orderNo}`
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await OrderApi.exportPurchaseOrder(queryParams)
    download.excel(data, '采购订单.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  await Promise.allSettled([loadOptions(), getList()])
})
</script>
