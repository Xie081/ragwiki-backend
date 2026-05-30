package cs.sbs.web.personalprojectweb2026.repository;

import cs.sbs.web.personalprojectweb2026.model.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    Optional<PromptTemplate> findByName(String name);
}
