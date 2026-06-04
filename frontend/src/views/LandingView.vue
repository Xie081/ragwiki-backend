<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="landing">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-bg">
        <div class="bg-blob blob-1" />
        <div class="bg-blob blob-2" />
        <div class="bg-blob blob-3" />
      </div>

      <div class="hero-content">
        <div class="hero-badge">AI 驱动的文档助手</div>
        <h1>上传文档，向 AI 提问</h1>
        <p class="hero-sub">
          上传你的 PDF、Markdown、Word 等文档，AI 自动理解内容，<br/>
          用自然语言提问即可获得精准答案，并附上原文出处。
        </p>
        <div class="hero-actions">
          <button class="btn-primary" @click="router.push('/register')">免费注册</button>
          <button class="btn-outline" @click="router.push('/login')">登录</button>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <div class="section-head">
        <h2>核心功能</h2>
      </div>
      <div class="feature-grid">
        <div class="feature-card">
          <div class="feature-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
            </svg>
          </div>
          <h3>多格式文档上传</h3>
          <p>支持 PDF、Markdown、TXT、DOCX、HTML 等多种格式，上传即自动解析分块。</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <h3>智能问答</h3>
          <p>基于文档内容回答，AI 引用原文出处。支持多轮追问，越问越深入。</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
          </div>
          <h3>混合搜索</h3>
          <p>关键词精确匹配 + 向量语义理解，确保找到最相关的文档内容。</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
              <line x1="3" y1="9" x2="21" y2="9"/>
              <line x1="9" y1="21" x2="9" y2="9"/>
            </svg>
          </div>
          <h3>知识库管理</h3>
          <p>创建多个知识库分类管理文档，适合团队项目资料、学习笔记等场景。</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
            </svg>
          </div>
          <h3>流式输出</h3>
          <p>类 ChatGPT 逐字渲染体验，AI 回答即时呈现，无需等待完整生成。</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 20h9"/>
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
            </svg>
          </div>
          <h3>AI 摘要</h3>
          <p>上传文档后自动生成精炼摘要，快速了解文档核心内容。</p>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta">
      <h2>开始构建你的知识库</h2>
      <p>注册即用，无需复杂配置</p>
      <button class="btn-primary btn-lg" @click="router.push('/register')">免费注册</button>
    </section>

    <!-- Footer -->
    <footer>
      <span>智能知识库</span>
      <span class="dot">·</span>
      <span>基于 RAG 技术</span>
    </footer>
  </div>
</template>

<style scoped>
.landing { min-height: 100vh; background: var(--bg); overflow: hidden; }

/* ── Hero ── */
.hero {
  position: relative;
  display: flex; align-items: center; justify-content: center;
  min-height: 85vh;
  padding: 80px 32px;
  text-align: center;
}
.hero-bg { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.4;
}
.blob-1 { width: 500px; height: 500px; background: var(--sage-light); top: -100px; right: -100px; }
.blob-2 { width: 350px; height: 350px; background: var(--blue-light); bottom: -80px; left: -80px; }
.blob-3 { width: 250px; height: 250px; background: var(--rose-light); top: 50%; left: 50%; transform: translate(-50%, -50%); }

.hero-content { position: relative; z-index: 1; max-width: 680px; }
.hero-badge {
  display: inline-block;
  padding: 6px 18px;
  border-radius: 20px;
  background: var(--surface);
  border: 1px solid var(--border-light);
  color: var(--text-secondary);
  font-size: var(--text-sm);
  margin-bottom: 24px;
}
.hero h1 {
  font-size: 52px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.2;
  color: var(--text);
  margin-bottom: 20px;
}
.hero-sub {
  font-size: var(--text-lg);
  line-height: 1.7;
  color: var(--text-secondary);
  margin-bottom: 40px;
}
.hero-actions { display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; }

@media (max-width: 768px) {
  .hero h1 { font-size: 34px; }
  .hero-sub { font-size: var(--text-base); }
}

/* ── Buttons ── */
.btn-primary {
  padding: 14px 36px;
  border: none;
  border-radius: var(--radius);
  background: linear-gradient(135deg, var(--sage), #7a8f74);
  color: #fff;
  font-size: var(--text-base);
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  transition: transform 0.15s var(--ease), box-shadow 0.15s var(--ease);
}
.btn-primary:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.btn-primary.btn-lg { padding: 18px 48px; font-size: var(--text-lg); }

.btn-outline {
  padding: 14px 36px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  color: var(--text);
  font-size: var(--text-base);
  font-weight: 500;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.2s var(--ease);
}
.btn-outline:hover { border-color: var(--sage); color: var(--sage); }

/* ── Features ── */
.features { padding: 80px 32px; max-width: 1100px; margin: 0 auto; }
.section-head { text-align: center; margin-bottom: 48px; }
.section-head h2 { font-size: var(--text-2xl); font-weight: 700; }

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}
.feature-card {
  padding: 32px;
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  transition: all 0.25s var(--ease);
}
.feature-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}
.feature-icon {
  width: 56px; height: 56px;
  border-radius: var(--radius);
  background: var(--sage-bg);
  color: var(--sage);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 18px;
}
.feature-card h3 { font-size: var(--text-base); font-weight: 600; margin-bottom: 8px; }
.feature-card p { font-size: var(--text-sm); color: var(--text-secondary); line-height: 1.7; }

/* ── CTA ── */
.cta {
  text-align: center;
  padding: 80px 32px;
  background: var(--surface);
  border-top: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
}
.cta h2 { font-size: var(--text-2xl); font-weight: 700; margin-bottom: 12px; }
.cta p { font-size: var(--text-lg); color: var(--text-secondary); margin-bottom: 32px; }

/* ── Footer ── */
footer {
  text-align: center;
  padding: 32px;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
footer .dot { margin: 0 8px; }
</style>
