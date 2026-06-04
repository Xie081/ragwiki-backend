package cs.sbs.web.personalprojectweb2026.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "file_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum FileType {
        PDF, MARKDOWN, TXT, DOCX, HTML
    }

    public enum DocumentStatus {
        UPLOADED, PROCESSING, COMPLETED, FAILED
    }
}
