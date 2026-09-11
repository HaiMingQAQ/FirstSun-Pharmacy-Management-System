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
      <el-form-item label="会员ID" prop="memberId">
        <el-input
          v-model="queryParams.memberId"
          placeholder="请输入会员ID"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品ID" prop="drugId">
        <el-input
          v-model="queryParams.drugId"
          placeholder="请输入商品ID"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap class="pharmacy-panel">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="会员ID" align="center" prop="memberId" width="100" />
      <el-table-column label="商品ID" align="center" prop="drugId" width="100" />
      <el-table-column label="数量" align="center" prop="qty" width="100" />
      <el-table-column label="是否勾选" align="center" prop="selectedFlag" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.PHARMACY_YES_NO" :value="scope.row.selectedFlag" />
        </template>
      </el-table-column>
      <el-table-column label="加购时间" align="center" prop="addTime" width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.addTime) }}
        </template>
      </el-table-column>
      <el-table-column label="门店ID" align="center" prop="storeId" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['pharmacy:member:cart:delete']"
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
</template>
<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDateTime } from '@/utils/formatTime'
import * as CartApi from '@/api/pharmacy/member/cart'

defineOptions({ name: 'PharmacyMemberCart' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  memberId: undefined,
  drugId: undefined
})
const queryFormRef = ref() // 搜索的表单

/** 查询购物车列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await CartApi.getCartPage(queryParams)
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

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await CartApi.deleteCart(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
