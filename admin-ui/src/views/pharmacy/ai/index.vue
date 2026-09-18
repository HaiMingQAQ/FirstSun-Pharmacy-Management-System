<template>
  <div class="ai-page" :class="{ embedded }">
    <el-card class="conversations" shadow="never">
      <template #header>
        <div class="header"
          ><span>AI 助手</span><el-button link @click="newConversation">新对话</el-button></div
        >
      </template>
      <div
        v-for="item in conversations"
        :key="item.id"
        class="conversation"
        :class="{ active: item.id === activeId }"
        @click="selectConversation(item.id)"
      >
        {{ item.title }}
      </div>
      <el-divider>待确认操作</el-divider>
      <div v-for="command in commands" :key="command.commandId" class="command-link">
        #{{ command.commandId }} {{ commandLabel(command.tool) }} ·
        {{ statusLabel(command.status) }}
      </div>
    </el-card>

    <el-card class="chat" shadow="never">
      <div ref="messageArea" class="messages">
        <el-empty
          v-if="messages.length === 0"
          description="可查询药品、当前门店库存，或生成药品增删改预览"
        />
        <div v-for="(item, index) in messages" :key="index" class="message" :class="item.role">
          <div class="bubble">
            <MarkdownView v-if="item.text" :content="item.text" />
            <el-card v-if="item.toolResult" class="tool-card" shadow="never">
              <template #header>工具结果 · {{ item.toolResult.tool }}</template>
              <div>{{ summarizeResult(item.toolResult.result) }}</div>
            </el-card>
            <el-card v-if="item.preview" class="tool-card preview" shadow="never">
              <template #header>{{ commandLabel(item.preview.tool) }}预览</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="命令"
                  >#{{ item.preview.commandId }}</el-descriptions-item
                >
                <el-descriptions-item label="状态">{{
                  statusLabel(item.preview.status)
                }}</el-descriptions-item>
                <el-descriptions-item label="变更">{{
                  diffText(item.preview)
                }}</el-descriptions-item>
                <el-descriptions-item label="有效期">{{
                  item.preview.expiresAt
                }}</el-descriptions-item>
              </el-descriptions>
              <div
                class="actions"
                v-if="item.preview.status === 'PENDING' && item.preview.confirmationToken"
              >
                <el-button
                  type="danger"
                  :loading="executing === item.preview.commandId"
                  @click="confirmCommand(item.preview)"
                  >确认{{ commandLabel(item.preview.tool) }}</el-button
                >
                <el-button
                  :disabled="executing === item.preview.commandId"
                  @click="cancelCommand(item.preview)"
                  >取消</el-button
                >
              </div>
              <el-alert
                v-else-if="item.preview.status === 'PENDING'"
                type="info"
                :closable="false"
                title="页面刷新后确认凭证不再保留；如需执行，请重新生成操作预览"
              />
              <el-alert
                v-if="item.preview.errorMessage"
                type="error"
                :closable="false"
                :title="item.preview.errorMessage"
              />
            </el-card>
          </div>
        </div>
      </div>
      <div class="composer">
        <el-input
          v-model="content"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          placeholder="例如：查询阿莫西林；查看药品 1 的当前门店库存；把药品 1 的零售价改为 18.50 元"
          @keydown.ctrl.enter="send"
        />
        <div class="composer-actions">
          <span class="hint">Ctrl + Enter 发送；写操作必须再次确认</span>
          <el-button v-if="generating" @click="stop">停止生成</el-button>
          <el-button v-else type="primary" :disabled="!content.trim()" @click="send"
            >发送</el-button
          >
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import MarkdownView from '@/components/MarkdownView/index.vue'
import { PharmacyAiApi, PharmacyAiCommand } from '@/api/pharmacy/ai'

withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false })

type Message = {
  role: 'user' | 'assistant'
  text?: string
  toolResult?: any
  preview?: PharmacyAiCommand
}

type Conversation = {
  id: number
  title: string
  messages: Message[]
}

const message = useMessage()
const initialId = Date.now()
const activeId = ref(initialId)
const conversations = ref<Conversation[]>([{ id: initialId, title: '新对话', messages: [] }])
const messages = computed<Message[]>({
  get: () => conversations.value.find((item) => item.id === activeId.value)?.messages || [],
  set: (value) => {
    const conversation = conversations.value.find((item) => item.id === activeId.value)
    if (conversation) conversation.messages = value
  }
})
const commands = ref<PharmacyAiCommand[]>([])
const content = ref('')
const generating = ref(false)
const executing = ref<number>()
const messageArea = ref<HTMLElement>()
let controller: AbortController | undefined
const DRAFT_KEY = 'pharmacy-ai-assistant-draft'
const HISTORY_KEY = 'pharmacy-ai-assistant-history'

watch(content, (value) => sessionStorage.setItem(DRAFT_KEY, value))
watch(
  [activeId, conversations],
  () => {
    const safeConversations = conversations.value.map((conversation) => ({
      ...conversation,
      messages: conversation.messages.map((item) => ({
        ...item,
        preview: item.preview ? { ...item.preview, confirmationToken: undefined } : undefined
      }))
    }))
    sessionStorage.setItem(
      HISTORY_KEY,
      JSON.stringify({ activeId: activeId.value, conversations: safeConversations })
    )
  },
  { deep: true }
)

const selectConversation = (id: number) => {
  activeId.value = id
  nextTick(() => messageArea.value?.scrollTo({ top: messageArea.value.scrollHeight }))
}

const titleFromMessage = (text: string) => {
  const normalized = text.replace(/\s+/g, ' ').trim()
  return normalized.length > 18 ? `${normalized.slice(0, 18)}…` : normalized
}

const newConversation = () => {
  const emptyConversation = conversations.value.find((item) => item.messages.length === 0)
  if (emptyConversation) {
    selectConversation(emptyConversation.id)
    return
  }
  activeId.value = Date.now()
  conversations.value.unshift({ id: activeId.value, title: '新对话', messages: [] })
}

const send = async () => {
  const question = content.value.trim()
  if (!question || generating.value) return
  content.value = ''
  const conversation = conversations.value.find((item) => item.id === activeId.value)
  if (conversation?.messages.length === 0) conversation.title = titleFromMessage(question)
  messages.value.push({ role: 'user', text: question })
  const answer: Message = { role: 'assistant', text: '' }
  messages.value.push(answer)
  generating.value = true
  controller = new AbortController()
  try {
    await PharmacyAiApi.stream(
      { conversationId: activeId.value, clientMessageId: crypto.randomUUID(), content: question },
      controller.signal,
      (event) => {
        const data = event.data ? JSON.parse(event.data) : {}
        if (event.event === 'delta') answer.text = (answer.text || '') + (data.content || '')
        if (event.event === 'tool_result') answer.toolResult = data
        if (event.event === 'tool_preview') {
          answer.preview = data
          commands.value.unshift(data)
        }
        if (event.event === 'error') answer.text = `${answer.text || ''}\n\n${data.message}`
        if (event.event === 'done') generating.value = false
        nextTick(() => messageArea.value?.scrollTo({ top: messageArea.value.scrollHeight }))
      }
    )
  } catch (error: any) {
    if (error?.name !== 'AbortError')
      answer.text = `${answer.text || ''}\n\n连接已异常终止，请重试。`
  } finally {
    generating.value = false
  }
}

const stop = () => {
  controller?.abort()
  generating.value = false
}

const confirmCommand = async (command: PharmacyAiCommand) => {
  if (!command.confirmationToken) return message.error('确认凭证不存在或页面已刷新，请重新生成预览')
  executing.value = command.commandId
  try {
    await PharmacyAiApi.confirm(command.commandId, command.confirmationToken)
    command.status = 'EXECUTED'
    message.success(`${commandLabel(command.tool)}成功`)
  } catch (error) {
    command.status = 'FAILED'
    throw error
  } finally {
    executing.value = undefined
  }
}

const cancelCommand = async (command: PharmacyAiCommand) => {
  await PharmacyAiApi.cancel(command.commandId)
  command.status = 'CANCELLED'
}

const commandLabel = (tool: string) =>
  ({ CREATE_DRUG: '新增药品', UPDATE_DRUG: '修改药品', DELETE_DRUG: '删除药品' })[tool] || tool
const statusLabel = (status: string) =>
  ({
    PENDING: '待确认',
    EXECUTING: '执行中',
    EXECUTED: '成功',
    FAILED: '失败',
    CANCELLED: '已取消',
    EXPIRED: '已过期'
  })[status] || status
const diffText = (command: PharmacyAiCommand) => {
  if (command.tool === 'CREATE_DRUG')
    return `将新增：${command.after?.genericName || command.after?.drugCode || '药品'}`
  if (command.tool === 'DELETE_DRUG')
    return `将删除：${command.before?.genericName || command.before?.drugCode || command.before?.id}`
  const changed = Object.keys(command.after || {}).filter(
    (key) => JSON.stringify(command.before?.[key]) !== JSON.stringify(command.after?.[key])
  )
  return (
    changed
      .map((key) => `${key}: ${command.before?.[key] ?? '-'} → ${command.after?.[key] ?? '-'}`)
      .join('；') || '无字段变化'
  )
}
const summarizeResult = (result: any) => {
  if (Array.isArray(result)) return `共返回 ${result.length} 条药品记录。`
  if (result?.items) return `当前门店返回 ${result.items.length} 个药品的库存摘要。`
  if (result?.found === false) return '未找到对应药品。'
  return `已返回药品 ${result?.genericName || result?.drugCode || ''} 的详情。`
}

onMounted(async () => {
  content.value = sessionStorage.getItem(DRAFT_KEY) || ''
  const history = sessionStorage.getItem(HISTORY_KEY)
  if (history) {
    try {
      const saved = JSON.parse(history)
      if (Array.isArray(saved.conversations) && saved.conversations.length > 0) {
        conversations.value = saved.conversations.map((conversation: Conversation) => {
          const firstUserMessage = conversation.messages?.find((item) => item.role === 'user')?.text
          return {
            ...conversation,
            title:
              conversation.title === '新对话' && firstUserMessage
                ? titleFromMessage(firstUserMessage)
                : conversation.title,
            messages: conversation.messages || []
          }
        })
        activeId.value = conversations.value.some((item) => item.id === saved.activeId)
          ? saved.activeId
          : conversations.value[0].id
      }
    } catch {
      sessionStorage.removeItem(HISTORY_KEY)
    }
  }
  commands.value = (await PharmacyAiApi.recentCommands()) || []
})
onBeforeUnmount(stop)
</script>

<style scoped>
.ai-page {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  height: calc(100vh - 120px);
  padding: 16px;
}

.ai-page.embedded {
  width: 100%;
  height: 100%;
  padding: 12px;
  grid-template-columns: minmax(180px, 28%) minmax(0, 1fr);
  box-sizing: border-box;
}

.conversations,
.chat {
  height: 100%;
}

.header,
.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conversation,
.command-link {
  padding: 10px;
  cursor: pointer;
  border-radius: 6px;
}

.conversation.active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.command-link {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.chat :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: calc(100% - 40px);
}

.messages {
  padding: 8px 20px;
  overflow: auto;
  flex: 1;
}

.message {
  display: flex;
  margin: 16px 0;
}

.message.user {
  justify-content: flex-end;
}

.bubble {
  max-width: 78%;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-radius: 10px;
  overflow-wrap: anywhere;
}

.user .bubble {
  color: white;
  background: var(--el-color-primary);
}

.tool-card {
  min-width: 440px;
  margin-top: 10px;
}

.preview {
  border-color: var(--el-color-warning);
}

.actions,
.composer-actions {
  margin-top: 12px;
}

.composer {
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color);
}

.hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (width <= 900px) {
  .ai-page.embedded {
    grid-template-columns: 1fr;
  }

  .ai-page.embedded .conversations {
    display: none;
  }

  .tool-card {
    min-width: 0;
  }
}
</style>
