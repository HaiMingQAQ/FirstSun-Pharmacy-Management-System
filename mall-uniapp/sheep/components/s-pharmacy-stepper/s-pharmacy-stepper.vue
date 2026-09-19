<template>
  <view class="fs-stepper">
    <button
      :disabled="disabled || modelValue <= 1 || max < 1"
      aria-label="减少数量"
      @tap="$emit('update:modelValue', Math.max(1, Math.min(max, modelValue - 1)))"
    >
      <uni-icons type="minus" size="17" />
    </button>
    <text class="count">{{ modelValue }}</text>
    <button
      :disabled="disabled || modelValue >= max"
      aria-label="增加数量"
      @tap="$emit('update:modelValue', modelValue + 1)"
    >
      <uni-icons type="plus" size="17" />
    </button>
  </view>
</template>
<script setup>
  defineOptions({ options: { styleIsolation: 'apply-shared' } });
  defineProps({ modelValue: Number, max: Number, disabled: Boolean });
  defineEmits(['update:modelValue']);
</script>
<style scoped>
  .fs-stepper {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    border: 1px solid #e5ebe8;
    border-radius: 6px;
  }
  .fs-stepper button {
    width: 40px;
    min-height: 44px;
    padding: 0;
    background: #f6f8f7;
  }
  .count {
    width: 40px;
    text-align: center;
    font-size: 15px;
    font-variant-numeric: tabular-nums;
  }
</style>
