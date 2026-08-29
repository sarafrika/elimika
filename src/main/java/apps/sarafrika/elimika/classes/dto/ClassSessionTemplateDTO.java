package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ClassSessionTemplate",
        description = "Time slot template used during class creation to generate scheduled instances with optional recurrence"
)
public record ClassSessionTemplateDTO(

        @Schema(description = "**[READ-ONLY]** Unique identifier for this persisted class session template.")
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "Start time for the first occurrence (UTC)", example = "2025-01-15T14:00:00Z")
        @NotNull(message = "Session start time is required")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "End time for the first occurrence (UTC). If duration_minutes is supplied, the backend derives this value.", example = "2025-01-15T15:30:00Z")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Positive session duration in minutes. When supplied it is authoritative and the backend derives end_time from start_time.", example = "90", minimum = "1")
        @Positive(message = "Session duration_minutes must be positive")
        @JsonProperty("duration_minutes")
        Integer durationMinutes,

        @Schema(description = "Inline recurrence rule for this session template", nullable = true)
        @JsonProperty("recurrence")
        ClassRecurrenceDTO recurrence,

        @Schema(description = "Conflict handling strategy: FAIL (default), SKIP, ROLLOVER", example = "FAIL")
        @JsonProperty("conflict_resolution")
        ConflictResolutionStrategy conflictResolution
) {
        public ClassSessionTemplateDTO(
                LocalDateTime startTime,
                LocalDateTime endTime,
                ClassRecurrenceDTO recurrence,
                ConflictResolutionStrategy conflictResolution
        ) {
                this(null, startTime, endTime, null, recurrence, conflictResolution);
        }

        public ClassSessionTemplateDTO(
                UUID uuid,
                LocalDateTime startTime,
                LocalDateTime endTime,
                ClassRecurrenceDTO recurrence,
                ConflictResolutionStrategy conflictResolution
        ) {
                this(uuid, startTime, endTime, null, recurrence, conflictResolution);
        }

        @JsonIgnore
        @AssertTrue(message = "Session template requires either end_time or duration_minutes")
        public boolean hasEndTimeOrDuration() {
                return endTime != null || durationMinutes != null;
        }

        @JsonIgnore
        @AssertTrue(message = "Session template end_time must be after start_time")
        public boolean hasValidEndTimeWhenDurationMissing() {
                if (durationMinutes != null || startTime == null || endTime == null) {
                        return true;
                }
                return startTime.isBefore(endTime);
        }

        public ClassSessionTemplateDTO withDurationApplied(Integer fallbackDurationMinutes) {
                Integer effectiveDurationMinutes = durationMinutes != null ? durationMinutes : fallbackDurationMinutes;
                if (startTime == null || effectiveDurationMinutes == null) {
                        return this;
                }
                if (effectiveDurationMinutes <= 0) {
                        throw new IllegalArgumentException("duration_minutes must be positive");
                }
                return new ClassSessionTemplateDTO(
                        uuid,
                        startTime,
                        startTime.plusMinutes(effectiveDurationMinutes.longValue()),
                        effectiveDurationMinutes,
                        recurrence,
                        conflictResolution
                );
        }
}
