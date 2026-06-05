<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase } from '@/api/knowledgeBase'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import type { KnowledgeBase } from '@/types'

const authStore = useAuthStore()
const router = useRouter()

const knowledgeBases = ref<KnowledgeBase[]>([])
const loading = ref(true)
const showCreate = ref(false)
const newName = ref('')
const newDesc = ref('')

// Confirm dialog state
const showConfirm = ref(false)
const confirmMessage = ref('')
let confirmAction: (() => Promise<void>) | null = null

function showConfirmDialog(message: string, action: () => Promise<void>) {
  confirmMessage.value = message
  confirmAction = action
  showConfirm.value = true
}

async function onConfirm() {
  showConfirm.value = false
  const action = confirmAction
  confirmAction = null
  if (action) await action()
}

function onCancel() {
  showConfirm.value = false
  confirmAction = null
}

async function loadKBs() {
  loading.value = true
  try {
    const { data } = await getKnowledgeBases()
    knowledgeBases.value = data
  } finally { loading.value = false }
}

async function handleCreate() {
  if (!newName.value.trim()) return
  await createKnowledgeBase({ name: newName.value, description: newDesc.value })
  newName.value = ''
  newDesc.value = ''
  showCreate.value = false
  await loadKBs()
}

async function handleDelete(id: number) {
  showConfirmDialog('确定删除该知识库及其所有文档？\n该操作不可恢复。', async () => {
    await deleteKnowledgeBase(id)
    await loadKBs()
  })
}

function goToKB(id: number) { router.push(`/knowledge-base/${id}`) }
onMounted(loadKBs)
</script>

<template>
  <div class="dashboard">
    <!-- Header -->
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand">
          <span class="brand-mark">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
              <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
            </svg>
          </span>
          <span class="brand-text">智能知识库</span>
        </div>
        <div class="topbar-actions">
          <button class="btn-primary" @click="showCreate = true">
            <span class="btn-icon">+</span> 新建知识库
          </button>
          <button class="btn-ghost" @click="authStore.logout()">退出</button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <!-- Create modal -->
      <Teleport to="body">
        <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
          <div class="modal">
            <h3>新建知识库</h3>
            <input
              v-model="newName"
              placeholder="知识库名称"
              autofocus
              @keyup.enter="handleCreate"
            />
            <textarea v-model="newDesc" placeholder="描述（选填）" rows="3" />
            <div class="modal-btns">
              <button class="btn-ghost" @click="showCreate = false">取消</button>
              <button class="btn-primary" @click="handleCreate" :disabled="!newName.trim()">创建</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- Loading -->
      <div v-if="loading" class="state-box">
        <div class="loader" />
        <p>加载中...</p>
      </div>

      <!-- Empty -->
      <div v-else-if="knowledgeBases.length === 0" class="state-box">
        <div class="empty-icon">📚</div>
        <h2>还没有知识库</h2>
        <p>创建你的第一个知识库，开始上传文档并与 AI 对话</p>
        <button class="btn-primary" @click="showCreate = true">创建知识库</button>
      </div>

      <!-- KB Grid -->
      <div v-else class="kb-grid">
        <div
          v-for="(kb, i) in knowledgeBases"
          :key="kb.id"
          class="kb-card"
          :style="{ animationDelay: `${i * 0.06}s` }"
          @click="goToKB(kb.id)"
        >
          <div class="kb-card-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
              <line x1="8" y1="7" x2="16" y2="7"/>
              <line x1="8" y1="11" x2="14" y2="11"/>
            </svg>
          </div>
          <div class="kb-card-body">
            <h3>{{ kb.name }}</h3>
            <p>{{ kb.description || '暂无描述' }}</p>
            <span class="kb-date">{{ new Date(kb.createdAt).toLocaleDateString('zh-CN', { year:'numeric', month:'long', day:'numeric' }) }}</span>
          </div>
          <button class="kb-delete" @click.stop="handleDelete(kb.id)" title="删除">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </div>
    </main>
  </div>

  <ConfirmDialog
    :visible="showConfirm"
    :message="confirmMessage"
    confirmText="删除"
    @confirm="onConfirm"
    @cancel="onCancel"
  />
</template>

<style scoped>
.dashboard { min-height: 100vh; background: var(--bg); }

/* ── Topbar ── */
.topbar {
  position: sticky; top: 0; z-index: 10;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-light);
}
.topbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 32px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand { display: flex; align-items: center; gap: 10px; }
.brand-mark {
  width: 36px; height: 36px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--sage), var(--dusty-blue));
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 16px;
}
.brand-text { font-weight: 600; font-size: var(--text-lg); color: var(--text); }
.topbar-actions { display: flex; gap: 12px; align-items: center; }

/* ── Buttons ── */
.btn-primary {
  padding: 10px 22px;
  border: none;
  border-radius: var(--radius);
  background: linear-gradient(135deg, var(--sage), #7a8f74);
  color: #fff;
  font-size: var(--text-sm);
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  display: inline-flex; align-items: center; gap: 6px;
  transition: transform 0.15s var(--ease), box-shadow 0.15s var(--ease);
}
.btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: var(--shadow-md); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-icon { font-size: 18px; line-height: 1; }

.btn-ghost {
  padding: 8px 16px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.2s var(--ease);
}
.btn-ghost:hover { background: var(--surface-alt); color: var(--text); }

/* ── Main ── */
.main-content { max-width: 1200px; margin: 0 auto; padding: 40px 32px; }

/* ── State ── */
.state-box {
  text-align: center;
  padding: 100px 20px;
  animation: fadeInUp 0.5s var(--ease);
}
.state-box h2 { margin-bottom: 8px; }
.state-box p { margin-bottom: 24px; }
.empty-icon { font-size: 56px; margin-bottom: 16px; }
.loader {
  width: 32px; height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--sage);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

/* ── Grid ── */
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}
.kb-card {
  position: relative;
  display: flex;
  gap: 18px;
  padding: 28px;
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all 0.25s var(--ease);
  animation: fadeIn 0.5s var(--ease) both;
}
.kb-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: var(--sage-light);
}
.kb-card-icon {
  flex-shrink: 0;
  width: 56px; height: 56px;
  border-radius: var(--radius);
  background: var(--sage-bg);
  color: var(--sage);
  display: flex;
  align-items: center;
  justify-content: center;
}
.kb-card-body { flex: 1; min-width: 0; }
.kb-card-body h3 {
  font-size: var(--text-base);
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kb-card-body p {
  font-size: var(--text-sm);
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}
.kb-date { font-size: var(--text-xs); color: var(--warm-gray); }

.kb-delete {
  position: absolute; top: 14px; right: 14px;
  width: 30px; height: 30px;
  border: none; border-radius: 50%;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  opacity: 0;
  transition: all 0.2s var(--ease);
}
.kb-card:hover .kb-delete { opacity: 1; }
.kb-delete:hover { background: var(--rose-light); color: var(--error); }

/* ── Modal ── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
}
.modal {
  background: var(--surface);
  border-radius: var(--radius-lg);
  padding: 36px;
  width: 100%;
  max-width: 460px;
  box-shadow: var(--shadow-lg);
  animation: fadeIn 0.3s var(--ease);
}
.modal h3 { margin-bottom: 20px; font-size: var(--text-lg); }
.modal input, .modal textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius);
  font-size: var(--text-base);
  font-family: var(--font);
  background: var(--surface-alt);
  color: var(--text);
  outline: none;
  margin-bottom: 12px;
  transition: border-color 0.2s var(--ease);
}
.modal input:focus, .modal textarea:focus { border-color: var(--sage); }
.modal textarea { resize: vertical; }
.modal-btns { display: flex; gap: 12px; justify-content: flex-end; margin-top: 8px; }
</style>
