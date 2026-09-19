<template>
  <s-pharmacy-page :tab="2" dock>
    <view class="fs-pad fs-white">
      <view class="fs-heading">购物车</view>
      <view class="fs-muted">{{ api.store.name }} · 下单前为您核对库存</view>
    </view>
    <s-pharmacy-state
      v-if="!loggedIn"
      title="登录后查看购物车"
      description="选好的药品会保存在您的测试账户中"
      action="去登录"
      @retry="go('login')"
    />
    <s-pharmacy-state
      v-else-if="loading || error || !items.length"
      :loading="loading"
      :error="error"
      title="购物车还是空的"
      description="去挑选需要的药品吧"
      action="去逛逛"
      @retry="error ? load() : go('category')"
    />
    <template v-else>
      <view class="fs-section">
        <view v-for="row in items" :key="row.drugId" class="cart-item">
          <view class="cart-main">
            <button
              class="fs-check"
              :disabled="busy || invalid(row)"
              :aria-label="row.checked ? '取消选择商品' : '选择商品'"
              @tap="change(row, { checked: !row.checked })"
            >
              <uni-icons
                :type="row.checked && !invalid(row) ? 'checkbox-filled' : 'circle'"
                size="23"
                :color="invalid(row) ? '#bcc5bf' : '#176b5b'"
              />
            </button>
            <view class="fs-grow">
              <s-pharmacy-product :product="row.product" compact />
              <view v-if="invalid(row)" class="fs-danger fs-small">
                {{
                  !row.product.active
                    ? '商品已失效，请删除'
                    : row.qty > row.product.stock
                    ? '库存不足，请减少数量或删除'
                    : '暂时缺货'
                }}
              </view>
              <view class="fs-between cart-actions">
                <button class="fs-text-btn fs-danger" :disabled="busy" @tap="remove(row)">
                  删除
                </button>
                <s-pharmacy-stepper
                  :model-value="row.qty"
                  :max="row.product.stock"
                  :disabled="busy || !row.product.active"
                  @update:model-value="change(row, { qty: $event })"
                />
              </view>
            </view>
          </view>
        </view>
      </view>
      <view class="fs-footer">处方药需上传处方并经药师审核</view>
      <view class="fs-dock above-tabs">
        <button
          class="fs-check cart-select-all"
          :disabled="busy"
          aria-label="全选商品"
          @tap="selectAll"
        >
          <uni-icons :type="allSelected ? 'checkbox-filled' : 'circle'" size="23" color="#176b5b" />
          <text>全选</text>
        </button>
        <view class="fs-grow">
          <text class="fs-small">合计</text>
          <text class="fs-price">¥{{ money(total) }}</text>
          <view class="fs-muted">已选 {{ selected.length }} 种，不含配送费</view>
        </view>
        <button class="fs-primary" :disabled="busy || !selected.length" @tap="checkout">
          去结算
        </button>
      </view>
    </template>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref, computed } from 'vue';
  import { onShow } from '@dcloudio/uni-app';
  import api, { money } from '@/sheep/api/pharmacy/client';
  import { go, confirm, useRequest, useAction } from './usePharmacy';
  const items = ref([]),
    loggedIn = ref(false);
  const { loading, error, run } = useRequest();
  const { busy, act } = useAction();
  const invalid = (r) => !r.product.active || !r.product.stock || r.qty > r.product.stock;
  const selected = computed(() => items.value.filter((r) => r.checked && !invalid(r)));
  const allSelected = computed(
    () =>
      selected.value.length > 0 &&
      selected.value.length === items.value.filter((r) => !invalid(r)).length,
  );
  const total = computed(() => selected.value.reduce((s, r) => s + r.product.price * r.qty, 0));
  const load = () =>
    run(async () => {
      items.value = await api.cart();
    });
  const change = (row, patch) =>
    act(async () => {
      await api.updateCart(row.drugId, patch);
      items.value = await api.cart();
    });
  const remove = (row) =>
    act(async () => {
      if (await confirm('删除药品', `确定从购物车移除「${row.product.name}」？`)) {
        await api.remove(row.drugId);
        items.value = await api.cart();
      }
    });
  const selectAll = () =>
    act(async () => {
      await api.selectAll(!allSelected.value);
      items.value = await api.cart();
    });
  const checkout = () =>
    act(async () => {
      for (const r of items.value.filter((r) => r.checked && invalid(r)))
        await api.updateCart(r.drugId, { checked: false });
      api.beginCheckout();
      go('checkout');
    });
  onShow(() => {
    loggedIn.value = !!api.session();
    if (loggedIn.value) load();
    else items.value = [];
  });
</script>
<style scoped>
  .cart-select-all {
    flex-direction: column;
    font-size: 11px;
    gap: 3px;
  }
  .cart-item:last-child {
    border-bottom: 0;
  }
  .cart-main {
    display: flex;
    align-items: flex-start;
    gap: 4px;
  }
  .cart-main > button {
    margin-top: 38px;
    margin-left: -8px;
  }
  .cart-actions {
    padding: 10px 0 18px;
  }
  .cart-item {
    border-bottom: 1px solid #e5ebe8;
  }
</style>
