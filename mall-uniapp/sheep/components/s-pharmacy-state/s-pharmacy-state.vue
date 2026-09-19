<template>
  <view class="fs-state">
    <uni-icons :type="loading ? 'loop' : error ? 'wifi' : 'info'" size="36" color="#8b9e94" />
    <view class="fs-title fs-gap">
      {{ loading ? '正在加载…' : error ? '暂时无法加载' : title }}
    </view>
    <view class="fs-muted fs-gap">{{ error || (loading ? '请稍候' : description) }}</view>
    <button v-if="!loading && (error || action)" class="fs-secondary fs-gap" @tap="$emit('retry')">
      {{ error ? '重新加载' : action }}
    </button>
  </view>
</template>
<script setup>
  defineOptions({ options: { styleIsolation: 'apply-shared' } });
  defineProps({
    loading: Boolean,
    error: String,
    title: { type: String, default: '暂无内容' },
    description: String,
    action: String,
  });
  defineEmits(['retry']);
</script>
<style scoped>
  .fs-state {
    padding: 48px 24px;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
</style>
