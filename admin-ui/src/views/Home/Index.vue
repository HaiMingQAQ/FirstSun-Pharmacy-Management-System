<template>
  <div class="pharmacy-page" v-loading="loading">
    <!-- 欢迎栏：真实用户与日期，不显示假天气/假宣传 -->
    <div class="pharmacy-welcome">
      <div class="pharmacy-welcome__main">
        <div class="pharmacy-page-title"> {{ welcomeText }}{{ username }} </div>
        <div class="pharmacy-text-secondary">
          {{ todayLabel }} · 欢迎使用 FirstSun 药店管理系统
        </div>
      </div>
    </div>

    <!-- 基础数据概览：来自真实分页接口的 total，不编造数字 -->
    <div class="pharmacy-section">
      <div class="pharmacy-section-title">基础数据概览</div>
      <el-row :gutter="16" class="mt-12px">
        <el-col
          v-for="kpi in baseKpis"
          :key="kpi.key"
          :xl="6"
          :lg="6"
          :md="12"
          :sm="12"
          :xs="24"
          class="mb-12px"
        >
          <div
            class="pharmacy-kpi"
            :class="{ 'pharmacy-kpi--clickable': !!kpi.route }"
            @click="kpi.route && goRoute(kpi.route)"
          >
            <div class="pharmacy-kpi__label">{{ kpi.label }}</div>
            <div v-if="kpi.error" class="pharmacy-kpi__empty">暂无统计数据</div>
            <div v-else-if="kpi.value === null" class="pharmacy-kpi__empty">--</div>
            <div v-else class="pharmacy-kpi__value">{{ kpi.value }}</div>
            <div v-if="kpi.hint" class="pharmacy-kpi__hint">{{ kpi.hint }}</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 经营待办：待审核药品有真实接口；销售额/订单数/库存预警暂无接口用空状态 -->
    <el-row :gutter="16" class="mt-8px">
      <el-col :xl="16" :lg="16" :md="24" :sm="24" :xs="24" class="mb-12px">
        <div class="pharmacy-panel pharmacy-todo">
          <div class="pharmacy-section-title">经营待办</div>
          <div class="pharmacy-todo__list mt-12px">
            <div
              v-if="hasApprovePermi && pendingDrugCount !== null"
              class="pharmacy-todo__item"
              @click="goRoute('/pharmacy/base/drug')"
            >
              <el-tag type="warning" size="small" effect="plain">待审核</el-tag>
              <span class="pharmacy-todo__text">待审核药品</span>
              <span class="pharmacy-todo__count">{{ pendingDrugCount }}</span>
              <span class="pharmacy-todo__action">前往处理 →</span>
            </div>
            <div
              v-if="hasApprovePermi && pendingDrugCount === 0"
              class="pharmacy-todo__item pharmacy-todo__item--empty"
            >
              <span class="pharmacy-text-weak">暂无待审核药品</span>
            </div>
            <div v-if="!hasApprovePermi" class="pharmacy-todo__item pharmacy-todo__item--empty">
              <span class="pharmacy-text-weak">暂无待办权限</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24" class="mb-12px">
        <div class="pharmacy-panel pharmacy-todo">
          <div class="pharmacy-section-title">经营数据</div>
          <div class="pharmacy-todo__list mt-12px">
            <div class="pharmacy-todo__item pharmacy-todo__item--empty">
              <span class="pharmacy-text-weak">
                销售额、订单数、库存预警等经营指标暂无统计接口，待后续接入后展示。
              </span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口：跳转真实存在且用户有权限的药店业务页面 -->
    <div class="pharmacy-section mt-8px">
      <div class="pharmacy-section-title">快捷入口</div>
      <el-row :gutter="16" class="mt-12px">
        <el-col
          v-for="sc in shortcuts"
          :key="sc.route"
          :xl="4"
          :lg="6"
          :md="8"
          :sm="12"
          :xs="24"
          class="mb-12px"
        >
          <div class="pharmacy-shortcut" @click="goRoute(sc.route)">
            <Icon :icon="sc.icon" :size="20" />
            <span class="pharmacy-shortcut__label">{{ sc.label }}</span>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { useUserStore } from '@/store/modules/user'
import { checkPermi } from '@/utils/permission'
import { useRouter } from 'vue-router'
import * as DrugApi from '@/api/pharmacy/base/drug'
import * as CategoryApi from '@/api/pharmacy/base/category'
import * as StoreApi from '@/api/pharmacy/base/store'
import * as EmployeeApi from '@/api/pharmacy/base/employee'

defineOptions({ name: 'Index' })

const router = useRouter()
const userStore = useUserStore()
const username = computed(() => userStore.getUser.nickname || '')

const loading = ref(true)

/** 欢迎语：按当前时段返回，不使用假天气数据 */
const welcomeText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好，'
  if (h < 9) return '早上好，'
  if (h < 12) return '上午好，'
  if (h < 14) return '中午好，'
  if (h < 18) return '下午好，'
  return '晚上好，'
})

/** 今日日期 */
const todayLabel = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} 星期${week}`
})

interface KpiItem {
  key: string
  label: string
  value: number | null
  error: boolean
  hint?: string
  route?: string
}

/** 基础数据 KPI：来自真实分页接口 total（药品条码移入快捷入口，保持四列不悬空） */
const baseKpis = reactive<KpiItem[]>([
  { key: 'drug', label: '药品总数', value: null, error: false, route: '/pharmacy/base/drug' },
  {
    key: 'category',
    label: '药品分类',
    value: null,
    error: false,
    route: '/pharmacy/base/category'
  },
  { key: 'store', label: '门店数', value: null, error: false, route: '/pharmacy/base/store' },
  {
    key: 'employee',
    label: '员工数',
    value: null,
    error: false,
    route: '/pharmacy/base/employee'
  }
])

/** 待审核药品数（真实接口，approveStatus=0） */
const pendingDrugCount = ref<number | null>(null)
const hasApprovePermi = computed(() => checkPermi(['pharmacy:base:drug:query']))

/** 快捷入口：仅跳转真实路由，按权限控制可见 */
const shortcuts = computed(() => {
  const list = [
    {
      label: '药品档案',
      icon: 'ep:first-aid-kit',
      route: '/pharmacy/base/drug',
      perm: 'pharmacy:base:drug:query'
    },
    {
      label: '药品分类',
      icon: 'ep:collection',
      route: '/pharmacy/base/category',
      perm: 'pharmacy:base:category:query'
    },
    {
      label: '门店',
      icon: 'ep:shop',
      route: '/pharmacy/base/store',
      perm: 'pharmacy:base:store:query'
    },
    {
      label: '员工',
      icon: 'ep:user',
      route: '/pharmacy/base/employee',
      perm: 'pharmacy:base:employee:query'
    },
    {
      label: '药品条码',
      icon: 'ep:barcode',
      route: '/pharmacy/base/barcode',
      perm: 'pharmacy:base:barcode:query'
    }
  ]
  return list.filter((s) => checkPermi([s.perm]))
})

/** 路由跳转 */
const goRoute = (path: string) => {
  router.push(path)
}

/** 取分页 total：pageSize=1 仅取计数，不编造数字 */
const fetchTotal = async (
  apiFn: (params: PageParam) => Promise<any>,
  extra?: Record<string, any>
): Promise<number> => {
  const data = await apiFn({ pageNo: 1, pageSize: 1, ...extra })
  return data?.total ?? 0
}

/** 加载所有真实数据：Promise.allSettled 独立隔离，任一失败不影响其他展示 */
const loadAll = async () => {
  loading.value = true
  try {
    const [drugR, categoryR, storeR, employeeR, pendingR] = await Promise.allSettled([
      fetchTotal(DrugApi.getDrugPage),
      fetchTotal(CategoryApi.getCategoryPage),
      fetchTotal(StoreApi.getStorePage),
      fetchTotal(EmployeeApi.getEmployeePage),
      fetchTotal(DrugApi.getDrugPage, { approveStatus: 0 })
    ])
    const results: Record<string, PromiseSettledResult<number>> = {
      drug: drugR,
      category: categoryR,
      store: storeR,
      employee: employeeR
    }
    baseKpis.forEach((kpi) => {
      const r = results[kpi.key]
      if (r.status === 'fulfilled') {
        kpi.value = r.value
      } else {
        kpi.error = true
      }
    })
    pendingDrugCount.value = pendingR.status === 'fulfilled' ? pendingR.value : null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>
<style lang="scss" scoped>
.pharmacy-welcome {
  background-color: var(--pharmacy-bg-panel);
  border: 1px solid var(--pharmacy-border-color);
  border-radius: var(--pharmacy-radius-md);
  padding: var(--pharmacy-spacing-md) var(--pharmacy-spacing-lg);
  margin-bottom: var(--pharmacy-spacing-md);

  &__main {
    display: flex;
    flex-direction: column;
    gap: var(--pharmacy-spacing-xs);
  }
}

.pharmacy-section {
  margin-bottom: var(--pharmacy-spacing-md);
}

.pharmacy-todo {
  height: 100%;
  padding: var(--pharmacy-spacing-md) var(--pharmacy-spacing-lg);

  &__list {
    display: flex;
    flex-direction: column;
    gap: var(--pharmacy-spacing-sm);
  }

  &__item {
    display: flex;
    align-items: center;
    gap: var(--pharmacy-spacing-sm);
    padding: var(--pharmacy-spacing-sm) var(--pharmacy-spacing-md);
    border: 1px solid var(--pharmacy-border-light);
    border-radius: var(--pharmacy-radius-sm);
    cursor: default;
    transition: border-color 0.16s;

    &:not(.pharmacy-todo__item--empty):hover {
      border-color: var(--pharmacy-color-accent);
      cursor: pointer;
    }

    &--empty {
      background-color: var(--pharmacy-bg-page);
      border-style: dashed;
      cursor: default;
    }
  }

  &__text {
    flex: 1;
    font-size: 14px;
    line-height: 22px;
    color: var(--pharmacy-text-primary);
  }

  &__count {
    font-size: 16px;
    font-weight: 700;
    color: var(--pharmacy-color-primary);
    font-variant-numeric: tabular-nums;
  }

  &__action {
    font-size: 13px;
    color: var(--pharmacy-color-accent);
    white-space: nowrap;
  }
}

.pharmacy-kpi {
  height: 100%;
  transition: border-color 0.16s;

  &--clickable {
    cursor: pointer;

    &:hover {
      border-color: var(--pharmacy-color-accent);
    }
  }
}
</style>
