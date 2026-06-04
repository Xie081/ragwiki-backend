<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
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

  // Build history from recent messages (last 3 rounds = 6 messages)
  const recentMessages = chatStore.messages.slice(-6)
  const history = recentMessages
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .map(m => ({ id: m.id, role: m.role, content: m.content, timestamp: m.timestamp }))

  abortController = streamQuestion(
    props.knowledgeBaseId,
    question,
    (token) => {
      chatStore.updateLastAssistantMessage(
        chatStore.messages[chatStore.messages.length - 1].content + token
      )
      scrollToBottom()
    },
    (srcs) => {
      sources.value = srcs
    },
    () => {
      isStreaming.value = false
    },
    () => {
      isStreaming.value = false
    },
    history
  )
}

function handleStop() {
  abortController?.abort()
  isStreaming.value = false
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(() => chatStore.messages.length, scrollToBottom)
</script>

<template>
  <div class="chat-panel">
    <div class="chat-header">💬 AI 问答</div>

    <div ref="messagesContainer" class="chat-messages">
      <div v-if="chatStore.messages.length === 0" class="chat-empty">
        <p>👋 向 AI 提问关于知识库文档的任何问题</p>
      </div>
      <ChatMessage
        v-for="msg in chatStore.messages"
        :key="msg.id"
        :message="msg"
        :sources="msg.role === 'assistant' ? sources : undefined"
      />
      <div v-if="isStreaming" class="typing-indicator">●</div>
    </div>

    <ChatInput
      :disabled="isStreaming"
      :isStreaming="isStreaming"
      @send="handleSend"
      @stop="handleStop"
    />
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow: hidden;
}
.chat-header {
  padding: 14px 20px;
  font-weight: 600;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 300px;
  max-height: 60vh;
}
.chat-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #999;
  font-size: 0.95rem;
}
.typing-indicator {
  color: #4f46e5;
  font-size: 0.8rem;
  padding: 4px 8px;
  animation: blink 0.8s infinite;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
