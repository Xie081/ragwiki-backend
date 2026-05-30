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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final PromptTemplateService promptTemplateService;
    private final ChatModel chatModel;

    private static final int TOP_K = 5;

    /**
     * RAG query: retrieve → augment → generate.
     */
    public RagResult ask(Long kbId, String question) {
        // Step 1: Embed the question
        float[] questionEmbedding = embeddingService.embed(question);
        String vectorStr = embeddingService.toPgVectorString(questionEmbedding);

        // Step 2: Retrieve top-K similar chunks from PGVector
        // Note: We need to use a different approach since findSimilarChunks returns chunks without similarity
        // We'll fetch chunks and build context
        List<DocumentChunk> chunks = chunkRepository.findSimilarChunks(vectorStr, kbId, TOP_K);

        if (chunks.isEmpty()) {
            return new RagResult("该知识库中暂无文档内容，请先上传文档。", List.of());
        }

        // Step 3: Build context from chunks
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            Document doc = documentRepository.findById(chunk.getDocumentId()).orElse(null);
            String docName = doc != null ? doc.getOriginalName() : "未知文档";
            context.append("【来源").append(i + 1).append("】")
                   .append("文档：《").append(docName).append("》\n")
                   .append(chunk.getContent())
                   .append("\n\n");
        }

        // Step 4: Collect source citations
        List<CitationSource> sources = chunks.stream()
                .map(chunk -> {
                    Document doc = documentRepository.findById(chunk.getDocumentId()).orElse(null);
                    return new CitationSource(
                            doc != null ? doc.getOriginalName() : "未知文档",
                            chunk.getContent().length() > 200
                                    ? chunk.getContent().substring(0, 200) + "..."
                                    : chunk.getContent()
                    );
                })
                .collect(Collectors.toList());

        // Step 5: Render prompt template
        RenderedPrompt rendered = promptTemplateService.render("rag-qa", Map.of(
                "context", context.toString(),
                "question", question
        ));

        // Step 6: Call LLM
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(rendered.systemPrompt()));
        messages.add(new UserMessage(rendered.userPrompt()));

        Prompt prompt = new Prompt(messages);
        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();

        log.info("RAG: question='{}', chunks={}, answer_length={}", question, chunks.size(), answer.length());

        return new RagResult(answer, sources);
    }

    /**
     * Build rendered prompt for the given question & knowledge base (for streaming reuse).
     */
    public PromptTemplateService.RenderedPrompt buildRenderedPrompt(Long kbId, String question) {
        float[] questionEmbedding = embeddingService.embed(question);
        String vectorStr = embeddingService.toPgVectorString(questionEmbedding);

        List<DocumentChunk> chunks = chunkRepository.findSimilarChunks(vectorStr, kbId, TOP_K);

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            Document doc = documentRepository.findById(chunk.getDocumentId()).orElse(null);
            String docName = doc != null ? doc.getOriginalName() : "未知文档";
            context.append("【来源").append(i + 1).append("】")
                   .append("文档：《").append(docName).append("》\n")
                   .append(chunk.getContent())
                   .append("\n\n");
        }

        if (chunks.isEmpty()) {
            context.append("（知识库中暂无相关内容）");
        }

        return promptTemplateService.render("rag-qa", Map.of(
                "context", context.toString(),
                "question", question
        ));
    }

    public record RagResult(String answer, List<CitationSource> sources) {}

    public record CitationSource(String documentTitle, String snippet) {}
}
