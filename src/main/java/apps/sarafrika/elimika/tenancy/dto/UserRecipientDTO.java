package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * The answer to an exact user-number lookup, e.g. when choosing a wallet transfer recipient.
 * Carries just enough to confirm the right person: never email, phone or the full name.
 */
@Schema(name = "UserRecipient", description = "A user resolved from their exact user number, with a masked display name.")
public record UserRecipientDTO(
        @Schema(description = "The user's UUID, to use as the transfer recipient.",
                example = "550e8400-e29b-41d4-a716-446655440001")
        @JsonProperty("user_uuid") UUID userUuid,

        @Schema(description = "First name plus the initial of the last name.", example = "Wilfred N.")
        @JsonProperty("display_name") String displayName
) {
}
