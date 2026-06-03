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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    private final ChatModel chatModel;

    private static final int TOP_K = 5;

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
     * RAG query: retrieve → augment → generate.
     */
    public RagResult ask(Long kbId, String question) {
        List<DocumentChunk> chunks = retrieveChunks(kbId, question);

        if (chunks.isEmpty()) {
            return new RagResult("该知识库中暂无文档内容，请先上传文档。", List.of());
        }

        // Batch-load all documents (fixes N+1)
        Map<Long, Document> docMap = buildDocMap(chunks);

        // Build context and sources
        String context = buildContext(chunks, docMap);
        List<CitationSource> sources = buildSources(chunks, docMap);

        // Render prompt template
        RenderedPrompt rendered = promptTemplateService.render("rag-qa", Map.of(
                "context", context,
                "question", question
        ));

        // Call LLM
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(rendered.systemPrompt()));
        messages.add(new UserMessage(rendered.userPrompt()));

        Prompt prompt = new Prompt(messages);
        long llmStart = System.currentTimeMillis();
        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        long llmMs = System.currentTimeMillis() - llmStart;

        log.info("RAG: question='{}', chunks={}, answer_length={}, llm_time={}ms",
                question, chunks.size(), answer.length(), llmMs);

        return new RagResult(answer, sources);
    }

    /**
     * Build rendered prompt + sources for streaming.
     */
    public RenderedPromptWithSources buildRenderedPrompt(Long kbId, String question) {
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

        var rendered = promptTemplateService.render("rag-qa", Map.of(
                "context", context,
                "question", question
        ));

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(rendered.systemPrompt()));
        messages.add(new UserMessage(rendered.userPrompt()));

        long totalMs = System.currentTimeMillis() - t0;
        log.info("RAG prep: keyword_search={}ms, total={}ms (chunks={})",
                searchMs, totalMs, chunks.size());

        return new RenderedPromptWithSources(messages, sources);
    }

    // ─── Private helpers ───

    /**
     * Extract keywords from question and search chunks by keyword matching.
     */
    private List<DocumentChunk> retrieveChunks(Long kbId, String question) {
        String[] keywords = extractKeywords(question);

        // Pad keywords array to exactly 5 (repository needs 5 params)
        String[] padded = Arrays.copyOf(keywords, 5);
        for (int i = 0; i < 5; i++) {
            if (padded[i] == null || padded[i].isEmpty()) {
                padded[i] = "";  // empty string → ILIKE '%' + '' + '%' matches nothing
            }
        }

        List<DocumentChunk> chunks = chunkRepository.searchByKeywords(
                padded[0], padded[1], padded[2], padded[3], padded[4],
                kbId, TOP_K);

        log.debug("Keywords: {}, matched {} chunks", Arrays.toString(keywords), chunks.size());
        return chunks;
    }

    /**
     * Extract meaningful keywords from a question.
     * Splits by noise characters, removes stop words, returns top-5 longest tokens.
     */
    private String[] extractKeywords(String text) {
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
                            chunk.getContent().length() > 200
                                    ? chunk.getContent().substring(0, 200) + "..."
                                    : chunk.getContent()
                    );
                })
                .collect(Collectors.toList());
    }

    public record RenderedPromptWithSources(List<Message> messages, List<CitationSource> sources) {}

    public record RagResult(String answer, List<CitationSource> sources) {}

    public record CitationSource(String documentTitle, String snippet) {}
}
