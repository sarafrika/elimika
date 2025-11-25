package apps.sarafrika.elimika.shared.spi.timetabling;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight instructor schedule projection shared outside the timetabling module.
 */
@Schema(name = "InstructorScheduleEntry", description = "Read-only schedule entry for an instructor")
public record InstructorScheduleEntry(

        @Schema(description = "Scheduled instance UUID", example = "1de2e945-2296-4c24-9b3a-33b7d77f0a8d")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Start time of the scheduled instance", example = "2024-09-15T09:00:00")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "End time of the scheduled instance", example = "2024-09-15T10:30:00")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Current schedule status", example = "SCHEDULED")
        @JsonProperty("status")
        InstructorScheduleStatus status,

        @Schema(description = "Title of the scheduled class instance", example = "Introduction to Java Programming")
        @JsonProperty("title")
        String title,

        @Schema(description = "Class definition UUID for the scheduled instance", example = "5c5f8c6a-1e3b-4d3a-9a93-2db6f9cbde5f")
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Location type for the scheduled instance", example = "ONLINE")
        @JsonProperty("location_type")
        String locationType,

        @Schema(description = "Cancellation reason when status is CANCELLED", example = "Instructor unavailable")
        @JsonProperty("cancellation_reason")
        String cancellationReason
) { }
