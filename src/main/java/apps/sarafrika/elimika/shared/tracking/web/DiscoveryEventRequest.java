package apps.sarafrika.elimika.shared.tracking.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * A click or dismissal of a recommended item, reported by the client. The user is taken from the
 * authenticated principal, never from the body.
 */
@Schema(name = "DiscoveryEventRequest", description = "A click or dismissal of an item from a recommendation response")
public record DiscoveryEventRequest(
        @Schema(description = "The recommendation_id returned with the recommendations", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @JsonProperty("recommendation_id") UUID recommendationId,

        @Schema(description = "The UUID of the item acted on", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @JsonProperty("item_uuid") UUID itemUuid,

        @Schema(description = "The item's type as returned with the recommendation, e.g. course", example = "course",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @JsonProperty("item_type") String itemType,

        @Schema(description = "CLICK or DISMISS; impressions are recorded by the server", example = "CLICK",
                allowableValues = {"CLICK", "DISMISS"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @JsonProperty("event_type") String eventType,

        @Schema(description = "0-based position the item was shown at", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(0)
        @Max(10_000)
        @JsonProperty("position") Integer position
) {
}
