package cs.sbs.web.personalprojectweb2026.repository;

import cs.sbs.web.personalprojectweb2026.model.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByKbIdOrderByCreatedAtDesc(Long kbId);
    Page<Document> findByKbIdOrderByCreatedAtDesc(Long kbId, Pageable pageable);
    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByKbId(Long kbId);
}
