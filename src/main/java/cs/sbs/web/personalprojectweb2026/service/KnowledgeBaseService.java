package cs.sbs.web.personalprojectweb2026.service;

import cs.sbs.web.personalprojectweb2026.model.entity.Document;
import cs.sbs.web.personalprojectweb2026.model.entity.KnowledgeBase;
import cs.sbs.web.personalprojectweb2026.repository.DocumentChunkRepository;
import cs.sbs.web.personalprojectweb2026.repository.DocumentRepository;
import cs.sbs.web.personalprojectweb2026.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository kbRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    public List<KnowledgeBase> listByUser(Long userId) {
        return kbRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public KnowledgeBase getById(Long id, Long userId) {
        KnowledgeBase kb = kbRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在"));
        if (!kb.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该知识库");
        }
        return kb;
    }

    public KnowledgeBase create(String name, String description, Long userId) {
        KnowledgeBase kb = KnowledgeBase.builder()
                .name(name)
                .description(description)
                .userId(userId)
                .build();
        return kbRepository.save(kb);
    }

    public KnowledgeBase update(Long id, String name, String description, Long userId) {
        KnowledgeBase kb = getById(id, userId);
        if (name != null) kb.setName(name);
        if (description != null) kb.setDescription(description);
        return kbRepository.save(kb);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        KnowledgeBase kb = getById(id, userId);
        List<Document> docs = documentRepository.findByKbIdOrderByCreatedAtDesc(id);
        // Delete chunks and files from disk
        for (Document doc : docs) {
            chunkRepository.deleteByDocumentId(doc.getId());
            try {
                Files.deleteIfExists(Path.of(doc.getStoragePath()));
            } catch (IOException ignored) {
            }
        }
        documentRepository.deleteByKbId(id);
        kbRepository.delete(kb);
    }
}
