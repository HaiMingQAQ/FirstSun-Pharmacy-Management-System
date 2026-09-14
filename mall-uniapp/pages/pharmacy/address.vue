<!-- 药店小程序 - 收货地址管理（真实接口：F 会员地址 CRUD） -->
<template>
  <view class="pharmacy-address">
    <view v-if="!isLogin" class="empty">
      <view class="empty__text">登录后管理收货地址</view>
      <view class="empty__btn" @tap="goLogin">去登录</view>
    </view>

    <template v-else>
      <!-- 地址列表 -->
      <view v-if="list.length > 0" class="addr-list">
        <view v-for="item in list" :key="item.id" class="addr">
          <view class="addr__main">
            <view class="addr__head">
              <text class="addr__name">{{ item.name }}</text>
              <text class="addr__mobile">{{ item.mobile }}</text>
              <text v-if="item.defaultStatus" class="addr__badge">默认</text>
            </view>
            <view class="addr__detail">{{ item.detailAddress }}</view>
          </view>
          <view class="addr__ops">
            <view class="addr__op" @tap="handleEdit(item)">编辑</view>
            <view v-if="!item.defaultStatus" class="addr__op" @tap="handleSetDefault(item)">
              设为默认
            </view>
            <view class="addr__op addr__op--danger" @tap="handleDelete(item)">删除</view>
          </view>
        </view>
      </view>

      <view v-else-if="!loading" class="empty">
        <view class="empty__text">还没有收货地址</view>
      </view>

      <view class="footer">
        <view class="footer__btn" @tap="handleAdd">新增收货地址</view>
      </view>

      <!-- 新增/编辑表单 -->
      <view v-if="showForm" class="form-mask" @tap="closeForm">
        <view class="form" @tap.stop>
          <view class="form__title">{{ form.id ? '编辑收货地址' : '新增收货地址' }}</view>
          <view class="form__field">
            <text class="form__label">收件人</text>
            <input v-model="form.name" class="form__input" placeholder="请输入收件人名称" maxlength="10" />
          </view>
          <view class="form__field">
            <text class="form__label">手机号</text>
            <input v-model="form.mobile" class="form__input" type="number" maxlength="11" placeholder="请输入手机号" />
          </view>
          <view class="form__field">
            <text class="form__label">地区编码</text>
            <input v-model="form.areaId" class="form__input" type="number" placeholder="如 110101" />
          </view>
          <view class="form__field">
            <text class="form__label">详细地址</text>
            <input v-model="form.detailAddress" class="form__input" placeholder="请输入详细地址" maxlength="250" />
          </view>
          <view class="form__field form__field--switch">
            <text class="form__label">设为默认</text>
            <switch :checked="form.defaultStatus" color="#176b5b" @change="handleDefaultChange" />
          </view>
          <view class="form__actions">
            <view class="form__btn" @tap="closeForm">取消</view>
            <view class="form__btn form__btn--primary" @tap="handleSubmit">保存</view>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { onShow } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import AddressApi from '@/sheep/api/member/address';

  const isLogin = computed(() => sheep.$store('user').isLogin);
  const list = ref([]);
  const loading = ref(false);
  const showForm = ref(false);
  const form = ref(createDefaultForm());

  function createDefaultForm() {
    return {
      id: null,
      name: '',
      mobile: '',
      areaId: '',
      detailAddress: '',
      defaultStatus: false,
    };
  }

  const loadList = async () => {
    if (!isLogin.value) {
      list.value = [];
      return;
    }
    loading.value = true;
    const { code, data } = await AddressApi.getAddressList();
    loading.value = false;
    if (code === 0) {
      list.value = data || [];
    }
  };

  const handleAdd = () => {
    form.value = createDefaultForm();
    showForm.value = true;
  };

  const handleEdit = (item) => {
    form.value = {
      id: item.id,
      name: item.name,
      mobile: item.mobile,
      areaId: item.areaId,
      detailAddress: item.detailAddress,
      defaultStatus: !!item.defaultStatus,
    };
    showForm.value = true;
  };

  const closeForm = () => {
    showForm.value = false;
  };

  const handleDefaultChange = (event) => {
    form.value.defaultStatus = event.detail.value;
  };

  const validate = () => {
    if (!form.value.name) {
      uni.showToast({ title: '请输入收件人名称', icon: 'none' });
      return false;
    }
    if (!/^\d{11}$/.test(form.value.mobile)) {
      uni.showToast({ title: '请输入正确的 11 位手机号', icon: 'none' });
      return false;
    }
    if (!form.value.areaId) {
      uni.showToast({ title: '请输入地区编码', icon: 'none' });
      return false;
    }
    if (!form.value.detailAddress) {
      uni.showToast({ title: '请输入详细地址', icon: 'none' });
      return false;
    }
    return true;
  };

  const handleSubmit = async () => {
    if (!validate()) {
      return;
    }
    const payload = {
      name: form.value.name,
      mobile: form.value.mobile,
      areaId: Number(form.value.areaId),
      detailAddress: form.value.detailAddress,
      defaultStatus: form.value.defaultStatus,
    };
    const res = form.value.id
      ? await AddressApi.updateAddress({ ...payload, id: form.value.id })
      : await AddressApi.createAddress(payload);
    if (res.code === 0) {
      showForm.value = false;
      loadList();
    }
  };

  const handleSetDefault = async (item) => {
    const { code } = await AddressApi.updateAddress({
      id: item.id,
      name: item.name,
      mobile: item.mobile,
      areaId: item.areaId,
      detailAddress: item.detailAddress,
      defaultStatus: true,
    });
    if (code === 0) {
      loadList();
    }
  };

  const handleDelete = (item) => {
    uni.showModal({
      title: '删除地址',
      content: '确定要删除该收货地址吗？',
      success: async (res) => {
        if (!res.confirm) {
          return;
        }
        const { code } = await AddressApi.deleteAddress(item.id);
        if (code === 0) {
          loadList();
        }
      },
    });
  };

  const goLogin = () => {
    uni.navigateTo({
      url: '/pages/pharmacy/login',
    });
  };

  onShow(() => {
    loadList();
  });
</script>

<style lang="scss" scoped>
  .pharmacy-address {
    min-height: 100vh;
    padding-bottom: 160rpx;
    background: #f5f7f8;
  }

  .addr-list {
    padding: 16rpx 24rpx;
  }

  .addr {
    padding: 24rpx;
    margin-bottom: 16rpx;
    background: #ffffff;
    border-radius: 12rpx;

    &__head {
      display: flex;
      align-items: center;
    }

    &__name {
      font-size: 30rpx;
      font-weight: 600;
      color: #1f2933;
    }

    &__mobile {
      margin-left: 16rpx;
      font-size: 26rpx;
      color: #667085;
    }

    &__badge {
      margin-left: 16rpx;
      padding: 2rpx 12rpx;
      font-size: 20rpx;
      color: #176b5b;
      border: 1rpx solid #176b5b;
      border-radius: 6rpx;
    }

    &__detail {
      margin-top: 10rpx;
      font-size: 26rpx;
      color: #667085;
      line-height: 1.6;
    }

    &__ops {
      display: flex;
      justify-content: flex-end;
      margin-top: 16rpx;
      padding-top: 16rpx;
      border-top: 1rpx solid #f0f2f5;
    }

    &__op {
      margin-left: 28rpx;
      font-size: 26rpx;
      color: #176b5b;

      &--danger {
        color: #ef4444;
      }
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
      color: #ffffff;
      background: #176b5b;
      border-radius: 40rpx;
    }
  }

  .form-mask {
    position: fixed;
    left: 0;
    right: 0;
    top: 0;
    bottom: 0;
    z-index: 200;
    background: rgba(0, 0, 0, 0.45);
  }

  .form {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
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

    &__field {
      display: flex;
      align-items: center;
      padding: 16rpx 0;
      border-bottom: 1rpx solid #f0f2f5;

      &--switch {
        justify-content: space-between;
      }
    }

    &__label {
      width: 180rpx;
      font-size: 27rpx;
      color: #667085;
    }

    &__input {
      flex: 1;
      font-size: 27rpx;
      color: #1f2933;
    }

    &__actions {
      display: flex;
      margin-top: 32rpx;
    }

    &__btn {
      flex: 1;
      height: 80rpx;
      line-height: 80rpx;
      text-align: center;
      font-size: 30rpx;
      color: #1f2933;
      border: 1rpx solid #e4e7ec;
      border-radius: 40rpx;

      &--primary {
        margin-left: 24rpx;
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
