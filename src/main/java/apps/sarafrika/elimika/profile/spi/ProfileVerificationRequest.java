package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** A platform admin's verdict on one profile item. */
@Schema(name = "ProfileVerificationRequest", description = "Marks a profile item VERIFIED or REJECTED")
public record ProfileVerificationRequest(
        @NotNull
        @Schema(allowableValues = {"VERIFIED", "REJECTED"})
        @JsonProperty("status") WalletVerificationStatus status,
        @JsonProperty("notes") String notes
) {
}
