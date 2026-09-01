package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.shared.validation.ValidTimeRange;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ClassSessionTemplate",
        description = "Time slot template used during class creation to generate scheduled instances with optional recurrence"
)
@ValidTimeRange(
        startField = "startTime",
        endField = "endTime",
        message = "Session template end_time must be after start_time"
)
public record ClassSessionTemplateDTO(

        @Schema(description = "**[READ-ONLY]** Unique identifier for this persisted class session template.")
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "**[REQUIRED]** Start time for the first occurrence (UTC)", example = "2025-01-15T14:00:00Z")
        @NotNull(message = "Session start time is required")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "**[REQUIRED]** End time for the first occurrence (UTC). Together with start_time this fixes the session length.", example = "2025-01-15T15:30:00Z")
        @NotNull(message = "Session end time is required")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Inline recurrence rule for this session template", nullable = true)
        @JsonProperty("recurrence")
        ClassRecurrenceDTO recurrence,

        @Schema(description = "**[OPTIONAL]** IANA timezone identifier used when displaying generated scheduled sessions.", example = "Africa/Nairobi")
        @Size(max = 64, message = "Timezone must not exceed 64 characters")
        @JsonProperty("timezone")
        String timezone,

        @Schema(description = "Conflict handling strategy: FAIL (default), SKIP, ROLLOVER", example = "FAIL")
        @JsonProperty("conflict_resolution")
        ConflictResolutionStrategy conflictResolution
) {
        public ClassSessionTemplateDTO(
                UUID uuid,
                LocalDateTime startTime,
                LocalDateTime endTime,
                ClassRecurrenceDTO recurrence,
                ConflictResolutionStrategy conflictResolution
        ) {
                this(uuid, startTime, endTime, recurrence, null, conflictResolution);
        }

        public ClassSessionTemplateDTO(
                LocalDateTime startTime,
                LocalDateTime endTime,
                ClassRecurrenceDTO recurrence,
                String timezone,
                ConflictResolutionStrategy conflictResolution
        ) {
                this(null, startTime, endTime, recurrence, timezone, conflictResolution);
        }

        public ClassSessionTemplateDTO(
                LocalDateTime startTime,
                LocalDateTime endTime,
                ClassRecurrenceDTO recurrence,
                ConflictResolutionStrategy conflictResolution
        ) {
                this(null, startTime, endTime, recurrence, null, conflictResolution);
        }

        /**
         * Reports how long the session runs, in minutes.
         * <p>
         * Derived rather than stored, and read-only on the wire, because the schedule the user set is
         * the start and the end. A duration accepted as input is a second, independent source of truth
         * for the same fact, and the two disagree the moment either is edited.
         *
         * @return the session length in minutes, or 0 when the window is not yet complete
         */
        @JsonProperty(value = "duration_minutes", access = JsonProperty.Access.READ_ONLY)
        @Schema(
                description = "**[READ-ONLY]** Computed session length in minutes, derived from start_time and end_time.",
                example = "90",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        public long getDurationMinutes() {
                if (startTime == null || endTime == null) {
                        return 0;
                }
                return Duration.between(startTime, endTime).toMinutes();
        }
}
