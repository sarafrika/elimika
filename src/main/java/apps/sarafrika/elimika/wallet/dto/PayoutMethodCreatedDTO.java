package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PayoutMethodCreated", description = "A newly added number and the code sent to it")
public record PayoutMethodCreatedDTO(
        @JsonProperty("payout_method") PayoutMethodDTO payoutMethod,
        @JsonProperty("verification") VerificationCodeDTO verification
) {
}
