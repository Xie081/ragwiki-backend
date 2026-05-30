<script setup lang="ts">
import { computed } from 'vue'
import type { ChatMessage as ChatMessageType, CitationSource } from '@/types'

const props = defineProps<{
  message: ChatMessageType
  sources?: CitationSource[]
}>()

const isUser = computed(() => props.message.role === 'user')
</script>

<template>
  <div class="message" :class="{ user: isUser, assistant: !isUser }">
    <div class="avatar">{{ isUser ? '👤' : '🤖' }}</div>
    <div class="content">
      <div class="text" v-html="message.content || (isUser ? '' : '思考中...')" />
      <div v-if="sources && sources.length > 0 && message.role === 'assistant'" class="sources">
        <div class="sources-title">📎 参考来源：</div>
        <div v-for="(src, i) in sources" :key="i" class="source-item">
          <span class="source-doc">{{ src.documentTitle }}</span>
          <span class="source-snippet">{{ src.snippet }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.message.user { flex-direction: row-reverse; }
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}
.user .avatar { background: #e0e7ff; }
.assistant .avatar { background: #f0fdf4; }
.content { max-width: 80%; }
.user .content { text-align: right; }
.text {
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 0.9rem;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.user .text { background: #4f46e5; color: #fff; border-bottom-right-radius: 4px; }
.assistant .text { background: #f3f4f6; color: #333; border-bottom-left-radius: 4px; }

.sources {
  margin-top: 8px;
  padding: 12px;
  background: #fffbeb;
  border-radius: 8px;
  font-size: 0.8rem;
}
.sources-title { font-weight: 600; margin-bottom: 6px; color: #92400e; }
.source-item {
  padding: 4px 0;
  border-bottom: 1px dashed #fde68a;
}
.source-item:last-child { border-bottom: none; }
.source-doc { color: #b45309; font-weight: 500; display: block; }
.source-snippet { color: #78716c; font-size: 0.75rem; }
</style>
