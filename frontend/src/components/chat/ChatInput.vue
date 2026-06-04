<script setup lang="ts">
import { ref, computed } from 'vue'

const MAX_LENGTH = 2000

const props = defineProps<{
  disabled: boolean
  isStreaming: boolean
}>()

const emit = defineEmits<{
  send: [question: string]
  stop: []
}>()

const input = ref('')

const charCount = computed(() => input.value.length)
const isOverLimit = computed(() => charCount.value > MAX_LENGTH)

function handleSubmit() {
  const text = input.value.trim()
  if (!text || props.disabled || isOverLimit.value) return
  emit('send', text)
  input.value = ''
}
</script>

<template>
  <div class="chat-input">
    <textarea
      v-model="input"
      placeholder="输入你的问题..."
      :disabled="disabled"
      rows="2"
      @keydown.enter.exact.prevent="handleSubmit"
    />
    <div class="input-footer">
      <span class="char-count" :class="{ 'over-limit': isOverLimit }">
        {{ charCount }}/{{ MAX_LENGTH }}
      </span>
      <div class="actions">
        <button v-if="isStreaming" class="btn-stop" @click="emit('stop')">⏹ 停止</button>
        <button v-else class="btn-send" :disabled="!input.trim() || disabled || isOverLimit" @click="handleSubmit">
          ➤ 发送
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #eee;
  background: #fafafa;
}
textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.9rem;
  resize: none;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}
textarea:focus { border-color: #4f46e5; }
.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.char-count {
  font-size: 0.75rem;
  color: #aaa;
}
.char-count.over-limit { color: #ef4444; font-weight: 600; }
.actions {
  display: flex;
  gap: 8px;
}
.btn-send {
  padding: 6px 20px;
  background: #4f46e5;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.85rem;
}
.btn-send:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-stop {
  padding: 6px 20px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.85rem;
}
</style>
