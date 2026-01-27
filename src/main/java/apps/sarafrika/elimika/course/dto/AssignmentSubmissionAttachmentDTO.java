package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "AssignmentSubmissionAttachment",
        description = "Attachment stored with a student assignment submission",
        example = """
        {
            \"uuid\": \"asa12345-7890-abcd-ef01-234567890abc\",
            \"submission_uuid\": \"s1u2b3m4-5i6s-7s8i-9o10-abcdefghijkl\",
            \"original_filename\": \"audio_example.mp3\",
            \"stored_filename\": \"assignments/submissions/s1u2b3m4/uuid.mp3\",
            \"file_url\": \"https://storage.sarafrika.com/assignments/submissions/s1u2b3m4/uuid.mp3\",
            \"file_size_bytes\": 5242880,
            \"mime_type\": \"audio/mpeg\",
            \"created_date\": \"2024-11-15T10:00:00\",
            \"created_by\": \"student@sarafrika.com\",
            \"updated_date\": \"2024-11-15T10:00:00\",
            \"updated_by\": \"student@sarafrika.com\"
        }
        """
)
public record AssignmentSubmissionAttachmentDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the submission attachment.",
                example = "asa12345-7890-abcd-ef01-234567890abc",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[REQUIRED]** Submission UUID that owns this attachment.",
                example = "s1u2b3m4-5i6s-7s8i-9o10-abcdefghijkl",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("submission_uuid")
        UUID submissionUuid,

        @Schema(
                description = "**[READ-ONLY]** Original filename as uploaded.",
                example = "audio_example.mp3",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "original_filename", access = JsonProperty.Access.READ_ONLY)
        String originalFilename,

        @Schema(
                description = "**[READ-ONLY]** Stored filename/path in the storage system.",
                example = "assignments/submissions/s1u2b3m4/uuid.mp3",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "stored_filename", access = JsonProperty.Access.READ_ONLY)
        String storedFilename,

        @Schema(
                description = "**[READ-ONLY]** Publicly accessible URL for the attachment.",
                example = "https://storage.sarafrika.com/assignments/submissions/s1u2b3m4/uuid.mp3",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "file_url", access = JsonProperty.Access.READ_ONLY)
        String fileUrl,

        @Schema(
                description = "**[READ-ONLY]** File size in bytes.",
                example = "5242880",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "file_size_bytes", access = JsonProperty.Access.READ_ONLY)
        Long fileSizeBytes,

        @Schema(
                description = "**[READ-ONLY]** MIME type of the stored file.",
                example = "audio/mpeg",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "mime_type", access = JsonProperty.Access.READ_ONLY)
        String mimeType,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the attachment was created.",
                example = "2024-11-15T10:00:00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** User who created the attachment.",
                example = "student@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the attachment was last updated.",
                example = "2024-11-15T10:00:00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** User who last updated the attachment.",
                example = "student@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
