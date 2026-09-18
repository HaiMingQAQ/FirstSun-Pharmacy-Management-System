import { defineStore } from 'pinia'
import { store } from '../index'

interface AiAssistantState {
  opened: boolean
  initialized: boolean
}

export const useAiAssistantStore = defineStore('ai-assistant', {
  state: (): AiAssistantState => ({
    opened: false,
    initialized: false
  }),
  actions: {
    open() {
      this.initialized = true
      this.opened = true
    },
    close() {
      this.opened = false
    },
    toggle() {
      this.opened ? this.close() : this.open()
    },
    reset() {
      this.opened = false
      this.initialized = false
      sessionStorage.removeItem('pharmacy-ai-assistant-draft')
      sessionStorage.removeItem('pharmacy-ai-assistant-history')
    }
  },
  persist: false
})

export const useAiAssistantStoreWithOut = () => useAiAssistantStore(store)
