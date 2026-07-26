package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "Competition", description = "An organisation-scoped competition/event.")
public record CompetitionDTO(

        @Schema(description = "Unique identifier.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "Owning organisation UUID.")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Competition name.")
        @JsonProperty("name")
        String name,

        @Schema(description = "Category (e.g. STEM, Music, TVET).", nullable = true)
        @JsonProperty("category")
        String category,

        @Schema(description = "Scheduled date/time of the event.", nullable = true)
        @JsonProperty("event_date")
        LocalDateTime eventDate,

        @Schema(description = "Venue name.", nullable = true)
        @JsonProperty("venue_name")
        String venueName,

        @Schema(description = "Maximum number of teams.", nullable = true)
        @JsonProperty("capacity")
        Integer capacity,

        @Schema(description = "Lifecycle status (Upcoming, Registration Open, In Progress, Completed).")
        @JsonProperty("status")
        String status,

        @Schema(description = "Optional description.", nullable = true)
        @JsonProperty("description")
        String description,

        @Schema(description = "Number of teams registered.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "team_count", access = JsonProperty.Access.READ_ONLY)
        long teamCount,

        @Schema(description = "When the competition was created.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
