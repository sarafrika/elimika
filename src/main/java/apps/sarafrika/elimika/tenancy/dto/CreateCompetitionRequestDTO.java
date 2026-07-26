package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Schema(name = "CreateCompetitionRequest", description = "Payload to create an organisation competition.")
public record CreateCompetitionRequestDTO(

        @Schema(description = "Competition name.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("name")
        @NotBlank(message = "Competition name is required")
        String name,

        @Schema(description = "Category.", nullable = true)
        @JsonProperty("category")
        String category,

        @Schema(description = "Scheduled date/time.", nullable = true)
        @JsonProperty("event_date")
        LocalDateTime eventDate,

        @Schema(description = "Venue name.", nullable = true)
        @JsonProperty("venue_name")
        String venueName,

        @Schema(description = "Maximum number of teams.", nullable = true)
        @JsonProperty("capacity")
        Integer capacity,

        @Schema(description = "Lifecycle status. Defaults to Upcoming when omitted.", nullable = true)
        @JsonProperty("status")
        String status,

        @Schema(description = "Optional description.", nullable = true)
        @JsonProperty("description")
        String description
) {
}
