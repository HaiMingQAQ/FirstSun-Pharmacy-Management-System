<!-- 药店小程序底部导航（自定义，避免依赖商城装修 tabbar） -->
<template>
  <view class="pharmacy-tabbar">
    <view
      v-for="(item, index) in tabs"
      :key="item.path"
      class="pharmacy-tabbar__item"
      :class="{ 'is-active': index === current }"
      @tap="handleSwitch(index, item)"
    >
      <text class="pharmacy-tabbar__text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup>
  const props = defineProps({
    // 当前选中的下标：0 首页 / 1 分类 / 2 购物车 / 3 我的
    current: {
      type: Number,
      default: 0,
    },
  });

  const tabs = [
    { text: '首页', path: '/pages/pharmacy/index' },
    { text: '分类', path: '/pages/pharmacy/category' },
    { text: '购物车', path: '/pages/pharmacy/cart' },
    { text: '我的', path: '/pages/pharmacy/user' },
  ];

  const handleSwitch = (index, item) => {
    if (index === props.current) {
      return;
    }
    uni.reLaunch({
      url: item.path,
    });
  };
</script>

<style lang="scss" scoped>
  .pharmacy-tabbar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 99;
    display: flex;
    height: 100rpx;
    background: #ffffff;
    border-top: 1rpx solid #e4e7ec;
    padding-bottom: constant(safe-area-inset-bottom);
    padding-bottom: env(safe-area-inset-bottom);

    &__item {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    &__text {
      font-size: 28rpx;
      color: #667085;
    }

    &__item.is-active &__text {
      color: #176b5b;
      font-weight: 600;
    }
  }
</style>
