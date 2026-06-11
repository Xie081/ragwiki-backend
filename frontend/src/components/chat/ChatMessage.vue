<script setup lang="ts">
import { computed, ref } from 'vue'
import { marked } from 'marked'
import { useToast } from '@/composables/useToast'
import type { ChatMessage as ChatMessageType, CitationSource } from '@/types'

const props = defineProps<{
  message: ChatMessageType
  sources?: CitationSource[]
}>()

const toast = useToast()
const copied = ref(false)

const isUser = computed(() => props.message.role === 'user')

marked.setOptions({ breaks: true, gfm: true })

function renderMarkdown(text: string): string {
  if (!text) return ''
  const sanitized = text.replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
  return marked.parse(sanitized) as string
}

async function copyContent() {
  try {
    await navigator.clipboard.writeText(props.message.content)
    copied.value = true
    toast.success('已复制到剪贴板')
    setTimeout(() => { copied.value = false }, 2000)
  } catch { toast.error('复制失败') }
}
</script>

<template>
  <div class="msg" :class="{ user: isUser, assistant: !isUser }">
    <!-- Avatar -->
    <div class="msg-avatar">
      <template v-if="isUser">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
      </template>
      <template v-else>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><rect x="3" y="3" width="18" height="14" rx="3"/><circle cx="8" cy="17" r="1.5" fill="currentColor" stroke="none"/><circle cx="16" cy="17" r="1.5" fill="currentColor" stroke="none"/></svg>
      </template>
    </div>

    <!-- Content -->
    <div class="msg-content">
      <!-- User text -->
      <div v-if="isUser" class="bubble user-bubble">{{ message.content }}</div>

      <!-- Assistant markdown -->
      <div v-else class="bubble assistant-bubble">
        <div v-if="!message.content" class="thinking">
          <span class="dot" />
          <span class="dot" />
          <span class="dot" />
        </div>
        <div v-else class="markdown-body" v-html="renderMarkdown(message.content)" />
      </div>

      <!-- Copy button -->
      <div v-if="message.role === 'assistant' && message.content" class="msg-meta">
        <button class="btn-copy" :class="{ copied }" @click="copyContent">
          <svg v-if="!copied" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
          <span>{{ copied ? '已复制' : '复制' }}</span>
        </button>
      </div>

      <!-- Sources -->
      <div v-if="sources && sources.length > 0 && message.role === 'assistant'" class="sources-box">
        <div class="sources-title">参考来源</div>
        <div v-for="(src, i) in sources" :key="i" class="source-row">
          <span class="source-dot">·</span>
          <span class="source-snippet">{{ src.snippet }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.msg {
  display: flex;
  gap: 14px;
  margin-bottom: 28px;
  animation: fadeIn 0.35s var(--ease);
}
.msg.user { flex-direction: row-reverse; }

/* ── Avatar ── */
.msg-avatar {
  width: 38px; height: 38px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user .msg-avatar { background: var(--sage-bg); color: var(--sage); }
.assistant .msg-avatar { background: var(--blue-bg); color: var(--dusty-blue); }

/* ── Bubbles ── */
.msg-content { max-width: 78%; min-width: 0; }
.user .msg-content { display: flex; flex-direction: column; align-items: flex-end; }

.bubble {
  padding: 14px 20px;
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  line-height: var(--leading);
  word-break: break-word;
}
.user-bubble {
  background: var(--sage);
  color: #fff;
  border-bottom-right-radius: var(--radius-sm);
}
.assistant-bubble {
  background: var(--surface);
  border: 1px solid var(--border);
  border-bottom-left-radius: var(--radius-sm);
}

/* ── Thinking dots ── */
.thinking { display: flex; gap: 5px; padding: 4px 0; }
.dot {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: var(--text-muted);
  animation: pulse 1.4s infinite;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

/* ── Markdown ── */
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) {
  margin: 12px 0 6px;
  font-size: 1.1em;
  font-weight: 600;
}
.markdown-body :deep(h1) { font-size: 1.3em; }
.markdown-body :deep(p) { margin: 6px 0; color: var(--text); }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 6px 0; padding-left: 22px; }
.markdown-body :deep(li) { margin: 3px 0; color: var(--text); }
.markdown-body :deep(code) {
  background: var(--surface-alt);
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 0.88em;
  font-family: var(--font-mono);
  color: var(--dusty-rose);
}
.markdown-body :deep(pre) {
  background: #1e1e1e;
  color: #e0e0e0;
  padding: 16px 20px;
  border-radius: var(--radius);
  overflow-x: auto;
  margin: 12px 0;
}
.markdown-body :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
  font-size: 0.85em;
}
.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--sage);
  padding-left: 14px;
  color: var(--text-secondary);
  margin: 10px 0;
}
.markdown-body :deep(table) {
  border-collapse: collapse; width: 100%; margin: 10px 0;
  font-size: 0.9em;
}
.markdown-body :deep(th), .markdown-body :deep(td) {
  border: 1px solid var(--border); padding: 8px 12px; text-align: left;
}
.markdown-body :deep(th) { background: var(--surface-alt); font-weight: 600; }
.markdown-body :deep(strong) { font-weight: 600; }
.markdown-body :deep(a) { color: var(--dusty-blue); }

/* ── Meta ── */
.msg-meta { margin-top: 6px; }
.btn-copy {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 12px;
  font-size: var(--text-xs);
  font-family: var(--font);
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-muted);
  transition: all 0.2s var(--ease);
}
.btn-copy:hover { color: var(--sage); border-color: var(--sage); }
.btn-copy.copied { color: var(--success); border-color: #b8d4b2; background: var(--sage-bg); }

/* ── Sources ── */
.sources-box {
  margin-top: 12px;
  padding: 16px;
  background: var(--surface-alt);
  border-radius: var(--radius);
  border: 1px solid var(--border-light);
}
.sources-title {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 10px;
}
.source-row {
  display: flex; gap: 6px;
  padding: 3px 0;
  border-bottom: 1px solid var(--border-light);
  font-size: var(--text-sm);
  align-items: baseline;
}
.source-row:last-child { border-bottom: none; }
.source-dot { flex-shrink: 0; color: var(--text-muted); }
.source-snippet {
  color: var(--text-muted);
  font-size: var(--text-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
