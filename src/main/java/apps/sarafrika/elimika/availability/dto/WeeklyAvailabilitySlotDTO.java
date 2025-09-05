package apps.sarafrika.elimika.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Weekly Availability Slot Data Transfer Object
 * <p>
 * Specialized DTO for weekly recurring availability patterns.
 * Simplifies the creation and management of weekly availability slots
 * by focusing only on weekly-specific fields.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Schema(
        name = "WeeklyAvailabilitySlot",
        description = "Weekly recurring availability slot for instructor scheduling",
        example = """
        {
            "instructor_uuid": "inst1234-5678-90ab-cdef-123456789abc",
            "day_of_week": 1,
            "start_time": "09:00:00",
            "end_time": "17:00:00",
            "is_available": true,
            "recurrence_interval": 1,
            "effective_start_date": "2024-09-01",
            "effective_end_date": "2024-12-31"
        }
        """
)
public record WeeklyAvailabilitySlotDTO(

        @Schema(
                description = "**[REQUIRED]** Reference to the instructor UUID for this availability slot.",
                example = "inst1234-5678-90ab-cdef-123456789abc",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Instructor UUID is required")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[REQUIRED]** Day of the week (1=Monday, 2=Tuesday, ..., 7=Sunday).",
                example = "1",
                minimum = "1",
                maximum = "7",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Day of week is required")
        @Min(value = 1, message = "Day of week must be between 1 and 7")
        @Max(value = 7, message = "Day of week must be between 1 and 7")
        @JsonProperty("day_of_week")
        Integer dayOfWeek,

        @Schema(
                description = "**[REQUIRED]** Start time of the availability slot.",
                example = "09:00:00",
                format = "time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Start time is required")
        @JsonProperty("start_time")
        LocalTime startTime,

        @Schema(
                description = "**[REQUIRED]** End time of the availability slot.",
                example = "17:00:00",
                format = "time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "End time is required")
        @JsonProperty("end_time")
        LocalTime endTime,

        @Schema(
                description = "**[OPTIONAL]** Whether this slot represents availability (true) or blocked time (false).",
                example = "true",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("is_available")
        Boolean isAvailable,

        @Schema(
                description = "**[OPTIONAL]** Interval for weekly recurrence. For example, 2 means every 2 weeks.",
                example = "1",
                minimum = "1",
                maximum = "52",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Positive(message = "Recurrence interval must be positive")
        @Max(value = 52, message = "Recurrence interval must not exceed 52")
        @JsonProperty("recurrence_interval")
        Integer recurrenceInterval,

        @Schema(
                description = "**[OPTIONAL]** Date when this weekly availability pattern becomes effective.",
                example = "2024-09-01",
                format = "date",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("effective_start_date")
        LocalDate effectiveStartDate,

        @Schema(
                description = "**[OPTIONAL]** Date when this weekly availability pattern expires.",
                example = "2024-12-31",
                format = "date",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("effective_end_date")
        LocalDate effectiveEndDate

) {

    /**
     * Returns the duration of this availability slot in minutes.
     *
     * @return Duration in minutes
     */
    @JsonProperty(value = "duration_minutes", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Duration of the availability slot in minutes.",
            example = "480",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Returns the day name for the day of week.
     *
     * @return Day name (e.g., "Monday", "Tuesday")
     */
    @JsonProperty(value = "day_name", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Human-readable day name.",
            example = "Monday",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public String getDayName() {
        if (dayOfWeek == null) {
            return "Unknown day";
        }
        return switch (dayOfWeek) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "Unknown day";
        };
    }

    /**
     * Returns a human-readable description of this weekly availability.
     *
     * @return Formatted description
     */
    @JsonProperty(value = "description", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Human-readable description of the weekly availability.",
            example = "Every Monday from 09:00 to 17:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public String getDescription() {
        String intervalText = (recurrenceInterval != null && recurrenceInterval > 1) 
            ? "Every " + recurrenceInterval + " weeks on " 
            : "Every ";
        
        String availabilityText = Boolean.FALSE.equals(isAvailable) ? " (blocked)" : "";
        
        return intervalText + getDayName() + " from " + 
               (startTime != null ? startTime.toString() : "?") + " to " + 
               (endTime != null ? endTime.toString() : "?") + availabilityText;
    }
}