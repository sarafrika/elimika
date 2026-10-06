package apps.sarafrika.elimika.profile.spi;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** A credential document filed on the user's profile, optionally backing one education, experience or membership. */
@Schema(name = "UserDocument", description = "A credential document on the user-owned professional profile")
public record UserDocumentDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "user_uuid", access = JsonProperty.Access.READ_ONLY) UUID userUuid,
        @NotNull(message = "Document type UUID is required")
        @JsonProperty("document_type_uuid") UUID documentTypeUuid,
        @JsonProperty("education_uuid") UUID educationUuid,
        @JsonProperty("experience_uuid") UUID experienceUuid,
        @JsonProperty("membership_uuid") UUID membershipUuid,
        @JsonProperty(value = "original_filename", access = JsonProperty.Access.READ_ONLY) String originalFilename,
        @JsonProperty(value = "stored_filename", access = JsonProperty.Access.READ_ONLY) String storedFilename,
        @JsonProperty(value = "file_path", access = JsonProperty.Access.READ_ONLY) String filePath,
        @JsonProperty(value = "file_size_bytes", access = JsonProperty.Access.READ_ONLY) Long fileSizeBytes,
        @JsonProperty(value = "mime_type", access = JsonProperty.Access.READ_ONLY) String mimeType,
        @JsonProperty(value = "file_hash", access = JsonProperty.Access.READ_ONLY) String fileHash,
        @Size(max = 255) @JsonProperty("title") String title,
        @Size(max = 2000) @JsonProperty("description") String description,
        @JsonProperty(value = "upload_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime uploadDate,
        @JsonProperty(value = "is_verified", access = JsonProperty.Access.READ_ONLY) Boolean isVerified,
        @JsonProperty(value = "verified_by", access = JsonProperty.Access.READ_ONLY) String verifiedBy,
        @JsonProperty(value = "verified_at", access = JsonProperty.Access.READ_ONLY) LocalDateTime verifiedAt,
        @JsonProperty(value = "verification_notes", access = JsonProperty.Access.READ_ONLY) String verificationNotes,
        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY) DocumentStatus status,
        @JsonProperty("expiry_date") LocalDate expiryDate,
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime createdDate,
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY) String createdBy,
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate,
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY) String updatedBy
) {

    /** API-relative URL streaming this document's file to anyone allowed to read the profile's credentials. */
    @JsonProperty(value = "file_url", access = JsonProperty.Access.READ_ONLY)
    public String getFileUrl() {
        if (userUuid == null || uuid == null) {
            return null;
        }
        return "/api/v1/users/" + userUuid + "/profile/documents/" + uuid + "/file";
    }
}
