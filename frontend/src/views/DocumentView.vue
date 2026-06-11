<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDocumentDetail, reprocessDocument, askDocument } from '@/api/document'
import { useToast } from '@/composables/useToast'
import type { Document } from '@/types'
import type { DocumentDetailResponse, DocQaResponse } from '@/api/document'
import { getStatusText, getStatusClass } from '@/utils/status'

const route = useRoute()
const router = useRouter()
const docId = Number(route.params.id)
const toast = useToast()

const loading = ref(true)
const reprocessing = ref(false)
const detail = ref<DocumentDetailResponse | null>(null)
const doc = ref<Document | null>(null)

// QA state
const qaQuestion = ref('')
const qaLoading = ref(false)
const qaResult = ref<DocQaResponse | null>(null)
const qaError = ref('')

function formatFileSize(bytes: number | null): string {
  if (!bytes) return '未知'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

async function loadDetail() {
  loading.value = true
  try {
    const { data } = await getDocumentDetail(docId)
    detail.value = data
    doc.value = data.document
  } catch (err: any) {
    toast.error(err.response?.data?.message || '加载文档失败')
    router.back()
  } finally { loading.value = false }
}

async function handleReprocess() {
  reprocessing.value = true
  try {
    await reprocessDocument(docId)
    toast.success('已重新加入处理队列')
    doc.value!.status = 'UPLOADED'
  } catch (err: any) {
    toast.error(err.response?.data?.message || '重新处理失败')
  } finally { reprocessing.value = false; await loadDetail() }
}

async function handleAsk() {
  const q = qaQuestion.value.trim()
  if (!q || qaLoading.value) return
  qaLoading.value = true
  qaError.value = ''
  qaResult.value = null
  try {
    const { data } = await askDocument(docId, q)
    qaResult.value = data
  } catch (err: any) {
    qaError.value = err.response?.data?.message || '提问失败'
  } finally { qaLoading.value = false }
}

onMounted(loadDetail)
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
        <!-- AI Summary -->
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

        <!-- Actions -->
        <section v-if="doc.status === 'FAILED'" class="card">
          <div class="card-head">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            <h2>处理失败</h2>
          </div>
          <p class="placeholder">文档处理过程中出现错误，可能是 API 调用异常或文件格式问题。</p>
          <button class="btn-retry" :disabled="reprocessing" @click="handleReprocess">
            {{ reprocessing ? '重新处理中...' : '重新处理' }}
          </button>
        </section>

        <!-- Document QA -->
        <section class="card">
          <div class="card-head">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <h2>文档问答</h2>
            <span v-if="detail.chunkCount" class="count-badge">{{ detail.chunkCount }} 个段落</span>
          </div>

          <div v-if="doc.status !== 'COMPLETED' && doc.status !== 'FAILED'" class="placeholder">
            文档处理完成后即可提问
          </div>
          <template v-else>
            <!-- QA Input -->
            <div class="qa-input-row">
              <input
                v-model="qaQuestion"
                class="qa-input"
                placeholder="针对此文档提问..."
                @keyup.enter="handleAsk"
                :disabled="qaLoading"
              />
              <button class="btn-ask" :disabled="qaLoading || !qaQuestion.trim()" @click="handleAsk">
                {{ qaLoading ? '思考中...' : '提问' }}
              </button>
            </div>

            <!-- QA Result -->
            <div v-if="qaLoading" class="qa-loading">
              <div class="loader-sm" />
              <span>正在分析文档...</span>
            </div>
            <div v-else-if="qaError" class="qa-error">{{ qaError }}</div>
            <div v-else-if="qaResult" class="qa-result">
              <p class="qa-answer">{{ qaResult.answer }}</p>
              <details v-if="qaResult.sources.length" class="qa-sources">
                <summary>参考来源 ({{ qaResult.sources.length }} 段)</summary>
                <div v-for="(s, i) in qaResult.sources" :key="i" class="source-item">
                  <span class="source-tag">段落 {{ i + 1 }}</span>
                  <span class="source-text">{{ s.snippet }}</span>
                </div>
              </details>
            </div>
          </template>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.doc-page { min-height: 100vh; background: var(--bg); }

/* Topbar */
.topbar { background: var(--surface); border-bottom: 1px solid var(--border-light); }
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
.status-badge { padding: 3px 12px; border-radius: 12px; font-size: var(--text-xs); font-weight: 600; }
.badge-pending { background: #fef7e0; color: #9d8100; }
.badge-processing { background: var(--blue-light); color: var(--dusty-blue); }
.badge-done { background: var(--sage-light); color: var(--sage); }
.badge-fail { background: var(--rose-light); color: var(--error); }
.meta-info { color: var(--text-muted); font-size: var(--text-sm); }

/* Main */
.doc-main { max-width: 900px; margin: 0 auto; padding: 32px; }

/* Card */
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

/* Retry button */
.btn-retry {
  margin-top: 16px;
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius);
  background: var(--dusty-blue);
  color: #fff;
  font-size: var(--text-sm); font-family: var(--font); cursor: pointer;
  transition: background 0.2s;
}
.btn-retry:hover { background: var(--sage); }
.btn-retry:disabled { opacity: 0.5; cursor: not-allowed; }

/* QA */
.qa-input-row {
  display: flex; gap: 10px;
}
.qa-input {
  flex: 1;
  padding: 10px 16px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius);
  font-size: var(--text-sm); font-family: var(--font);
  background: var(--surface-alt);
  color: var(--text);
  outline: none;
  transition: border-color 0.2s;
}
.qa-input:focus { border-color: var(--sage); }
.qa-input:disabled { opacity: 0.5; }

.btn-ask {
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius);
  background: var(--sage);
  color: #fff;
  font-size: var(--text-sm); font-family: var(--font); cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}
.btn-ask:hover { background: var(--sage-dark); }
.btn-ask:disabled { opacity: 0.5; cursor: not-allowed; }

.qa-loading {
  display: flex; align-items: center; gap: 10px;
  margin-top: 18px; padding: 16px;
  color: var(--text-muted); font-size: var(--text-sm);
}
.loader-sm {
  width: 18px; height: 18px;
  border: 2px solid var(--border);
  border-top-color: var(--sage);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.qa-error {
  margin-top: 16px; padding: 14px 18px;
  background: var(--rose-light);
  border-radius: var(--radius);
  color: var(--error);
  font-size: var(--text-sm);
}

.qa-result {
  margin-top: 18px;
}
.qa-answer {
  font-size: var(--text-base);
  line-height: var(--leading);
  color: var(--text);
  white-space: pre-wrap;
}
.qa-sources {
  margin-top: 16px;
  padding: 14px 18px;
  background: var(--surface-alt);
  border-radius: var(--radius);
  font-size: var(--text-sm);
}
.qa-sources summary {
  cursor: pointer;
  color: var(--text-muted);
  font-weight: 500;
  margin-bottom: 8px;
}
.source-item {
  display: flex; gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid var(--border-light);
  align-items: baseline;
}
.source-item:last-child { border-bottom: none; }
.source-tag {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--sage-light);
  color: var(--sage);
  font-size: var(--text-xs);
  font-weight: 600;
}
.source-text {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* States */
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
