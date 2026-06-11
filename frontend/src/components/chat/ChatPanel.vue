<script setup lang="ts">
import { ref, nextTick, watch, onUnmounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { streamQuestion } from '@/api/chat'
import ChatMessage from './ChatMessage.vue'
import ChatInput from './ChatInput.vue'
import type { CitationSource } from '@/types'

const props = defineProps<{ knowledgeBaseId: number }>()

const chatStore = useChatStore()
const isStreaming = ref(false)
const sources = ref<CitationSource[]>([])
const messagesContainer = ref<HTMLElement>()

let abortController: AbortController | null = null

async function handleSend(question: string) {
  const userMsg = {
    id: Date.now().toString(),
    role: 'user' as const,
    content: question,
    timestamp: new Date().toISOString()
  }
  chatStore.addMessage(userMsg)

  const assistantId = (Date.now() + 1).toString()
  chatStore.addMessage({
    id: assistantId,
    role: 'assistant',
    content: '',
    timestamp: new Date().toISOString()
  })

  isStreaming.value = true
  sources.value = []

  const recentMessages = chatStore.messages.slice(-8, -2)
  const history = recentMessages
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .map(m => ({ id: m.id, role: m.role, content: m.content, timestamp: m.timestamp }))

  abortController = streamQuestion(
    props.knowledgeBaseId, question,
    (token) => {
      const last = chatStore.messages[chatStore.messages.length - 1]
      if (last) chatStore.updateLastAssistantMessage(last.content + token)
      scrollToBottom()
    },
    (srcs) => { sources.value = srcs },
    () => { isStreaming.value = false },
    () => { isStreaming.value = false; chatStore.flushSync() },
    history
  )
}

function handleStop() {
  abortController?.abort()
  isStreaming.value = false
  // 确保手动停止时也同步部分回复到后端
  chatStore.flushSync()
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(() => chatStore.messages.length, scrollToBottom)

onUnmounted(() => {
  abortController?.abort()
})
</script>

<template>
  <div class="chat-panel">
    <div class="chat-topbar">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><circle cx="12" cy="12" r="9"/><path d="M8 12h8M12 8v8"/></svg>
      <span>AI 问答</span>
      <span v-if="isStreaming" class="streaming-badge">生成中</span>
    </div>

    <div ref="messagesContainer" class="chat-body">
      <div v-if="chatStore.messages.length === 0" class="chat-welcome">
        <div class="welcome-icon">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round"><circle cx="12" cy="12" r="9"/><path d="M8 12h8M12 8v12"/></svg>
        </div>
        <p>向 AI 提问关于知识库文档的任何问题</p>
        <span>AI 会根据文档内容给出准确回答</span>
      </div>
      <ChatMessage
        v-for="msg in chatStore.messages"
        :key="msg.id"
        :message="msg"
        :sources="msg.role === 'assistant' ? sources : undefined"
      />
    </div>

    <ChatInput :disabled="false" :isStreaming="isStreaming" @send="handleSend" @stop="handleStop" />
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  overflow: hidden;
}
.chat-topbar {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: var(--text-sm);
  color: var(--text);
  border-bottom: 1px solid var(--border-light);
  background: var(--surface);
}
.streaming-badge {
  margin-left: auto;
  padding: 3px 10px;
  border-radius: 12px;
  background: var(--sage-light);
  color: var(--sage);
  font-size: var(--text-xs);
  font-weight: 500;
  animation: pulse 2s infinite;
}
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  scroll-behavior: smooth;
}
.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: var(--text-muted);
  gap: 8px;
}
.welcome-icon {
  color: var(--sage);
  opacity: 0.5;
  margin-bottom: 8px;
}
.chat-welcome p { font-size: var(--text-base); color: var(--text-muted); }
.chat-welcome span { font-size: var(--text-sm); }
</style>
