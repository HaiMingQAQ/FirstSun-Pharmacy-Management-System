<template>
  <el-tooltip content="AI助手" placement="right" :disabled="!collapse">
    <button
      type="button"
      class="ai-assistant-entry"
      :class="{ 'is-active': assistant.opened, 'is-collapsed': collapse }"
      :aria-expanded="assistant.opened"
      aria-controls="ai-assistant-drawer"
      @click="assistant.toggle"
    >
      <Icon icon="ep:chat-dot-round" />
      <span v-show="!collapse">AI助手</span>
    </button>
  </el-tooltip>
</template>

<script setup lang="ts">
import { useAiAssistantStore } from '@/store/modules/aiAssistant'

defineProps<{ collapse: boolean }>()

const assistant = useAiAssistantStore()
</script>

<style scoped lang="scss">
.ai-assistant-entry {
  display: flex;
  width: calc(100% - 16px);
  height: 44px;
  padding: 0 20px;
  margin: 6px 8px 8px;
  color: var(--left-menu-text-color);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 7px;
  transition:
    color 0.16s ease,
    background-color 0.16s ease;
  align-items: center;
  gap: 8px;

  &:hover,
  &.is-active {
    color: var(--left-menu-text-active-color);
    background: var(--left-menu-bg-active-color);
  }

  &.is-collapsed {
    justify-content: center;
    padding: 0;
  }

  :deep(svg) {
    width: 19px;
    height: 19px;
    flex: none;
  }
}
</style>
