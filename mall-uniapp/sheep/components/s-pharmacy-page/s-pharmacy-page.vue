<template>
  <view class="fs-page" :class="{ 'has-tabs': tab >= 0, 'has-dock': dock }">
    <view class="fs-demo">
      {{
        PHARMACY_DEMO
          ? '测试体验 · 商品价格及库存为模拟数据'
          : '受限演示 · 商品价格及库存来自演示环境'
      }}
    </view>
    <slot />
    <s-pharmacy-tabbar v-if="tab >= 0" :current="tab" />
  </view>
</template>
<script setup>
  import { PHARMACY_DEMO } from '@/sheep/api/pharmacy/config';
  defineOptions({ options: { styleIsolation: 'apply-shared' } });
  defineProps({ tab: { type: Number, default: -1 }, dock: Boolean });
</script>
