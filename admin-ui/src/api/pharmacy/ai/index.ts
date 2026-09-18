import request from '@/config/axios'
import { config } from '@/config/axios/config'
import { getAccessToken } from '@/utils/auth'
import { fetchEventSource, EventSourceMessage } from '@microsoft/fetch-event-source'

export interface PharmacyAiCommand {
  commandId: number
  tool: string
  status: 'PENDING' | 'EXECUTING' | 'EXECUTED' | 'FAILED' | 'CANCELLED' | 'EXPIRED'
  before?: Record<string, any>
  after?: Record<string, any>
  expiresAt: string
  confirmationToken?: string
  errorMessage?: string
}

export const PharmacyAiApi = {
  stream: (data: any, signal: AbortSignal, onMessage: (event: EventSourceMessage) => void) =>
    fetchEventSource(`${config.base_url}/pharmacy/ai/chat/message/send-stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        Authorization: `Bearer ${getAccessToken()}`
      },
      body: JSON.stringify(data),
      signal,
      openWhenHidden: true,
      onmessage: onMessage,
      onerror: (error) => {
        throw error
      }
    }),
  recentCommands: () => request.get({ url: '/pharmacy/ai/command/recent' }),
  confirm: (id: number, confirmationToken: string) =>
    request.post({
      url: `/pharmacy/ai/command/${id}/confirm`,
      data: { confirmationToken }
    }),
  cancel: (id: number) => request.post({ url: `/pharmacy/ai/command/${id}/cancel` })
}
