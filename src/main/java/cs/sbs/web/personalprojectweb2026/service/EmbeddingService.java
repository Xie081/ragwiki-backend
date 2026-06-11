package cs.sbs.web.personalprojectweb2026.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generate embedding vectors via SiliconFlow API (OpenAI-compatible).
 * Bypasses Spring AI auto-configuration to avoid base-url conflicts with DeepSeek chat.
 */
@Service
@Slf4j
public class EmbeddingService {

    private static final int BATCH_SIZE = 16;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String model;

    public EmbeddingService(
            @Value("${spring.ai.openai.embedding.base-url:https://api.siliconflow.cn/v1}") String baseUrl,
            @Value("${spring.ai.openai.embedding.api-key:}") String apiKey,
            @Value("${spring.ai.openai.embedding.options.model:BAAI/bge-m3}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl + "/embeddings")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("EmbeddingService initialized: baseUrl={}, model={}, batchSize={}", baseUrl, model, BATCH_SIZE);
    }

    /**
     * Generate embedding vector for a single text.
     */
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    /**
     * Generate embedding vectors for multiple texts in batches.
     * Each batch is sent as a single API call, dramatically reducing total processing time.
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        // Process in batches
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);
            results.addAll(doEmbedBatch(batch));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<float[]> doEmbedBatch(List<String> texts) {
        try {
            long start = System.currentTimeMillis();
            String body = restClient.post()
                    .body(Map.of("model", model, "input", texts))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");

            // Sort by index and convert to float arrays
            List<float[]> embeddings = new ArrayList<>();
            for (var node : data) {
                JsonNode embeddingNode = node.path("embedding");
                float[] embedding = new float[embeddingNode.size()];
                for (int j = 0; j < embedding.length; j++) {
                    embedding[j] = (float) embeddingNode.get(j).asDouble();
                }
                embeddings.add(embedding);
            }

            log.debug("Batch embedding: {} texts, {} ms", texts.size(),
                    System.currentTimeMillis() - start);
            return embeddings;
        } catch (Exception e) {
            log.error("Batch embedding API call failed: {}", e.getMessage());
            throw new RuntimeException("Embedding 批量生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * Convert embedding float array to PGVector-compatible string: [0.1,0.2,...]
     */
    public String toPgVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
