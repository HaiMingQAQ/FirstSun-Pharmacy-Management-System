<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="600">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="分类编码" prop="catCode">
        <el-input v-model="formData.catCode" placeholder="请输入分类编码" />
      </el-form-item>
      <el-form-item label="分类名" prop="catName">
        <el-input v-model="formData.catName" placeholder="请输入分类名" />
      </el-form-item>
      <el-form-item label="上级分类" prop="parentId">
        <el-tree-select
          v-model="formData.parentId"
          :data="categoryTree"
          :props="{ label: 'catName', children: 'children' }"
          check-strictly
          placeholder="不选则为顶级分类"
          clearable
          node-key="id"
          class="!w-full"
        />
      </el-form-item>
      <el-form-item label="分类类型" prop="catType">
        <el-select v-model="formData.catType" placeholder="请选择分类类型" clearable>
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_CATEGORY_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number
          v-model="formData.sort"
          :min="0"
          controls-position="right"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.PHARMACY_STATUS)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as CategoryApi from '@/api/pharmacy/base/category'
import { handleTree } from '@/utils/tree'

defineOptions({ name: 'PharmacyBaseCategoryForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  catCode: '',
  catName: '',
  parentId: 0,
  catType: 0,
  sort: 0,
  status: 1
})
const formRules = reactive({
  catCode: [{ required: true, message: '分类编码不能为空', trigger: 'blur' }],
  catName: [{ required: true, message: '分类名不能为空', trigger: 'blur' }],
  catType: [{ required: true, message: '分类类型不能为空', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 上级分类树 */
const categoryTree = ref<any[]>([])
const getCategoryTree = async () => {
  const list = await CategoryApi.getSimpleCategoryList()
  // 补一个虚拟根节点（顶级 parentId 为 0）
  const root = { id: 0, catName: '顶级分类', children: handleTree(list, 'id', 'parentId') }
  categoryTree.value = [root]
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 加载上级分类树（仅启用）
  await getCategoryTree()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      const data = await CategoryApi.getCategory(id)
      formData.value = data
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as CategoryApi.CategoryVO
    if (formType.value === 'create') {
      await CategoryApi.createCategory(data)
      message.success(t('common.createSuccess'))
    } else {
      await CategoryApi.updateCategory(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    catCode: '',
    catName: '',
    parentId: 0,
    catType: 0,
    sort: 0,
    status: 1
  } as any
  formRef.value?.resetFields()
}
</script>
