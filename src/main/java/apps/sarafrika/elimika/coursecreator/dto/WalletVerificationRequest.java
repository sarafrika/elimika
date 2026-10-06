package apps.sarafrika.elimika.coursecreator.dto;

import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "WalletVerificationRequest", description = "A platform admin's check of one skills wallet item")
public record WalletVerificationRequest(

        @NotNull
        @Schema(allowableValues = {"VERIFIED", "REJECTED"})
        @JsonProperty("status")
        WalletVerificationStatus status,

        @JsonProperty("notes")
        String notes
) {
}
