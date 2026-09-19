<template>
  <s-pharmacy-page>
    <scroll-view scroll-x class="order-tabs fs-white">
      <view class="tabs-inner">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="{ active: filter === tab.value }"
          @tap="filter = tab.value"
        >
          {{ tab.label }}
        </button>
      </view>
    </scroll-view>
    <s-pharmacy-state
      v-if="!loggedIn"
      title="登录后查看订单"
      action="去登录"
      @retry="go('login')"
    />
    <s-pharmacy-state
      v-else-if="loading || error || !filtered.length"
      :loading="loading"
      :error="error"
      title="暂无相关订单"
      description="购买后的订单会显示在这里"
      action="去选购药品"
      @retry="error ? load() : go('category')"
    />
    <view v-else v-for="order in filtered" :key="order.id" class="fs-section">
      <view class="fs-between">
        <text class="fs-small">阳光店</text>
        <text class="order-status">{{ statusNames[order.status] }}</text>
      </view>
      <view class="fs-muted fs-gap">订单号 {{ order.id }}</view>
      <s-pharmacy-product
        v-for="row in order.items"
        :key="row.drugId"
        :product="row.product"
        compact
        hide-stock
        :qty="row.qty"
      />
      <view class="fs-between fs-gap">
        <text class="fs-muted">{{ order.paid ? '已支付（模拟）' : '未支付' }}</text>
        <text>
          {{ order.paid ? '实付合计' : '应付合计' }}
          <text class="fs-price">¥{{ money(order.total) }}</text>
        </text>
      </view>
      <s-pharmacy-order-actions :order="order" detail @change="load" />
    </view>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref, computed } from 'vue';
  import { onLoad, onShow, onPullDownRefresh } from '@dcloudio/uni-app';
  import api, { money, statusNames } from '@/sheep/api/pharmacy/client';
  import { go, useRequest } from './usePharmacy';
  const orders = ref([]),
    filter = ref('all'),
    loggedIn = ref(false);
  const { loading, error, run } = useRequest();
  const tabs = [
    { value: 'all', label: '全部' },
    ...Object.entries(statusNames).map(([value, label]) => ({ value, label })),
  ];
  const filtered = computed(() =>
    orders.value.filter((o) => filter.value === 'all' || o.status === filter.value),
  );
  const load = () =>
    run(async () => {
      orders.value = await api.orders();
    });
  onLoad((q) => (filter.value = q.status || 'all'));
  onShow(() => {
    loggedIn.value = !!api.session();
    if (loggedIn.value) load();
    else orders.value = [];
  });
  onPullDownRefresh(async () => {
    if (loggedIn.value) await load();
    uni.stopPullDownRefresh();
  });
</script>
<style scoped>
  .order-tabs {
    width: 100%;
    white-space: nowrap;
  }
  .tabs-inner {
    display: inline-flex;
    min-width: 100%;
  }
  .tabs-inner button {
    display: inline-flex;
    padding: 14px 16px;
    background: #fff;
    flex-shrink: 0;
    border-radius: 0;
    font-size: 14px;
    color: #64716c;
  }
  .tabs-inner .active {
    color: #176b5b;
    border-bottom: 2px solid #176b5b;
    font-weight: 600;
  }
  .order-status {
    color: #176b5b;
    font-size: 14px;
  }
</style>
