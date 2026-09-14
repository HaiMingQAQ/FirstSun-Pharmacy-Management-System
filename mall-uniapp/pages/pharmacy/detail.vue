<!-- 药店小程序 - 商品详情（真实接口：A 商品详情） -->
<template>
  <view class="pharmacy-detail">
    <view v-if="drug" class="detail">
      <view class="detail__head">
        <view class="detail__name">
          {{ drug.genericName }}
          <text v-if="drug.isRx === 1" class="detail__rx">处方药</text>
        </view>
        <view v-if="drug.tradeName" class="detail__trade">{{ drug.tradeName }}</view>
        <view class="detail__price">
          <text class="detail__price-now">￥{{ displayPrice }}</text>
          <text v-if="drug.memberPrice" class="detail__price-old">￥{{ drug.retailPrice }}</text>
        </view>
      </view>

      <view class="detail__rows">
        <view class="detail__row">
          <text class="detail__row-label">规格</text>
          <text class="detail__row-value">{{ drug.specification || '—' }}</text>
        </view>
        <view class="detail__row">
          <text class="detail__row-label">剂型</text>
          <text class="detail__row-value">{{ drug.dosageForm || '—' }}</text>
        </view>
        <view class="detail__row">
          <text class="detail__row-label">生产厂家</text>
          <text class="detail__row-value">{{ drug.manufacturer || '—' }}</text>
        </view>
        <view class="detail__row">
          <text class="detail__row-label">销售单位</text>
          <text class="detail__row-value">{{ drug.unit || '—' }}</text>
        </view>
        <view class="detail__row">
          <text class="detail__row-label">所属分类</text>
          <text class="detail__row-value">{{ drug.categoryName || '—' }}</text>
        </view>
      </view>

      <view v-if="drug.description" class="detail__desc">
        <view class="detail__desc-title">药品说明</view>
        <view class="detail__desc-text">{{ drug.description }}</view>
      </view>

      <view class="store-tip">
        履约门店：{{ storeName || '未选择' }}
      </view>

      <!-- 数量与加购 -->
      <view class="buy-bar">
        <view class="buy-bar__qty">
          <view class="buy-bar__qty-btn" @tap="changeQty(-1)">-</view>
          <text class="buy-bar__qty-num">{{ qty }}</text>
          <view class="buy-bar__qty-btn" @tap="changeQty(1)">+</view>
        </view>
        <view class="buy-bar__add" @tap="handleAddCart">加入购物车</view>
      </view>
    </view>

    <view v-else-if="!loading" class="empty">商品不存在或不可线上销售</view>
    <view v-else class="empty">加载中…</view>
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import DrugApi from '@/sheep/api/pharmacy/drug';
  import StoreApi from '@/sheep/api/pharmacy/store';
  import CartApi from '@/sheep/api/pharmacy/cart';

  const drug = ref(null);
  const loading = ref(true);
  const qty = ref(1);
  const storeId = ref(null);
  const storeName = ref('');

  const displayPrice = computed(() => {
    if (!drug.value) {
      return '—';
    }
    const price = drug.value.memberPrice !== null && drug.value.memberPrice !== undefined
      ? drug.value.memberPrice
      : drug.value.retailPrice;
    return price === null || price === undefined ? '—' : price;
  });

  const loadStore = async () => {
    const savedId = uni.getStorageSync('pharmacy-store-id');
    const { code, data } = await StoreApi.getStoreList();
    if (code !== 0) {
      return;
    }
    const stores = data || [];
    const found = stores.find((item) => item.id === savedId) || stores[0] || null;
    if (found) {
      storeId.value = found.id;
      storeName.value = found.storeName;
      uni.setStorageSync('pharmacy-store-id', found.id);
    }
  };

  const loadDrug = async (id) => {
    loading.value = true;
    const { code, data } = await DrugApi.getDrug(id);
    loading.value = false;
    if (code === 0) {
      drug.value = data;
    }
  };

  const changeQty = (delta) => {
    const next = qty.value + delta;
    if (next < 1) {
      return;
    }
    qty.value = next;
  };

  const handleAddCart = async () => {
    if (!sheep.$store('user').isLogin) {
      uni.navigateTo({
        url: '/pages/pharmacy/login',
      });
      return;
    }
    if (!storeId.value) {
      uni.showToast({
        title: '请先选择履约门店',
        icon: 'none',
      });
      return;
    }
    await CartApi.addCart({
      drugId: drug.value.id,
      qty: qty.value,
      storeId: storeId.value,
    });
  };

  onLoad((options) => {
    loadStore();
    if (options.id) {
      loadDrug(options.id);
    } else {
      loading.value = false;
    }
  });
</script>

<style lang="scss" scoped>
  .pharmacy-detail {
    min-height: 100vh;
    padding-bottom: 140rpx;
    background: #f5f7f8;
  }

  .detail {
    &__head {
      padding: 32rpx 24rpx;
      background: #ffffff;
    }

    &__name {
      font-size: 34rpx;
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

    &__trade {
      margin-top: 8rpx;
      font-size: 26rpx;
      color: #667085;
    }

    &__price {
      margin-top: 16rpx;
    }

    &__price-now {
      font-size: 44rpx;
      font-weight: 600;
      color: #ef4444;
    }

    &__price-old {
      margin-left: 16rpx;
      font-size: 26rpx;
      color: #98a2b3;
      text-decoration: line-through;
    }

    &__rows {
      margin-top: 16rpx;
      padding: 8rpx 24rpx;
      background: #ffffff;
    }

    &__row {
      display: flex;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #f0f2f5;
      font-size: 28rpx;
    }

    &__row-label {
      width: 180rpx;
      color: #98a2b3;
    }

    &__row-value {
      flex: 1;
      color: #1f2933;
    }

    &__desc {
      margin-top: 16rpx;
      padding: 24rpx;
      background: #ffffff;
    }

    &__desc-title {
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2933;
    }

    &__desc-text {
      margin-top: 12rpx;
      font-size: 26rpx;
      color: #667085;
      line-height: 1.7;
    }
  }

  .store-tip {
    margin-top: 16rpx;
    padding: 20rpx 24rpx;
    font-size: 26rpx;
    color: #667085;
    background: #ffffff;
  }

  .buy-bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    height: 110rpx;
    padding: 0 24rpx;
    background: #ffffff;
    border-top: 1rpx solid #e4e7ec;

    &__qty {
      display: flex;
      align-items: center;
    }

    &__qty-btn {
      width: 60rpx;
      height: 60rpx;
      line-height: 56rpx;
      text-align: center;
      font-size: 32rpx;
      color: #1f2933;
      border: 1rpx solid #e4e7ec;
      border-radius: 8rpx;
    }

    &__qty-num {
      width: 80rpx;
      text-align: center;
      font-size: 30rpx;
      color: #1f2933;
    }

    &__add {
      flex: 1;
      margin-left: 32rpx;
      height: 80rpx;
      line-height: 80rpx;
      text-align: center;
      font-size: 30rpx;
      color: #ffffff;
      background: #176b5b;
      border-radius: 40rpx;
    }
  }

  .empty {
    padding: 120rpx 0;
    text-align: center;
    font-size: 28rpx;
    color: #98a2b3;
  }
</style>
