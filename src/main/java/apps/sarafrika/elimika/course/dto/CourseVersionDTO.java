package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "CourseVersion", description = "Immutable publish snapshot metadata for a course")
public record CourseVersionDTO(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Stable course UUID")
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "Incrementing publish version number")
        @JsonProperty("version_number")
        Integer versionNumber,

        @Schema(description = "SHA-256 hash of snapshot payload")
        @JsonProperty("snapshot_hash")
        String snapshotHash,

        @Schema(description = "Serialized immutable publish snapshot payload")
        @JsonProperty("snapshot_payload_json")
        String snapshotPayloadJson,

        @Schema(description = "UTC timestamp when this version was published")
        @JsonProperty("published_at")
        LocalDateTime publishedAt,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("created_date")
        LocalDateTime createdDate,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("created_by")
        String createdBy,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("updated_date")
        LocalDateTime updatedDate,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("updated_by")
        String updatedBy
) {
}
