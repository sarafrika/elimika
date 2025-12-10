package apps.sarafrika.elimika.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * Request DTO representing a single blocked time slot window.
 */
@Schema(
        name = "BlockedTimeSlotRequest",
        description = "Represents a single blocked time slot window for an instructor."
)
public record BlockedTimeSlotRequestDTO(

        @Schema(
                description = "**[REQUIRED]** Start date and time to block (ISO format: YYYY-MM-DDTHH:mm:ss)",
                example = "2024-10-15T09:00:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Start time cannot be null")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(
                description = "**[REQUIRED]** End date and time to block (ISO format: YYYY-MM-DDTHH:mm:ss)",
                example = "2024-10-15T10:30:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "End time cannot be null")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(
                description = "**[OPTIONAL]** Hex color code used to visualize the blocked slot.",
                example = "#FF6B6B",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color code must be a valid hex value like #FF6B6B")
        @JsonProperty("color_code")
        String colorCode
) { }
