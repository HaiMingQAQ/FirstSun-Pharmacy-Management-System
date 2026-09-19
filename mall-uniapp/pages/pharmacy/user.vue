<template>
  <s-pharmacy-page :tab="3">
    <view class="member-head">
      <view class="fs-row">
        <view class="avatar"><uni-icons type="person" size="30" color="#176b5b" /></view>
        <view class="fs-grow">
          <view class="fs-heading">{{ profile ? profile.name : '欢迎来到 FirstSun' }}</view>
          <view class="fs-muted">{{ profile ? maskedMobile : '登录，开启便捷购药体验' }}</view>
        </view>
      </view>
      <view v-if="profile" class="membership">
        <text>{{ profile.level }}</text>
        <text>
          当前积分
          <text class="points">{{ profile.points }}</text>
        </text>
      </view>
      <button v-else class="fs-primary fs-gap" @tap="go('login')">登录 / 注册</button>
    </view>
    <s-pharmacy-state v-if="loading || error" :loading="loading" :error="error" @retry="load" />
    <view class="fs-section">
      <view class="fs-between">
        <text class="fs-title">我的订单</text>
        <button class="fs-text-btn" @tap="open('order')">
          全部订单
          <uni-icons type="right" size="13" color="#176b5b" />
        </button>
      </view>
      <view class="order-shortcuts">
        <button
          v-for="item in shortcuts"
          :key="item.status"
          @tap="open('order?status=' + item.status)"
        >
          <uni-icons :type="item.icon" size="26" color="#526b5b" />
          <text>{{ item.name }}</text>
        </button>
      </view>
    </view>
    <view class="fs-section">
      <view class="fs-menu" @tap="open('address')">
        <view class="fs-row">
          <uni-icons type="location" size="22" color="#526b5b" />
          <text>收货地址</text>
        </view>
        <uni-icons type="right" size="16" color="#8b968f" />
      </view>
      <view class="fs-menu" @tap="open('prescription-upload')">
        <view class="fs-row">
          <uni-icons type="paperclip" size="22" color="#526b5b" />
          <text>我的处方</text>
        </view>
        <uni-icons type="right" size="16" color="#8b968f" />
      </view>
      <view class="fs-menu" @tap="contact">
        <view class="fs-row">
          <uni-icons type="phone" size="22" color="#526b5b" />
          <text>联系门店</text>
        </view>
        <uni-icons type="right" size="16" color="#8b968f" />
      </view>
    </view>
    <view v-if="profile" class="fs-pad">
      <button class="fs-outline" @tap="logout">退出登录</button>
    </view>
    <view class="fs-footer">FirstSun 药店 · 测试体验版</view>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref, computed } from 'vue';
  import { onShow } from '@dcloudio/uni-app';
  import api from '@/sheep/api/pharmacy/client';
  import { go, requireLogin, confirm, useRequest } from './usePharmacy';
  const profile = ref(null);
  const { loading, error, run } = useRequest();
  const maskedMobile = computed(() =>
    profile.value?.mobile.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2'),
  );
  const shortcuts = [
    { status: 'unpaid', name: '待支付', icon: 'wallet' },
    { status: 'review', name: '待审核', icon: 'help' },
    { status: 'ready', name: '待收取', icon: 'shop' },
    { status: 'completed', name: '已完成', icon: 'checkbox' },
  ];
  const load = () =>
    run(async () => {
      profile.value = await api.profile();
    });
  const open = (path) => {
    if (requireLogin()) go(path);
  };
  const contact = () => {
    if (api.store.phone) uni.makePhoneCall({ phoneNumber: api.store.phone });
    else
      uni.showModal({
        title: api.store.name,
        content: `${api.store.address}\n营业时间：${api.store.hours}\n测试门店暂无联系电话，正式接入后可一键拨打。`,
        showCancel: false,
        confirmColor: '#176b5b',
      });
  };
  const logout = async () => {
    if (await confirm('退出登录', '确定退出当前测试账户？')) {
      api.logout();
      profile.value = null;
      error.value = '';
    }
  };
  onShow(() => {
    if (api.session()) load();
    else profile.value = null;
  });
</script>
<style scoped>
  .member-head {
    padding: 28px 20px 24px;
    background: #fff;
  }
  .avatar {
    width: 58px;
    height: 58px;
    border-radius: 8px;
    background: #eaf2ed;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  .membership {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid #e5ebe8;
    margin-top: 24px;
    padding-top: 16px;
    color: #526b5b;
    font-size: 14px;
  }
  .points {
    font-size: 24px;
    font-weight: 600;
    margin-left: 8px;
    color: #176b5b;
  }
  .order-shortcuts {
    display: flex;
    margin-top: 12px;
  }
  .order-shortcuts button {
    flex: 1;
    padding: 8px 0;
    background: #fff;
    display: flex;
    flex-direction: column;
    font-size: 13px;
    gap: 10px;
  }
</style>
