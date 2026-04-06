package apps.sarafrika.elimika.coursecreator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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

        @JsonProperty(value = "is_verified", access = JsonProperty.Access.READ_ONLY)
        Boolean isVerified,

        @JsonProperty(value = "verified_by", access = JsonProperty.Access.READ_ONLY)
        String verifiedBy,

        @JsonProperty(value = "verified_at", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime verifiedAt,

        @JsonProperty(value = "verification_notes", access = JsonProperty.Access.READ_ONLY)
        String verificationNotes,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
    @JsonProperty(value = "file_url", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** API-relative URL for previewing or downloading the uploaded document.",
            example = "/api/v1/course-creators/c1e2a3t4-5o6r-7c8r-9e10-abcdefghijkl/documents/files/profile_documents/course-creators/c1e2a3t4-5o6r-7c8r-9e10-abcdefghijkl/550e8400-e29b-41d4-a716-446655440000.pdf",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public String getFileUrl() {
        if (courseCreatorUuid == null || storedFilename == null || storedFilename.isBlank()) {
            return null;
        }
        return "/api/v1/course-creators/" + courseCreatorUuid + "/documents/files/" + storedFilename;
    }
}
