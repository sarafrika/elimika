package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.common.enums.DocumentStatus;
import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "instructor_documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InstructorDocument extends BaseEntity {

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "document_type_uuid")
    private UUID documentTypeUuid;

    // Optional references to specific records
    @Column(name = "education_uuid")
    private UUID educationUuid;

    @Column(name = "experience_uuid")
    private UUID experienceUuid;

    @Column(name = "membership_uuid")
    private UUID membershipUuid;

    // File information
    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "stored_filename")
    private String storedFilename;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size_bytes")
    @Positive
    private Long fileSizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_hash")
    private String fileHash;

    // Metadata
    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "document_status_enum")
    @JdbcTypeCode(SqlTypes.ENUM)
    private DocumentStatus status;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}