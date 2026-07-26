package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "CompetitionTeam", description = "A team registered for a competition.")
public record CompetitionTeamDTO(

        @Schema(description = "Unique identifier.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "Competition UUID.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "competition_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID competitionUuid,

        @Schema(description = "Team name.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("team_name")
        @NotBlank(message = "Team name is required")
        String teamName,

        @Schema(description = "When the team registered.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
