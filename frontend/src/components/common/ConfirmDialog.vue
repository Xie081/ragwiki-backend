<template>
  <Teleport to="body">
    <div v-if="visible" class="confirm-overlay" @click.self="cancel">
      <div class="confirm-dialog">
        <p class="confirm-message" style="white-space:pre-line">{{ message }}</p>
        <div class="confirm-actions">
          <button class="btn-cancel" @click="cancel">取消</button>
          <button class="btn-confirm" @click="confirm">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  message: string
  confirmText?: string
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

function confirm() { emit('confirm') }
function cancel() { emit('cancel') }
</script>

<style scoped>
.confirm-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
  z-index: 9999;
}
.confirm-dialog {
  background: var(--surface);
  border-radius: var(--radius-lg);
  padding: 28px 32px;
  min-width: 340px;
  max-width: 420px;
  box-shadow: var(--shadow-lg);
}
.confirm-message {
  font-size: var(--text-base);
  color: var(--text);
  margin: 0 0 24px;
  line-height: var(--leading);
}
.confirm-actions {
  display: flex; gap: 12px; justify-content: flex-end;
}
.btn-cancel, .btn-confirm {
  padding: 8px 20px;
  border-radius: var(--radius);
  border: none;
  font-size: var(--text-sm);
  font-family: var(--font);
  cursor: pointer;
  transition: background .2s;
}
.btn-cancel {
  background: var(--surface-alt);
  color: var(--text-secondary);
}
.btn-cancel:hover { background: var(--border); }
.btn-confirm {
  background: var(--dusty-rose);
  color: #fff;
}
.btn-confirm:hover { background: #B05A55; }
</style>
