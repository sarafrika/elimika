package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import apps.sarafrika.elimika.course.util.enums.ModerationContentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ContentModerationHistory",
        description = "Audit record of an admin moderation decision on a course or training program.",
        example = """
        {
          "uuid": "mod-1234-5678-90ab-cdef12345678",
          "content_type": "course",
          "content_uuid": "course-1234-5678-90ab-cdef12345678",
          "action": "rejected",
          "reason": "Lesson 3 video is missing captions.",
          "moderator_uuid": "user-1234-5678-90ab-cdef12345678",
          "created_date": "2026-07-13T09:00:00",
          "created_by": "admin@example.com"
        }
        """
)
public record ContentModerationHistoryDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the moderation record.",
                example = "mod-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[READ-ONLY]** Type of the moderated content.",
                example = "course",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "content_type", access = JsonProperty.Access.READ_ONLY)
        ModerationContentType contentType,

        @Schema(
                description = "**[READ-ONLY]** UUID of the moderated course or training program.",
                example = "course-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "content_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID contentUuid,

        @Schema(
                description = "**[READ-ONLY]** Moderation decision taken.",
                example = "rejected",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "action", access = JsonProperty.Access.READ_ONLY)
        ModerationAction action,

        @Schema(
                description = "**[READ-ONLY]** Reason provided by the moderator.",
                example = "Lesson 3 video is missing captions.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "reason", access = JsonProperty.Access.READ_ONLY)
        String reason,

        @Schema(
                description = "**[READ-ONLY]** Internal user UUID of the admin who made the decision.",
                example = "user-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "moderator_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID moderatorUuid,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the decision was recorded.",
                example = "2026-07-13T09:00:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** Created by identifier (typically the admin email or system).",
                example = "admin@example.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy
) {
}
