package cs.sbs.web.personalprojectweb2026.controller;

import cs.sbs.web.personalprojectweb2026.config.SecurityUtil;
import cs.sbs.web.personalprojectweb2026.model.entity.Document;
import cs.sbs.web.personalprojectweb2026.model.entity.DocumentChunk;
import cs.sbs.web.personalprojectweb2026.repository.DocumentChunkRepository;
import cs.sbs.web.personalprojectweb2026.repository.DocumentRepository;
import cs.sbs.web.personalprojectweb2026.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final SecurityUtil securityUtil;

    /**
     * Semantic search across all documents in a knowledge base.
     */
    @GetMapping("/{kbId}")
    public ResponseEntity<?> search(@PathVariable Long kbId, @RequestParam("q") String query) {
        Long userId = securityUtil.getCurrentUser().getId();

        // Embed the query
        float[] queryEmbedding = embeddingService.embed(query);
        String vectorStr = embeddingService.toPgVectorString(queryEmbedding);

        // Search across the knowledge base (top 10 results)
        List<DocumentChunk> chunks = chunkRepository.findSimilarChunks(vectorStr, kbId, 10);

        // Build search results with document info
        List<Map<String, Object>> results = chunks.stream()
                .map(chunk -> {
                    Document doc = documentRepository.findById(chunk.getDocumentId()).orElse(null);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("chunkId", chunk.getId());
                    item.put("content", chunk.getContent().length() > 300
                            ? chunk.getContent().substring(0, 300) + "..."
                            : chunk.getContent());
                    item.put("fullContent", chunk.getContent());
                    item.put("documentId", chunk.getDocumentId());
                    item.put("documentTitle", doc != null ? doc.getOriginalName() : "未知文档");
                    item.put("chunkIndex", chunk.getChunkIndex());
                    return item;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "query", query,
                "totalHits", results.size(),
                "results", results
        ));
    }
}
