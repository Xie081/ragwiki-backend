# 智能知识库管理系统 (RAG-based Wiki)

> 2026春季 - Web 应用程序开发课程项目  
> 选题：主题一 · 基于 RAG（检索增强生成）技术的智能知识库问答系统

[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/wYRiuCZ1)

---

## 📖 项目简介

本项目是一个**智能知识库管理系统**，支持用户上传 PDF/Markdown 文档，系统自动解析并构建向量化知识库，用户可通过自然语言对知识库内容进行 **RAG 问答**、**语义搜索**，AI 还能自动生成文档摘要。

### 核心功能

| 功能 | 说明 |
|------|------|
| 📄 **文档管理** | 支持上传 PDF、Markdown 文件，自动解析、分块、向量化存储 |
| 💬 **RAG 智能问答** | 基于文档内容的检索增强生成，AI 只根据你的知识库回答，并提供来源溯源 |
| 🔍 **语义搜索** | 跨文档语义检索，比关键词匹配更智能 |
| 📝 **AI 摘要生成** | 上传文档后自动生成内容摘要 |
| 📂 **知识库分级管理** | 支持创建多个知识库，树形分类管理 |
| ⚡ **流式输出** | ChatGPT 风格的逐字流式渲染，体验流畅 |
| 🔐 **JWT 安全认证** | 无状态用户认证，数据隔离 |

---

## 🏗️ 系统架构

```
┌────────────────────────────────────────────────────┐
│                   前端 (Vue3 + TypeScript)            │
│            Vite + Pinia + Vue Router                 │
│    ┌──────────┐  ┌──────────┐  ┌───────────────┐   │
│    │ 知识库管理 │  │ 文档上传  │  │ AI 对话面板    │   │
│    └──────────┘  └──────────┘  └───────────────┘   │
└──────────────────────┬─────────────────────────────┘
                       │ REST API + SSE (JWT Auth)
└──────────────────────┴─────────────────────────────┘
│              后端 (SpringBoot 3.5 + Java 21)          │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐ │
│  │ Security │ │Document  │ │ RAG Service           │ │
│  │ JWT      │ │Service   │ │ (检索→增强→生成)       │ │
│  └──────────┘ └──────────┘ └──────────────────────┘ │
│  ┌──────────────────────────────────────────────────┐ │
│  │         Spring AI (DeepSeek API)                  │ │
│  │    Chat API (流式)  │  Embedding API (向量化)     │ │
│  └──────────────────────────────────────────────────┘ │
└──────────────┬────────────────────┬──────────────────┘
               │                    │
└──────────────┴────────────────────┴──────────────────┘
│      PostgreSQL 16 + PGVector       │   DeepSeek API  │
│   (结构化数据 + 向量存储 + 相似检索)   │   (LLM 服务)    │
└─────────────────────────────────────┘─────────────────┘
```

### RAG 问答数据流

```
用户提问
  → Embedding 向量化
  → PGVector 余弦相似度检索 Top-K 相关文档块
  → 拼装 Prompt（System Prompt + 检索上下文 + 用户问题）
  → DeepSeek Chat API（SSE 流式返回）
  → 前端 Markdown 渲染 + 来源文档高亮溯源
```

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.5.14 |
| **语言** | Java | 21 |
| **AI 框架** | Spring AI (OpenAI Starter) | 1.0.0-M6 |
| **LLM** | DeepSeek (deepseek-chat / deepseek-embedding) | — |
| **数据库** | PostgreSQL + PGVector 扩展 | 16+ |
| **安全** | Spring Security + JWT (jjwt) | 0.12.6 |
| **PDF 解析** | Apache PDFBox | 3.0.3 |
| **Markdown 解析** | CommonMark | 0.22.0 |
| **数据库迁移** | Flyway | — |
| **API 文档** | SpringDoc OpenAPI | 2.8.16 |
| **前端框架** | Vue 3 + TypeScript | — |
| **构建工具** | Vite | — |
| **状态管理** | Pinia | — |
| **HTTP 客户端** | Axios | — |
| **Markdown 渲染** | marked + highlight.js | — |

---

## 🚀 快速开始

### 前置条件

- JDK 21+
- Node.js 18+
- PostgreSQL 16+（需安装 PGVector 扩展）
- DeepSeek API Key（[申请地址](https://platform.deepseek.com/)）

### 1. 克隆项目

```bash
git clone git@github.com:cs-sbs/personal-project-Xie081.git
cd personal-project-Xie081
```

### 2. 配置环境变量

创建 `src/main/resources/application-local.yml`（或设置系统环境变量）：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_wiki
    username: postgres
    password: your_password
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}       # DeepSeek API Key
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
      embedding:
        options:
          model: deepseek-embedding

app:
  jwt:
    secret: your-jwt-secret-key-at-least-256-bits
    expiration-ms: 86400000              # 24 hours
  upload:
    dir: ./uploads                       # 文档上传目录
```

### 3. 初始化数据库

```bash
# 安装 PGVector 扩展（在 PostgreSQL 中执行）
CREATE EXTENSION IF NOT EXISTS vector;

# Flyway 会在应用启动时自动创建表结构
```

### 4. 启动后端

```bash
# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

后端启动后访问 Swagger UI：`http://localhost:8080/swagger-ui.html`

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问：`http://localhost:5173`

---

## 📡 API 概览

### 认证
| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录，返回 JWT Token |

### 知识库管理
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/knowledge-bases` | 获取当前用户的知识库列表 |
| POST | `/api/knowledge-bases` | 创建新知识库 |
| GET | `/api/knowledge-bases/{id}` | 获取知识库详情 |
| DELETE | `/api/knowledge-bases/{id}` | 删除知识库及其所有文档 |

### 文档管理
| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/knowledge-bases/{kbId}/documents` | 上传文档（multipart/form-data） |
| GET | `/api/knowledge-bases/{kbId}/documents` | 获取知识库下文档列表 |
| GET | `/api/documents/{id}` | 获取文档内容和分块信息 |
| DELETE | `/api/documents/{id}` | 删除文档及其向量数据 |

### RAG 问答
| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/chat/stream` | **SSE 流式问答**（推荐） |
| POST | `/api/chat/ask` | 普通问答（非流式） |

### Prompt 模板管理
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/prompt-templates` | 获取所有 Prompt 模板 |
| POST | `/api/prompt-templates` | 创建新模板 |
| PUT | `/api/prompt-templates/{id}` | 更新模板 |

---

## 🗄️ 数据库设计

### ER 图

```
┌──────────────┐       ┌──────────────────┐
│    users     │       │ knowledge_bases  │
├──────────────┤       ├──────────────────┤
│ id (PK)      │──┐    │ id (PK)          │
│ username     │  │    │ name             │
│ password     │  │    │ description      │
│ email        │  │    │ user_id (FK)     │◄──┐
│ created_at   │  │    │ created_at       │   │
└──────────────┘  │    └────────┬─────────┘   │
                  │             │              │
                  │    ┌────────┴─────────┐    │
                  │    │   documents      │    │
                  │    ├──────────────────┤    │
                  └───►│ user_id (FK)     │    │
                       │ kb_id (FK)       │────┘
                       │ title            │
                       │ file_type        │
                       │ storage_path     │
                       │ status           │
                       │ summary (AI生成) │
                       │ created_at       │
                       └────────┬─────────┘
                                │
                       ┌────────┴──────────────┐
                       │  document_chunks      │
                       ├───────────────────────┤
                       │ id (PK)               │
                       │ document_id (FK)      │
                       │ kb_id (FK)            │
                       │ content (文本内容)     │
                       │ chunk_index           │
                       │ embedding (PGVector)  │ ◄── IVFFlat 索引
                       │ created_at            │
                       └───────────────────────┘

┌──────────────────────┐
│  prompt_templates    │
├──────────────────────┤
│ id (PK)              │
│ name                 │
│ system_prompt        │
│ user_prompt_template │
│ variables (JSON)     │
│ created_at           │
└──────────────────────┘
```

---

## 🧠 Prompt 设计

系统采用模板化 Prompt 管理，支持 `{变量}` 注入。核心模板示例：

### RAG 问答模板 (`rag-qa`)

**System Prompt:**
```
你是一个专业的知识库助手。你的任务是严格根据提供的文档内容回答用户问题。

规则：
1. 只使用下面【参考文档】中的信息回答问题
2. 如果文档中没有相关信息，请明确告知用户"该知识库中未找到相关信息"
3. 回答时引用具体的文档名称和段落
4. 保持回答简洁、准确、结构化
5. 使用 Markdown 格式组织回答
```

**User Prompt:**
```
【参考文档】
{context}

【用户问题】
{question}

请根据以上参考文档回答用户的问题。
```

### 摘要生成模板 (`summarize`)

**System Prompt:**
```
你是一个文档摘要专家。请为以下文档内容生成一个简洁的摘要。

要求：
1. 摘要长度控制在 200 字以内
2. 突出文档的核心主题和关键结论
3. 使用清晰的中文表达
```

**User Prompt:**
```
【文档标题】{document_title}
【文档内容】{content}

请生成摘要：
```

> 📝 详细的 Prompt 迭代过程记录请参见 [Prompt 报告](./docs/prompt-report.md)

---

## 🌐 部署

### 部署地址

| 服务 | 平台 | 地址 |
|------|------|------|
| 前端 | ▲ Vercel | https://frontend-beige-five-92.vercel.app |
| 后端 | 🚂 Railway | https://ragwiki-api-production-c4d2.up.railway.app |
| 数据库 | 🚂 Railway | PostgreSQL 16 + PGVector |

### Docker 一键启动

```bash
docker-compose up -d
```

包含 PostgreSQL 16 + PGVector + SpringBoot 后端（多阶段 Docker 构建）。

### Railway 部署

项目已配置 `Dockerfile`（多阶段构建：Maven 编译 + JRE 运行），Railway 可直接从 Git 仓库自动构建部署。

```bash
# 推送后 Railway 自动部署
git push railway main
```

### 环境变量（部署平台需配置）

| 变量名 | 说明 |
|--------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 |
| `JWT_SECRET` | JWT 签名密钥 |
| `SPRING_DATASOURCE_URL` | PostgreSQL 连接地址 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |

---

## 📋 提交物清单

- [x] GitHub 代码仓库（含频繁 Commit 记录）
- [x] 系统架构图（见上方）
- [x] ER 数据库设计图（见上方）
- [x] Prompt 报告（`docs/prompt-report.md`）
- [x] 部署外网链接（Vercel + Railway）
- [ ] 功能演示视频

---

## 🤖 AI 辅助开发说明

本项目在开发过程中使用了以下 AI 工具辅助：

- **Claude Code**：项目架构设计、代码生成、代码审查、文档编写
- AI 辅助开发比例：约 **70%**

AI 主要参与了以下环节：
- 项目架构规划与技术选型
- 后端 Controller / Service / Repository 层代码生成
- 前端 Vue3 组件编写
- Prompt 模板设计与迭代优化
- 数据库迁移脚本编写
- 部署配置文件编写
- README 与技术文档撰写

所有 AI 生成的代码均经过人工审查和测试验证。

---

## 📄 许可证

本项目为课程作业项目，仅供学习参考。

---

> **截止日期：2026年6月14日 24:00**
