package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "CourseVersionSnapshot",
        description = """
                An approved version of a course's content. One is written each time an edit is
                promoted onto the live course, giving a durable record of what the course
                looked like at each approved version.
                """,
        example = """
        {
          "uuid": "snap-1234-5678-90ab-cdef12345678",
          "course_uuid": "course-1234-5678-90ab-cdef12345678",
          "version_number": 3,
          "pending_edit_uuid": "edit-1234-5678-90ab-cdef12345678",
          "created_date": "2026-07-17T11:30:00",
          "created_by": "admin@example.com"
        }
        """
)
public record CourseVersionSnapshotDTO(

        @Schema(description = "**[READ-ONLY]** Unique identifier for the snapshot.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "**[READ-ONLY]** The course this version belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "course_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID courseUuid,

        @Schema(description = "**[READ-ONLY]** Monotonic version number, starting at 1.", example = "3", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "version_number", access = JsonProperty.Access.READ_ONLY)
        Integer versionNumber,

        @Schema(
                description = "**[READ-ONLY]** Full course tree at this version: course fields, category uuids, lessons and their content.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "snapshot", access = JsonProperty.Access.READ_ONLY)
        JsonNode snapshot,

        @Schema(description = "**[READ-ONLY]** The edit whose promotion produced this version, if any.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "pending_edit_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID pendingEditUuid,

        @Schema(description = "**[READ-ONLY]** When this version became live.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(description = "**[READ-ONLY]** Who promoted this version.", example = "admin@example.com", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy
) {
}
