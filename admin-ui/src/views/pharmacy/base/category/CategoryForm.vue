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

/** 统一默认值，避免隐式 any 与类型漂移 */
const createDefaultFormData = (): CategoryApi.CategoryVO => ({
  id: undefined,
  catCode: '',
  catName: '',
  parentId: 0,
  catType: 0,
  sort: 0,
  status: 1
})

const formData = ref<CategoryApi.CategoryVO>(createDefaultFormData())

const formRules = reactive({
  catCode: [{ required: true, message: '分类编码不能为空', trigger: 'blur' }],
  catName: [{ required: true, message: '分类名不能为空', trigger: 'blur' }],
  catType: [{ required: true, message: '分类类型不能为空', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 上级分类树（含虚拟根节点 id=0 = 顶级分类） */
const categoryTree = ref<any[]>([])

/**
 * 构建上级分类树，递归排除当前编辑分类及其全部子孙，防止循环引用。
 * 数据源使用分页接口（pageSize=-1，后端 PageParam.PAGE_SIZE_NONE）取「启用+停用」完整平面列表，
 * 不依赖 /simple-list（仅返回启用分类，无法识别停用分类的后代）。
 */
const getCategoryTree = async (excludeId?: number) => {
  const page = await CategoryApi.getCategoryPage({ pageNo: 1, pageSize: -1 })
  const all = (page.list || []) as CategoryApi.CategoryVO[]
  // 递归收集 excludeId 的全部后代 id
  const excludeSet = new Set<number>()
  if (excludeId) {
    excludeSet.add(excludeId)
    const stack = [excludeId]
    while (stack.length) {
      const cur = stack.pop()!
      for (const item of all) {
        if (item.parentId === cur && !excludeSet.has(item.id!)) {
          excludeSet.add(item.id!)
          stack.push(item.id!)
        }
      }
    }
  }
  const filtered = all.filter((c) => !excludeSet.has(c.id!))
  const tree = handleTree(filtered, 'id', 'parentId')
  categoryTree.value = [{ id: 0, catName: '顶级分类', children: tree }]
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    if (id) {
      // 先取详情设置 formData.id，再据此构建过滤树（时序保证 excludeId 可用）
      const data = await CategoryApi.getCategory(id)
      formData.value = { ...createDefaultFormData(), ...data }
      await getCategoryTree(formData.value.id)
    } else {
      await getCategoryTree()
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 构造保存载荷，显式逐字段返回，不使用 as 断言 */
const buildSaveData = (): CategoryApi.CategoryVO => {
  const v = formData.value
  const payload: CategoryApi.CategoryVO = {
    catCode: v.catCode,
    catName: v.catName,
    parentId: v.parentId ?? 0,
    catType: v.catType,
    sort: v.sort,
    status: v.status
  }
  if (formType.value === 'update') {
    payload.id = v.id
  }
  return payload
}

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
    const data = buildSaveData()
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
  formData.value = createDefaultFormData()
  formRef.value?.resetFields()
}
</script>
