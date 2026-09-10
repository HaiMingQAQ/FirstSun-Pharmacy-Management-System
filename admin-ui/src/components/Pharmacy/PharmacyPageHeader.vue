<template>
  <header class="pharmacy-modern-header">
    <div class="pharmacy-modern-header__copy">
      <div class="pharmacy-modern-header__eyebrow">{{ eyebrow }}</div>
      <div class="pharmacy-modern-header__title-row">
        <span class="pharmacy-modern-header__icon" aria-hidden="true">
          <Icon :icon="icon" />
        </span>
        <div>
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>
      </div>
    </div>
    <div v-if="$slots.actions" class="pharmacy-modern-header__actions">
      <slot name="actions"></slot>
    </div>
  </header>

  <section class="pharmacy-metric-strip" aria-label="页面数据概览">
    <div class="pharmacy-metric-strip__primary">
      <span>{{ totalLabel }}</span>
      <strong>{{ total }}</strong>
      <div class="pharmacy-metric-strip__bars" aria-hidden="true">
        <i v-for="height in barHeights" :key="height" :style="{ height: `${height}px` }"></i>
      </div>
    </div>
    <div class="pharmacy-metric-strip__item">
      <span>当前页</span>
      <strong>{{ currentCount }} <small>条记录</small></strong>
    </div>
    <div class="pharmacy-metric-strip__item">
      <span>页面类型</span>
      <strong class="pharmacy-metric-strip__text">{{ pageType }}</strong>
    </div>
    <div class="pharmacy-metric-strip__item">
      <span>数据状态</span>
      <strong class="pharmacy-metric-strip__status">
        <i></i>{{ loading ? '同步中' : '已同步' }}
      </strong>
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    eyebrow: string
    icon: string
    totalLabel: string
    total: number
    currentCount: number
    subtitle?: string
    pageType?: string
    loading?: boolean
  }>(),
  {
    subtitle: 'FirstSun 药店管理系统 · 基础资料',
    pageType: '档案管理',
    loading: false
  }
)

const barHeights = [18, 27, 21, 34, 25, 39, 31]
</script>
