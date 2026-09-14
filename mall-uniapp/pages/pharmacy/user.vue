<!-- 药店小程序 - 会员中心（真实接口：F 会员资料 + 积分） -->
<template>
  <view class="pharmacy-user">
    <!-- 未登录 -->
    <view v-if="!isLogin" class="guest">
      <view class="guest__avatar" />
      <view class="guest__text">登录后查看会员信息</view>
      <view class="guest__btn" @tap="goLogin">立即登录</view>
    </view>

    <!-- 已登录 -->
    <template v-else>
      <view class="profile">
        <view class="profile__avatar" />
        <view class="profile__info">
          <view class="profile__nickname">{{ userInfo.nickname || '会员' }}</view>
          <view class="profile__mobile">{{ userInfo.mobile || '—' }}</view>
        </view>
      </view>

      <view class="assets">
        <view class="assets__item">
          <view class="assets__value">{{ userInfo.point || 0 }}</view>
          <view class="assets__label">积分</view>
        </view>
        <view class="assets__item">
          <view class="assets__value">{{ userInfo.experience || 0 }}</view>
          <view class="assets__label">经验</view>
        </view>
        <view class="assets__item">
          <view class="assets__value">{{ userInfo.levelName || '普通会员' }}</view>
          <view class="assets__label">等级</view>
        </view>
      </view>

      <!-- 积分明细 -->
      <view class="panel">
        <view class="panel__title">积分明细</view>
        <view v-if="pointList.length > 0">
          <view v-for="item in pointList" :key="item.id" class="point">
            <view class="point__main">
              <view class="point__title">{{ item.title || '积分变动' }}</view>
              <view class="point__time">{{ formatTime(item.createTime) }}</view>
            </view>
            <view class="point__value" :class="{ 'is-minus': item.point < 0 }">
              {{ item.point > 0 ? '+' : '' }}{{ item.point }}
            </view>
          </view>
        </view>
        <view v-else class="panel__empty">暂无积分记录</view>
      </view>

      <!-- 菜单 -->
      <view class="menu">
        <view class="menu__item" @tap="goPage('/pages/pharmacy/order')">
          <text class="menu__label">我的订单</text>
          <text class="menu__arrow">›</text>
        </view>
        <view class="menu__item" @tap="goPage('/pages/pharmacy/address')">
          <text class="menu__label">收货地址</text>
          <text class="menu__arrow">›</text>
        </view>
        <view class="menu__item" @tap="handleLogout">
          <text class="menu__label">退出登录</text>
          <text class="menu__arrow">›</text>
        </view>
      </view>
    </template>

    <s-pharmacy-tabbar :current="3" />
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onShow } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import UserApi from '@/sheep/api/member/user';
  import PointApi from '@/sheep/api/member/point';
  import AuthUtil from '@/sheep/api/member/auth';

  const isLogin = computed(() => sheep.$store('user').isLogin);
  const userInfo = ref({});
  const pointList = ref([]);

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

  const loadUser = async () => {
    if (!isLogin.value) {
      userInfo.value = {};
      pointList.value = [];
      return;
    }
    const { code, data } = await UserApi.getUserInfo();
    if (code === 0) {
      userInfo.value = data || {};
    }
    const pointRes = await PointApi.getPointRecordPage({ pageNo: 1, pageSize: 10 });
    if (pointRes.code === 0) {
      pointList.value = pointRes.data.list || [];
    }
  };

  const goLogin = () => {
    uni.navigateTo({
      url: '/pages/pharmacy/login',
    });
  };

  const handleLogout = () => {
    uni.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: async (res) => {
        if (!res.confirm) {
          return;
        }
        await AuthUtil.logout();
        sheep.$store('user').resetUserData();
        uni.reLaunch({
          url: '/pages/pharmacy/index',
        });
      },
    });
  };

  const goPage = (url) => {
    uni.navigateTo({
      url,
    });
  };

  onShow(() => {
    loadUser();
  });
</script>

<style lang="scss" scoped>
  .pharmacy-user {
    min-height: 100vh;
    padding-bottom: 140rpx;
    background: #f5f7f8;
  }

  .guest {
    padding: 160rpx 0;
    text-align: center;

    &__avatar {
      width: 140rpx;
      height: 140rpx;
      margin: 0 auto;
      background: #e4e7ec;
      border-radius: 50%;
    }

    &__text {
      margin-top: 24rpx;
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

  .profile {
    display: flex;
    align-items: center;
    padding: 40rpx 24rpx;
    background: #176b5b;

    &__avatar {
      width: 120rpx;
      height: 120rpx;
      background: rgba(255, 255, 255, 0.25);
      border-radius: 50%;
    }

    &__info {
      margin-left: 24rpx;
    }

    &__nickname {
      font-size: 34rpx;
      font-weight: 600;
      color: #ffffff;
    }

    &__mobile {
      margin-top: 8rpx;
      font-size: 26rpx;
      color: rgba(255, 255, 255, 0.85);
    }
  }

  .assets {
    display: flex;
    margin: -20rpx 24rpx 0;
    padding: 24rpx 0;
    background: #ffffff;
    border-radius: 12rpx;

    &__item {
      flex: 1;
      text-align: center;
    }

    &__value {
      font-size: 32rpx;
      font-weight: 600;
      color: #1f2933;
    }

    &__label {
      margin-top: 6rpx;
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

    &__empty {
      padding: 40rpx 0;
      text-align: center;
      font-size: 26rpx;
      color: #98a2b3;
    }
  }

  .point {
    display: flex;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f0f2f5;

    &__main {
      flex: 1;
    }

    &__title {
      font-size: 27rpx;
      color: #1f2933;
    }

    &__time {
      margin-top: 6rpx;
      font-size: 23rpx;
      color: #98a2b3;
    }

    &__value {
      font-size: 30rpx;
      font-weight: 600;
      color: #176b5b;

      &.is-minus {
        color: #ef4444;
      }
    }
  }

  .menu {
    margin: 16rpx 24rpx 0;
    background: #ffffff;
    border-radius: 12rpx;

    &__item {
      display: flex;
      align-items: center;
      padding: 28rpx 24rpx;
      border-bottom: 1rpx solid #f0f2f5;
    }

    &__label {
      flex: 1;
      font-size: 28rpx;
      color: #1f2933;
    }

    &__arrow {
      font-size: 32rpx;
      color: #98a2b3;
    }
  }
</style>
