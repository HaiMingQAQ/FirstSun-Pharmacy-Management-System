<!-- 药店小程序 - 购物车（真实接口：F 购物车 + 下单） -->
<template>
  <view class="pharmacy-cart">
    <view v-if="!isLogin" class="empty">
      <view class="empty__text">登录后查看购物车</view>
      <view class="empty__btn" @tap="goLogin">去登录</view>
    </view>

    <template v-else>
      <view v-if="list.length > 0" class="cart-list">
        <view v-for="item in list" :key="item.id" class="cart-item">
          <view
            class="cart-item__check"
            :class="{ 'is-checked': item.selectedFlag === 1 }"
            @tap="toggleSelected(item)"
          />
          <view class="cart-item__main" @tap="goDetail(item.drugId)">
            <view class="cart-item__name">
              {{ item.drugName || '商品' }}
              <text v-if="item.isRx === 1" class="cart-item__rx">处方药</text>
            </view>
            <view class="cart-item__spec">{{ item.specification || '—' }}</view>
            <view class="cart-item__price">￥{{ itemPrice(item) }}</view>
          </view>
          <view class="cart-item__ops">
            <view class="cart-item__qty">
              <view class="cart-item__qty-btn" @tap.stop="changeQty(item, -1)">-</view>
              <text class="cart-item__qty-num">{{ item.qty }}</text>
              <view class="cart-item__qty-btn" @tap.stop="changeQty(item, 1)">+</view>
            </view>
            <view class="cart-item__del" @tap.stop="handleDelete(item)">删除</view>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <view class="empty__text">购物车还是空的</view>
        <view class="empty__btn" @tap="goHome">去逛逛</view>
      </view>

      <view v-if="list.length > 0" class="footer">
        <view class="footer__total">
          已选 <text class="footer__count">{{ selectedCount }}</text> 件 合计
          <text class="footer__amount">￥{{ selectedAmount }}</text>
        </view>
        <view class="footer__submit" @tap="handleCheckout">去结算</view>
      </view>
    </template>

    <s-pharmacy-tabbar :current="2" />
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onShow } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import CartApi from '@/sheep/api/pharmacy/cart';
  import OrderApi from '@/sheep/api/pharmacy/order';
  import AddressApi from '@/sheep/api/member/address';

  const list = ref([]);
  const isLogin = computed(() => sheep.$store('user').isLogin);

  const itemPrice = (item) => {
    const price = item.memberPrice !== null && item.memberPrice !== undefined
      ? item.memberPrice
      : item.retailPrice;
    return price === null || price === undefined ? '—' : price;
  };

  const selectedList = computed(() => list.value.filter((item) => item.selectedFlag === 1));

  const selectedCount = computed(() =>
    selectedList.value.reduce((sum, item) => sum + (item.qty || 0), 0),
  );

  const selectedAmount = computed(() => {
    const total = selectedList.value.reduce((sum, item) => {
      const price = item.memberPrice !== null && item.memberPrice !== undefined
        ? item.memberPrice
        : item.retailPrice;
      return sum + Number(price || 0) * Number(item.qty || 0);
    }, 0);
    return total.toFixed(2);
  });

  const loadCart = async () => {
    if (!isLogin.value) {
      list.value = [];
      return;
    }
    const { code, data } = await CartApi.getCartList();
    if (code === 0) {
      list.value = data || [];
    }
  };

  const toggleSelected = async (item) => {
    const next = item.selectedFlag === 1 ? 0 : 1;
    const { code } = await CartApi.updateCartSelected(item.id, next);
    if (code === 0) {
      item.selectedFlag = next;
    }
  };

  const changeQty = async (item, delta) => {
    const next = (item.qty || 1) + delta;
    if (next < 1) {
      return;
    }
    const { code } = await CartApi.updateCartQty(item.id, next);
    if (code === 0) {
      item.qty = next;
    }
  };

  const handleDelete = (item) => {
    uni.showModal({
      title: '提示',
      content: '确定要从购物车删除该商品吗？',
      success: async (res) => {
        if (!res.confirm) {
          return;
        }
        const { code } = await CartApi.deleteCart(item.id);
        if (code === 0) {
          loadCart();
        }
      },
    });
  };

  const handleCheckout = () => {
    if (selectedList.value.length === 0) {
      uni.showToast({
        title: '请先勾选要结算的商品',
        icon: 'none',
      });
      return;
    }
    const storeId = uni.getStorageSync('pharmacy-store-id');
    if (!storeId) {
      uni.showToast({
        title: '请先在首页选择履约门店',
        icon: 'none',
      });
      return;
    }
    uni.showActionSheet({
      itemList: ['到店自提', '同城配送'],
      success: async (res) => {
        if (res.tapIndex === 0) {
          await submitOrder(storeId, 0, null);
        } else {
          await submitDeliveryOrder(storeId);
        }
      },
    });
  };

  const submitOrder = async (storeId, orderType, addressId) => {
    const { code, data, msg } = await OrderApi.createOrder({
      storeId,
      orderType,
      addressId,
      remark: '',
    });
    if (code !== 0) {
      return;
    }
    uni.showModal({
      title: '下单成功',
      content: `订单号：${data}，可在「我的-我的订单」查看`,
      showCancel: false,
    });
    loadCart();
  };

  const submitDeliveryOrder = async (storeId) => {
    const { code, data } = await AddressApi.getDefaultAddress();
    if (code !== 0 || !data) {
      uni.showToast({
        title: '请先在会员中心添加默认收货地址',
        icon: 'none',
      });
      return;
    }
    await submitOrder(storeId, 1, data.id);
  };

  const goLogin = () => {
    uni.navigateTo({
      url: '/pages/pharmacy/login',
    });
  };

  const goHome = () => {
    uni.reLaunch({
      url: '/pages/pharmacy/index',
    });
  };

  const goDetail = (drugId) => {
    uni.navigateTo({
      url: `/pages/pharmacy/detail?id=${drugId}`,
    });
  };

  onShow(() => {
    loadCart();
  });
</script>

<style lang="scss" scoped>
  .pharmacy-cart {
    min-height: 100vh;
    padding-bottom: 200rpx;
    background: #f5f7f8;
  }

  .cart-list {
    padding: 16rpx 24rpx;
  }

  .cart-item {
    display: flex;
    align-items: center;
    padding: 24rpx;
    margin-bottom: 16rpx;
    background: #ffffff;
    border-radius: 12rpx;

    &__check {
      width: 40rpx;
      height: 40rpx;
      margin-right: 20rpx;
      border: 2rpx solid #e4e7ec;
      border-radius: 50%;
      box-sizing: border-box;

      &.is-checked {
        background: #176b5b;
        border-color: #176b5b;
      }
    }

    &__main {
      flex: 1;
    }

    &__name {
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2933;
    }

    &__rx {
      margin-left: 12rpx;
      padding: 2rpx 10rpx;
      font-size: 20rpx;
      font-weight: 400;
      color: #ef4444;
      border: 1rpx solid #ef4444;
      border-radius: 6rpx;
    }

    &__spec {
      margin-top: 6rpx;
      font-size: 24rpx;
      color: #667085;
    }

    &__price {
      margin-top: 8rpx;
      font-size: 28rpx;
      color: #ef4444;
      font-weight: 600;
    }

    &__ops {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
    }

    &__qty {
      display: flex;
      align-items: center;
    }

    &__qty-btn {
      width: 52rpx;
      height: 52rpx;
      line-height: 48rpx;
      text-align: center;
      font-size: 28rpx;
      color: #1f2933;
      border: 1rpx solid #e4e7ec;
      border-radius: 8rpx;
    }

    &__qty-num {
      width: 64rpx;
      text-align: center;
      font-size: 28rpx;
      color: #1f2933;
    }

    &__del {
      margin-top: 16rpx;
      font-size: 24rpx;
      color: #98a2b3;
    }
  }

  .footer {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 100rpx;
    display: flex;
    align-items: center;
    height: 110rpx;
    padding: 0 24rpx;
    background: #ffffff;
    border-top: 1rpx solid #e4e7ec;

    &__total {
      flex: 1;
      font-size: 26rpx;
      color: #667085;
    }

    &__count {
      color: #176b5b;
      font-weight: 600;
    }

    &__amount {
      font-size: 32rpx;
      color: #ef4444;
      font-weight: 600;
    }

    &__submit {
      padding: 0 48rpx;
      height: 80rpx;
      line-height: 80rpx;
      font-size: 30rpx;
      color: #ffffff;
      background: #176b5b;
      border-radius: 40rpx;
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
