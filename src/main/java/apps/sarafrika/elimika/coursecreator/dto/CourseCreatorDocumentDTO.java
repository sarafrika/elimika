package apps.sarafrika.elimika.coursecreator.dto;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document record for course creator verification and credential support.
 */
public record CourseCreatorDocumentDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @NotNull(message = "Course creator UUID is required")
        @JsonProperty("course_creator_uuid")
        UUID courseCreatorUuid,

        @NotNull(message = "Document type UUID is required")
        @JsonProperty("document_type_uuid")
        UUID documentTypeUuid,

        @JsonProperty("education_uuid")
        UUID educationUuid,

        @NotBlank(message = "Original filename is required")
        @Size(max = 255, message = "Original filename must not exceed 255 characters")
        @JsonProperty("original_filename")
        String originalFilename,

        @JsonProperty(value = "stored_filename", access = JsonProperty.Access.READ_ONLY)
        String storedFilename,

        @JsonProperty(value = "file_path", access = JsonProperty.Access.READ_ONLY)
        String filePath,

        @Positive(message = "File size must be positive")
        @JsonProperty(value = "file_size_bytes", access = JsonProperty.Access.READ_ONLY)
        Long fileSizeBytes,

        @JsonProperty(value = "mime_type", access = JsonProperty.Access.READ_ONLY)
        String mimeType,

        @JsonProperty(value = "file_hash", access = JsonProperty.Access.READ_ONLY)
        String fileHash,

        @NotBlank(message = "Document title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @JsonProperty("title")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        @JsonProperty("description")
        String description,

        @JsonProperty(value = "upload_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime uploadDate,

        @JsonProperty(value = "is_verified", access = JsonProperty.Access.READ_ONLY)
        Boolean isVerified,

        @JsonProperty(value = "verified_by", access = JsonProperty.Access.READ_ONLY)
        String verifiedBy,

        @JsonProperty(value = "verified_at", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime verifiedAt,

        @JsonProperty(value = "verification_notes", access = JsonProperty.Access.READ_ONLY)
        String verificationNotes,

        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
        DocumentStatus status,

        @JsonProperty("expiry_date")
        LocalDate expiryDate,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
