<template>
  <s-pharmacy-page>
    <s-pharmacy-state
      v-if="!loggedIn"
      title="登录后查看订单"
      action="去登录"
      @retry="go('login')"
    />
    <s-pharmacy-state
      v-else-if="loading || error || !order"
      :loading="loading"
      :error="error"
      @retry="load"
    />
    <template v-else>
      <view class="status-head">
        <view class="fs-row">
          <uni-icons
            :type="order.status === 'completed' ? 'checkbox-filled' : 'info'"
            size="30"
            color="#176b5b"
          />
          <view>
            <view class="fs-heading">{{ statusNames[order.status] }}</view>
            <view class="fs-muted fs-gap">{{ statusDescription }}</view>
          </view>
        </view>
      </view>
      <view class="fs-section">
        <view class="fs-between">
          <text class="fs-title">{{ order.mode === 'pickup' ? '到店自提' : '门店配送' }}</text>
          <text class="fs-tag">{{ order.paymentLabel || (order.paid ? '已支付（模拟）' : '未支付') }}</text>
        </view>
        <view class="fs-gap">
          {{
            order.mode === 'pickup'
              ? order.store.name
              : [order.address?.name, order.address?.mobile].filter(Boolean).join(' ')
          }}
        </view>
        <view class="fs-muted fs-gap">
          {{ order.mode === 'pickup' ? order.store.address : order.address?.detail }}
        </view>
        <view v-if="order.mode === 'pickup'" class="fs-muted">
          营业时间 {{ order.store.hours }} · 凭订单号到店取药
        </view>
      </view>
      <view class="fs-section">
        <view class="fs-title">{{ order.store.name }}</view>
        <s-pharmacy-product
          v-for="row in order.items"
          :key="row.drugId"
          :product="row.product"
          hide-stock
          :qty="row.qty"
        />
        <view class="fs-between fs-gap">
          <text>商品金额</text>
          <text>¥{{ money(order.subtotal) }}</text>
        </view>
        <view class="fs-between fs-gap">
          <text>配送费</text>
          <text>¥{{ money(order.shipping) }}</text>
        </view>
        <view class="fs-between fs-gap">
          <text>积分抵扣</text>
          <text>− ¥{{ money(order.points) }}</text>
        </view>
        <view class="fs-between fs-gap">
          <text>{{ order.paid ? '实付金额' : '应付金额' }}</text>
          <text class="fs-price">¥{{ money(order.total) }}</text>
        </view>
      </view>
      <view class="fs-section">
        <view class="fs-title">处方审核</view>
        <view class="fs-gap">
          <text class="fs-tag" :class="{ 'fs-tag-rx': order.prescriptions.length }">
            {{
              order.status === 'cancelled' && order.prescriptions.length
                ? '订单已取消，审核已终止'
                : order.review
            }}
          </text>
        </view>
        <view v-if="order.prescriptions.length" class="fs-muted fs-gap">
          已提交
          {{ order.prescriptions.length }}
          张处方。需经药师审核，不承诺审核结果；测试环境不进行真实审核。
        </view>
      </view>
      <view class="fs-section">
        <view class="fs-title">订单信息</view>
        <view class="meta-row">
          <text>订单编号</text>
          <text>{{ order.id }}</text>
        </view>
        <view class="meta-row">
          <text>创建时间</text>
          <text>{{ order.createdAt }}</text>
        </view>
        <view class="meta-row">
          <text>支付方式</text>
          <text>{{ order.paymentLabel ? '以门店支付记录为准' : '模拟支付（不会扣款）' }}</text>
        </view>
        <view class="meta-row">
          <text>订单备注</text>
          <text>{{ order.remark || '无' }}</text>
        </view>
        <s-pharmacy-order-actions :order="order" @change="load" />
      </view>
    </template>
  </s-pharmacy-page>
</template>
<script setup>
  import { ref, computed } from 'vue';
  import { onLoad, onShow } from '@dcloudio/uni-app';
  import api, { money, statusNames } from '@/sheep/api/pharmacy/client';
  import { go, useRequest } from './usePharmacy';
  const id = ref(''),
    order = ref(null),
    loggedIn = ref(false);
  const { loading, error, run } = useRequest();
  const statusDescription = computed(
    () =>
      ({
        unpaid: order.value?.paymentLabel ? '订单尚未支付，请联系门店' : '订单已提交，请完成模拟支付',
        review: '处方已提交，请等待药师审核',
        ready:
          order.value?.mode === 'pickup'
            ? '订单已支付，等待门店备货'
            : '订单已支付，等待门店配送',
        completed: '感谢您的信任，请按说明书使用药品',
        cancelled: '订单已取消，使用的测试积分已退回',
      }[order.value?.status]),
  );
  const load = () =>
    run(async () => {
      order.value = await api.order(id.value);
    });
  onLoad((q) => (id.value = q.id));
  onShow(() => {
    loggedIn.value = !!api.session();
    if (loggedIn.value) load();
    else order.value = null;
  });
</script>
<style scoped>
  .status-head {
    padding: 28px 20px;
    background: #eef5f0;
  }
  .meta-row {
    display: flex;
    justify-content: space-between;
    gap: 18px;
    font-size: 13px;
    padding-top: 16px;
  }
  .meta-row text:first-child {
    width: 60px;
    flex-shrink: 0;
    color: #64716c;
  }
  .meta-row text:last-child {
    min-width: 0;
    text-align: right;
  }
</style>
