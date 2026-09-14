<!-- 药店小程序 - 我的订单（真实接口：F 线上订单） -->
<template>
  <view class="pharmacy-order">
    <!-- 状态筛选 -->
    <view class="tabs">
      <view
        v-for="item in statusTabs"
        :key="item.value === null ? 'all' : item.value"
        class="tabs__item"
        :class="{ 'is-active': activeStatus === item.value }"
        @tap="handleStatus(item.value)"
      >
        {{ item.label }}
      </view>
    </view>

    <view v-if="!isLogin" class="empty">
      <view class="empty__text">登录后查看订单</view>
      <view class="empty__btn" @tap="goLogin">去登录</view>
    </view>

    <template v-else>
      <view v-if="list.length > 0" class="order-list">
        <view v-for="item in list" :key="item.id" class="order-card" @tap="goDetail(item.id)">
          <view class="order-card__head">
            <text class="order-card__no">{{ item.orderNo }}</text>
            <text class="order-card__status">{{ statusText(item.status) }}</text>
          </view>
          <view class="order-card__body">
            <view class="order-card__row">
              <text class="order-card__label">类型</text>
              <text class="order-card__value">{{ item.orderType === 1 ? '同城配送' : '到店自提' }}</text>
            </view>
            <view class="order-card__row">
              <text class="order-card__label">金额</text>
              <text class="order-card__amount">￥{{ item.payableAmount }}</text>
            </view>
            <view class="order-card__row">
              <text class="order-card__label">下单时间</text>
              <text class="order-card__value">{{ formatTime(item.createTime) }}</text>
            </view>
            <view v-if="item.pickupCode && item.status === 3" class="order-card__row">
              <text class="order-card__label">取货码</text>
              <text class="order-card__code">{{ item.pickupCode }}</text>
            </view>
          </view>
          <view class="order-card__ops">
            <view
              v-if="canCancel(item.status)"
              class="order-card__btn"
              @tap.stop="handleCancel(item)"
            >
              取消订单
            </view>
            <view class="order-card__btn order-card__btn--primary" @tap.stop="goDetail(item.id)">
              查看详情
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <view class="empty__text">{{ loading ? '加载中…' : '暂无订单' }}</view>
      </view>
    </template>
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onShow, onReachBottom } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import OrderApi from '@/sheep/api/pharmacy/order';

  const statusTabs = [
    { label: '全部', value: null },
    { label: '待支付', value: 0 },
    { label: '待拣货', value: 1 },
    { label: '待自提', value: 3 },
    { label: '已完成', value: 4 },
    { label: '已取消', value: -1 },
  ];

  const activeStatus = ref(null);
  const list = ref([]);
  const pageNo = ref(1);
  const pageSize = 10;
  const total = ref(0);
  const loading = ref(false);
  const finished = ref(false);

  const isLogin = computed(() => sheep.$store('user').isLogin);

  const STATUS_MAP = {
    '-1': '已取消',
    0: '待支付',
    1: '待拣货',
    2: '拣货中',
    3: '待自提',
    4: '已完成',
  };

  const statusText = (status) => STATUS_MAP[status] || '未知';

  const canCancel = (status) => status === 0 || status === 1;

  const formatTime = (value) => {
    if (!value) {
      return '';
    }
    const date = new Date(value);
    const pad = (num) => (num < 10 ? `0${num}` : `${num}`);
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
      date.getHours(),
    )}:${pad(date.getMinutes())}`;
  };

  const loadList = async (reset = false) => {
    if (!isLogin.value) {
      list.value = [];
      return;
    }
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
    if (activeStatus.value !== null) {
      params.status = activeStatus.value;
    }
    const { code, data } = await OrderApi.getOrderPage(params);
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

  const handleStatus = (status) => {
    activeStatus.value = status;
    loadList(true);
  };

  const handleCancel = (item) => {
    uni.showModal({
      title: '取消订单',
      content: '确定要取消该订单吗？取消后不可恢复。',
      success: async (res) => {
        if (!res.confirm) {
          return;
        }
        const { code } = await OrderApi.cancelOrder(item.id, '会员主动取消');
        if (code === 0) {
          loadList(true);
        }
      },
    });
  };

  const goDetail = (id) => {
    uni.navigateTo({
      url: `/pages/pharmacy/order-detail?id=${id}`,
    });
  };

  const goLogin = () => {
    uni.navigateTo({
      url: '/pages/pharmacy/login',
    });
  };

  onShow(() => {
    loadList(true);
  });

  onReachBottom(() => {
    loadList(false);
  });
</script>

<style lang="scss" scoped>
  .pharmacy-order {
    min-height: 100vh;
    padding-bottom: 40rpx;
    background: #f5f7f8;
  }

  .tabs {
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

  .order-list {
    padding: 16rpx 24rpx;
  }

  .order-card {
    padding: 24rpx;
    margin-bottom: 16rpx;
    background: #ffffff;
    border-radius: 12rpx;

    &__head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-bottom: 16rpx;
      border-bottom: 1rpx solid #f0f2f5;
    }

    &__no {
      font-size: 26rpx;
      color: #667085;
    }

    &__status {
      font-size: 26rpx;
      font-weight: 600;
      color: #176b5b;
    }

    &__body {
      padding: 16rpx 0;
    }

    &__row {
      display: flex;
      padding: 6rpx 0;
      font-size: 26rpx;
    }

    &__label {
      width: 160rpx;
      color: #98a2b3;
    }

    &__value {
      flex: 1;
      color: #1f2933;
    }

    &__amount {
      flex: 1;
      color: #ef4444;
      font-weight: 600;
    }

    &__code {
      flex: 1;
      color: #176b5b;
      font-weight: 700;
      letter-spacing: 4rpx;
    }

    &__ops {
      display: flex;
      justify-content: flex-end;
      padding-top: 16rpx;
      border-top: 1rpx solid #f0f2f5;
    }

    &__btn {
      margin-left: 16rpx;
      padding: 10rpx 28rpx;
      font-size: 26rpx;
      color: #1f2933;
      border: 1rpx solid #e4e7ec;
      border-radius: 32rpx;

      &--primary {
        color: #ffffff;
        background: #176b5b;
        border-color: #176b5b;
      }
    }
  }

  .empty {
    padding: 160rpx 0;
    text-align: center;

    &__text {
      font-size: 28rpx;
      color: #98a2b3;
    }

    &__btn {
      display: inline-block;
      margin-top: 32rpx;
      padding: 16rpx 56rpx;
      font-size: 28rpx;
      color: #ffffff;
      background: #176b5b;
      border-radius: 40rpx;
    }
  }
</style>
