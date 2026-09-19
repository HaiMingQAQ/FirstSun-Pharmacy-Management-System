<!-- 药店小程序 - 会员登录（复用后端 /app-api/member/auth 登录） -->
<template>
  <view class="pharmacy-login">
    <view class="login-card">
      <view class="login-card__title">FirstSun 药店</view>
      <view class="login-card__subtitle">会员登录</view>

      <view class="login-card__tabs">
        <view
          class="login-card__tab"
          :class="{ 'is-active': mode === 'password' }"
          @tap="mode = 'password'"
        >
          密码登录
        </view>
        <view
          class="login-card__tab"
          :class="{ 'is-active': mode === 'quick' }"
          @tap="mode = 'quick'"
        >
          快捷登录
        </view>
      </view>

      <view class="login-card__field">
        <input
          v-model="mobile"
          class="login-card__input"
          type="number"
          maxlength="11"
          placeholder="请输入 11 位手机号"
        />
      </view>

      <view v-if="mode === 'password'" class="login-card__field">
        <input
          v-model="password"
          class="login-card__input"
          password
          placeholder="请输入密码"
        />
      </view>

      <!-- 快捷登录：验证码必填，由后端校验（手机号 + 验证码） -->
      <view v-if="mode === 'quick'" class="login-card__field login-card__code-row">
        <input
          v-model="code"
          class="login-card__input login-card__input--code"
          type="number"
          maxlength="8"
          placeholder="请输入验证码"
        />
        <view
          class="login-card__code-btn"
          :class="{ 'is-disabled': countdown > 0 }"
          @tap="handleSendCode"
        >
          {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
        </view>
      </view>

      <view class="login-card__submit" @tap="handleLogin">
        {{ mode === 'password' ? '登 录' : '登录 / 注册' }}
      </view>

      <view class="login-card__tip">
        {{ mode === 'password'
          ? '初始密码为空（管理员创建会员后请及时修改）'
          : '验证码登录：未注册的手机号校验通过后自动创建会员账号' }}
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue';
  import sheep from '@/sheep';
  import AuthUtil from '@/sheep/api/member/auth';

  const mode = ref('password');
  const mobile = ref('');
  const password = ref('');
  const code = ref('');
  const countdown = ref(0);
  let countdownTimer = null;

  const validateMobile = () => {
    if (!/^\d{11}$/.test(mobile.value)) {
      uni.showToast({
        title: '请输入正确的 11 位手机号',
        icon: 'none',
      });
      return false;
    }
    return true;
  };

  const validate = () => {
    if (!validateMobile()) {
      return false;
    }
    if (mode.value === 'password' && !password.value) {
      uni.showToast({
        title: '请输入密码',
        icon: 'none',
      });
      return false;
    }
    // 快捷登录必须填写验证码（后端同样强校验，前端只做提前提示）
    if (mode.value === 'quick' && !code.value) {
      uni.showToast({
        title: '请输入验证码',
        icon: 'none',
      });
      return false;
    }
    return true;
  };

  // 获取验证码：后端不返回验证码内容，本地开发使用 .env 中配置的 PHARMACY_DEV_SMS_CODE
  const handleSendCode = async () => {
    if (countdown.value > 0 || !validateMobile()) {
      return;
    }
    const res = await AuthUtil.sendSmsCode(mobile.value);
    if (res && res.code === 0) {
      countdown.value = 60;
      countdownTimer = setInterval(() => {
        countdown.value -= 1;
        if (countdown.value <= 0) {
          clearInterval(countdownTimer);
          countdownTimer = null;
        }
      }, 1000);
    }
  };

  const handleLogin = async () => {
    if (!validate()) {
      return;
    }
    const res = mode.value === 'password'
      ? await AuthUtil.login({
          mobile: mobile.value,
          password: password.value,
        })
      : await AuthUtil.loginOrRegister(mobile.value, code.value);
    if (res && res.code === 0) {
      // 登录成功后回到上一页或首页
      setTimeout(() => {
        const pages = getCurrentPages();
        if (pages.length > 1) {
          uni.navigateBack();
        } else {
          uni.reLaunch({
            url: '/pages/pharmacy/index',
          });
        }
      }, 600);
    }
  };
</script>

<style lang="scss" scoped>
  .pharmacy-login {
    min-height: 100vh;
    padding: 120rpx 48rpx 0;
    box-sizing: border-box;
    background: #f5f7f8;
  }

  .login-card {
    padding: 48rpx 32rpx;
    background: #ffffff;
    border-radius: 16rpx;

    &__title {
      font-size: 40rpx;
      font-weight: 700;
      color: #176b5b;
    }

    &__subtitle {
      margin-top: 8rpx;
      font-size: 26rpx;
      color: #98a2b3;
    }

    &__tabs {
      display: flex;
      margin: 40rpx 0 24rpx;
    }

    &__tab {
      margin-right: 40rpx;
      padding-bottom: 12rpx;
      font-size: 28rpx;
      color: #98a2b3;

      &.is-active {
        color: #176b5b;
        font-weight: 600;
        border-bottom: 4rpx solid #176b5b;
      }
    }

    &__field {
      margin-top: 24rpx;
    }

    &__input {
      height: 88rpx;
      padding: 0 24rpx;
      font-size: 28rpx;
      background: #f5f7f8;
      border-radius: 12rpx;

      &--code {
        flex: 1;
      }
    }

    &__code-row {
      display: flex;
      align-items: center;
    }

    &__code-btn {
      margin-left: 16rpx;
      padding: 0 24rpx;
      height: 88rpx;
      line-height: 88rpx;
      font-size: 26rpx;
      color: #176b5b;
      background: #e8f3f0;
      border-radius: 12rpx;
      white-space: nowrap;

      &.is-disabled {
        color: #98a2b3;
        background: #f0f1f2;
      }
    }

    &__submit {
      margin-top: 48rpx;
      height: 88rpx;
      line-height: 88rpx;
      text-align: center;
      font-size: 30rpx;
      color: #ffffff;
      background: #176b5b;
      border-radius: 44rpx;
    }

    &__tip {
      margin-top: 24rpx;
      font-size: 24rpx;
      color: #98a2b3;
      text-align: center;
    }
  }
</style>
