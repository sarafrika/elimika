package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationEventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** One entry in a marketplace job application's history. */
@Schema(name = "ClassMarketplaceJobApplicationEvent",
        description = "A step in a marketplace job application's history, newest first")
public record ClassMarketplaceJobApplicationEventDTO(

        @Schema(description = "**[READ-ONLY]** Identifier of the event.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "**[READ-ONLY]** The application the event belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "application_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID applicationUuid,

        @Schema(description = "**[READ-ONLY]** The job the application was made to.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "job_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID jobUuid,

        @Schema(description = "**[READ-ONLY]** What happened. interviewing is an interview invitation and assigned is the class being created.",
                allowableValues = {"applied", "reapplied", "shortlisted", "interviewing", "offered", "hired", "assigned",
                        "rejected", "not_selected", "withdrawn"},
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "event_type", access = JsonProperty.Access.READ_ONLY)
        ClassMarketplaceJobApplicationEventType eventType,

        @Schema(description = "**[READ-ONLY]** The user who took the step; null for system actions such as expiry.",
                nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "actor_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID actorUuid,

        @Schema(description = "**[READ-ONLY]** The actor's name as it was when the step was taken.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "actor_name", access = JsonProperty.Access.READ_ONLY)
        String actorName,

        @Schema(description = "**[READ-ONLY]** The application note, the organisation's review note or the closing reason.",
                nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "note", access = JsonProperty.Access.READ_ONLY)
        String note,

        @Schema(description = "**[READ-ONLY]** When the interview is (UTC); set on interviewing events only.",
                format = "date-time", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "interview_at", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime interviewAt,

        @Schema(description = "**[READ-ONLY]** When it happened (UTC).", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
