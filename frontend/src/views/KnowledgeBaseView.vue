<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getKnowledgeBase, deleteKnowledgeBase } from '@/api/knowledgeBase'
import { getDocuments, uploadDocument, deleteDocument } from '@/api/document'
import { useChatStore } from '@/stores/chat'
import { useToast } from '@/composables/useToast'
import ChatPanel from '@/components/chat/ChatPanel.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import type { KnowledgeBase, Document } from '@/types'
import { getStatusText, getStatusClass } from '@/utils/status'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.id)
const chatStore = useChatStore()
const toast = useToast()

const kb = ref<KnowledgeBase | null>(null)
const documents = ref<Document[]>([])
const loading = ref(true)
const uploading = ref(false)
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loadingMore = ref(false)
const PAGE_SIZE = 20

// Confirm dialog state
const showConfirm = ref(false)
const confirmMessage = ref('')
const pendingDeleteId = ref<number | null>(null)
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
  pendingDeleteId.value = null
}

async function load() {
  loading.value = true
  currentPage.value = 0
  try {
    const [kbRes, docRes] = await Promise.all([
      getKnowledgeBase(kbId),
      getDocuments(kbId, 0, PAGE_SIZE)
    ])
    kb.value = kbRes.data
    documents.value = docRes.data.data
    totalPages.value = docRes.data.totalPages
    totalElements.value = docRes.data.totalElements
  } finally { loading.value = false }
}

async function loadMore() {
  if (loadingMore.value || currentPage.value >= totalPages.value - 1) return
  loadingMore.value = true
  try {
    const next = currentPage.value + 1
    const { data } = await getDocuments(kbId, next, PAGE_SIZE)
    documents.value.push(...data.data)
    currentPage.value = next
    totalPages.value = data.totalPages
  } finally { loadingMore.value = false }
}

async function handleUpload(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  uploading.value = true
  try { await uploadDocument(kbId, file); await load() }
  catch (err: any) { toast.error(err.response?.data?.message || '上传失败') }
  finally { uploading.value = false; target.value = '' }
}

function goToDoc(id: number) { router.push(`/document/${id}`) }

function handleDeleteDoc(id: number) {
  pendingDeleteId.value = id
  showConfirmDialog('确定删除该文档？', async () => {
    const docId = pendingDeleteId.value
    if (docId === null) return
    pendingDeleteId.value = null
    // Optimistic remove
    const idx = documents.value.findIndex(d => d.id === docId)
    if (idx !== -1) documents.value.splice(idx, 1)
    try {
      await deleteDocument(docId)
      toast.success('文档已删除')
      await load()
    } catch (err: any) {
      toast.error(err.response?.data?.message || '删除失败')
      await load()
    }
  })
}

function handleDeleteKB() {
  showConfirmDialog('确定删除整个知识库？\n该操作不可恢复。', async () => {
    await deleteKnowledgeBase(kbId)
    router.push('/app')
  })
}

onMounted(() => { chatStore.setKbId(kbId); load() })
onUnmounted(() => chatStore.clearMessages())
</script>

<template>
  <div class="kb-page">
    <!-- Topbar -->
    <header class="topbar">
      <div class="topbar-left">
        <button class="btn-back" @click="router.push('/app')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <div v-if="kb">
          <h1>{{ kb.name }}</h1>
          <p v-if="kb.description" class="kb-desc">{{ kb.description }}</p>
        </div>
      </div>
      <div class="topbar-right">
        <label class="btn-upload" :class="{ disabled: uploading }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          {{ uploading ? '上传中...' : '上传文档' }}
          <input type="file" accept=".pdf,.md,.markdown,.txt,.docx,.html,.htm" hidden @change="handleUpload" :disabled="uploading" />
        </label>
        <button class="btn-ghost-danger" @click="handleDeleteKB">删除知识库</button>
      </div>
    </header>

    <main class="kb-main">
      <div class="kb-layout">
        <!-- Documents section -->
        <div class="doc-section">
          <div class="section-header">
            <h2>文档</h2>
            <span v-if="totalElements" class="count-badge">{{ totalElements }}</span>
          </div>

          <div v-if="loading" class="state-box"><div class="loader" /><p>加载中...</p></div>

          <div v-else-if="documents.length === 0" class="state-box">
            <div class="empty-icon">📄</div>
            <h3>还没有文档</h3>
            <p>上传 PDF、Markdown、TXT、DOCX 或 HTML 文档开始构建知识库</p>
            <label class="btn-upload">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
              上传第一个文档
              <input type="file" accept=".pdf,.md,.markdown,.txt,.docx,.html,.htm" hidden @change="handleUpload" />
            </label>
          </div>

          <div v-else class="doc-list">
            <div class="doc-list-header">
              <span class="col-name">文档名称</span>
              <span class="col-type">类型</span>
              <span class="col-status">状态</span>
              <span class="col-date">上传时间</span>
              <span class="col-action"></span>
            </div>
            <div v-for="doc in documents" :key="doc.id" class="doc-row">
              <span class="col-name doc-name" :title="doc.originalName" @click="goToDoc(doc.id)">{{ doc.originalName }}</span>
              <span class="col-type"><span class="type-badge" :class="doc.fileType.toLowerCase()">{{ doc.fileType }}</span></span>
              <span class="col-status"><span class="status-badge" :class="getStatusClass(doc.status)">{{ getStatusText(doc.status) }}</span></span>
              <span class="col-date">{{ new Date(doc.createdAt).toLocaleString('zh-CN') }}</span>
              <span class="col-action"><button class="btn-row-del" @click="handleDeleteDoc(doc.id)">删除</button></span>
            </div>
            <div v-if="totalElements" class="doc-footer">
              <span class="count-text">共 {{ totalElements }} 个文档</span>
              <button v-if="currentPage < totalPages - 1" class="btn-more" :disabled="loadingMore" @click="loadMore">
                {{ loadingMore ? '加载中...' : '加载更多' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Chat section -->
        <div class="chat-section">
          <ChatPanel :knowledgeBaseId="kbId" />
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
.kb-page { min-height: 100vh; background: var(--bg); }

/* ── Topbar ── */
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 32px;
  background: var(--surface);
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap; gap: 12px;
}
.topbar-left { display: flex; align-items: center; gap: 14px; }
.topbar-left h1 { font-size: var(--text-lg); margin: 0; }
.kb-desc { font-size: var(--text-sm); color: var(--text-muted); margin-top: 2px; }
.topbar-right { display: flex; gap: 10px; align-items: center; }

.btn-back {
  width: 36px; height: 36px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.btn-back:hover { background: var(--surface-alt); color: var(--text); }

.btn-upload {
  padding: 9px 18px;
  border: none;
  border-radius: var(--radius);
  background: linear-gradient(135deg, var(--sage), #7a8f74);
  color: #fff;
  font-size: var(--text-sm); font-weight: 600; font-family: var(--font);
  cursor: pointer;
  display: inline-flex; align-items: center; gap: 6px;
  transition: transform 0.15s var(--ease), box-shadow 0.15s var(--ease);
}
.btn-upload:hover:not(.disabled) { transform: translateY(-1px); box-shadow: var(--shadow-md); }
.btn-upload.disabled { opacity: 0.5; pointer-events: none; }

.btn-ghost-danger {
  padding: 8px 16px;
  border: 1.5px solid #edd5ce;
  border-radius: var(--radius);
  background: transparent;
  color: var(--dusty-rose);
  font-size: var(--text-sm); font-family: var(--font); cursor: pointer;
  transition: all 0.2s;
}
.btn-ghost-danger:hover { background: var(--rose-light); }

/* ── Main ── */
.kb-main { max-width: 1400px; margin: 0 auto; padding: 28px 32px; }
.kb-layout {
  display: grid;
  grid-template-columns: 1fr 440px;
  gap: 24px;
  align-items: start;
}
@media (max-width: 1100px) { .kb-layout { grid-template-columns: 1fr; } }
.chat-section { position: sticky; top: 24px; max-height: calc(100vh - 120px); }
.chat-section > * { max-height: inherit; min-height: 500px; }

/* ── Section ── */
.section-header {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 16px;
}
.section-header h2 { font-size: var(--text-lg); }
.count-badge {
  padding: 2px 10px;
  border-radius: 10px;
  background: var(--sage-light);
  color: var(--sage);
  font-size: var(--text-xs); font-weight: 600;
}

/* ── States ── */
.state-box {
  text-align: center; padding: 80px 20px;
  animation: fadeInUp 0.5s var(--ease);
}
.state-box h3 { margin-bottom: 6px; }
.state-box p { margin-bottom: 20px; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.loader {
  width: 28px; height: 28px;
  border: 3px solid var(--border);
  border-top-color: var(--sage);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 14px;
}

/* ── Doc list ── */
.doc-list {
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  overflow: hidden;
}
.doc-list-header, .doc-row {
  display: grid;
  grid-template-columns: 2fr 80px 80px 1fr 70px;
  gap: 12px;
  padding: 14px 20px;
  align-items: center;
  font-size: var(--text-sm);
}
.doc-list-header {
  background: var(--surface-alt);
  color: var(--text-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  font-size: var(--text-xs);
  border-bottom: 1px solid var(--border);
}
.doc-row { border-bottom: 1px solid var(--border-light); transition: background 0.15s; }
.doc-row:last-child { border-bottom: none; }
.doc-row:hover { background: var(--sage-bg); }
.col-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-name { color: var(--text); cursor: pointer; font-weight: 500; }
.doc-name:hover { color: var(--sage); text-decoration: underline; }

.type-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: uppercase;
}
.type-badge.pdf      { background: var(--rose-light); color: var(--dusty-rose); }
.type-badge.markdown { background: var(--blue-light); color: var(--dusty-blue); }
.type-badge.txt      { background: var(--sage-light); color: var(--sage); }
.type-badge.docx     { background: #e8daef; color: #7b5ea7; }
.type-badge.html     { background: #fdebd0; color: #b9770e; }

.status-badge {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: var(--text-xs);
  font-weight: 600;
}
.badge-pending { background: #fef7e0; color: #9d8100; }
.badge-processing { background: var(--blue-light); color: var(--dusty-blue); }
.badge-done { background: var(--sage-light); color: var(--sage); }
.badge-fail { background: var(--rose-light); color: var(--error); }

.col-date { color: var(--text-muted); font-size: var(--text-xs); }
.btn-row-del {
  padding: 4px 12px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: var(--text-xs);
  font-family: var(--font);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}
.btn-row-del:hover { background: var(--rose-light); color: var(--error); }

.doc-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 20px;
  background: var(--surface-alt);
  border-top: 1px solid var(--border-light);
}
.count-text { font-size: var(--text-xs); color: var(--text-muted); }
.btn-more {
  padding: 6px 16px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--text-secondary);
  font-size: var(--text-xs); font-family: var(--font); cursor: pointer;
  transition: all 0.2s;
}
.btn-more:hover { background: var(--surface-alt); color: var(--text); }
.btn-more:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
