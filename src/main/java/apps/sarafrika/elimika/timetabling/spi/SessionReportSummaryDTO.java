package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "SessionReportSummary", description = "A session report's status, for due and overdue lists")
public record SessionReportSummaryDTO(
        @JsonProperty("report_uuid") UUID reportUuid,
        @JsonProperty("instance_uuid") UUID instanceUuid,
        @JsonProperty("class_definition_uuid") UUID classDefinitionUuid,
        @JsonProperty("instructor_uuid") UUID instructorUuid,
        @JsonProperty("session_start") LocalDateTime sessionStart,
        @JsonProperty("session_end") LocalDateTime sessionEnd,
        @JsonProperty("status") SessionReportStatus status,
        @JsonProperty("due_at") LocalDateTime dueAt,
        @JsonProperty("overdue") boolean overdue,
        @JsonProperty("submitted_at") LocalDateTime submittedAt,
        @Schema(description = "Submitted after the due time")
        @JsonProperty("late") boolean late,
        @JsonProperty("editable_until") LocalDateTime editableUntil,
        @JsonProperty("revision_count") int revisionCount
) {
}
