package cs.sbs.web.personalprojectweb2026.controller;

import cs.sbs.web.personalprojectweb2026.config.SecurityUtil;
import cs.sbs.web.personalprojectweb2026.model.entity.KnowledgeBase;
import cs.sbs.web.personalprojectweb2026.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;
    private final SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<?> list() {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(kbService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(kbService.getById(id, userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Long userId = securityUtil.getCurrentUserId();
        String name = body.getOrDefault("name", "未命名知识库");
        String description = body.getOrDefault("description", "");
        KnowledgeBase kb = kbService.create(name, description, userId);
        return ResponseEntity.ok(kb);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = securityUtil.getCurrentUserId();
        KnowledgeBase kb = kbService.update(id, body.get("name"), body.get("description"), userId);
        return ResponseEntity.ok(kb);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        kbService.delete(id, userId);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
}
