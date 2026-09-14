<!-- 药店小程序 - 首页（真实接口：A 商品 + 门店） -->
<template>
  <view class="pharmacy-home">
    <!-- 门店选择 -->
    <view class="store-bar" @tap="showStorePicker = true">
      <text class="store-bar__label">履约门店</text>
      <text class="store-bar__name">{{ currentStore ? currentStore.storeName : '请选择门店' }}</text>
      <text class="store-bar__arrow">›</text>
    </view>

    <!-- 搜索 -->
    <view class="search-bar">
      <input
        v-model="keyword"
        class="search-bar__input"
        type="text"
        placeholder="搜索药品名称"
        confirm-type="search"
        @confirm="handleSearch"
      />
      <view class="search-bar__btn" @tap="handleSearch">搜索</view>
    </view>

    <!-- 分类快捷入口 -->
    <view class="category-bar">
      <view
        class="category-bar__item"
        :class="{ 'is-active': activeCategoryId === null }"
        @tap="handleCategory(null)"
      >
        全部
      </view>
      <view
        v-for="item in categories"
        :key="item.id"
        class="category-bar__item"
        :class="{ 'is-active': activeCategoryId === item.id }"
        @tap="handleCategory(item.id)"
      >
        {{ item.catName }}
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="goods-list">
      <view v-for="item in list" :key="item.id" class="goods-card" @tap="goDetail(item.id)">
        <view class="goods-card__main">
          <view class="goods-card__name">
            {{ item.genericName }}
            <text v-if="item.isRx === 1" class="goods-card__rx">处方药</text>
          </view>
          <view class="goods-card__spec">{{ item.specification || '—' }}</view>
          <view class="goods-card__price">
            <text class="goods-card__price-now">￥{{ displayPrice(item) }}</text>
            <text v-if="item.memberPrice" class="goods-card__price-old">￥{{ item.retailPrice }}</text>
          </view>
        </view>
        <view class="goods-card__action">
          <view class="goods-card__add" @tap.stop="handleAddCart(item)">加购</view>
        </view>
      </view>

      <view v-if="!loading && list.length === 0" class="empty">暂无商品</view>
      <view v-if="loading" class="loading">加载中…</view>
      <view v-if="!loading && list.length > 0 && finished" class="finished">没有更多了</view>
    </view>

    <s-pharmacy-tabbar :current="0" />

    <!-- 门店选择弹窗 -->
    <view v-if="showStorePicker" class="picker-mask" @tap="showStorePicker = false">
      <view class="picker" @tap.stop>
        <view class="picker__title">选择履约门店</view>
        <view class="picker__list">
          <view
            v-for="item in stores"
            :key="item.id"
            class="picker__item"
            :class="{ 'is-active': currentStore && currentStore.id === item.id }"
            @tap="handleSelectStore(item)"
          >
            <view class="picker__item-name">{{ item.storeName }}</view>
            <view class="picker__item-addr">{{ item.address || '' }}</view>
          </view>
          <view v-if="stores.length === 0" class="empty">暂无可选门店</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue';
  import { onShow, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import DrugApi from '@/sheep/api/pharmacy/drug';
  import StoreApi from '@/sheep/api/pharmacy/store';
  import CartApi from '@/sheep/api/pharmacy/cart';

  const keyword = ref('');
  const activeCategoryId = ref(null);
  const categories = ref([]);
  const stores = ref([]);
  const currentStore = ref(null);
  const showStorePicker = ref(false);

  const list = ref([]);
  const pageNo = ref(1);
  const pageSize = 10;
  const total = ref(0);
  const loading = ref(false);
  const finished = ref(false);

  const displayPrice = (item) => {
    const price = item.memberPrice !== null && item.memberPrice !== undefined
      ? item.memberPrice
      : item.retailPrice;
    return price === null || price === undefined ? '—' : price;
  };

  const loadCategories = async () => {
    const { code, data } = await DrugApi.getCategoryList();
    if (code === 0) {
      categories.value = data || [];
    }
  };

  const loadStores = async () => {
    const { code, data } = await StoreApi.getStoreList();
    if (code !== 0) {
      return;
    }
    stores.value = data || [];
    // 优先使用上次选择，其次使用第一个营业门店
    const savedId = uni.getStorageSync('pharmacy-store-id');
    const saved = stores.value.find((item) => item.id === savedId);
    currentStore.value = saved || stores.value[0] || null;
    if (currentStore.value) {
      uni.setStorageSync('pharmacy-store-id', currentStore.value.id);
    }
  };

  const loadList = async (reset = false) => {
    if (loading.value) {
      return;
    }
    if (reset) {
      pageNo.value = 1;
      finished.value = false;
    }
    if (finished.value) {
      return;
    }
    loading.value = true;
    const params = {
      pageNo: pageNo.value,
      pageSize,
    };
    if (keyword.value) {
      params.keyword = keyword.value;
    }
    if (activeCategoryId.value !== null) {
      params.categoryId = activeCategoryId.value;
    }
    const { code, data } = await DrugApi.getDrugPage(params);
    loading.value = false;
    if (code !== 0) {
      return;
    }
    list.value = reset ? data.list || [] : list.value.concat(data.list || []);
    total.value = data.total || 0;
    finished.value = list.value.length >= total.value;
    if (!finished.value) {
      pageNo.value += 1;
    }
  };

  const handleSearch = () => {
    loadList(true);
  };

  const handleCategory = (categoryId) => {
    activeCategoryId.value = categoryId;
    loadList(true);
  };

  const handleSelectStore = (store) => {
    currentStore.value = store;
    uni.setStorageSync('pharmacy-store-id', store.id);
    showStorePicker.value = false;
  };

  const goDetail = (id) => {
    uni.navigateTo({
      url: `/pages/pharmacy/detail?id=${id}`,
    });
  };

  const handleAddCart = async (item) => {
    if (!sheep.$store('user').isLogin) {
      uni.navigateTo({
        url: '/pages/pharmacy/login',
      });
      return;
    }
    if (!currentStore.value) {
      uni.showToast({
        title: '请先选择履约门店',
        icon: 'none',
      });
      return;
    }
    const { code } = await CartApi.addCart({
      drugId: item.id,
      qty: 1,
      storeId: currentStore.value.id,
    });
    if (code === 0) {
      CartApi.getCartCount();
    }
  };

  onShow(() => {
    if (categories.value.length === 0) {
      loadCategories();
    }
    if (stores.value.length === 0) {
      loadStores();
    }
    loadList(true);
  });

  onReachBottom(() => {
    loadList(false);
  });

  onPullDownRefresh(async () => {
    await loadList(true);
    uni.stopPullDownRefresh();
  });
</script>

<style lang="scss" scoped>
  .pharmacy-home {
    min-height: 100vh;
    padding-bottom: 120rpx;
    background: #f5f7f8;
  }

  .store-bar {
    display: flex;
    align-items: center;
    padding: 20rpx 24rpx;
    background: #ffffff;

    &__label {
      font-size: 24rpx;
      color: #98a2b3;
      margin-right: 12rpx;
    }

    &__name {
      flex: 1;
      font-size: 28rpx;
      color: #1f2933;
      font-weight: 600;
    }

    &__arrow {
      font-size: 32rpx;
      color: #98a2b3;
    }
  }

  .search-bar {
    display: flex;
    align-items: center;
    padding: 16rpx 24rpx;
    background: #ffffff;

    &__input {
      flex: 1;
      height: 72rpx;
      padding: 0 24rpx;
      font-size: 28rpx;
      background: #f5f7f8;
      border-radius: 36rpx;
    }

    &__btn {
      margin-left: 16rpx;
      padding: 0 28rpx;
      height: 72rpx;
      line-height: 72rpx;
      font-size: 28rpx;
      color: #ffffff;
      background: #176b5b;
      border-radius: 36rpx;
    }
  }

  .category-bar {
    display: flex;
    flex-wrap: wrap;
    padding: 16rpx 24rpx;
    background: #ffffff;

    &__item {
      margin: 0 16rpx 12rpx 0;
      padding: 10rpx 28rpx;
      font-size: 26rpx;
      color: #667085;
      background: #f5f7f8;
      border-radius: 28rpx;

      &.is-active {
        color: #ffffff;
        background: #176b5b;
      }
    }
  }

  .goods-list {
    padding: 16rpx 24rpx;
  }

  .goods-card {
    display: flex;
    align-items: center;
    padding: 24rpx;
    margin-bottom: 16rpx;
    background: #ffffff;
    border-radius: 12rpx;

    &__main {
      flex: 1;
    }

    &__name {
      font-size: 30rpx;
      font-weight: 600;
      color: #1f2933;
    }

    &__rx {
      margin-left: 12rpx;
      padding: 2rpx 12rpx;
      font-size: 20rpx;
      font-weight: 400;
      color: #ef4444;
      border: 1rpx solid #ef4444;
      border-radius: 6rpx;
    }

    &__spec {
      margin-top: 8rpx;
      font-size: 24rpx;
      color: #667085;
    }

    &__price {
      margin-top: 12rpx;
    }

    &__price-now {
      font-size: 32rpx;
      color: #ef4444;
      font-weight: 600;
    }

    &__price-old {
      margin-left: 12rpx;
      font-size: 24rpx;
      color: #98a2b3;
      text-decoration: line-through;
    }

    &__action {
      margin-left: 16rpx;
    }

    &__add {
      padding: 12rpx 32rpx;
      font-size: 26rpx;
      color: #ffffff;
      background: #176b5b;
      border-radius: 32rpx;
    }
  }

  .empty,
  .loading,
  .finished {
    padding: 40rpx 0;
    text-align: center;
    font-size: 26rpx;
    color: #98a2b3;
  }

  .picker-mask {
    position: fixed;
    left: 0;
    right: 0;
    top: 0;
    bottom: 0;
    z-index: 200;
    background: rgba(0, 0, 0, 0.45);
  }

  .picker {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    max-height: 70vh;
    padding: 24rpx;
    background: #ffffff;
    border-radius: 16rpx 16rpx 0 0;

    &__title {
      font-size: 30rpx;
      font-weight: 600;
      color: #1f2933;
      text-align: center;
      padding-bottom: 16rpx;
    }

    &__list {
      max-height: 55vh;
      overflow-y: auto;
    }

    &__item {
      padding: 20rpx 16rpx;
      border-bottom: 1rpx solid #f0f2f5;
    }

    &__item.is-active .picker__item-name {
      color: #176b5b;
    }

    &__item-name {
      font-size: 28rpx;
      color: #1f2933;
    }

    &__item-addr {
      margin-top: 6rpx;
      font-size: 24rpx;
      color: #98a2b3;
    }
  }
</style>
