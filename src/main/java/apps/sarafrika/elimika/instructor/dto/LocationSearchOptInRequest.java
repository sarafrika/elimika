package apps.sarafrika.elimika.instructor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * The owner's near-me search opt-in.
 *
 * @param enabled true to appear in near-me results (at about 1 km precision), false to leave them
 */
@Schema(name = "LocationSearchOptInRequest",
        description = "Turns an instructor's near-me search opt-in on or off")
public record LocationSearchOptInRequest(
        @Schema(description = "**[REQUIRED]** true to appear in near-me search (location rounded to about 1 km, "
                + "only while verified and with coordinates set); false to leave it", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "enabled is required")
        @JsonProperty("enabled")
        Boolean enabled
) {
}
