package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import apps.sarafrika.elimika.tenancy.util.converter.DocumentStatusConverter;
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
@Table(name = "organisation_documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganisationDocument extends BaseEntity {

    @Column(name = "organisation_uuid")
    @Filterable
    private UUID organisationUuid;

    @Column(name = "document_type_uuid")
    @Filterable
    private UUID documentTypeUuid;

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
    @Filterable
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "upload_date")
    @Filterable
    private LocalDateTime uploadDate;

    @Column(name = "is_verified")
    @Filterable
    private Boolean isVerified;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes")
    private String verificationNotes;

    @Convert(converter = DocumentStatusConverter.class)
    @Column(name = "status")
    @Filterable
    private DocumentStatus status;

    @Column(name = "expiry_date")
    @Filterable
    private LocalDate expiryDate;
}
