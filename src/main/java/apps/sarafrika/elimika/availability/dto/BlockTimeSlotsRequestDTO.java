package apps.sarafrika.elimika.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO used to block multiple time slots in a single request.
 */
@Schema(
        name = "BlockTimeSlotsRequest",
        description = "Payload used to block multiple time slots for an instructor."
)
public record BlockTimeSlotsRequestDTO(

        @Schema(
                description = "**[REQUIRED]** Collection of blocked slots to create.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Blocked time slots payload cannot be null")
        @NotEmpty(message = "At least one blocked time slot is required")
        @Valid
        @JsonProperty("slots")
        List<BlockedTimeSlotRequestDTO> slots
) { }
