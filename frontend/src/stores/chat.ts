import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage } from '@/types'

const STORAGE_PREFIX = 'chat_messages_'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  let currentKbId: number | null = null

  function setKbId(kbId: number) {
    currentKbId = kbId
    const saved = localStorage.getItem(STORAGE_PREFIX + kbId)
    messages.value = saved ? JSON.parse(saved) : []
  }

  function persist() {
    if (currentKbId != null) {
      localStorage.setItem(STORAGE_PREFIX + currentKbId, JSON.stringify(messages.value))
    }
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
      localStorage.removeItem(STORAGE_PREFIX + currentKbId)
    }
  }

  return { messages, setKbId, addMessage, updateLastAssistantMessage, clearMessages }
})
