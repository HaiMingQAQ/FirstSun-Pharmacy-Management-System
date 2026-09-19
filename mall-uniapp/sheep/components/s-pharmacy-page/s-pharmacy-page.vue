<template>
  <view class="fs-page" :class="{ 'has-tabs': tab >= 0, 'has-dock': dock }">
    <view class="fs-demo">测试体验 · 商品价格及库存为模拟数据</view>
    <slot />
    <s-pharmacy-tabbar v-if="tab >= 0" :current="tab" />
  </view>
</template>
<script setup>
  defineOptions({ options: { styleIsolation: 'apply-shared' } });
  defineProps({ tab: { type: Number, default: -1 }, dock: Boolean });
</script>
