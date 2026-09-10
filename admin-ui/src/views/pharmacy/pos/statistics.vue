<template>
  <div class="pharmacy-modern-page">
    <PharmacyPageHeader
      title="销售统计"
      eyebrow="SALES STATISTICS"
      icon="ep:data-analysis"
      total-label="统计天数"
      :total="rows.length"
      :current-count="sum('orderCount')"
      page-type="经营分析"
      :loading="loading"
      subtitle="FirstSun 药店管理系统 · 药店 POS"
    />
    <ContentWrap class="pharmacy-panel">
    <!-- 查询栏 -->
    <el-form class="-mb-15px" :inline="true" label-width="80px">
      <el-form-item label="门店">
        <el-input-number v-model="queryParams.storeId" :min="1" :controls="false" class="!w-140px" />
      </el-form-item>
      <el-form-item label="统计区间">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="datetimerange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="!w-360px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <el-row :gutter="16">
    <el-col :span="8">
      <ContentWrap class="pharmacy-panel">
        <div class="font-600 mb-10px">汇总</div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单笔数">
            <span class="font-700">{{ sum('orderCount') }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="销售额(元)">
            <span class="font-700 color-#f56c6c">{{ formatMoney(sum('saleAmount')) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="成本(元)">
            <span class="font-700">{{ formatMoney(sum('costAmount')) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="毛利(元)">
            <span class="font-700 color-#67c23a">{{ formatMoney(sum('saleAmount') - sum('costAmount')) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </ContentWrap>
    </el-col>
    <el-col :span="16">
      <ContentWrap class="pharmacy-panel">
        <Echart :options="echartsOption" height="360px" />
      </ContentWrap>
    </el-col>
  </el-row>

  <ContentWrap class="pharmacy-panel">
    <el-table v-loading="loading" :data="rows">
      <el-table-column label="日期" align="center" prop="bizDate" width="140" />
      <el-table-column label="订单笔数" align="center" prop="orderCount" width="110" />
      <el-table-column label="销售额(元)" align="center" width="130">
        <template #default="scope">{{ formatMoney(scope.row.saleAmount) }}</template>
      </el-table-column>
      <el-table-column label="成本(元)" align="center" width="130">
        <template #default="scope">{{ formatMoney(scope.row.costAmount) }}</template>
      </el-table-column>
      <el-table-column label="毛利(元)" align="center" width="130">
        <template #default="scope">
          {{ formatMoney(scope.row.saleAmount - scope.row.costAmount) }}
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && rows.length === 0" description="暂无统计数据" />
  </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import type { EChartsOption } from 'echarts'
import { SalesStatisticsApi } from '@/api/pharmacy/pos/statistics'

/** POS 销售统计 */
defineOptions({ name: 'PharmacyPosStatistics' })

const loading = ref(false)
const rows = ref<any[]>([])
const dateRange = ref<any[]>([getDefaultStart(), getDefaultEnd()])
const queryParams = reactive({ storeId: 1 })

function getDefaultStart() {
  const d = new Date()
  d.setDate(d.getDate() - 6)
  d.setHours(0, 0, 0, 0)
  return formatDateTime(d)
}
function getDefaultEnd() {
  const d = new Date()
  d.setHours(23, 59, 59, 999)
  return formatDateTime(d)
}
function formatDateTime(date: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const formatMoney = (value: number) => (value ?? 0).toFixed(2)
const sum = (key: string) => rows.value.reduce((acc, row) => acc + (row[key] ?? 0), 0)

const echartsOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['销售额', '成本', '毛利'] },
  grid: { left: 50, right: 20, top: 40, bottom: 40 },
  xAxis: { type: 'category', data: rows.value.map((row) => row.bizDate) },
  yAxis: { type: 'value' },
  series: [
    {
      name: '销售额',
      type: 'bar',
      data: rows.value.map((row) => row.saleAmount ?? 0),
      itemStyle: { color: '#f56c6c' }
    },
    {
      name: '成本',
      type: 'bar',
      data: rows.value.map((row) => row.costAmount ?? 0),
      itemStyle: { color: '#909399' }
    },
    {
      name: '毛利',
      type: 'line',
      smooth: true,
      data: rows.value.map((row) => (row.saleAmount ?? 0) - (row.costAmount ?? 0)),
      itemStyle: { color: '#67c23a' }
    }
  ]
}))

const loadData = async () => {
  loading.value = true
  try {
    rows.value = await SalesStatisticsApi.getDailyStatistics(
      queryParams.storeId,
      dateRange.value[0],
      dateRange.value[1]
    )
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  dateRange.value = [getDefaultStart(), getDefaultEnd()]
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.pos-stat-chart {
  width: 100%;
  height: 360px;
}
</style>
