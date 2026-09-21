<template>
  <s-pharmacy-page :tab="0">
    <view class="home-head fs-pad fs-white">
      <view class="fs-between">
        <view>
          <view class="brand">
            FirstSun
            <text>药店</text>
          </view>
          <view class="fs-muted">身边的药店，安心的选择</view>
        </view>
        <uni-icons type="shop" size="30" color="#176b5b" />
      </view>
      <view class="store-line fs-between fs-gap" @tap="showStore">
        <view class="fs-grow">
          <view class="store-title">
            <uni-icons type="location" size="17" color="#176b5b" />
            {{ storeInfo.name }}
            <text class="fs-tag">演示门店</text>
          </view>
          <view class="fs-muted">到店自提 / 门店配送 · {{ storeInfo.hours }}</view>
        </view>
        <uni-icons type="right" size="16" color="#758078" />
      </view>
      <view class="fs-search fs-gap" @tap="go('category')">
        <uni-icons type="search" size="21" color="#64716c" />
        <text class="fs-muted">搜索药品名称、通用名、条码</text>
      </view>
    </view>
    <view class="shortcuts fs-white">
      <button
        v-for="cat in categories.slice(1)"
        :key="cat.id"
        @tap="go('category?category=' + cat.id)"
      >
        <view class="shortcut-icon"><uni-icons :type="cat.icon" size="24" color="#176b5b" /></view>
        <text>{{ cat.name }}</text>
      </button>
    </view>
    <view class="service-strip">
      <uni-icons type="checkmarkempty" size="16" color="#176b5b" />
      <text>门店履约</text>
      <text class="dot">·</text>
      <text>处方药需药师审核</text>
    </view>
    <view class="fs-section home-products">
      <view class="fs-between">
        <view>
          <view class="fs-title">家庭常备</view>
          <view class="fs-muted">常用药品，一目了然</view>
        </view>
        <button class="fs-text-btn" @tap="go('category')">
          全部
          <uni-icons type="right" size="14" color="#176b5b" />
        </button>
      </view>
      <s-pharmacy-state
        v-if="loading || error || !list.length"
        :loading="loading"
        :error="error"
        title="药品正在整理中"
        description="稍后再来看看，或尝试刷新"
        @retry="load"
      />
      <template v-else>
        <s-pharmacy-product
          v-for="product in list"
          :key="product.id"
          :product="product"
          add
          :busy="busy"
          @add="add"
        />
      </template>
    </view>
    <view class="fs-footer">请仔细阅读说明书，按说明书或药师指导使用</view>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref } from 'vue';
  import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
  import api from '@/sheep/api/pharmacy/client';
  import { go, toast, requireLogin, useRequest, useAction } from './usePharmacy';
  const list = ref([]);
  const storeInfo = ref(api.store);
  const categories = ref(api.categories);
  const { loading, error, run } = useRequest();
  const { busy, act } = useAction();
  const load = () =>
    run(async () => {
      list.value = (await api.products()).slice(0, 5);
      categories.value = api.categories;
      storeInfo.value = api.store;
    });
  const add = (p) => {
    if (requireLogin())
      act(async () => {
        await api.add(p.id);
        toast('已加入购物车');
      });
  };
  const showStore = () =>
    uni.showModal({
      title: api.store.name,
      content: `${api.store.address}\n营业时间 ${api.store.hours}\n支持门店配送与到店自提，结算时选择。`,
      showCancel: false,
      confirmColor: '#176b5b',
    });
  onShow(load);
  onPullDownRefresh(async () => {
    await load();
    uni.stopPullDownRefresh();
  });
</script>
<style scoped>
  .brand {
    color: #176b5b;
    font-size: 25px;
    font-weight: 750;
    letter-spacing: -0.7px;
  }
  .brand text {
    font-size: 20px;
    margin-left: 6px;
    letter-spacing: 0;
  }
  .store-line {
    padding-top: 14px;
    border-top: 1px solid #edf0ee;
  }
  .store-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 4px;
  }
  .shortcuts {
    display: flex;
    padding: 2px 8px 16px;
  }
  .shortcuts button {
    flex: 1;
    min-width: 0;
    background: #fff;
    flex-direction: column;
    padding: 0;
    gap: 8px;
    font-size: 12px;
  }
  .shortcut-icon {
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f0f5f1;
    border-radius: 8px;
  }
  .service-strip {
    display: flex;
    gap: 8px;
    justify-content: center;
    align-items: center;
    padding: 10px 8px;
    color: #63766a;
    font-size: 12px;
  }
  .dot {
    color: #bcc8c0;
  }
  .home-products {
    margin-top: 0;
  }
</style>
