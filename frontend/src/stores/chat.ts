import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage } from '@/types'
import { loadHistory, syncHistory, type RemoteMessage } from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  let currentKbId: number | null = null
  let syncTimer: ReturnType<typeof setTimeout> | null = null

  function toRemote(): RemoteMessage[] {
    return messages.value.map(m => ({
      role: m.role,
      content: m.content,
      sources: m.sources ? JSON.stringify(m.sources) : undefined,
      timestamp: m.timestamp
    }))
  }

  function doSync() {
    if (syncTimer) { clearTimeout(syncTimer); syncTimer = null }
    if (currentKbId == null || messages.value.length === 0) return
    syncHistory(currentKbId, toRemote()).catch(e => {
      console.error('Chat sync failed:', e)
    })
  }

  /**
   * 页面卸载专用：使用 fetch keepalive 确保浏览器不取消请求。
   * keepalive body 上限 64KB，聊天消息通常远小于此。
   */
  function doUnloadSync() {
    if (currentKbId == null || messages.value.length === 0) return
    const token = localStorage.getItem('token')
    const body = JSON.stringify(toRemote())
    // 64KB 是各浏览器 keepalive body 上限，超限时降级为普通 fetch（可能被取消）
    if (body.length > 64 * 1024) {
      console.warn('Chat history too large for keepalive beacon, attempting regular fetch')
    }
    fetch(`/api/chat/history/${currentKbId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body,
      keepalive: true
    }).catch(e => {
      console.error('Unload chat sync failed:', e)
    })
  }

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

  function addMessage(message: ChatMessage) {
    messages.value.push(message)
    // User message: sync immediately
    if (syncTimer) clearTimeout(syncTimer)
    syncTimer = setTimeout(doSync, 500)
  }

  function updateLastAssistantMessage(content: string) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.content = content
      // Debounce stream tokens, force sync when done
      if (syncTimer) clearTimeout(syncTimer)
      syncTimer = setTimeout(doSync, 300)
    }
  }

  /** Call when streaming completes — force immediate sync */
  function flushSync() {
    doSync()
  }

  function clearMessages() {
    messages.value = []
    if (currentKbId != null) {
      syncHistory(currentKbId, []).catch(() => {})
    }
  }

  // Sync on page unload (refresh / close tab) — 使用 keepalive fetch 防止浏览器取消请求
  if (typeof window !== 'undefined') {
    window.addEventListener('beforeunload', doUnloadSync)
  }

  return { messages, setKbId, addMessage, updateLastAssistantMessage, flushSync, clearMessages }
})
