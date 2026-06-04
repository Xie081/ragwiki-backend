package cs.sbs.web.personalprojectweb2026.service;

import cs.sbs.web.personalprojectweb2026.model.entity.DocumentChunk;
import cs.sbs.web.personalprojectweb2026.service.RagService.ConversationMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private cs.sbs.web.personalprojectweb2026.repository.DocumentChunkRepository chunkRepository;
    @Mock
    private cs.sbs.web.personalprojectweb2026.repository.DocumentRepository documentRepository;
    @Mock
    private PromptTemplateService promptTemplateService;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private org.springframework.ai.chat.model.ChatModel chatModel;

    @InjectMocks
    private RagService ragService;

    // ─── Keyword extraction tests ───

    @Test
    void shouldExtractChineseKeywords() {
        String[] keywords = ragService.extractKeywords("请告诉我Spring Boot的版本是多少？");
        // Keywords are token-based: English separated from Chinese, stop words removed
        assertThat(keywords.length).isGreaterThan(0);
        // At least one token should contain "Spring" or "Boot"
        boolean found = false;
        for (String kw : keywords) {
            if (kw.contains("Spring") || kw.contains("Boot")) found = true;
        }
        assertThat(found).isTrue();
    }

    @Test
    void shouldExtractEnglishKeywords() {
        String[] keywords = ragService.extractKeywords("What is the version of Spring Boot?");
        assertThat(keywords).contains("Spring", "Boot");
        assertThat(keywords).doesNotContain("the", "is", "of");
    }

    @Test
    void shouldFilterStopWords() {
        String[] keywords = ragService.extractKeywords("这个项目是如何部署的？");
        assertThat(keywords).doesNotContain("这个", "如何", "的");
        // Should contain meaningful words
        assertThat(keywords.length).isGreaterThan(0);
    }

    @Test
    void shouldReturnEmptyForBlankInput() {
        String[] keywords = ragService.extractKeywords(null);
        assertThat(keywords).isEmpty();

        keywords = ragService.extractKeywords("   ");
        assertThat(keywords).isEmpty();
    }

    @Test
    void shouldLimitToMaxFiveKeywords() {
        String[] keywords = ragService.extractKeywords(
                "项目使用Spring Boot PostgreSQL Vue TypeScript Docker进行全栈开发部署");
        assertThat(keywords.length).isLessThanOrEqualTo(5);
    }

    // ─── Conversation history tests ───

    @Test
    void shouldBuildHistoryText() {
        List<ConversationMessage> history = List.of(
                new ConversationMessage("user", "项目的技术栈是什么？"),
                new ConversationMessage("assistant", "项目使用Spring Boot + Vue 3 + PostgreSQL。")
        );

        String result = ragService.buildHistoryText(history);

        assertThat(result).contains("👤 用户：", "项目的技术栈是什么？");
        assertThat(result).contains("🤖 助手：", "Spring Boot");
    }

    @Test
    void shouldReturnEmptyStringForNullHistory() {
        String result = ragService.buildHistoryText(null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyStringForEmptyHistory() {
        String result = ragService.buildHistoryText(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldTruncateLongMessagesInHistory() {
        String longContent = "A".repeat(600);
        List<ConversationMessage> history = List.of(
                new ConversationMessage("user", longContent)
        );

        String result = ragService.buildHistoryText(history);

        assertThat(result).contains("...");
        assertThat(result.length()).isLessThan(600);
    }

    // ─── Chunk merging tests ───

    @Test
    void shouldMergeAndDeduplicateChunks() {
        List<DocumentChunk> keywordChunks = new ArrayList<>();
        keywordChunks.add(buildChunk(1L, "keyword match 1"));
        keywordChunks.add(buildChunk(2L, "keyword match 2"));

        List<DocumentChunk> vectorChunks = new ArrayList<>();
        vectorChunks.add(buildChunk(2L, "duplicate - same id"));
        vectorChunks.add(buildChunk(3L, "vector match"));

        List<DocumentChunk> merged = ragService.mergeChunkResults(keywordChunks, vectorChunks, 5);

        assertThat(merged).hasSize(3);
        assertThat(merged.get(0).getId()).isEqualTo(1L);
        assertThat(merged.get(1).getId()).isEqualTo(2L);
        assertThat(merged.get(2).getId()).isEqualTo(3L);
    }

    @Test
    void shouldRespectMaxResultsLimit() {
        List<DocumentChunk> keywordChunks = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            keywordChunks.add(buildChunk(i, "keyword " + i));
        }

        List<DocumentChunk> vectorChunks = new ArrayList<>();
        for (long i = 6; i <= 10; i++) {
            vectorChunks.add(buildChunk(i, "vector " + i));
        }

        List<DocumentChunk> merged = ragService.mergeChunkResults(keywordChunks, vectorChunks, 3);

        assertThat(merged).hasSize(3);
    }

    @Test
    void shouldHandleEmptyKeywordResults() {
        List<DocumentChunk> keywordChunks = List.of();
        List<DocumentChunk> vectorChunks = new ArrayList<>();
        vectorChunks.add(buildChunk(1L, "vector result 1"));
        vectorChunks.add(buildChunk(2L, "vector result 2"));

        List<DocumentChunk> merged = ragService.mergeChunkResults(keywordChunks, vectorChunks, 5);

        assertThat(merged).hasSize(2);
        assertThat(merged.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void shouldHandleEmptyVectorResults() {
        List<DocumentChunk> keywordChunks = new ArrayList<>();
        keywordChunks.add(buildChunk(1L, "keyword result"));
        List<DocumentChunk> vectorChunks = List.of();

        List<DocumentChunk> merged = ragService.mergeChunkResults(keywordChunks, vectorChunks, 5);

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).getId()).isEqualTo(1L);
    }

    // ─── Helper ───

    private DocumentChunk buildChunk(Long id, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setContent(content);
        chunk.setChunkIndex(0);
        chunk.setDocumentId(1L);
        chunk.setKbId(1L);
        return chunk;
    }
}
