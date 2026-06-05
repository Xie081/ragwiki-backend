# 智能知识库管理系统 (RAG Wiki)

> 基于 RAG（检索增强生成）技术的智能知识库问答系统——上传文档，用自然语言提问，AI 精准回答。

---

## 项目简介

**RAG Wiki** 是一款智能知识库管理系统。用户上传文档后，系统自动解析分块并构建可检索的知识库，支持自然语言问答、语义搜索和 AI 摘要生成。AI 基于文档内容回答，提供来源溯源。

### 核心功能

| 功能 | 说明 |
|------|------|
| 文档管理 | 支持 PDF / Markdown / TXT / DOCX / HTML 上传（DOCX 支持段落+表格），自动解析分块存储 |
| RAG 问答 | 混合搜索（向量语义为主、关键词匹配为辅），支持多轮对话上下文 |
| 单文档问答 | 针对单个文档提问，快速验证解析效果 |
| AI 摘要 | 文档上传后自动生成中文摘要 |
| 知识库管理 | 多知识库创建与管理，用户数据隔离 |
| 流式输出 | SSE 逐字流式渲染，类 ChatGPT 交互体验 |
| 重新处理 | 处理失败时可一键重新处理文档 |
| 语义搜索 | 跨文档向量语义检索 |
| 安全认证 | Spring Security + JWT 无状态鉴权 |
| Prompt 模板 | 数据库存储模板，支持变量注入与热更新 |

---

## 系统架构

```
                            前端 (Vue 3 + TypeScript)
                     Vite + Pinia + Vue Router + Axios
                知识库管理 / 文档上传 / AI 对话面板 / 单文档问答
                                  |
                       REST API + SSE (JWT Auth)
                                  |
                    后端 (Spring Boot 3.5 + Java 21)
                 Security / Document / RAG Service
                     Spring AI (OpenAI Starter)
                     Chat: DeepSeek V4 Pro
                     Embedding: SiliconFlow BAAI/bge-m3
                           |               |
                PostgreSQL 16 + PGVector    DeepSeek V4 Pro
                (结构化数据 + 向量检索)      (聊天 LLM)
                                  |
                          SiliconFlow API
                         (BAAI/bge-m3 嵌入)
```

### RAG 数据流

```
用户提问
  -> 问题向量化 (SiliconFlow BAAI/bge-m3)
  -> 向量语义搜索 (主通道) + 关键词匹配 (补充)
  -> 混合搜索 (向量优先，关键词精确匹配补充)
  -> 检索 Top-K 相关文档块
  -> 拼装 Prompt (System Prompt + 对话历史 + 上下文 + 问题)
  -> DeepSeek V4 Pro (SSE 流式)
  -> 前端 Markdown 渲染 + 来源溯源
```

### 文档处理流程

```
上传文档
  -> 解析 (PDF/DOCX/MD/TXT/HTML，DOCX 支持段落+表格)
  -> 分块 (1000 字符/块，150 字符重叠)
  -> 批量嵌入 (一次 API 调用最多 16 个块，SiliconFlow BAAI/bge-m3)
  -> 存储向量 + 块内容到 PostgreSQL (PGVector)
  -> AI 摘要生成 (DeepSeek V4 Pro)
```

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.5.14 |
| 语言 | Java | 21 |
| AI 框架 | Spring AI (OpenAI Starter) | 1.0.0-M6 |
| 聊天模型 | DeepSeek V4 Pro | `deepseek-v4-pro` |
| 嵌入模型 | SiliconFlow BAAI/bge-m3 | 1024 维 |
| 数据库 | PostgreSQL + PGVector 扩展 | 16+ |
| 安全 | Spring Security + JWT (jjwt) | 0.12.6 |
| PDF 解析 | Apache PDFBox | 3.0.3 |
| DOCX 解析 | Apache POI (段落+表格) | 5.4.0 |
| HTML 解析 | Jsoup | 1.19.1 |
| Markdown 解析 | CommonMark | 0.22.0 |
| 数据库迁移 | Flyway | - |
| API 文档 | SpringDoc OpenAPI | 2.8.16 |
| 前端框架 | Vue 3 + TypeScript | - |
| 构建工具 | Vite | - |
| 状态管理 | Pinia | - |
| Markdown 渲染 | marked + highlight.js | - |

---

## 快速开始

### 前置条件

- JDK 21+
- Node.js 18+
- PostgreSQL 16+（需安装 PGVector 扩展）
- DeepSeek API Key（[申请地址](https://platform.deepseek.com/)）
- SiliconFlow API Key（[申请地址](https://siliconflow.cn)）

### 1. 克隆项目

```bash
git clone git@github.com:Xie081/rag-wiki.git
cd rag-wiki
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env`，填入 API Key：

```bash
DEEPSEEK_API_KEY=your_deepseek_api_key_here
SILICONFLOW_API_KEY=your_siliconflow_api_key_here
JWT_SECRET=generate-a-random-secret-key
```

### 3. 初始化数据库

```sql
CREATE DATABASE rag_wiki;
-- Flyway 会在首次启动时自动创建表结构
```

### 4. Docker 一键启动（推荐）

```bash
docker compose up -d
```

包含 PostgreSQL 16 + PGVector + Spring Boot 后端。

### 5. 本地开发启动

```bash
# 后端
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 前端（新终端）
cd frontend && npm install && npm run dev
```

- 后端：`http://localhost:8080`
- 前端：`http://localhost:5173`
- Swagger：`http://localhost:8080/swagger-ui.html`

---

## API 概览

### 认证

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录，返回 JWT Token |

### 知识库

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/knowledge-bases` | 获取知识库列表 |
| POST | `/api/knowledge-bases` | 创建知识库 |
| GET | `/api/knowledge-bases/{id}` | 获取详情 |
| DELETE | `/api/knowledge-bases/{id}` | 删除知识库及其文档 |

### 文档

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/knowledge-bases/{kbId}/documents` | 上传文档（multipart） |
| GET | `/api/knowledge-bases/{kbId}/documents` | 文档列表（支持分页） |
| GET | `/api/documents/{id}/detail` | 文档详情（含摘要与分段） |
| DELETE | `/api/documents/{id}` | 删除文档 |
| POST | `/api/documents/{id}/reprocess` | 重新处理文档 |
| POST | `/api/documents/{id}/ask` | 单文档问答 |

### RAG 问答

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/chat/stream` | SSE 流式问答（推荐） |
| POST | `/api/chat/ask` | 普通问答 |

### 语义搜索

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/search/{kbId}` | 跨文档向量语义搜索 |

### 系统

| Method | Path | 说明 |
|--------|------|------|
| GET | `/health` | 健康检查 |

---

## 数据库设计

```
users                   knowledge_bases         documents
+--------------+       +------------------+     +-------------------+
| id (PK)      |       | id (PK)          |     | id (PK)           |
| username     |---+   | name             |     | title             |
| password     |   |   | description      |     | file_type         |
| email        |   |   | user_id (FK)     |<--+ | original_name     |
| created_at   |   |   | created_at       |   | | storage_path      |
+--------------+   |   +--------+---------+   | | file_size         |
                   |            |             | | status            |
                   |   +--------+---------+   | | summary（AI 生成） |
                   |   | document_chunks  |   | | kb_id (FK)        |
                   |   +------------------+   | | user_id (FK)      |
                   |   | id (PK)          |   | | created_at        |
                   +-->| document_id (FK) |   | +-------------------+
                       | kb_id (FK)       |---+
                       | content          |
                       | chunk_index      |       prompt_templates
                       | embedding (PGVec)|       +-------------------+
                       | created_at       |       | id (PK)           |
                       +------------------+       | name              |
                                                  | system_prompt     |
                                                  | user_prompt       |
                                                  | variables (JSON)  |
                                                  +-------------------+
```

- `document_chunks` 的 `embedding` 列使用 PGVector 存储 1024 维向量（BAAI/bge-m3）
- 使用 IVFFlat 索引加速余弦相似度检索

---

## Prompt 设计

系统采用数据库模板化管理，支持 `{variable}` 占位符注入。核心模板：

### RAG 问答（rag-qa）

**System Prompt：**

```
你是一个专业的知识库助手。

规则：
1. 如果参考文档中有相关信息，请基于文档内容回答，并自然提及参考了哪篇文档
2. 如果参考文档中没有相关信息或为空，请直接基于知识正常回答
3. 保持回答简洁、准确、结构化
4. 使用 Markdown 格式组织回答
5. 如果有对话历史，请结合上下文理解追问意图
```

**User Prompt：**

```
对话历史
{history}
参考文档
{context}

用户问题
{question}

请回答用户的问题。
```

### 摘要生成（summarize）

**System Prompt：**

```
你是一个文档摘要专家。请为以下文档内容生成一个简洁的摘要。

要求：
1. 摘要长度控制在 200 字以内
2. 突出文档的核心主题和关键结论
3. 使用清晰的中文表达
```

详细的 Prompt 迭代过程请参见 [Prompt 报告](./docs/prompt-report.md)

---

## 部署

### 部署地址

| 服务 | 平台 | 地址 |
|------|------|------|
| 前端 | Vercel | [https://rag-wiki.vercel.app](https://rag-wiki.vercel.app) |
| 后端 | Railway | `https://ragwiki-api-production-c4d2.up.railway.app` |
| 数据库 | Railway | PostgreSQL 16 + PGVector |

### 环境变量

| 变量名 | 说明 |
|--------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥（聊天用） |
| `SILICONFLOW_API_KEY` | SiliconFlow API 密钥（嵌入用） |
| `JWT_SECRET` | JWT 签名密钥（256 bits） |
| `SPRING_DATASOURCE_URL` | PostgreSQL 连接地址 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |
| `UPLOAD_DIR` | 文档上传目录 |

---

## AI 辅助开发说明

本项目在开发过程中使用了以下 AI 工具辅助：

- **Claude Code**：项目架构设计、代码生成、代码审查、文档编写
- AI 辅助开发比例：约 70%

AI 主要参与：项目架构规划与技术选型、后端全栈代码生成、前端 Vue 3 组件编写、Prompt 模板设计与迭代优化、数据库迁移脚本、部署配置、技术文档撰写。

所有 AI 生成代码均经过人工审查和测试验证。
