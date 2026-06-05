package cs.sbs.web.personalprojectweb2026.controller;

import cs.sbs.web.personalprojectweb2026.config.SecurityUtil;
import cs.sbs.web.personalprojectweb2026.model.entity.Document;
import cs.sbs.web.personalprojectweb2026.service.DocumentService;
import cs.sbs.web.personalprojectweb2026.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final RagService ragService;
    private final SecurityUtil securityUtil;

    @GetMapping("/knowledge-bases/{kbId}/documents")
    public ResponseEntity<?> listByKb(
            @PathVariable Long kbId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = securityUtil.getCurrentUserId();
        var result = documentService.listByKb(kbId, userId, page, size);
        return ResponseEntity.ok(Map.of(
                "data", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "currentPage", result.getNumber(),
                "pageSize", result.getSize()
        ));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(documentService.getById(id, userId));
    }

    @GetMapping("/documents/{id}/detail")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(documentService.getDetailWithChunks(id, userId));
    }

    @PostMapping("/knowledge-bases/{kbId}/documents")
    public ResponseEntity<?> upload(@PathVariable Long kbId, @RequestParam("file") MultipartFile file) {
        Long userId = securityUtil.getCurrentUserId();
        try {
            Document doc = documentService.upload(kbId, file, userId);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        documentService.delete(id, userId);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @PostMapping("/documents/{id}/reprocess")
    public ResponseEntity<?> reprocess(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        documentService.reprocess(id, userId);
        return ResponseEntity.ok(Map.of("message", "已重新加入处理队列"));
    }

    @PostMapping("/documents/{id}/ask")
    public ResponseEntity<?> askDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long userId = securityUtil.getCurrentUserId();
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入问题"));
        }
        // Verify access
        documentService.getById(id, userId);
        var result = ragService.askSingleDocument(id, question);
        return ResponseEntity.ok(result);
    }
}
