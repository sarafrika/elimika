package apps.sarafrika.elimika.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Custom Availability Slot Data Transfer Object
 * <p>
 * Specialized DTO for custom availability patterns using cron-like expressions
 * or other complex scheduling patterns that don't fit into daily, weekly, or monthly patterns.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Schema(
        name = "CustomAvailabilitySlot",
        description = "Custom availability slot with complex scheduling patterns",
        example = """
        {
            "instructor_uuid": "inst1234-5678-90ab-cdef-123456789abc",
            "custom_pattern": "0 0 9 ? * MON-FRI",
            "start_time": "09:00:00",
            "end_time": "17:00:00",
            "is_available": true,
            "effective_start_date": "2024-09-01",
            "effective_end_date": "2024-12-31"
        }
        """
)
public record CustomAvailabilitySlotDTO(

        @Schema(
                description = "**[REQUIRED]** Reference to the instructor UUID for this availability slot.",
                example = "inst1234-5678-90ab-cdef-123456789abc",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Instructor UUID is required")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[REQUIRED]** Custom pattern expression for complex availability rules. Supports cron-like expressions.",
                example = "0 0 9 ? * MON-FRI",
                maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Custom pattern is required")
        @Size(max = 255, message = "Custom pattern must not exceed 255 characters")
        @JsonProperty("custom_pattern")
        String customPattern,

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
                description = "**[OPTIONAL]** Date when this custom availability pattern becomes effective.",
                example = "2024-09-01",
                format = "date",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("effective_start_date")
        LocalDate effectiveStartDate,

        @Schema(
                description = "**[OPTIONAL]** Date when this custom availability pattern expires.",
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
     * Returns a human-readable description of this custom availability.
     *
     * @return Formatted description
     */
    @JsonProperty(value = "description", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Human-readable description of the custom availability pattern.",
            example = "Custom pattern (0 0 9 ? * MON-FRI) from 09:00 to 17:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public String getDescription() {
        String patternText = (customPattern != null && !customPattern.trim().isEmpty()) 
            ? "Custom pattern (" + customPattern + ")" 
            : "Custom pattern";
        
        String availabilityText = Boolean.FALSE.equals(isAvailable) ? " (blocked)" : "";
        
        return patternText + " from " + 
               (startTime != null ? startTime.toString() : "?") + " to " + 
               (endTime != null ? endTime.toString() : "?") + availabilityText;
    }

    /**
     * Returns a simplified pattern description for common cron patterns.
     *
     * @return Simplified description if pattern is recognized, otherwise returns the pattern itself
     */
    @JsonProperty(value = "pattern_description", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Simplified description for common cron patterns.",
            example = "Weekdays (Monday to Friday)",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public String getPatternDescription() {
        if (customPattern == null || customPattern.trim().isEmpty()) {
            return "No pattern specified";
        }
        
        String pattern = customPattern.trim().toLowerCase();
        
        // Common cron pattern translations
        if (pattern.contains("mon-fri") || pattern.contains("1-5")) {
            return "Weekdays (Monday to Friday)";
        } else if (pattern.contains("sat-sun") || pattern.contains("6-7")) {
            return "Weekends (Saturday to Sunday)";
        } else if (pattern.contains("* * *")) {
            return "Every day";
        } else if (pattern.matches(".*\\d+/\\d+.*")) {
            return "Recurring interval pattern";
        } else {
            return customPattern; // Return original pattern if not recognized
        }
    }
}