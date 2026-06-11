package cs.sbs.web.personalprojectweb2026.service;

import cs.sbs.web.personalprojectweb2026.model.entity.Document;
import cs.sbs.web.personalprojectweb2026.model.entity.DocumentChunk;
import cs.sbs.web.personalprojectweb2026.repository.DocumentChunkRepository;
import cs.sbs.web.personalprojectweb2026.repository.DocumentRepository;
import cs.sbs.web.personalprojectweb2026.service.PromptTemplateService.RenderedPrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final PromptTemplateService promptTemplateService;
    private final EmbeddingService embeddingService;
    private final ChatModel chatModel;

    private static final int TOP_K = 5;

    private static final List<String> SOLVER_KEYWORDS = List.of(
            "答案", "解析", "解题", "答题", "回答", "求解", "计算",
            "选择", "填空", "判断", "问答", "题目", "试题", "考题",
            "solve", "answer", "explain", "calculate");

    /**
     * Auto-detect solver mode: if the question contains problem-solving
     * keywords, prepend an instruction for step-by-step answer + explanation.
     */
    private String maybeSolverPrefix(String question) {
        String lower = question.toLowerCase();
        for (String kw : SOLVER_KEYWORDS) {
            if (lower.contains(kw)) {
                return "【请给出答案和详细解析】\n" + question;
            }
        }
        return question;
    }

    // Chinese/English stop words and noise characters
    private static final Pattern NOISE = Pattern.compile(
            "[，。！？、；：\"'（）《》【】\\[\\]\\s,.!?;:'\"()\\-_=+@#$%^&*<>/\\\\|~`]+");
    private static final List<String> STOP_WORDS = List.of(
            "的", "是", "在", "了", "和", "有", "我", "你", "他", "她", "它", "们",
            "这", "那", "吗", "呢", "吧", "啊", "什么", "怎么", "为什么", "如何",
            "可以", "能够", "应该", "需要", "一个", "这个", "那个", "什么", "哪个",
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "can", "shall", "to", "of", "in", "for",
            "on", "with", "at", "by", "from", "as", "into", "through", "during",
            "what", "which", "who", "whom", "how", "when", "where", "why");

    /**
     * RAG 问答：检索 → 增强 → 生成。
     */
    public RagResult ask(Long kbId, String question, List<ConversationMessage> history) {
        List<DocumentChunk> chunks = retrieveChunks(kbId, question);

        // Batch-load all documents (fixes N+1)
        Map<Long, Document> docMap = buildDocMap(chunks);

        // Build context and sources (allow fallback when no documents)
        String context = buildContext(chunks, docMap);
        if (chunks.isEmpty()) {
            context = "（暂无参考文档，请直接基于你的知识回答）";
        }
        List<CitationSource> sources = buildSources(chunks, docMap);

        // Build conversation history text
        String historyText = buildHistoryText(history);

        // Render prompt template
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("context", context);
        variables.put("question", maybeSolverPrefix(question));
        variables.put("history", historyText);
        RenderedPrompt rendered = promptTemplateService.render("rag-qa", variables);

        // Call LLM
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(rendered.systemPrompt()));
        messages.add(new UserMessage(rendered.userPrompt()));

        Prompt prompt = new Prompt(messages);
        long llmStart = System.currentTimeMillis();
        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        long llmMs = System.currentTimeMillis() - llmStart;

        log.info("RAG: question='{}', chunks={}, history_rounds={}, answer_length={}, llm_time={}ms",
                question, chunks.size(), history.size() / 2, answer.length(), llmMs);

        return new RagResult(answer, sources);
    }

    /**
     * 构建渲染后的 Prompt + 来源引用，供流式问答使用。
     */
    public RenderedPromptWithSources buildRenderedPrompt(Long kbId, String question,
                                                          List<ConversationMessage> history) {
        long t0 = System.currentTimeMillis();

        List<DocumentChunk> chunks = retrieveChunks(kbId, question);
        long searchMs = System.currentTimeMillis() - t0;

        Map<Long, Document> docMap = buildDocMap(chunks);

        String context = buildContext(chunks, docMap);
        List<CitationSource> sources = buildSources(chunks, docMap);

        if (chunks.isEmpty()) {
            // If no keyword matches, include a fallback message for the AI
            context = "（知识库中暂无直接匹配的内容，请基于你的知识尽力回答，但要说明知识库中未找到相关内容）";
        }

        String historyText = buildHistoryText(history);

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("context", context);
        variables.put("question", maybeSolverPrefix(question));
        variables.put("history", historyText);
        var rendered = promptTemplateService.render("rag-qa", variables);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(rendered.systemPrompt()));
        messages.add(new UserMessage(rendered.userPrompt()));

        long totalMs = System.currentTimeMillis() - t0;
        log.info("RAG prep: keyword_search={}ms, total={}ms (chunks={}, history_rounds={})",
                searchMs, totalMs, chunks.size(), history.size() / 2);

        return new RenderedPromptWithSources(messages, sources);
    }

    /**
     * Build a formatted string from recent conversation history.
     */
    String buildHistoryText(List<ConversationMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ConversationMessage msg : history) {
            sb.append(msg.role().equals("user") ? "👤 用户：" : "🤖 助手：");
            // Truncate very long messages in history
            String content = msg.content();
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            sb.append(content).append("\n");
        }
        return sb.toString();
    }

    // ─── Private helpers ───

    /**
     * 混合检索：向量语义搜索为主，关键词匹配为辅。
     * 向量搜索负责语义召回，关键词结果补充精确匹配，合并去重后取 TOP_K。
     */
    private List<DocumentChunk> retrieveChunks(Long kbId, String question) {
        // 第一步：向量语义搜索（主通道）
        List<DocumentChunk> chunks;
        try {
            chunks = vectorSearch(kbId, question);
            log.debug("向量搜索: {} 个 chunk", chunks.size());
        } catch (Exception e) {
            log.warn("向量搜索失败: {}，回退到关键词搜索", e.getMessage());
            chunks = List.of();
        }

        // 第二步：关键词搜索（补充精确匹配，提升召回精度）
        String[] keywords = extractKeywords(question);
        if (keywords.length > 0) {
            String[] padded = Arrays.copyOf(keywords, 5);
            for (int i = 0; i < 5; i++) {
                if (padded[i] == null || padded[i].isEmpty()) {
                    padded[i] = "";
                }
            }
            List<DocumentChunk> kwChunks = chunkRepository.searchByKeywords(
                    padded[0], padded[1], padded[2], padded[3], padded[4],
                    kbId, TOP_K);
            log.debug("关键词搜索: keywords={}, {} 个 chunk", Arrays.toString(keywords), kwChunks.size());

            // 合并：向量结果在前，关键词结果去重追加，取 TOP_K
            chunks = mergeChunkResults(chunks, kwChunks, TOP_K);
            log.info("混合检索: 合并后 {} 个 chunk（向量 + 关键词）", chunks.size());
        }

        return chunks;
    }

    /**
     * 向量语义搜索：将问题转为 embedding，用 PGVector 余弦相似度检索 top-K。
     */
    private List<DocumentChunk> vectorSearch(Long kbId, String question) {
        float[] queryEmbedding = embeddingService.embed(question);
        String vectorStr = embeddingService.toPgVectorString(queryEmbedding);
        return chunkRepository.findSimilarChunks(vectorStr, kbId, TOP_K);
    }

    /**
     * 合并两组搜索结果，按 chunk ID 去重。
     * primary 在前（向量语义召回），secondary 去重追加（关键词精确匹配补充）。
     * Package-private，方便测试。
     */
    List<DocumentChunk> mergeChunkResults(
            List<DocumentChunk> primary,
            List<DocumentChunk> secondary,
            int maxResults) {
        java.util.LinkedHashSet<Long> seenIds = new java.util.LinkedHashSet<>();
        List<DocumentChunk> merged = new ArrayList<>();

        for (DocumentChunk c : primary) {
            if (seenIds.add(c.getId())) {
                merged.add(c);
            }
        }
        for (DocumentChunk c : secondary) {
            if (seenIds.add(c.getId()) && merged.size() < maxResults) {
                merged.add(c);
            }
        }

        return merged.subList(0, Math.min(merged.size(), maxResults));
    }

    /**
     * Extract meaningful keywords from a question.
     * Splits by noise characters, removes stop words, returns top-5 longest tokens.
     * Package-private for testing.
     */
    String[] extractKeywords(String text) {
        if (text == null || text.isBlank()) return new String[0];

        return NOISE.splitAsStream(text)
                .map(String::trim)
                .filter(s -> s.length() >= 1)
                .filter(s -> !STOP_WORDS.contains(s.toLowerCase()))
                .distinct()
                .limit(5)
                .toArray(String[]::new);
    }

    private Map<Long, Document> buildDocMap(List<DocumentChunk> chunks) {
        List<Long> docIds = chunks.stream()
                .map(DocumentChunk::getDocumentId)
                .distinct()
                .toList();
        return docIds.isEmpty() ? Map.of()
                : documentRepository.findAllById(docIds).stream()
                        .collect(Collectors.toMap(Document::getId, Function.identity()));
    }

    private String buildContext(List<DocumentChunk> chunks, Map<Long, Document> docMap) {
        StringBuilder ctx = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            Document doc = docMap.get(chunk.getDocumentId());
            String docName = doc != null ? doc.getOriginalName() : "未知文档";
            ctx.append("【来源").append(i + 1).append("】")
               .append("文档：《").append(docName).append("》\n")
               .append(chunk.getContent())
               .append("\n\n");
        }
        if (chunks.isEmpty()) {
            ctx.append("（知识库中暂无相关内容）");
        }
        return ctx.toString();
    }

    private List<CitationSource> buildSources(List<DocumentChunk> chunks, Map<Long, Document> docMap) {
        return chunks.stream()
                .map(chunk -> {
                    Document doc = docMap.get(chunk.getDocumentId());
                    return new CitationSource(
                            doc != null ? doc.getOriginalName() : "未知文档",
                            chunk.getContent().length() > 120
                                    ? chunk.getContent().substring(0, 120) + "..."
                                    : chunk.getContent()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 单文档问答：只检索指定文档的 chunk 来回答问题。
     */
    public RagResult askSingleDocument(Long documentId, String question) {
        List<DocumentChunk> chunks;
        try {
            float[] queryEmbedding = embeddingService.embed(question);
            String vectorStr = embeddingService.toPgVectorString(queryEmbedding);
            chunks = chunkRepository.findSimilarChunksByDocumentId(vectorStr, documentId, TOP_K);
        } catch (Exception e) {
            log.warn("单文档向量搜索失败: {}，回退取前 {} chunk", e.getMessage(), TOP_K);
            chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId)
                    .stream().limit(TOP_K).toList();
        }

        if (chunks.isEmpty()) {
            return new RagResult("该文档尚未完成处理，请等待处理完成后重试。", List.of());
        }

        Document doc = documentRepository.findById(documentId).orElse(null);
        String docName = doc != null ? doc.getOriginalName() : "未知文档";

        // Build context
        StringBuilder ctx = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            ctx.append("【段落").append(i + 1).append("】\n")
               .append(chunks.get(i).getContent()).append("\n\n");
        }

        List<CitationSource> sources = chunks.stream()
                .map(c -> new CitationSource(docName,
                        c.getContent().length() > 120
                                ? c.getContent().substring(0, 120) + "..."
                                : c.getContent()))
                .toList();

        // Call LLM — 宽松模式：文档有则引用，无则用自身知识回答（联网效果）
        String prompt = """
                你是一个文档问答助手。请根据以下文档内容回答用户的问题。

                规则：
                1. 如果文档中有相关信息，请基于文档内容回答，在回答中自然地提及文档名（如"根据文档..."），但不要大段复制原文，最多引用一句关键句
                2. 如果文档中没有相关信息，请直接基于你的知识正常回答，不要声明"文档中未找到"
                3. 回答保持简洁、准确、结构化
                4. 使用 Markdown 格式组织回答

                【文档名称】%s

                【文档内容】
                %s

                【用户问题】
                %s
                """.formatted(docName, ctx.toString(), maybeSolverPrefix(question));

        Prompt llmPrompt = new Prompt(List.of(
                new UserMessage(prompt)));
        ChatResponse response = chatModel.call(llmPrompt);
        String answer = response.getResult().getOutput().getText();

        return new RagResult(answer, sources);
    }

    public record RenderedPromptWithSources(List<Message> messages, List<CitationSource> sources) {}

    public record RagResult(String answer, List<CitationSource> sources) {}

    public record CitationSource(String documentTitle, String snippet) {}

    public record ConversationMessage(String role, String content) {}
}
