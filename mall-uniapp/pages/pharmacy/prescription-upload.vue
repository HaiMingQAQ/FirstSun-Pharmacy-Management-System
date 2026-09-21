<template>
  <s-pharmacy-page :dock="checkout">
    <s-pharmacy-state
      v-if="!loggedIn"
      title="登录后查看处方"
      action="去登录"
      @retry="go('login')"
    />
    <template v-else-if="checkout">
      <view class="fs-section">
        <view class="fs-title">上传处方图片</view>
        <view class="fs-muted fs-gap">
          请确保姓名、开方日期、药品名称和医师签名清晰可见。最多
          {{ MAX_PRESCRIPTIONS }} 张，每张不超过 10MB。
        </view>
        <view class="fs-notice fs-gap">
          隐私提示：处方图片将上传至服务器，仅用于药师审方，请使用测试图片，不要上传真实处方。
        </view>
        <view class="upload-grid fs-gap">
          <view v-for="(image, index) in images" :key="image" class="upload-item">
            <image :src="image" mode="aspectFill" @tap="preview(index)" />
            <button
              class="remove-image"
              :disabled="busy"
              :aria-label="'删除第' + (index + 1) + '张图片'"
              @tap="images.splice(index, 1)"
            >
              <uni-icons type="closeempty" size="18" color="#fff" />
            </button>
          </view>
          <button
            v-if="images.length < MAX_PRESCRIPTIONS"
            class="upload-add"
            :disabled="busy"
            @tap="choose"
          >
            <uni-icons type="camera" size="28" color="#176b5b" />
            <text>选择图片</text>
          </button>
        </view>
        <view class="fs-muted fs-gap">
          已选择 {{ images.length }}/{{ MAX_PRESCRIPTIONS }} 张 · 点击图片可预览
        </view>
      </view>
      <view class="fs-section">
        <view class="fs-title">审核流程</view>
        <view class="review-step">1. 上传处方，提交订单</view>
        <view class="review-step">2. 由药师核对处方信息</view>
        <view class="review-step">3. 审核通过后继续购买</view>
        <view class="fs-muted fs-gap">
          需上传处方并经药师审核。审核结果以药师实际处理为准，本功能不提供诊断建议。
        </view>
      </view>
      <view class="fs-dock">
        <button class="fs-primary fs-grow" :disabled="busy || !images.length" @tap="save">
          保存处方，返回订单
        </button>
      </view>
    </template>
    <template v-else>
      <view class="fs-pad fs-white">
        <view class="fs-title">我的处方</view>
        <view class="fs-muted">查看随订单提交的处方及审核状态</view>
      </view>
      <s-pharmacy-state
        v-if="loading || error || !prescriptions.length"
        :loading="loading"
        :error="error"
        title="暂无处方记录"
        description="购买处方药时，可在确认订单页上传测试处方"
        @retry="load"
      />
      <view
        v-else
        v-for="order in prescriptions"
        :key="order.id"
        class="fs-section"
        @tap="go('order-detail?id=' + order.id)"
      >
        <view class="fs-between">
          <text class="fs-title">{{ order.prescriptions.length }} 张处方</text>
          <text class="fs-tag fs-tag-rx">
            {{ order.status === 'cancelled' ? '审核已终止' : order.review }}
          </text>
        </view>
        <view class="fs-muted fs-gap">{{ order.id }}</view>
        <view class="fs-muted">{{ order.createdAt }}</view>
        <button class="fs-text-btn fs-gap">
          查看关联订单
          <uni-icons type="right" size="14" color="#176b5b" />
        </button>
      </view>
    </template>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref } from 'vue';
  import { onLoad, onShow } from '@dcloudio/uni-app';
  import api from '@/sheep/api/pharmacy/client';
  import { MAX_PRESCRIPTIONS } from '@/sheep/api/pharmacy/config';
  import { go, toast, useRequest, useAction } from './usePharmacy';
  const checkout = ref(false),
    loggedIn = ref(false),
    images = ref([]),
    prescriptions = ref([]);
  const { loading, error, run } = useRequest();
  const { busy, act } = useAction();
  const load = () =>
    run(async () => {
      prescriptions.value = (await api.orders()).filter((o) => o.prescriptions.length);
    });
  const choose = () =>
    act(async () => {
      const result = await new Promise((resolve, reject) =>
        uni.chooseImage({
          count: MAX_PRESCRIPTIONS - images.value.length,
          sizeType: ['compressed'],
          sourceType: ['album', 'camera'],
          success: resolve,
          fail: (e) =>
            e.errMsg?.includes('cancel')
              ? resolve(null)
              : reject(new Error('无法打开相册，请检查权限后重试')),
        }),
      );
      if (!result) return;
      if (result.tempFiles.some((f) => f.size > 10 * 1024 * 1024)) {
        toast('图片过大，请选择 10MB 以内的图片');
        return;
      }
      images.value = [...images.value, ...result.tempFilePaths].slice(0, MAX_PRESCRIPTIONS);
    });
  const preview = (index) => uni.previewImage({ urls: images.value, current: images.value[index] });
  const save = () =>
    act(async () => {
      api.savePrescriptions(images.value);
      toast('测试处方已保存');
      uni.navigateBack();
    });
  onLoad((q) => {
    checkout.value = q.checkout === '1';
    images.value = api.prescriptionDraft();
  });
  onShow(() => {
    loggedIn.value = !!api.session();
    if (loggedIn.value && !checkout.value) load();
  });
</script>
<style scoped>
  .upload-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }
  .upload-item {
    position: relative;
    width: 92px;
    height: 112px;
  }
  .upload-item image {
    width: 92px;
    height: 112px;
    border-radius: 6px;
  }
  .remove-image {
    position: absolute;
    right: 0;
    top: 0;
    width: 44px;
    height: 44px;
    padding: 0 !important;
    background: rgba(32, 45, 41, 0.8);
  }
  .upload-add {
    width: 92px;
    height: 112px;
    border: 1px dashed #b9cbbf;
    background: #f4f8f5;
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 8px !important;
    font-size: 13px !important;
  }
  .review-step {
    margin-top: 16px;
    font-size: 14px;
  }
</style>
