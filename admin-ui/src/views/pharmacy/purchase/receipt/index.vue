<template>
  <div class="pharmacy-page pharmacy-modern-page">
    <PharmacyPageHeader
      title="采购收货"
      eyebrow="PURCHASE RECEIPT"
      icon="ep:box"
      total-label="收货单总数"
      :total="total"
      :current-count="list.length"
      page-type="库房收货"
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
        <el-form-item label="收货单号" prop="receiptNo">
          <el-input
            v-model="queryParams.receiptNo"
            placeholder="请输入收货单号"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="收货状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="请选择收货单状态"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_RECEIPT_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="门店" prop="storeId">
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
        <el-form-item v-show="expandQuery" label="采购订单" prop="orderId">
          <el-select
            v-model="queryParams.orderId"
            placeholder="请选择采购订单"
            filterable
            clearable
            class="!w-260px"
          >
            <el-option
              v-for="item in orderList"
              :key="item.id"
              :label="item.orderNo"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-show="expandQuery" label="差异标记" prop="diffType">
          <el-select
            v-model="queryParams.diffType"
            placeholder="请选择差异标记"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_RECEIPT_DIFF_TYPE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-show="expandQuery" label="收货时间" prop="receiveDate">
          <!-- 后端 LocalDateTime[] 查询条件用 @DateTimeFormat("yyyy-MM-dd HH:mm:ss")，必须带时分秒 -->
          <el-date-picker
            v-model="queryParams.receiveDate"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="!w-380px"
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
        v-hasPermi="['pharmacy:purchase:receipt:create']"
      >
        <Icon icon="ep:plus" class="mr-5px" /> 新增
      </el-button>
      <el-button
        class="ml-10px"
        type="success"
        plain
        @click="handleExport"
        :loading="exportLoading"
        v-hasPermi="['pharmacy:purchase:receipt:export']"
      >
        <Icon icon="ep:download" class="mr-5px" /> 导出
      </el-button>

      <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" class="mt-10px">
        <el-table-column label="收货单号" align="left" prop="receiptNo" min-width="170" />
        <el-table-column label="采购订单号" align="left" prop="orderNo" min-width="170">
          <template #default="scope">
            <span>{{ scope.row.orderNo || '无单收货' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="门店" align="left" prop="storeName" min-width="140" />
        <el-table-column label="收货人" align="left" prop="receiveByName" width="110" />
        <el-table-column
          label="收货时间"
          align="center"
          prop="receiveDate"
          width="170"
          :formatter="dateFormatter"
        />
        <el-table-column label="实收数量" align="right" prop="totalQty" width="100" />
        <el-table-column label="实收金额" align="right" prop="totalAmount" width="120">
          <template #default="scope">
            <span>{{ formatAmount(scope.row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异" align="center" prop="diffType" width="110">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_RECEIPT_DIFF_TYPE" :value="scope.row.diffType" />
          </template>
        </el-table-column>
        <el-table-column label="质检" align="center" prop="qualityStatus" width="100">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_QUALITY_STATUS" :value="scope.row.qualityStatus" />
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_RECEIPT_STATUS" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
              v-if="scope.row.status === 0"
              v-hasPermi="['pharmacy:purchase:receipt:update']"
            >
              编辑
            </el-button>
            <el-dropdown
              v-hasPermi="[
                'pharmacy:purchase:receipt:query',
                'pharmacy:purchase:receipt:submit',
                'pharmacy:purchase:receipt:post',
                'pharmacy:purchase:receipt:void',
                'pharmacy:purchase:receipt:delete'
              ]"
              @command="(command) => handleCommand(command, scope.row)"
            >
              <el-button link type="primary">
                更多<Icon icon="ep:arrow-down" class="ml-5px" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="checkPermi(['pharmacy:purchase:receipt:query'])"
                    command="detail"
                  >
                    查看详情
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="
                      scope.row.status === 0 && checkPermi(['pharmacy:purchase:receipt:submit'])
                    "
                    command="submit"
                  >
                    提交
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="scope.row.status === 1 && checkPermi(['pharmacy:purchase:receipt:post'])"
                    command="post"
                  >
                    收货入账
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="scope.row.status === 0 && checkPermi(['pharmacy:purchase:receipt:void'])"
                    command="void"
                  >
                    作废
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="canDelete(scope.row.status) && checkPermi(['pharmacy:purchase:receipt:delete'])"
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
    <ReceiptForm ref="formRef" @success="getList" />

    <!-- 详情弹窗：收货单头 + 明细 -->
    <Dialog v-model="detailVisible" :title="detailTitle" width="1100">
      <el-descriptions v-if="detail" :column="3" border>
        <el-descriptions-item label="收货单号">{{ detail.receiptNo }}</el-descriptions-item>
        <el-descriptions-item label="采购订单号">
          {{ detail.orderNo || '无单收货' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :type="DICT_TYPE.PHARMACY_RECEIPT_STATUS" :value="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="门店">{{ detail.storeName }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detail.receiveByName }}</el-descriptions-item>
        <el-descriptions-item label="收货时间">{{ formatDate(detail.receiveDate) }}</el-descriptions-item>
        <el-descriptions-item label="实收总数量">{{ detail.totalQty }}</el-descriptions-item>
        <el-descriptions-item label="实收总金额">
          {{ formatAmount(detail.totalAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="差异">
          <dict-tag :type="DICT_TYPE.PHARMACY_RECEIPT_DIFF_TYPE" :value="detail.diffType" />
        </el-descriptions-item>
        <el-descriptions-item label="质检">
          <dict-tag :type="DICT_TYPE.PHARMACY_QUALITY_STATUS" :value="detail.qualityStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="入账时间">{{ formatDate(detail.postedAt) || '未入账' }}</el-descriptions-item>
        <el-descriptions-item label="仓库编号">{{ detail.warehouseId }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="detail?.lines || []" class="mt-10px" :show-overflow-tooltip="true">
        <el-table-column label="行号" align="center" prop="lineNo" width="70" />
        <el-table-column label="药品编码" align="left" prop="drugCode" width="120" />
        <el-table-column label="药品名称" align="left" prop="drugName" min-width="150" />
        <el-table-column label="批号" align="left" prop="batchNo" width="140" />
        <el-table-column
          label="生产日期"
          align="center"
          prop="manufactureDate"
          width="110"
          :formatter="localDateFormatter"
        />
        <el-table-column
          label="有效期至"
          align="center"
          prop="expiryDate"
          width="110"
          :formatter="localDateFormatter"
        />
        <el-table-column label="实收数量" align="right" prop="qty" width="100" />
        <el-table-column label="单价" align="right" prop="unitPrice" width="100">
          <template #default="scope">
            <span>{{ formatAmount(scope.row.unitPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" align="right" prop="amount" width="110">
          <template #default="scope">
            <span>{{ formatAmount(scope.row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="货位" align="center" prop="locationId" width="90" />
        <el-table-column label="质检" align="center" prop="qualityFlag" width="110">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.PHARMACY_QUALITY_FLAG" :value="scope.row.qualityFlag" />
          </template>
        </el-table-column>
        <el-table-column label="批次编号" align="center" prop="createBatchId" width="110">
          <template #default="scope">
            <span>{{ scope.row.createBatchId || '未入账' }}</span>
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
import { dateFormatter, formatDate } from '@/utils/formatTime'
import { localDateFormatter } from '../utils/date'
import * as ReceiptApi from '@/api/pharmacy/purchase/receipt'
import * as OrderApi from '@/api/pharmacy/purchase/order'
import * as StoreApi from '@/api/pharmacy/base/store'
import ReceiptForm from './ReceiptForm.vue'

defineOptions({ name: 'PharmacyPurchaseReceipt' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<ReceiptApi.PurchaseReceiptVO[]>([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  receiptNo: '',
  status: undefined,
  storeId: undefined,
  orderId: undefined,
  diffType: undefined,
  receiveDate: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
/** 查询区「更多筛选」展开状态：默认仅展示高频条件 */
const expandQuery = ref(false)

/** 下拉数据：门店与采购订单 */
const storeList = ref<StoreApi.StoreSimpleVO[]>([])
const orderList = ref<OrderApi.PurchaseOrderVO[]>([])
const loadOptions = async () => {
  const [stores, orders] = await Promise.all([
    StoreApi.getSimpleStoreList(),
    OrderApi.getPurchaseOrderPage({ pageNo: 1, pageSize: 100 })
  ])
  storeList.value = stores
  orderList.value = orders.list || []
}

const formatAmount = (value?: number) => {
  if (value === undefined || value === null) return '0.00'
  return Number(value).toFixed(2)
}

/** 与后端一致：仅待提交与已作废可删除 */
const canDelete = (status?: number) => status === 0 || status === 3

/** 查询收货单列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ReceiptApi.getPurchaseReceiptPage(queryParams)
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
const handleCommand = (command: string, row: ReceiptApi.PurchaseReceiptVO) => {
  if (!row.id) return
  if (command === 'detail') openDetail(row.id)
  else if (command === 'submit') handleSubmit(row.id)
  else if (command === 'post') handlePost(row.id)
  else if (command === 'void') handleVoid(row.id)
  else if (command === 'delete') handleDelete(row.id)
}

/** 提交收货单 */
const handleSubmit = async (id: number) => {
  try {
    await message.confirm('确认提交该收货单?', '提交收货单')
    await ReceiptApi.submitPurchaseReceipt(id)
    message.success('提交成功')
    await getList()
  } catch {}
}

/**
 * 收货入账：会调用库存服务写入批次与货位库存，并把订单已收数量累计在同一事务内。
 * 库存服务未就绪时后端返回明确业务错误，状态不会变为已入账。
 */
const handlePost = async (id: number) => {
  try {
    await message.confirm('确认对该收货单入账? 入账后将写入批次库存且不可重复入账。', '收货入账')
    await ReceiptApi.postPurchaseReceipt(id)
    message.success('入账成功')
    await getList()
  } catch {}
}

/** 作废收货单 */
const handleVoid = async (id: number) => {
  try {
    await message.confirm('确认作废该收货单?', '作废收货单')
    await ReceiptApi.voidPurchaseReceipt(id)
    message.success('已作废')
    await getList()
  } catch {}
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await ReceiptApi.deletePurchaseReceipt(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailTitle = ref('')
const detail = ref<ReceiptApi.PurchaseReceiptDetailVO>()
const openDetail = async (id: number) => {
  detailVisible.value = true
  const data = (await ReceiptApi.getPurchaseReceipt(id)) as ReceiptApi.PurchaseReceiptDetailVO
  detail.value = data
  detailTitle.value = `收货单详情 - ${data.receiptNo}`
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await ReceiptApi.exportPurchaseReceipt(queryParams)
    download.excel(data, '采购收货单.xls')
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
