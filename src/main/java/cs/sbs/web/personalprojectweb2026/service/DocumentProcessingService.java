package cs.sbs.web.personalprojectweb2026.service;

import cs.sbs.web.personalprojectweb2026.model.entity.Document;
import cs.sbs.web.personalprojectweb2026.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final DocumentParserService parserService;
    private final TextChunker textChunker;
    private final PromptTemplateService promptTemplateService;
    private final ChatModel chatModel;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    private static final int SUMMARY_MAX_CHARS = 3000;

    /**
     * Process a document asynchronously: parse → chunk → embed → store → summarize.
     */
    @Async
    @Transactional
    public void processDocument(Long documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));

        try {
            // Update status to PROCESSING
            doc.setStatus(Document.DocumentStatus.PROCESSING);
            documentRepository.save(doc);

            // Step 1: Parse
            Path filePath = Path.of(doc.getStoragePath());
            String text = parserService.parse(filePath, doc.getFileType().name());

            // Step 2: Chunk
            List<String> chunks = textChunker.chunk(text);
            log.info("Document {} split into {} chunks", documentId, chunks.size());

            if (chunks.isEmpty()) {
                doc.setStatus(Document.DocumentStatus.COMPLETED);
                documentRepository.save(doc);
                return;
            }

            // Step 3 & 4: Embed + store each chunk via DeepSeek embedding API
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                float[] embedding = embeddingService.embed(chunkText);
                String vectorStr = embeddingService.toPgVectorString(embedding);

                jdbcTemplate.update("""
                    INSERT INTO document_chunks (content, chunk_index, embedding, document_id, kb_id, created_at)
                    VALUES (?, ?, ?::vector, ?, ?, NOW())
                    """, chunkText, i, vectorStr, doc.getId(), doc.getKbId());
            }

            // Step 5: Generate AI summary
            try {
                String summary = generateSummary(doc.getOriginalName(), text);
                doc.setSummary(summary);
                log.info("Document {} summary generated", documentId);
            } catch (Exception e) {
                log.warn("Summary generation failed for document {}: {}", documentId, e.getMessage());
                doc.setSummary("摘要生成失败");
            }

            // Update status to COMPLETED
            doc.setStatus(Document.DocumentStatus.COMPLETED);
            documentRepository.save(doc);
            log.info("Document {} processing completed, {} chunks stored", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to process document {}: {}", documentId, e.getMessage(), e);
            doc.setStatus(Document.DocumentStatus.FAILED);
            documentRepository.save(doc);
        }
    }

    private String generateSummary(String title, String fullText) {
        // Truncate text for summary generation
        String content = fullText.length() > SUMMARY_MAX_CHARS
                ? fullText.substring(0, SUMMARY_MAX_CHARS) + "..."
                : fullText;

        // Render summarize prompt template
        var rendered = promptTemplateService.render("summarize", Map.of(
                "document_title", title,
                "content", content
        ));

        // Call LLM
        var prompt = new Prompt(List.of(
                new SystemMessage(rendered.systemPrompt()),
                new UserMessage(rendered.userPrompt())
        ));
        var response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }
}
