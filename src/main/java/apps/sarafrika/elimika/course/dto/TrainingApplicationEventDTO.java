package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.TrainingApplicationEventType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** One entry in a training application's history. */
@Schema(name = "TrainingApplicationEvent", description = "A step in a training application's history, newest first")
public record TrainingApplicationEventDTO(

        @Schema(description = "**[READ-ONLY]** Identifier of the event.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "**[READ-ONLY]** Course or program application.", allowableValues = {"course", "program"},
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "application_type", access = JsonProperty.Access.READ_ONLY)
        TrainingApplicationType applicationType,

        @Schema(description = "**[READ-ONLY]** The application the event belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "application_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID applicationUuid,

        @Schema(description = "**[READ-ONLY]** What happened.",
                allowableValues = {"submitted", "edited", "opened_by_creator", "approved", "rejected", "revoked", "withdrawn",
                        "rates_update_submitted", "rates_update_approved", "rates_update_rejected", "rates_update_withdrawn"},
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "event_type", access = JsonProperty.Access.READ_ONLY)
        TrainingApplicationEventType eventType,

        @Schema(description = "**[READ-ONLY]** The user who took the step; null for system actions.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "actor_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID actorUuid,

        @Schema(description = "**[READ-ONLY]** The actor's name as it was when the step was taken.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "actor_name", access = JsonProperty.Access.READ_ONLY)
        String actorName,

        @Schema(description = "**[READ-ONLY]** Notes captured with the step (application, review or rate update notes).",
                nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "note", access = JsonProperty.Access.READ_ONLY)
        String note,

        @Schema(description = "**[READ-ONLY]** When it happened (UTC).", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
