<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getKnowledgeBase, deleteKnowledgeBase } from '@/api/knowledgeBase'
import { getDocuments, uploadDocument, deleteDocument } from '@/api/document'
import { useChatStore } from '@/stores/chat'
import ChatPanel from '@/components/chat/ChatPanel.vue'
import SearchBar from '@/components/knowledge/SearchBar.vue'
import type { KnowledgeBase, Document } from '@/types'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.id)
const chatStore = useChatStore()

const kb = ref<KnowledgeBase | null>(null)
const documents = ref<Document[]>([])
const loading = ref(true)
const uploading = ref(false)

async function load() {
  loading.value = true
  try {
    const [kbRes, docRes] = await Promise.all([
      getKnowledgeBase(kbId),
      getDocuments(kbId)
    ])
    kb.value = kbRes.data
    documents.value = docRes.data
  } finally {
    loading.value = false
  }
}

async function handleUpload(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    await uploadDocument(kbId, file)
    await load()
  } catch (err: any) {
    alert(err.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
    target.value = ''
  }
}

async function handleDeleteDoc(id: number) {
  if (!confirm('确定删除该文档？')) return
  await deleteDocument(id)
  await load()
}

async function handleDeleteKB() {
  if (!confirm('确定删除整个知识库？')) return
  await deleteKnowledgeBase(kbId)
  router.push('/')
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    UPLOADED: '待处理',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status] || status
}

function getStatusClass(status: string): string {
  const map: Record<string, string> = {
    UPLOADED: 'status-pending',
    PROCESSING: 'status-processing',
    COMPLETED: 'status-done',
    FAILED: 'status-fail'
  }
  return map[status] || ''
}

onMounted(load)
onUnmounted(() => chatStore.clearMessages())
</script>

<template>
  <div class="kb-page">
    <header>
      <button class="btn-back" @click="router.push('/')">← 返回</button>
      <div v-if="kb">
        <h1>{{ kb.name }}</h1>
        <p v-if="kb.description">{{ kb.description }}</p>
      </div>
      <div class="header-actions">
        <SearchBar :knowledgeBaseId="kbId" />
        <label class="btn-primary" :class="{ disabled: uploading }">
          {{ uploading ? '上传中...' : '+ 上传文档' }}
          <input type="file" accept=".pdf,.md,.markdown" hidden @change="handleUpload" :disabled="uploading" />
        </label>
        <button class="btn-danger" @click="handleDeleteKB">删除知识库</button>
      </div>
    </header>

    <main>
      <div class="content-layout">
        <div class="doc-section">
          <div v-if="loading" class="empty">加载中...</div>

          <div v-else-if="documents.length === 0" class="empty">
            <p>📄 还没有文档</p>
            <label class="btn-primary">
              上传第一个文档
              <input type="file" accept=".pdf,.md,.markdown" hidden @change="handleUpload" />
            </label>
          </div>

          <div v-else class="doc-table">
        <div class="doc-header">
          <span class="col-name">文档名称</span>
          <span class="col-type">类型</span>
          <span class="col-status">状态</span>
          <span class="col-date">上传时间</span>
          <span class="col-action">操作</span>
        </div>
        <div v-for="doc in documents" :key="doc.id" class="doc-row">
          <span class="col-name" :title="doc.originalName">{{ doc.originalName }}</span>
          <span class="col-type">
            <span class="badge" :class="doc.fileType.toLowerCase()">{{ doc.fileType }}</span>
          </span>
          <span class="col-status">
            <span class="status-badge" :class="getStatusClass(doc.status)">
              {{ getStatusText(doc.status) }}
            </span>
          </span>
          <span class="col-date">{{ new Date(doc.createdAt).toLocaleString() }}</span>
          <span class="col-action">
            <button class="btn-sm" @click="handleDeleteDoc(doc.id)">删除</button>
          </span>
        </div>
      </div>
        </div>

        <div class="chat-section">
          <ChatPanel :knowledgeBaseId="kbId" />
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.kb-page { min-height: 100vh; background: #f0f2f5; }
header {
  padding: 20px 32px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
header h1 { font-size: 1.4rem; margin: 4px 0; }
header p { color: #888; font-size: 0.9rem; margin: 0; }
.header-actions { display: flex; gap: 12px; margin-top: 12px; }

.btn-back {
  padding: 6px 12px;
  background: transparent;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  margin-bottom: 8px;
}
.btn-primary {
  display: inline-block;
  padding: 8px 20px;
  background: #4f46e5;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
}
.btn-primary.disabled { opacity: 0.5; pointer-events: none; }
.btn-danger {
  padding: 8px 16px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.btn-sm {
  padding: 4px 12px;
  background: transparent;
  color: #ef4444;
  border: 1px solid #fecaca;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.8rem;
}

main { padding: 24px 32px; max-width: 1400px; margin: 0 auto; }
.content-layout {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 24px;
  align-items: start;
}
@media (max-width: 1100px) {
  .content-layout { grid-template-columns: 1fr; }
}
.doc-section { min-width: 0; }
.chat-section { position: sticky; top: 24px; }

.doc-table {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow: hidden;
}
.doc-header, .doc-row {
  display: grid;
  grid-template-columns: 2fr 80px 80px 1fr 80px;
  gap: 16px;
  padding: 14px 20px;
  align-items: center;
  font-size: 0.9rem;
}
.doc-header {
  background: #f9fafb;
  color: #888;
  font-weight: 600;
  border-bottom: 1px solid #eee;
}
.doc-row { border-bottom: 1px solid #f3f4f6; }
.doc-row:last-child { border-bottom: none; }
.doc-row:hover { background: #fafbff; }
.col-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}
.badge.pdf { background: #fee2e2; color: #dc2626; }
.badge.markdown { background: #dbeafe; color: #2563eb; }

.empty {
  text-align: center;
  padding: 80px 20px;
  color: #999;
}
.empty p { font-size: 1.1rem; margin-bottom: 16px; }

.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 0.75rem;
  font-weight: 600;
}
.status-pending { background: #fef3c7; color: #d97706; }
.status-processing { background: #dbeafe; color: #2563eb; }
.status-done { background: #d1fae5; color: #059669; }
.status-fail { background: #fee2e2; color: #dc2626; }
</style>
