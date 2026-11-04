package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PayoutBatchDetail", description = "A payout batch with its withdrawals and the live payout float")
public record PayoutBatchDetailDTO(
        @JsonProperty("batch") PayoutBatchDTO batch,
        @Schema(description = "The batch's withdrawals, oldest first, with their review flags, wallet check and wallet flags")
        @JsonProperty("withdrawals") List<WithdrawalDTO> withdrawals,
        @Schema(description = "Read from M-Pesa now; null when unknown or unavailable")
        @JsonProperty("payout_float") PayoutFloatDTO payoutFloat
) {
}
