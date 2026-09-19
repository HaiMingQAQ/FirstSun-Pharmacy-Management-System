<template>
  <view class="order-actions">
    <button
      v-if="['unpaid', 'review'].includes(order.status)"
      class="fs-outline"
      :disabled="busy"
      @tap="action('cancel')"
    >
      取消订单
    </button>
    <button
      v-if="order.status === 'unpaid'"
      class="fs-primary"
      :disabled="busy"
      :loading="busy"
      @tap="action('pay')"
    >
      模拟支付
    </button>
    <button
      v-if="order.status === 'ready'"
      class="fs-primary"
      :disabled="busy"
      :loading="busy"
      @tap="action('receive')"
    >
      {{ order.mode === 'pickup' ? '确认已取药' : '确认收货' }}
    </button>
    <button v-if="detail" class="fs-outline" @tap="go('order-detail?id=' + order.id)">
      查看详情
    </button>
  </view>
</template>
<script setup>
  defineOptions({ options: { styleIsolation: 'apply-shared' } });
  import api from '@/sheep/api/pharmacy/client';
  import { go, confirm, toast, useAction } from '@/pages/pharmacy/usePharmacy';
  const props = defineProps({ order: Object, detail: Boolean });
  const emit = defineEmits(['change']);
  const { busy, act } = useAction();
  const action = (type) =>
    act(async () => {
      const messages = {
        cancel: ['取消订单', '确定取消此订单？已使用的测试积分会退回。'],
        pay: ['模拟支付', '本次为模拟操作，不会真实扣款。确认完成测试支付？'],
        receive: ['确认收货', '请确认已收到或取到全部药品。'],
      };
      if (!(await confirm(...messages[type]))) return;
      await api.orderAction(props.order.id, type);
      toast(type === 'pay' ? '模拟支付成功' : '操作成功');
      emit('change');
    });
</script>
<style scoped>
  .order-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    flex-wrap: wrap;
    margin-top: 16px;
  }
  .order-actions button {
    padding: 10px 12px;
    font-size: 14px;
  }
</style>
