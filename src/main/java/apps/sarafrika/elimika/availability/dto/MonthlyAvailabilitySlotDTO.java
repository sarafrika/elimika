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
 * Monthly Availability Slot Data Transfer Object
 * <p>
 * Specialized DTO for monthly recurring availability patterns.
 * Simplifies the creation and management of monthly availability slots
 * by focusing only on monthly-specific fields.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Schema(
        name = "MonthlyAvailabilitySlot",
        description = "Monthly recurring availability slot for instructor scheduling",
        example = """
        {
            "instructor_uuid": "inst1234-5678-90ab-cdef-123456789abc",
            "day_of_month": 15,
            "start_time": "09:00:00",
            "end_time": "17:00:00",
            "is_available": true,
            "recurrence_interval": 1,
            "effective_start_date": "2024-09-01",
            "effective_end_date": "2024-12-31"
        }
        """
)
public record MonthlyAvailabilitySlotDTO(

        @Schema(
                description = "**[REQUIRED]** Reference to the instructor UUID for this availability slot.",
                example = "inst1234-5678-90ab-cdef-123456789abc",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Instructor UUID is required")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[REQUIRED]** Day of the month (1-31) when the instructor is available.",
                example = "15",
                minimum = "1",
                maximum = "31",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Day of month is required")
        @Min(value = 1, message = "Day of month must be between 1 and 31")
        @Max(value = 31, message = "Day of month must be between 1 and 31")
        @JsonProperty("day_of_month")
        Integer dayOfMonth,

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
                description = "**[OPTIONAL]** Interval for monthly recurrence. For example, 2 means every 2 months.",
                example = "1",
                minimum = "1",
                maximum = "12",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Positive(message = "Recurrence interval must be positive")
        @Max(value = 12, message = "Recurrence interval must not exceed 12")
        @JsonProperty("recurrence_interval")
        Integer recurrenceInterval,

        @Schema(
                description = "**[OPTIONAL]** Date when this monthly availability pattern becomes effective.",
                example = "2024-09-01",
                format = "date",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("effective_start_date")
        LocalDate effectiveStartDate,

        @Schema(
                description = "**[OPTIONAL]** Date when this monthly availability pattern expires.",
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
     * Returns a human-readable description of this monthly availability.
     *
     * @return Formatted description
     */
    @JsonProperty(value = "description", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Human-readable description of the monthly availability.",
            example = "Every month on the 15th from 09:00 to 17:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public String getDescription() {
        String intervalText = (recurrenceInterval != null && recurrenceInterval > 1) 
            ? "Every " + recurrenceInterval + " months on " 
            : "Every month on ";
        
        String dayText = "the " + getDayWithSuffix(dayOfMonth);
        String availabilityText = Boolean.FALSE.equals(isAvailable) ? " (blocked)" : "";
        
        return intervalText + dayText + " from " + 
               (startTime != null ? startTime.toString() : "?") + " to " + 
               (endTime != null ? endTime.toString() : "?") + availabilityText;
    }

    /**
     * Helper method to add ordinal suffix to day number.
     *
     * @param day Day number
     * @return Day with suffix (e.g., "1st", "2nd", "3rd", "15th")
     */
    private String getDayWithSuffix(Integer day) {
        if (day == null) {
            return "?";
        }
        
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        
        return switch (day % 10) {
            case 1 -> day + "st";
            case 2 -> day + "nd";  
            case 3 -> day + "rd";
            default -> day + "th";
        };
    }
}