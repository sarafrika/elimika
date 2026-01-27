package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "AssignmentAttachment",
        description = "Attachment stored with an assignment for instructor-provided resources",
        example = """
        {
            \"uuid\": \"aa123456-7890-abcd-ef01-234567890abc\",
            \"assignment_uuid\": \"a1s2s3g4-5n6m-7e8n-9t10-abcdefghijkl\",
            \"original_filename\": \"assignment_brief.pdf\",
            \"stored_filename\": \"assignments/a1s2s3g4/attachments/uuid.pdf\",
            \"file_url\": \"https://storage.sarafrika.com/assignments/a1s2s3g4/attachments/uuid.pdf\",
            \"file_size_bytes\": 2048576,
            \"mime_type\": \"application/pdf\",
            \"created_date\": \"2024-11-15T09:30:00\",
            \"created_by\": \"instructor@sarafrika.com\",
            \"updated_date\": \"2024-11-15T09:30:00\",
            \"updated_by\": \"instructor@sarafrika.com\"
        }
        """
)
public record AssignmentAttachmentDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the attachment.",
                example = "aa123456-7890-abcd-ef01-234567890abc",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[REQUIRED]** Assignment UUID that owns this attachment.",
                example = "a1s2s3g4-5n6m-7e8n-9t10-abcdefghijkl",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("assignment_uuid")
        UUID assignmentUuid,

        @Schema(
                description = "**[READ-ONLY]** Original filename as uploaded.",
                example = "assignment_brief.pdf",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "original_filename", access = JsonProperty.Access.READ_ONLY)
        String originalFilename,

        @Schema(
                description = "**[READ-ONLY]** Stored filename/path in the storage system.",
                example = "assignments/a1s2s3g4/attachments/uuid.pdf",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "stored_filename", access = JsonProperty.Access.READ_ONLY)
        String storedFilename,

        @Schema(
                description = "**[READ-ONLY]** Publicly accessible URL for the attachment.",
                example = "https://storage.sarafrika.com/assignments/a1s2s3g4/attachments/uuid.pdf",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "file_url", access = JsonProperty.Access.READ_ONLY)
        String fileUrl,

        @Schema(
                description = "**[READ-ONLY]** File size in bytes.",
                example = "2048576",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "file_size_bytes", access = JsonProperty.Access.READ_ONLY)
        Long fileSizeBytes,

        @Schema(
                description = "**[READ-ONLY]** MIME type of the stored file.",
                example = "application/pdf",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "mime_type", access = JsonProperty.Access.READ_ONLY)
        String mimeType,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the attachment was created.",
                example = "2024-11-15T09:30:00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** User who created the attachment.",
                example = "instructor@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the attachment was last updated.",
                example = "2024-11-15T09:30:00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** User who last updated the attachment.",
                example = "instructor@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
