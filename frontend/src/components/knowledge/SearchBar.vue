<script setup lang="ts">
import { ref, watch } from 'vue'
import api from '@/api'

const props = defineProps<{ knowledgeBaseId: number }>()

const query = ref('')
const results = ref<any[]>([])
const searching = ref(false)
const showResults = ref(false)
const selectedIndex = ref(0)

let debounceTimer: ReturnType<typeof setTimeout>

watch(query, (val) => {
  clearTimeout(debounceTimer)
  if (!val.trim()) {
    results.value = []
    showResults.value = false
    return
  }
  debounceTimer = setTimeout(() => doSearch(), 400)
})

async function doSearch() {
  if (!query.value.trim()) return
  searching.value = true
  try {
    const { data } = await api.get(`/search/${props.knowledgeBaseId}`, {
      params: { q: query.value }
    })
    results.value = data.results
    showResults.value = true
    selectedIndex.value = 0
  } finally {
    searching.value = false
  }
}

function highlightText(text: string, q: string): string {
  const escaped = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>')
}
</script>

<template>
  <div class="search-bar">
    <input
      v-model="query"
      type="text"
      placeholder="🔍 语义搜索知识库内容..."
      class="search-input"
    />
    <span v-if="searching" class="searching">搜索中...</span>

    <div v-if="showResults && results.length > 0" class="search-results" @mouseleave="showResults = false">
      <div class="results-header">{{ results.length }} 条结果</div>
      <div v-for="(r, i) in results" :key="r.chunkId" class="result-item" :class="{ active: i === selectedIndex }">
        <div class="result-doc">📄 {{ r.documentTitle }}</div>
        <div class="result-snippet" v-html="highlightText(r.content, query)" />
      </div>
    </div>
    <div v-else-if="showResults && query.trim()" class="search-results">
      <div class="results-empty">未找到相关内容</div>
    </div>
  </div>
</template>

<style scoped>
.search-bar { position: relative; max-width: 500px; }
.search-input {
  width: 100%;
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.9rem;
  box-sizing: border-box;
  outline: none;
}
.search-input:focus { border-color: #4f46e5; box-shadow: 0 0 0 3px rgba(79,70,229,0.1); }
.searching { position: absolute; right: 12px; top: 10px; font-size: 0.75rem; color: #888; }

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.12);
  z-index: 50;
  max-height: 400px;
  overflow-y: auto;
}
.results-header { padding: 8px 14px; font-size: 0.75rem; color: #888; border-bottom: 1px solid #eee; }
.results-empty { padding: 20px; text-align: center; color: #999; font-size: 0.85rem; }
.result-item { padding: 10px 14px; cursor: pointer; border-bottom: 1px solid #f5f5f5; }
.result-item:hover, .result-item.active { background: #f5f3ff; }
.result-item:last-child { border-bottom: none; }
.result-doc { font-size: 0.75rem; color: #4f46e5; margin-bottom: 4px; font-weight: 500; }
.result-snippet { font-size: 0.8rem; color: #555; line-height: 1.5; }
.result-snippet :deep(mark) { background: #fef08a; color: #333; padding: 0 2px; border-radius: 2px; }
</style>
