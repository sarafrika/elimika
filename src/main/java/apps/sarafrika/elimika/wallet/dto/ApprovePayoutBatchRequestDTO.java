package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApprovePayoutBatchRequest", description = "Approval of a payout batch")
public record ApprovePayoutBatchRequestDTO(
        @Schema(description = "Release the batch even though the payout float is unknown; recorded in the audit trail")
        @JsonProperty("override_unknown_float") boolean overrideUnknownFloat,
        @Schema(description = "Release the batch even though rows carry wallet flags (wallet_issue_count above zero); "
                + "recorded in the approval audit event")
        @JsonProperty("acknowledge_wallet_issues") boolean acknowledgeWalletIssues
) {
}
