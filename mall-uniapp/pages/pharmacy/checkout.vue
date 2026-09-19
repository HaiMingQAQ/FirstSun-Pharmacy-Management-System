<template>
  <s-pharmacy-page dock>
    <s-pharmacy-state v-if="!loggedIn" title="请先登录" action="去登录" @retry="go('login')" />
    <s-pharmacy-state
      v-else-if="error || !data"
      :loading="loading"
      :error="error"
      @retry="load"
    />
    <template v-else>
      <view class="fs-section">
        <view class="fs-choice">
          <button :class="{ active: mode === 'delivery' }" @tap="mode = 'delivery'">
            门店配送
          </button>
          <button :class="{ active: mode === 'pickup' }" @tap="mode = 'pickup'">到店自提</button>
        </view>
        <view v-if="mode === 'delivery'" class="fs-menu" @tap="go('address?select=1')">
          <view class="fs-grow">
            <view class="fs-title">
              {{ data.address ? data.address.name + '  ' + data.address.mobile : '请选择收货地址' }}
            </view>
            <view class="fs-muted fs-gap">
              {{ data.address ? data.address.detail : '添加地址，方便门店配送' }}
            </view>
          </view>
          <uni-icons type="right" size="18" />
        </view>
        <view v-else class="fs-gap">
          <view class="fs-title">{{ api.store.name }}</view>
          <view class="fs-muted fs-gap">
            {{ api.store.address }}
            <br />
            营业时间 {{ api.store.hours }}
          </view>
          <view class="fs-muted">请等待门店备货通知后到店取药</view>
        </view>
      </view>
      <view class="fs-section">
        <view class="fs-title">商品清单</view>
        <s-pharmacy-product
          v-for="row in data.items"
          :key="row.drugId"
          :product="row.product"
          hide-stock
          :qty="row.qty"
        />
        <view class="fs-muted fs-gap">履约门店：{{ api.store.name }}</view>
      </view>
      <view v-if="hasRx" class="fs-section">
        <view class="fs-between">
          <view class="fs-title">
            处方资料
            <text class="fs-tag fs-tag-rx">必填</text>
          </view>
          <text class="fs-muted">{{ data.prescriptions.length }}/3 张</text>
        </view>
        <view class="fs-notice fs-gap">
          需上传处方并经药师审核。提交订单后进入待审核状态，审核结果以药师实际处理为准。
        </view>
        <button class="fs-outline fs-gap" @tap="go('prescription-upload?checkout=1')">
          {{ data.prescriptions.length ? '查看 / 修改处方' : '上传处方图片' }}
        </button>
      </view>
      <view class="fs-section">
        <view class="fs-between fs-field">
          <view class="fs-grow">
            <view>积分抵扣</view>
            <view class="fs-muted">可用 {{ data.points }} 积分 · 100 积分抵 ¥1</view>
            <view class="fs-muted">每单最多抵 ¥5，配送费不参与</view>
          </view>
          <switch
            :checked="usePoints"
            :disabled="!data.points"
            color="#176b5b"
            @change="usePoints = $event.detail.value"
          />
        </view>
        <view class="fs-between fs-field">
          <text>支付方式</text>
          <text class="fs-tag">模拟支付</text>
        </view>
        <view class="fs-field">
          <text class="fs-label">订单备注（选填）</text>
          <textarea v-model="remark" maxlength="200" placeholder="如配送前请先电话联系" />
          <view class="fs-muted">{{ remark.length }}/200</view>
        </view>
      </view>
      <view class="fs-section">
        <view class="fs-between">
          <text>商品金额</text>
          <text>¥{{ money(subtotal) }}</text>
        </view>
        <view class="fs-between fs-gap">
          <text>配送费</text>
          <text>{{ shipping ? '¥' + money(shipping) : '免配送费' }}</text>
        </view>
        <view class="fs-between fs-gap">
          <text>积分抵扣</text>
          <text>− ¥{{ money(discount) }}</text>
        </view>
        <view class="fs-between fs-gap">
          <text class="fs-title">应付合计</text>
          <text class="fs-price">¥{{ money(total) }}</text>
        </view>
      </view>
      <view class="fs-footer">测试订单不会扣款，也不会实际配送</view>
      <view class="fs-dock">
        <view class="fs-grow">
          <text class="fs-small">合计</text>
          <text class="fs-price">¥{{ money(total) }}</text>
        </view>
        <button class="fs-primary" :disabled="loading || busy || submitted" :loading="busy" @tap="submit">
          {{ submitted ? '已提交' : busy ? '提交中…' : hasRx ? '提交审核订单' : '提交订单' }}
        </button>
      </view>
    </template>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref, computed } from 'vue';
  import { onShow } from '@dcloudio/uni-app';
  import api, { money } from '@/sheep/api/pharmacy/client';
  import { go, toast, useRequest, useAction } from './usePharmacy';
  const data = ref(null),
    mode = ref('pickup'),
    usePoints = ref(false),
    remark = ref(''),
    loggedIn = ref(false),
    submitted = ref(false);
  const { loading, error, run } = useRequest();
  const { busy, act } = useAction();
  const hasRx = computed(() => data.value?.items.some((r) => r.product.rx));
  const subtotal = computed(
    () => data.value?.items.reduce((s, r) => s + r.product.price * r.qty, 0) || 0,
  );
  const shipping = computed(() => (mode.value === 'delivery' ? 600 : 0));
  const discount = computed(() =>
    usePoints.value ? Math.min(data.value?.points || 0, subtotal.value, 500) : 0,
  );
  const total = computed(() => subtotal.value + shipping.value - discount.value);
  const load = () =>
    run(async () => {
      // 返回地址/处方页后保留已挂载的表单，避免 H5 ResizeSensor 激活时引用已卸载节点。
      data.value = await api.checkout();
    });
  const submit = () => {
    if (submitted.value) return;
    act(async () => {
      const order = await api.createOrder({
        mode: mode.value,
        address: data.value.address,
        usePoints: usePoints.value,
        remark: remark.value,
      });
      submitted.value = true;
      toast('订单已提交');
      uni.redirectTo({ url: `/pages/pharmacy/order-detail?id=${order.id}` });
    });
  };
  onShow(() => {
    loggedIn.value = !!api.session();
    if (loggedIn.value && !submitted.value) load();
  });
</script>
