<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDocumentDetail } from '@/api/document'
import { useToast } from '@/composables/useToast'
import type { Document } from '@/types'
import type { DocumentDetailResponse } from '@/api/document'

const route = useRoute()
const router = useRouter()
const docId = Number(route.params.id)
const toast = useToast()

const loading = ref(true)
const detail = ref<DocumentDetailResponse | null>(null)
const doc = ref<Document | null>(null)

function getStatusText(status: string): string {
  return { UPLOADED:'待处理', PROCESSING:'处理中', COMPLETED:'已完成', FAILED:'失败' }[status] || status
}
function getStatusClass(status: string): string {
  return { UPLOADED:'badge-pending', PROCESSING:'badge-processing', COMPLETED:'badge-done', FAILED:'badge-fail' }[status] || ''
}

function formatFileSize(bytes: number | null): string {
  if (!bytes) return '未知'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await getDocumentDetail(docId)
    detail.value = data
    doc.value = data.document
  } catch (err: any) {
    toast.error(err.response?.data?.message || '加载文档失败')
    router.back()
  } finally { loading.value = false }
})
</script>

<template>
  <div class="doc-page">
    <header class="topbar">
      <div class="topbar-inner">
        <button class="btn-back" @click="router.back()">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
          返回
        </button>
        <div v-if="doc" class="doc-meta">
          <h1>{{ doc.originalName }}</h1>
          <div class="meta-tags">
            <span class="type-badge" :class="doc.fileType.toLowerCase()">{{ doc.fileType }}</span>
            <span class="status-badge" :class="getStatusClass(doc.status)">{{ getStatusText(doc.status) }}</span>
            <span class="meta-info">{{ formatFileSize((doc as any).fileSize) }}</span>
            <span class="meta-info">{{ new Date(doc.createdAt).toLocaleString('zh-CN') }}</span>
          </div>
        </div>
      </div>
    </header>

    <main class="doc-main">
      <div v-if="loading" class="state-box"><div class="loader" /><p>加载中...</p></div>

      <template v-else-if="doc && detail">
        <!-- Summary -->
        <section class="card">
          <div class="card-head">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <h2>AI 摘要</h2>
          </div>
          <p v-if="doc.summary" class="summary-text">{{ doc.summary }}</p>
          <p v-else-if="doc.status === 'PROCESSING'" class="placeholder">摘要生成中...</p>
          <p v-else-if="doc.status === 'FAILED'" class="placeholder">摘要生成失败</p>
          <p v-else class="placeholder">文档处理完成后将自动生成摘要</p>
        </section>

        <!-- Chunks -->
        <section class="card">
          <div class="card-head">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
            <h2>文档分块</h2>
            <span class="count-badge">{{ detail.chunkCount }}</span>
          </div>
          <div v-if="detail.chunks.length === 0" class="placeholder">
            文档尚未完成分块处理，请等待处理完成。
          </div>
          <div v-else class="chunk-list">
            <div v-for="(chunk, i) in detail.chunks" :key="i" class="chunk-item">
              <div class="chunk-num">{{ chunk.chunkIndex + 1 }}</div>
              <div class="chunk-text">{{ chunk.content }}</div>
            </div>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.doc-page { min-height: 100vh; background: var(--bg); }

/* ── Topbar ── */
.topbar {
  background: var(--surface);
  border-bottom: 1px solid var(--border-light);
}
.topbar-inner { max-width: 900px; margin: 0 auto; padding: 20px 32px; }
.topbar-inner h1 { font-size: var(--text-xl); margin: 12px 0 8px; word-break: break-all; }

.btn-back {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  color: var(--text-secondary);
  font-size: var(--text-sm); font-family: var(--font); cursor: pointer;
  transition: all 0.2s;
}
.btn-back:hover { background: var(--surface-alt); color: var(--text); }

.meta-tags { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.type-badge {
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs); font-weight: 600;
  text-transform: uppercase;
}
.type-badge.pdf      { background: var(--rose-light); color: var(--dusty-rose); }
.type-badge.markdown { background: var(--blue-light); color: var(--dusty-blue); }
.type-badge.txt      { background: var(--sage-light); color: var(--sage); }
.type-badge.docx     { background: #e8daef; color: #7b5ea7; }
.type-badge.html     { background: #fdebd0; color: #b9770e; }
.status-badge {
  padding: 3px 12px;
  border-radius: 12px;
  font-size: var(--text-xs); font-weight: 600;
}
.badge-pending { background: #fef7e0; color: #9d8100; }
.badge-processing { background: var(--blue-light); color: var(--dusty-blue); }
.badge-done { background: var(--sage-light); color: var(--sage); }
.badge-fail { background: var(--rose-light); color: var(--error); }
.meta-info { color: var(--text-muted); font-size: var(--text-sm); }

/* ── Main ── */
.doc-main { max-width: 900px; margin: 0 auto; padding: 32px; }

/* ── Card ── */
.card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  padding: 28px;
  margin-bottom: 24px;
  animation: fadeIn 0.5s var(--ease);
}
.card-head {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 18px;
}
.card-head h2 { font-size: var(--text-base); font-weight: 600; }
.card-head svg { color: var(--sage); }
.count-badge {
  padding: 2px 10px;
  border-radius: 10px;
  background: var(--sage-light);
  color: var(--sage);
  font-size: var(--text-xs); font-weight: 600;
}

.summary-text { font-size: var(--text-base); line-height: var(--leading); color: var(--text); }

.placeholder { color: var(--text-muted); font-style: italic; font-size: var(--text-sm); }

/* ── Chunks ── */
.chunk-list { display: flex; flex-direction: column; gap: 16px; }
.chunk-item {
  display: flex; gap: 16px;
  padding: 18px;
  background: var(--surface-alt);
  border-radius: var(--radius);
  border: 1px solid var(--border-light);
  transition: border-color 0.2s;
}
.chunk-item:hover { border-color: var(--border); }
.chunk-num {
  flex-shrink: 0;
  width: 32px; height: 32px;
  border-radius: 50%;
  background: var(--sage);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: var(--text-xs); font-weight: 700;
}
.chunk-text {
  flex: 1;
  font-size: var(--text-sm);
  line-height: var(--leading);
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

/* ── States ── */
.state-box { text-align: center; padding: 80px 20px; }
.loader {
  width: 28px; height: 28px;
  border: 3px solid var(--border);
  border-top-color: var(--sage);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 14px;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
