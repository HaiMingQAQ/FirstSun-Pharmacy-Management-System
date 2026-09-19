<template>
  <s-pharmacy-page>
    <view class="login-head">
      <uni-icons type="shop" size="42" color="#176b5b" />
      <view class="fs-heading fs-gap">欢迎来到 FirstSun 药店</view>
      <view class="fs-muted fs-gap">登录后选购药品、查看订单与会员积分</view>
    </view>
    <view class="fs-section">
      <view class="fs-notice">
        当前为测试环境，不发送短信、不产生真实消费。请使用测试手机号，避免填写个人敏感信息。
      </view>
      <view class="fs-field">
        <text class="fs-label">手机号</text>
        <input
          v-model="mobile"
          class="fs-input"
          type="number"
          maxlength="11"
          placeholder="请输入 11 位测试手机号"
        />
        <text v-if="mobile && !validMobile" class="fs-small fs-danger">请输入正确的手机号格式</text>
      </view>
      <view class="fs-field">
        <text class="fs-label">测试验证码</text>
        <view class="fs-row">
          <input
            v-model="code"
            class="fs-input fs-grow"
            type="number"
            maxlength="6"
            placeholder="6 位测试验证码"
          />
          <button class="fs-secondary" :disabled="!validMobile" @tap="fillCode">获取测试码</button>
        </view>
        <view class="fs-muted fs-gap">点击“获取测试码”自动填入，仅用于本地体验</view>
      </view>
      <view class="agreement fs-gap">
        <button class="fs-check" aria-label="同意测试用户协议" @tap="agreed = !agreed">
          <uni-icons :type="agreed ? 'checkbox-filled' : 'circle'" size="22" color="#176b5b" />
        </button>
        <view class="fs-small">
          我已阅读并同意
          <text class="link" @tap="showTerms">《测试用户协议与隐私说明》</text>
        </view>
      </view>
      <button
        class="fs-primary fs-gap"
        :disabled="busy || !agreed || !validMobile || code.length !== 6"
        :loading="busy"
        @tap="login"
      >
        {{ busy ? '登录中…' : '登录 / 注册测试账户' }}
      </button>
    </view>
    <view class="fs-footer">无需微信授权 · 不接入真实短信服务</view>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref, computed } from 'vue';
  import api from '@/sheep/api/pharmacy/client';
  import { TEST_CODE } from '@/sheep/api/pharmacy/config';
  import { toast, useAction } from './usePharmacy';
  const mobile = ref(''),
    code = ref(''),
    agreed = ref(false);
  const { busy, act } = useAction();
  const validMobile = computed(() => /^1[3-9]\d{9}$/.test(mobile.value));
  const fillCode = () => {
    code.value = TEST_CODE;
    toast('测试验证码已填入，不发送短信');
  };
  const showTerms = () =>
    uni.showModal({
      title: '测试用户协议与隐私说明',
      content:
        '本版本仅用于功能体验，不提供真实购药服务。测试手机号、地址和订单保存在当前设备，可通过清除应用数据删除。请勿上传真实处方或填写真实个人信息。处方图片仅在当前会话用于模拟审核，不上传服务器。正式服务协议将在上线前提供。',
      showCancel: false,
      confirmColor: '#176b5b',
    });
  const login = () =>
    act(async () => {
      if (!agreed.value) return;
      await api.login(mobile.value, code.value);
      toast('登录成功');
      if (getCurrentPages().length > 1) uni.navigateBack();
      else uni.reLaunch({ url: '/pages/pharmacy/user' });
    });
</script>
<style scoped>
  .login-head {
    padding: 40px 24px 16px;
  }
  .agreement {
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .link {
    color: #176b5b;
  }
</style>
