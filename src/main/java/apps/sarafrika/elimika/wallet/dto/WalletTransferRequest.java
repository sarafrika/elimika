package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(
        name = "WalletTransferRequest",
        description = "Payload for transferring funds between wallets"
)
public record WalletTransferRequest(

        @Schema(
                description = "**[REQUIRED]** Target user UUID that receives the transfer.",
                format = "uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Target user is required")
        @JsonProperty("target_user_uuid")
        UUID targetUserUuid,

        @Schema(
                description = "**[REQUIRED]** Amount to transfer.",
                example = "250.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(
                description = "**[OPTIONAL]** ISO currency code. Defaults to platform currency when omitted.",
                example = "KES"
        )
        @Size(max = 3, message = "Currency code must not exceed 3 characters")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(
                description = "**[OPTIONAL]** External reference for the transfer.",
                example = "TRANSFER-2025-0001"
        )
        @Size(max = 128, message = "Reference must not exceed 128 characters")
        @JsonProperty("reference")
        String reference,

        @Schema(
                description = "**[OPTIONAL]** Description for the transfer.",
                example = "Reward payout"
        )
        @JsonProperty("description")
        String description
) {
}
