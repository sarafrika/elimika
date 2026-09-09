package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A validation document attached to an organisation during registration or review.
 */
@Schema(name = "OrganisationDocument", description = "Validation document attached to an organisation")
public record OrganisationDocumentDTO(

        @Schema(description = "Unique identifier of the document", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Organisation the document belongs to")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Document type this file satisfies")
        @JsonProperty("document_type_uuid")
        UUID documentTypeUuid,

        @Schema(description = "Filename as supplied by the uploader")
        @JsonProperty("original_filename")
        String originalFilename,

        @Schema(description = "Filename as stored")
        @JsonProperty("stored_filename")
        String storedFilename,

        @Schema(description = "Path the stored file is served from")
        @JsonProperty("file_path")
        String filePath,

        @Schema(description = "Size of the stored file in bytes")
        @JsonProperty("file_size_bytes")
        Long fileSizeBytes,

        @Schema(description = "MIME type of the stored file")
        @JsonProperty("mime_type")
        String mimeType,

        @Schema(description = "Human-readable title for the document")
        @JsonProperty("title")
        String title,

        @Schema(description = "Notes supplied with the document")
        @JsonProperty("description")
        String description,

        @Schema(description = "When the document was uploaded", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("upload_date")
        LocalDateTime uploadDate,

        @Schema(description = "Whether a reviewer has verified the document", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("is_verified")
        Boolean isVerified,

        @Schema(description = "Review status of the document", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("status")
        DocumentStatus status,

        @Schema(description = "Expiry date, where the document type carries one")
        @JsonProperty("expiry_date")
        LocalDate expiryDate,

        @Schema(description = "When the record was created", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("created_date")
        LocalDateTime createdDate
) {
}
