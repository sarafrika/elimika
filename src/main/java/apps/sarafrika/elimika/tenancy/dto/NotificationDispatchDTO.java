package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An outgoing notification an organisation has broadcast to an audience.
 */
@Schema(name = "NotificationDispatch",
        description = "A notification an organisation has sent to an audience of its members")
public record NotificationDispatchDTO(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "The user who sent it.", nullable = true)
        @JsonProperty("sender_user_uuid")
        UUID senderUserUuid,

        @Schema(description = "Audience the message went to: all, students, instructors, parents or staff.")
        @JsonProperty("audience")
        String audience,

        @Schema(description = "Channel: in-app or email.")
        @JsonProperty("channel")
        String channel,

        @JsonProperty("title")
        String title,

        @JsonProperty("body")
        String body,

        @Schema(description = "How many recipients the broadcast reached.")
        @JsonProperty("recipient_count")
        Integer recipientCount,

        @Schema(nullable = true)
        @JsonProperty("scheduled_at")
        LocalDateTime scheduledAt,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
