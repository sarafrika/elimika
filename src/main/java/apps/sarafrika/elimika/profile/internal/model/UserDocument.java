package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.shared.utils.Filterable;
import apps.sarafrika.elimika.shared.utils.converter.DocumentStatusConverter;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_documents")
@Getter
@Setter
@NoArgsConstructor
public class UserDocument extends UserOwnedEntity {

    @Column(name = "document_type_uuid")
    @Filterable
    private UUID documentTypeUuid;

    @Column(name = "education_uuid")
    @Filterable
    private UUID educationUuid;

    @Column(name = "experience_uuid")
    @Filterable
    private UUID experienceUuid;

    @Column(name = "membership_uuid")
    @Filterable
    private UUID membershipUuid;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "stored_filename")
    private String storedFilename;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size_bytes")
    @Filterable
    private Long fileSizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "title")
    @Filterable
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "upload_date")
    @Filterable
    private LocalDateTime uploadDate;

    @Column(name = "is_verified")
    @Filterable
    private Boolean isVerified = Boolean.FALSE;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes")
    private String verificationNotes;

    @Column(name = "status")
    @Convert(converter = DocumentStatusConverter.class)
    @Filterable
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "expiry_date")
    @Filterable
    private LocalDate expiryDate;
}
