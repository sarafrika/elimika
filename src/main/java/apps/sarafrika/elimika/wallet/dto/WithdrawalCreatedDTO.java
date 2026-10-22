package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WithdrawalCreated", description = "A new withdrawal and the SMS code sent to confirm it")
public record WithdrawalCreatedDTO(
        @JsonProperty("withdrawal") WithdrawalDTO withdrawal,
        @Schema(description = "Null when this was a repeat of an earlier request")
        @JsonProperty("verification") VerificationCodeDTO verification
) {
}
