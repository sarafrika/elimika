package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.converter.DocumentStatusConverter;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_creator_documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseCreatorDocument extends BaseEntity {

    @Column(name = "course_creator_uuid")
    private UUID courseCreatorUuid;

    @Column(name = "document_type_uuid")
    private UUID documentTypeUuid;

    @Column(name = "education_uuid")
    private UUID educationUuid;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "stored_filename")
    private String storedFilename;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes")
    private String verificationNotes;

    @Column(name = "status")
    @Convert(converter = DocumentStatusConverter.class)
    private DocumentStatus status;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
