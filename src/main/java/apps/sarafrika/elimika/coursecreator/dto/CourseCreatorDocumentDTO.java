package apps.sarafrika.elimika.coursecreator.dto;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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

        @JsonProperty("experience_uuid")
        UUID experienceUuid,

        @JsonProperty("membership_uuid")
        UUID membershipUuid,

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

    @JsonProperty(value = "file_size_formatted", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Human-readable formatted file size.",
            example = "2.0 MB",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public String getFileSizeFormatted() {
        if (fileSizeBytes == null || fileSizeBytes <= 0) {
            return "0 B";
        }

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = fileSizeBytes.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }

    @JsonProperty(value = "is_expired", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Indicates if the document has expired based on the expiry date.",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now(ZoneOffset.UTC));
    }

    @JsonProperty(value = "days_until_expiry", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Number of days until document expiry. Returns null if no expiry date or already expired.",
            example = "1095",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public Integer getDaysUntilExpiry() {
        if (expiryDate == null || isExpired()) {
            return null;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(LocalDate.now(ZoneOffset.UTC), expiryDate));
    }

    @JsonProperty(value = "is_pending_verification", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Indicates if the document is pending verification.",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public boolean isPendingVerification() {
        return isVerified == null || !isVerified;
    }

    @JsonProperty(value = "has_expiry_date", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Indicates if the document has an expiry date configured.",
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public boolean hasExpiryDate() {
        return expiryDate != null;
    }

    @JsonProperty(value = "verification_status", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Human-readable verification status of the document.",
            example = "VERIFIED",
            allowableValues = {"VERIFIED", "PENDING", "REJECTED"},
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public String getVerificationStatus() {
        if (isVerified == null) {
            return "PENDING";
        }
        return isVerified ? "VERIFIED" : "REJECTED";
    }
}
