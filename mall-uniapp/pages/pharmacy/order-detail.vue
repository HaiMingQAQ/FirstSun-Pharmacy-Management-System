<!-- 药店小程序 - 订单详情（真实接口：F 线上订单，含取货码） -->
<template>
  <view class="pharmacy-order-detail">
    <template v-if="order">
      <!-- 状态 -->
      <view class="status">
        <view class="status__text">{{ statusText(order.status) }}</view>
        <view v-if="order.status === 3" class="status__tip">请到店出示取货码完成核销</view>
        <view v-else-if="order.status === 0" class="status__tip">
          请在 {{ formatTime(order.expireAt) }} 前完成支付
        </view>
      </view>

      <!-- 取货码 -->
      <view v-if="order.pickupCode && order.status === 3" class="pickup">
        <view class="pickup__label">取货码</view>
        <view class="pickup__code">{{ order.pickupCode }}</view>
        <view class="pickup__store">履约门店编号：{{ order.storeId }}</view>
      </view>

      <!-- 商品明细 -->
      <view class="panel">
        <view class="panel__title">商品明细</view>
        <view v-for="line in order.lines || []" :key="line.id" class="line">
          <view class="line__main">
            <view class="line__name">{{ line.drugName || '商品' }}</view>
            <view class="line__spec">{{ line.specification || '—' }} × {{ line.qty }}</view>
          </view>
          <view class="line__amount">￥{{ line.lineAmount }}</view>
        </view>
      </view>

      <!-- 金额 -->
      <view class="panel">
        <view class="panel__title">金额信息</view>
        <view class="row">
          <text class="row__label">商品金额</text>
          <text class="row__value">￥{{ order.goodsAmount }}</text>
        </view>
        <view class="row">
          <text class="row__label">配送费</text>
          <text class="row__value">￥{{ order.freightAmount }}</text>
        </view>
        <view class="row">
          <text class="row__label">优惠</text>
          <text class="row__value">￥{{ order.discountAmount }}</text>
        </view>
        <view class="row row--total">
          <text class="row__label">应付金额</text>
          <text class="row__amount">￥{{ order.payableAmount }}</text>
        </view>
      </view>

      <!-- 订单信息 -->
      <view class="panel">
        <view class="panel__title">订单信息</view>
        <view class="row">
          <text class="row__label">订单号</text>
          <text class="row__value">{{ order.orderNo }}</text>
        </view>
        <view class="row">
          <text class="row__label">订单类型</text>
          <text class="row__value">{{ order.orderType === 1 ? '同城配送' : '到店自提' }}</text>
        </view>
        <view class="row">
          <text class="row__label">支付状态</text>
          <text class="row__value">{{ payStatusText(order.payStatus) }}</text>
        </view>
        <view v-if="order.addressSnapshot" class="row">
          <text class="row__label">收货地址</text>
          <text class="row__value">{{ order.addressSnapshot }}</text>
        </view>
        <view v-if="order.remark" class="row">
          <text class="row__label">备注</text>
          <text class="row__value">{{ order.remark }}</text>
        </view>
        <view class="row">
          <text class="row__label">下单时间</text>
          <text class="row__value">{{ formatTime(order.createTime) }}</text>
        </view>
        <view v-if="order.status === -1 && order.cancelReason" class="row">
          <text class="row__label">取消原因</text>
          <text class="row__value">{{ order.cancelReason }}</text>
        </view>
      </view>

      <!-- 操作 -->
      <view v-if="canCancel" class="footer">
        <view class="footer__btn" @tap="handleCancel">取消订单</view>
      </view>
    </template>

    <view v-else class="empty">{{ loading ? '加载中…' : '订单不存在' }}</view>
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import OrderApi from '@/sheep/api/pharmacy/order';

  const order = ref(null);
  const loading = ref(true);

  const STATUS_MAP = {
    '-1': '已取消',
    0: '待支付',
    1: '待拣货',
    2: '拣货中',
    3: '待自提',
    4: '已完成',
  };

  const PAY_STATUS_MAP = {
    0: '待支付',
    1: '已支付',
    2: '已退款',
  };

  const statusText = (status) => STATUS_MAP[status] || '未知';

  const payStatusText = (payStatus) => PAY_STATUS_MAP[payStatus] || '未知';

  const canCancel = computed(() => order.value && (order.value.status === 0 || order.value.status === 1));

  const formatTime = (value) => {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    const pad = (num) => (num < 10 ? `0${num}` : `${num}`);
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
      date.getHours(),
    )}:${pad(date.getMinutes())}`;
  };

  const loadOrder = async (id) => {
    loading.value = true;
    const { code, data } = await OrderApi.getOrder(id);
    loading.value = false;
    if (code === 0) {
      order.value = data;
    }
  };

  const handleCancel = () => {
    uni.showModal({
      title: '取消订单',
      content: '确定要取消该订单吗？取消后不可恢复。',
      success: async (res) => {
        if (!res.confirm) {
          return;
        }
        const { code } = await OrderApi.cancelOrder(order.value.id, '会员主动取消');
        if (code === 0) {
          loadOrder(order.value.id);
        }
      },
    });
  };

  onLoad((options) => {
    if (options.id) {
      loadOrder(options.id);
    } else {
      loading.value = false;
    }
  });
</script>

<style lang="scss" scoped>
  .pharmacy-order-detail {
    min-height: 100vh;
    padding-bottom: 160rpx;
    background: #f5f7f8;
  }

  .status {
    padding: 40rpx 24rpx;
    background: #176b5b;

    &__text {
      font-size: 36rpx;
      font-weight: 700;
      color: #ffffff;
    }

    &__tip {
      margin-top: 12rpx;
      font-size: 26rpx;
      color: rgba(255, 255, 255, 0.85);
    }
  }

  .pickup {
    margin: 16rpx 24rpx 0;
    padding: 32rpx 24rpx;
    text-align: center;
    background: #ffffff;
    border-radius: 12rpx;

    &__label {
      font-size: 26rpx;
      color: #98a2b3;
    }

    &__code {
      margin-top: 12rpx;
      font-size: 60rpx;
      font-weight: 700;
      letter-spacing: 8rpx;
      color: #176b5b;
    }

    &__store {
      margin-top: 12rpx;
      font-size: 24rpx;
      color: #98a2b3;
    }
  }

  .panel {
    margin: 16rpx 24rpx 0;
    padding: 24rpx;
    background: #ffffff;
    border-radius: 12rpx;

    &__title {
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2933;
      padding-bottom: 16rpx;
      border-bottom: 1rpx solid #f0f2f5;
    }
  }

  .line {
    display: flex;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f0f2f5;

    &__main {
      flex: 1;
    }

    &__name {
      font-size: 28rpx;
      color: #1f2933;
    }

    &__spec {
      margin-top: 6rpx;
      font-size: 24rpx;
      color: #98a2b3;
    }

    &__amount {
      font-size: 28rpx;
      color: #1f2933;
    }
  }

  .row {
    display: flex;
    padding: 14rpx 0;
    font-size: 26rpx;

    &__label {
      width: 200rpx;
      color: #98a2b3;
    }

    &__value {
      flex: 1;
      color: #1f2933;
    }

    &__amount {
      flex: 1;
      font-size: 32rpx;
      font-weight: 700;
      color: #ef4444;
    }

    &--total {
      padding-top: 20rpx;
      border-top: 1rpx solid #f0f2f5;
    }
  }

  .footer {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    padding: 16rpx 24rpx;
    padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
    background: #ffffff;
    border-top: 1rpx solid #e4e7ec;

    &__btn {
      height: 80rpx;
      line-height: 80rpx;
      text-align: center;
      font-size: 30rpx;
      color: #ef4444;
      border: 1rpx solid #ef4444;
      border-radius: 40rpx;
    }
  }

  .empty {
    padding: 160rpx 0;
    text-align: center;
    font-size: 28rpx;
    color: #98a2b3;
  }
</style>
