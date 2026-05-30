<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  disabled: boolean
  isStreaming: boolean
}>()

const emit = defineEmits<{
  send: [question: string]
  stop: []
}>()

const input = ref('')

function handleSubmit() {
  const text = input.value.trim()
  if (!text || props.disabled) return
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
    <div class="actions">
      <button v-if="isStreaming" class="btn-stop" @click="emit('stop')">⏹ 停止</button>
      <button v-else class="btn-send" :disabled="!input.trim() || disabled" @click="handleSubmit">
        ➤ 发送
      </button>
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
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
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
