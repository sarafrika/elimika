package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.PendingEditStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "CoursePendingEdit",
        description = """
                An edit to a published course that is awaiting admin review.

                The live course is unaffected while the edit is pending: it stays published,
                keeps accepting enrollments, and continues to serve its last-approved content.
                The proposed content lives on the draft course referenced by `draft_course_uuid`.
                """,
        example = """
        {
          "uuid": "edit-1234-5678-90ab-cdef12345678",
          "course_uuid": "course-1234-5678-90ab-cdef12345678",
          "draft_course_uuid": "draft-1234-5678-90ab-cdef12345678",
          "status": "pending",
          "submitted_by_uuid": "user-1234-5678-90ab-cdef12345678",
          "submitted_at": "2026-07-17T09:00:00",
          "reviewed_by_uuid": null,
          "reviewed_at": null,
          "review_reason": null
        }
        """
)
public record CoursePendingEditDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the pending edit.",
                example = "edit-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[READ-ONLY]** The live course this edit applies to.",
                example = "course-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "course_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID courseUuid,

        @Schema(
                description = "**[READ-ONLY]** Draft course holding the proposed content. Null once the edit is resolved.",
                example = "draft-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "draft_course_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID draftCourseUuid,

        @Schema(
                description = "**[READ-ONLY]** Review state of the edit.",
                example = "pending",
                allowableValues = {"pending", "approved", "rejected", "withdrawn"},
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
        PendingEditStatus status,

        @Schema(
                description = "**[READ-ONLY]** Internal user UUID of the course creator who submitted the edit.",
                example = "user-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "submitted_by_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID submittedByUuid,

        @Schema(
                description = "**[READ-ONLY]** When the edit was submitted for review.",
                example = "2026-07-17T09:00:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "submitted_at", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime submittedAt,

        @Schema(
                description = "**[READ-ONLY]** Internal user UUID of the admin who reviewed the edit.",
                example = "user-9999-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "reviewed_by_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID reviewedByUuid,

        @Schema(
                description = "**[READ-ONLY]** When the edit was reviewed.",
                example = "2026-07-17T11:30:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "reviewed_at", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime reviewedAt,

        @Schema(
                description = "**[READ-ONLY]** Reason the admin gave for their decision.",
                example = "Pricing change is not justified.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "review_reason", access = JsonProperty.Access.READ_ONLY)
        String reviewReason
) {
}
