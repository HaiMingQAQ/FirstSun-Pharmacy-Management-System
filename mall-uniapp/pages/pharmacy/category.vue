<!-- 药店小程序 - 分类页（真实接口：A 药品分类 + 商品） -->
<template>
  <view class="pharmacy-category">
    <!-- 左侧分类 -->
    <view class="side">
      <view
        class="side__item"
        :class="{ 'is-active': activeCategoryId === null }"
        @tap="handleCategory(null)"
      >
        全部
      </view>
      <view
        v-for="item in categories"
        :key="item.id"
        class="side__item"
        :class="{ 'is-active': activeCategoryId === item.id }"
        @tap="handleCategory(item.id)"
      >
        {{ item.catName }}
      </view>
    </view>

    <!-- 右侧商品 -->
    <view class="main">
      <view class="main__title">{{ currentCategoryName }}</view>
      <view v-for="item in list" :key="item.id" class="goods" @tap="goDetail(item.id)">
        <view class="goods__name">
          {{ item.genericName }}
          <text v-if="item.isRx === 1" class="goods__rx">处方药</text>
        </view>
        <view class="goods__spec">{{ item.specification || '—' }}</view>
        <view class="goods__price">
          <text class="goods__price-now">￥{{ displayPrice(item) }}</text>
          <text v-if="item.memberPrice" class="goods__price-old">￥{{ item.retailPrice }}</text>
        </view>
      </view>

      <view v-if="!loading && list.length === 0" class="empty">该分类下暂无商品</view>
      <view v-if="loading" class="empty">加载中…</view>
      <view v-if="!loading && finished && list.length > 0" class="empty">没有更多了</view>
    </view>

    <s-pharmacy-tabbar :current="1" />
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onShow, onReachBottom } from '@dcloudio/uni-app';
  import DrugApi from '@/sheep/api/pharmacy/drug';

  const categories = ref([]);
  const activeCategoryId = ref(null);
  const list = ref([]);
  const pageNo = ref(1);
  const pageSize = 10;
  const total = ref(0);
  const loading = ref(false);
  const finished = ref(false);

  const currentCategoryName = computed(() => {
    if (activeCategoryId.value === null) {
      return '全部商品';
    }
    const found = categories.value.find((item) => item.id === activeCategoryId.value);
    return found ? found.catName : '全部商品';
  });

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

  const handleCategory = (categoryId) => {
    activeCategoryId.value = categoryId;
    loadList(true);
  };

  const loadMore = () => {
    loadList(false);
  };

  const goDetail = (id) => {
    uni.navigateTo({
      url: `/pages/pharmacy/detail?id=${id}`,
    });
  };

  onShow(() => {
    if (categories.value.length === 0) {
      loadCategories();
    }
    loadList(true);
  });

  onReachBottom(() => {
    loadMore();
  });
</script>

<style lang="scss" scoped>
  .pharmacy-category {
    display: flex;
    align-items: flex-start;
    min-height: 100vh;
    padding-bottom: 120rpx;
    box-sizing: border-box;
    background: #f5f7f8;
  }

  .side {
    width: 200rpx;
    background: #ffffff;

    &__item {
      padding: 28rpx 16rpx;
      font-size: 26rpx;
      color: #667085;
      text-align: center;

      &.is-active {
        color: #176b5b;
        font-weight: 600;
        background: #f5f7f8;
        border-left: 6rpx solid #176b5b;
      }
    }
  }

  .main {
    flex: 1;
    padding: 16rpx;

    &__title {
      padding: 8rpx 8rpx 16rpx;
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2933;
    }
  }

  .goods {
    padding: 24rpx;
    margin-bottom: 16rpx;
    background: #ffffff;
    border-radius: 12rpx;

    &__name {
      font-size: 28rpx;
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
      font-size: 30rpx;
      font-weight: 600;
      color: #ef4444;
    }

    &__price-old {
      margin-left: 12rpx;
      font-size: 24rpx;
      color: #98a2b3;
      text-decoration: line-through;
    }
  }

  .empty {
    padding: 40rpx 0;
    text-align: center;
    font-size: 26rpx;
    color: #98a2b3;
  }
</style>
