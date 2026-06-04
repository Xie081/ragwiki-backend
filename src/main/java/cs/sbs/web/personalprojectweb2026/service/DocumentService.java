package cs.sbs.web.personalprojectweb2026.service;

import cs.sbs.web.personalprojectweb2026.model.entity.Document;
import cs.sbs.web.personalprojectweb2026.model.entity.DocumentChunk;
import cs.sbs.web.personalprojectweb2026.repository.DocumentChunkRepository;
import cs.sbs.web.personalprojectweb2026.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeBaseService kbService;
    private final DocumentProcessingService processingService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public List<Document> listByKb(Long kbId, Long userId) {
        kbService.getById(kbId, userId); // verify access
        return documentRepository.findByKbIdOrderByCreatedAtDesc(kbId);
    }

    public Page<Document> listByKb(Long kbId, Long userId, int page, int size) {
        kbService.getById(kbId, userId); // verify access
        Pageable pageable = PageRequest.of(page, size);
        return documentRepository.findByKbIdOrderByCreatedAtDesc(kbId, pageable);
    }

    public Document getById(Long id, Long userId) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        if (!doc.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该文档");
        }
        return doc;
    }

    public Document upload(Long kbId, MultipartFile file, Long userId) throws IOException {
        kbService.getById(kbId, userId); // verify access

        // Determine file type
        String originalName = file.getOriginalFilename();
        Document.FileType fileType = detectFileType(originalName);

        // Store file
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String storedName = UUID.randomUUID() + "_" + originalName;
        Path filePath = uploadPath.resolve(storedName);
        file.transferTo(filePath.toFile());

        // Create document record
        Document doc = Document.builder()
                .title(originalName != null ? originalName : "未命名文档")
                .fileType(fileType)
                .originalName(originalName)
                .storagePath(filePath.toString())
                .fileSize(file.getSize())
                .status(Document.DocumentStatus.UPLOADED)
                .kbId(kbId)
                .userId(userId)
                .build();

        doc = documentRepository.save(doc);

        // Trigger async processing (parse → chunk → embed → store)
        processingService.processDocument(doc.getId());

        return doc;
    }

    /**
     * Get document detail with chunks and summary.
     */
    public Map<String, Object> getDetailWithChunks(Long id, Long userId) {
        Document doc = getById(id, userId);
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(id);

        List<Map<String, Object>> chunkList = chunks.stream()
                .map(chunk -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("chunkIndex", chunk.getChunkIndex());
                    item.put("content", chunk.getContent());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("document", doc);
        result.put("chunks", chunkList);
        result.put("chunkCount", chunks.size());
        return result;
    }

    public void delete(Long id, Long userId) {
        Document doc = getById(id, userId);
        // Delete chunks
        chunkRepository.deleteByDocumentId(id);
        // Delete file from disk
        try {
            Files.deleteIfExists(Path.of(doc.getStoragePath()));
        } catch (IOException ignored) {
        }
        documentRepository.delete(doc);
    }

    /**
     * Detect file type from extension.
     */
    private Document.FileType detectFileType(String originalName) {
        if (originalName == null) {
            throw new RuntimeException("无法识别文件类型");
        }
        String name = originalName.toLowerCase();
        if (name.endsWith(".pdf"))               return Document.FileType.PDF;
        if (name.endsWith(".md") || name.endsWith(".markdown")) return Document.FileType.MARKDOWN;
        if (name.endsWith(".txt"))               return Document.FileType.TXT;
        if (name.endsWith(".docx"))              return Document.FileType.DOCX;
        if (name.endsWith(".html") || name.endsWith(".htm")) return Document.FileType.HTML;
        throw new RuntimeException("不支持的文档格式，支持: PDF, Markdown, TXT, DOCX, HTML");
    }
}
