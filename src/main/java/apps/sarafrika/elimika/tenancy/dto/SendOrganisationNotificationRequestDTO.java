package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request to broadcast a notification from an organisation to an audience of its members.
 */
@Schema(name = "SendOrganisationNotificationRequest",
        description = "Compose an organisation notification to an audience of members")
public record SendOrganisationNotificationRequestDTO(
        @Schema(description = "Audience: all, students, instructors, parents or staff.", example = "students")
        @JsonProperty("audience")
        @NotBlank(message = "Audience is required")
        String audience,

        @Schema(description = "Channel: in-app or email. Email deliveries also land in the in-app inbox.",
                example = "in-app")
        @JsonProperty("channel")
        @NotBlank(message = "Channel is required")
        String channel,

        @JsonProperty("title")
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @JsonProperty("message")
        @NotBlank(message = "Message is required")
        String message,

        @Schema(description = "Optional time to associate with the send.", nullable = true)
        @JsonProperty("scheduled_at")
        LocalDateTime scheduledAt
) {
}
