<template>
  <view class="pharmacy-tabbar">
    <button
      v-for="(item, index) in tabs"
      :key="item.path"
      class="tab"
      :class="{ active: index === current }"
      @tap="switchTab(index, item)"
    >
      <uni-icons :type="item.icon" size="23" :color="index === current ? '#176b5b' : '#758078'" />
      <text>{{ item.text }}</text>
    </button>
  </view>
</template>
<script setup>
  defineOptions({ options: { styleIsolation: 'apply-shared' } });
  const props = defineProps({ current: Number });
  const tabs = [
    { text: '首页', icon: 'home', path: 'index' },
    { text: '分类', icon: 'list', path: 'category' },
    { text: '购物车', icon: 'cart', path: 'cart' },
    { text: '我的', icon: 'person', path: 'user' },
  ];
  function switchTab(index, item) {
    if (index !== props.current) uni.reLaunch({ url: `/pages/pharmacy/${item.path}` });
  }
</script>
<style scoped>
  .pharmacy-tabbar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 40;
    display: flex;
    background: #fff;
    border-top: 1px solid #e5ebe8;
    padding-bottom: env(safe-area-inset-bottom);
  }
  .pharmacy-tabbar .tab {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
    height: 60px;
    min-height: 60px;
    padding: 5px 0;
    background: #fff;
    border-radius: 0;
    color: #758078;
    font-size: 12px;
  }
  .pharmacy-tabbar .active {
    color: #176b5b;
    font-weight: 600;
  }
</style>
