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
      <el-form-item label="收件人" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入收件人"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          @click="openForm()"
          v-hasPermi="['pharmacy:member:address:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap class="pharmacy-panel">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="会员ID" align="center" prop="userId" width="100" />
      <el-table-column label="收件人" align="center" prop="name" width="120" />
      <el-table-column label="手机号" align="center" prop="mobile" width="130" />
      <el-table-column label="地区" align="center" prop="areaId" width="100" />
      <el-table-column label="详细地址" align="center" prop="detailAddress" show-overflow-tooltip />
      <el-table-column label="是否默认" align="center" prop="defaultStatus" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.PHARMACY_YES_NO" :value="scope.row.defaultStatus" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="handleView(scope.row.id)"
            v-hasPermi="['pharmacy:member:address:query']"
          >
            查看
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm(scope.row)"
            v-hasPermi="['pharmacy:member:address:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['pharmacy:member:address:delete']"
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

  <!-- 查看详情抽屉 -->
  <el-drawer v-model="detailVisible" title="收货地址详情" direction="rtl" size="500px">
    <el-descriptions :column="1" border v-loading="detailLoading">
      <el-descriptions-item label="会员ID">{{ detailData.userId }}</el-descriptions-item>
      <el-descriptions-item label="收件人">{{ detailData.name }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ detailData.mobile }}</el-descriptions-item>
      <el-descriptions-item label="地区ID">{{ detailData.areaId }}</el-descriptions-item>
      <el-descriptions-item label="详细地址">{{ detailData.detailAddress }}</el-descriptions-item>
      <el-descriptions-item label="是否默认">
        <dict-tag :type="DICT_TYPE.PHARMACY_YES_NO" :value="detailData.defaultStatus" />
      </el-descriptions-item>
      <el-descriptions-item label="创建时间">
        {{ formatDate(detailData.createTime) }}
      </el-descriptions-item>
    </el-descriptions>
  </el-drawer>

  <!-- 新增 / 编辑收货地址 -->
  <el-dialog v-model="formVisible" :title="formTitle" width="520px">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="90px">
      <el-form-item label="会员ID" prop="userId">
        <el-input-number v-model="formData.userId" :min="1" controls-position="right" class="!w-200px" />
      </el-form-item>
      <el-form-item label="收件人" prop="name">
        <el-input v-model="formData.name" placeholder="请输入收件人" />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="formData.mobile" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="地区ID" prop="areaId">
        <el-input-number v-model="formData.areaId" :min="0" controls-position="right" class="!w-200px" />
      </el-form-item>
      <el-form-item label="详细地址" prop="detailAddress">
        <el-input
          v-model="formData.detailAddress"
          type="textarea"
          :rows="3"
          placeholder="请输入详细地址"
        />
      </el-form-item>
      <el-form-item label="是否默认" prop="defaultStatus">
        <el-radio-group v-model="formData.defaultStatus">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_YES_NO)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">保 存</el-button>
    </template>
  </el-dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as AddressApi from '@/api/pharmacy/member/address'

defineOptions({ name: 'PharmacyMemberAddress' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  userId: undefined,
  name: ''
})
const queryFormRef = ref() // 搜索的表单

/** 详情相关 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<AddressApi.MemberAddressVO>({
  id: undefined,
  userId: 0,
  name: '',
  mobile: '',
  areaId: 0,
  detailAddress: '',
  defaultStatus: 0
})

/** 查询收货地址列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await AddressApi.getAddressPage(queryParams)
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

/** 查看详情 */
const handleView = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const data = await AddressApi.getAddress(id)
    detailData.value = data
  } finally {
    detailLoading.value = false
  }
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AddressApi.deleteAddress(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 新增 / 编辑表单 */
const formVisible = ref(false)
const formLoading = ref(false)
const formTitle = ref('')
const formRef = ref()
const defaultFormData = (): AddressApi.MemberAddressSaveVO => ({
  id: undefined,
  userId: 0,
  name: '',
  mobile: '',
  areaId: 0,
  detailAddress: '',
  defaultStatus: 0
})
const formData = ref<AddressApi.MemberAddressSaveVO>(defaultFormData())
const formRules = reactive({
  userId: [{ required: true, message: '会员ID不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '收件人不能为空', trigger: 'blur' }],
  mobile: [{ required: true, message: '手机号不能为空', trigger: 'blur' }],
  areaId: [{ required: true, message: '地区ID不能为空', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '详细地址不能为空', trigger: 'blur' }]
})

/** 打开新增 / 编辑表单 */
const openForm = (row?: AddressApi.MemberAddressVO) => {
  formVisible.value = true
  formTitle.value = row?.id ? '编辑收货地址' : '新增收货地址'
  // 只取后端 SaveReqVO 支持的字段，避免把 createTime 等只读字段提交回去
  formData.value = row
    ? {
        id: row.id,
        userId: row.userId,
        name: row.name,
        mobile: row.mobile,
        areaId: row.areaId,
        detailAddress: row.detailAddress,
        defaultStatus: row.defaultStatus
      }
    : defaultFormData()
}

/** 提交新增 / 编辑 */
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const payload = { ...formData.value }
    if (payload.id) {
      await AddressApi.updateAddress(payload)
      message.success(t('common.updateSuccess'))
    } else {
      await AddressApi.createAddress(payload)
      message.success(t('common.createSuccess'))
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
