import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage } from '@/types'
import { loadHistory, syncHistory, type RemoteMessage } from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  let currentKbId: number | null = null
  let syncTimer: ReturnType<typeof setTimeout> | null = null

  async function setKbId(kbId: number) {
    currentKbId = kbId
    try {
      const { data } = await loadHistory(kbId)
      messages.value = data.map((m: RemoteMessage, i: number) => ({
        id: `${kbId}_${i}`,
        role: m.role as 'user' | 'assistant',
        content: m.content,
        sources: m.sources ? JSON.parse(m.sources) : undefined,
        timestamp: m.timestamp
      }))
    } catch {
      messages.value = []
    }
  }

  function persist() {
    if (syncTimer) clearTimeout(syncTimer)
    syncTimer = setTimeout(() => {
      if (currentKbId != null) {
        const remote: RemoteMessage[] = messages.value.map(m => ({
          role: m.role,
          content: m.content,
          sources: m.sources ? JSON.stringify(m.sources) : undefined,
          timestamp: m.timestamp
        }))
        syncHistory(currentKbId, remote).catch(() => {})
      }
    }, 1000)
  }

  function addMessage(message: ChatMessage) {
    messages.value.push(message)
    persist()
  }

  function updateLastAssistantMessage(content: string) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.content = content
      persist()
    }
  }

  function clearMessages() {
    messages.value = []
    if (currentKbId != null) {
      syncHistory(currentKbId, []).catch(() => {})
    }
  }

  return { messages, setKbId, addMessage, updateLastAssistantMessage, clearMessages }
})
